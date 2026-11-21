package com.kingsejong.global.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.domain.content.entity.ContentKind;
import com.kingsejong.domain.content.entity.ContentLine;
import com.kingsejong.domain.content.entity.ContentTask;
import com.kingsejong.domain.content.repository.ContentRepository;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 콘텐츠 시드 (seed/contents.json). contents가 비어 있을 때만 넣는다. 배포에도 필요해서 모든 프로필에서 돈다.
 * 콘텐츠를 고치려면 JSON을 고치고 DB를 초기화(docker-compose down -v)한 뒤 다시 켠다. (관리자 API는 1차 범위 밖)
 * 정답 문장이 교정 엔진 규칙에 안 맞으면 서버가 켜지지 않는다 (시드 실수를 바로 알기 위해).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSeeder implements ApplicationRunner {

    /** 교정 엔진 app/schemas.py GradeRequest의 정답 문장 규칙과 같다. */
    private static final Pattern TARGET_PATTERN = Pattern.compile("[가-힣ㄱ-ㅣ ?!.,]+");
    private static final Pattern JAMO_PATTERN = Pattern.compile("[ㄱ-ㅣ]");
    private static final int MAX_TARGET_LENGTH = 20;
    private static final int MAX_INTERMEDIATE_WORDS = 3;

    private final SeedReader seedReader;
    private final ContentRepository contentRepository;

    record LineSeed(String text, Double startSec, Double endSec, String translation) {
    }

    record TaskSeed(Stage stage, String target, String meaning, String title) {
    }

    record ContentSeed(Mode mode, ContentKind kind, String title, String source, String thumbnailUrl,
                       String youtubeVideoId, Double startSec, Double endSec, boolean embeddable,
                       String situation, List<LineSeed> lines, List<TaskSeed> tasks) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (contentRepository.count() > 0) {
            return;
        }
        List<ContentSeed> seeds = seedReader.read("seed/contents.json", new TypeReference<>() {
        });
        Map<Mode, Integer> order = new EnumMap<>(Mode.class);
        for (ContentSeed seed : seeds) {
            Content content = Content.builder()
                    .mode(seed.mode())
                    .kind(seed.kind())
                    .title(seed.title())
                    .source(seed.source())
                    .thumbnailUrl(seed.thumbnailUrl())
                    .youtubeVideoId(seed.youtubeVideoId())
                    .startSec(seed.startSec())
                    .endSec(seed.endSec())
                    .embeddable(seed.embeddable())
                    .situation(seed.situation())
                    .sortOrder(order.merge(seed.mode(), 1, Integer::sum))
                    .build();
            List<LineSeed> lines = seed.lines() != null ? seed.lines() : List.of();
            for (int i = 0; i < lines.size(); i++) {
                LineSeed line = lines.get(i);
                content.addLine(ContentLine.builder()
                        .text(line.text()).startSec(line.startSec()).endSec(line.endSec())
                        .translation(line.translation()).sortOrder(i + 1)
                        .build());
            }
            for (TaskSeed task : seed.tasks()) {
                checkTarget(seed.title(), task.stage(), task.target());
                content.addTask(ContentTask.builder()
                        .stage(task.stage()).target(task.target())
                        .meaning(task.meaning()).title(task.title())
                        .build());
            }
            contentRepository.save(content);
        }
        log.info("Seeded {} contents", seeds.size());
    }

    static void checkTarget(String title, Stage stage, String target) {
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
