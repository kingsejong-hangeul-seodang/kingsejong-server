package com.kingsejong.domain.content.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.domain.content.entity.ContentKind;
import com.kingsejong.domain.content.entity.ContentLine;
import com.kingsejong.domain.content.entity.ContentTask;
import com.kingsejong.domain.content.repository.ContentRepository;
import com.kingsejong.domain.taste.service.TasteService;
import org.springframework.core.annotation.Order;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import com.kingsejong.global.seed.SeedReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 콘텐츠 시드 (seed/contents.json). 배포에도 필요해서 모든 프로필에서 돈다.
 * 서버가 켜질 때마다 key로 찾아서 있으면 덮어쓰고 없으면 만든다 (upsert). JSON만 고치고 다시 켜면 반영된다.
 * JSON에서 지운 콘텐츠는 DB에서 지우지 않는다 (쓰기 기록이 가리키고 있을 수 있음). 관리자 API는 1차 범위 밖.
 * 정답 문장이 교정 엔진 규칙에 안 맞으면 서버가 켜지지 않는다 (시드 실수를 바로 알기 위해).
 */
@Slf4j
@Order(2) // 취향 시드(TasteSeeder) 다음
@Component
@RequiredArgsConstructor
public class ContentSeeder implements ApplicationRunner {

    /** 교정 엔진 app/schemas.py GradeRequest의 정답 문장 규칙과 같다. */
    private static final Pattern TARGET_PATTERN = Pattern.compile("[가-힣ㄱ-ㅣ ?!.,]+");
    private static final Pattern JAMO_PATTERN = Pattern.compile("[ㄱ-ㅣ]");
    private static final int MAX_TARGET_LENGTH = 20;
    private static final int MAX_INTERMEDIATE_WORDS = 3;
    private static final double MAX_CLIP_SECONDS = 30;

    private final SeedReader seedReader;
    private final ContentRepository contentRepository;
    private final TasteService tasteService;

    record LineSeed(String text, Double startSec, Double endSec, String translation) {
    }

    record TaskSeed(Stage stage, String target, String meaning, String title) {
    }

    record ContentSeed(String key, Mode mode, ContentKind kind, String title, String source, String thumbnailUrl,
                       String youtubeVideoId, Double startSec, Double endSec, boolean embeddable,
                       String situation, List<LineSeed> lines, List<TaskSeed> tasks, String taste) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        List<ContentSeed> seeds = seedReader.read("seed/contents.json", new TypeReference<>() {
        });
        Map<Mode, Integer> order = new EnumMap<>(Mode.class);
        Set<String> keys = new HashSet<>();
        int created = 0;
        for (ContentSeed seed : seeds) {
            if (seed.key() == null || seed.key().isBlank() || !keys.add(seed.key())) {
                throw new IllegalStateException("시드 콘텐츠 '" + seed.title() + "': key가 없거나 겹쳐요");
            }
            checkClip(seed);
            seed.tasks().forEach(task -> checkTarget(seed.title(), task.stage(), task.target()));

            int sortOrder = order.merge(seed.mode(), 1, Integer::sum);
            Content content = contentRepository.findBySeedKey(seed.key()).orElse(null);
            if (content == null) {
                content = Content.builder().seedKey(seed.key()).mode(seed.mode()).kind(seed.kind())
                        .title(seed.title()).sortOrder(sortOrder).build();
                created++;
            }
            Long tasteId = seed.taste() == null ? null : tasteService.findIdBySeedKey(seed.taste())
                    .orElseThrow(() -> new IllegalStateException(
                            "시드 콘텐츠 '" + seed.title() + "': 없는 취향 key " + seed.taste()));
            content.update(seed.mode(), seed.kind(), seed.title(), seed.source(), seed.thumbnailUrl(),
                    seed.youtubeVideoId(), seed.startSec(), seed.endSec(), seed.embeddable(),
                    seed.situation(), sortOrder, tasteId);

            List<LineSeed> lines = seed.lines() != null ? seed.lines() : List.of();
            List<ContentLine> newLines = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                LineSeed line = lines.get(i);
                newLines.add(ContentLine.builder()
                        .text(line.text()).startSec(line.startSec()).endSec(line.endSec())
                        .translation(line.translation()).sortOrder(i + 1)
                        .build());
            }
            content.replaceLines(newLines);

            for (TaskSeed task : seed.tasks()) {
                content.putTask(task.stage(), task.target(), task.meaning(), task.title());
            }
            content.retainTasks(seed.tasks().stream().map(TaskSeed::stage).collect(Collectors.toSet()));
            contentRepository.save(content);
        }
        // 시드에서 빠진 콘텐츠는 지우지 않고 숨긴다 (쓰기 기록이 가리킬 수 있음)
        List<Content> removed = contentRepository.findBySeedKeyNotInAndActiveTrue(keys);
        removed.forEach(Content::deactivate);
        log.info("Seeded contents: {} total, {} new, {} hidden", seeds.size(), created, removed.size());
    }

    /** 클립 구간 검사. 실제 영상으로 바꿀 때 실수를 기동에서 잡는다. */
    static void checkClip(ContentSeed seed) {
        String where = "시드 콘텐츠 '" + seed.title() + "': ";
        boolean clip = seed.kind() == ContentKind.CLIP;
        if (!clip) {
            if (seed.youtubeVideoId() != null || seed.startSec() != null || seed.endSec() != null) {
                throw new IllegalStateException(where + "영상이 없는 콘텐츠는 youtubeVideoId·startSec·endSec를 비워요");
            }
            return;
        }
        if (seed.youtubeVideoId() == null || seed.youtubeVideoId().isBlank()
                || seed.startSec() == null || seed.endSec() == null) {
            throw new IllegalStateException(where + "클립은 youtubeVideoId·startSec·endSec가 필요해요");
        }
        if (seed.startSec() < 0 || seed.endSec() <= seed.startSec() || seed.endSec() - seed.startSec() > MAX_CLIP_SECONDS) {
            throw new IllegalStateException(where + "재생 구간은 0초 이상, 끝 > 시작, 30초 이하");
        }
        if (seed.lines() != null) {
            for (LineSeed line : seed.lines()) {
                if (line.startSec() == null || line.endSec() == null
                        || line.startSec() < seed.startSec() || line.endSec() > seed.endSec() || line.endSec() <= line.startSec()) {
                    throw new IllegalStateException(where + "자막 '" + line.text() + "'의 시간이 재생 구간 안에 있어야 해요");
                }
            }
        }
    }

    public static void checkTarget(String title, Stage stage, String target) {
        String where = "시드 콘텐츠 '" + title + "' (" + stage.getCode() + ") 정답 '" + target + "': ";
        if (target == null || target.isEmpty() || target.length() > MAX_TARGET_LENGTH
                || !TARGET_PATTERN.matcher(target).matches()
                || !target.equals(target.strip()) || target.contains("  ")) {
            throw new IllegalStateException(where + "한글·띄어쓰기·? ! . , 만, 1~20칸, 앞뒤·연속 공백 없이");
        }
        if (JAMO_PATTERN.matcher(target).find() && (stage != Stage.BEGINNER || target.length() != 1)) {
            throw new IllegalStateException(where + "자모는 초보에서 한 글자만");
        }
        if (stage == Stage.BEGINNER && target.contains(" ")) {
            throw new IllegalStateException(where + "초보는 띄어쓰기 없는 단어 하나");
        }
        if (stage == Stage.INTERMEDIATE && target.split(" ").length > MAX_INTERMEDIATE_WORDS) {
            throw new IllegalStateException(where + "중수는 어절 3개 이하");
        }
    }
}
