package com.kingsejong.domain.content.youtube;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.domain.content.entity.ContentKind;
import com.kingsejong.domain.content.entity.ContentLine;
import com.kingsejong.domain.content.repository.ContentRepository;
import com.kingsejong.domain.content.seed.ContentSeeder;
import com.kingsejong.domain.content.youtube.dto.YoutubeImportRequest;
import com.kingsejong.domain.content.youtube.dto.YoutubeImportResponse;
import com.kingsejong.domain.content.youtube.dto.YoutubePreviewResponse;
import com.kingsejong.domain.taste.service.TasteService;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;
import com.kingsejong.infra.youtube.YoutubeClient;
import com.kingsejong.infra.youtube.YoutubeVideo;
import com.kingsejong.infra.youtube.YoutubeVideoId;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 유튜브 영상으로 학습 콘텐츠를 만든다 (관리자). 영상 정보·자막은 YoutubeClient,
 * 구간·쓸 글 추천은 SubtitleStudyPicker. 만든 콘텐츠는 일반 콘텐츠와 같아서 목록·상세·쓰기 제출에 그대로 쓰인다.
 *
 * - 같은 영상·같은 시작 초는 key("yt:영상ID:시작초")가 같아서 다시 만들면 덮어쓴다
 * - 시드 파일(contents.json)은 "yt:" key를 건드리지 않는다 (서버를 다시 켜도 남음, DB를 지우면 사라짐)
 */
@Service
@RequiredArgsConstructor
public class YoutubeContentService {

    private static final int MAX_LINE = 200;
    private static final Pattern QUOTED_TITLE = Pattern.compile("['‘’\"“”]([^'‘’\"“”]{1,40})['‘’\"“”]");

    private final YoutubeClient youtubeClient;
    private final ContentRepository contentRepository;
    private final TasteService tasteService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public YoutubePreviewResponse preview(String url) {
        YoutubeVideo video = youtubeClient.fetch(YoutubeVideoId.parse(url));
        List<YoutubeVideo.Line> lines = video.captions() != null ? video.captions().lines() : List.of();
        YoutubePreviewResponse.Suggestion suggestion = SubtitleStudyPicker.window(lines, null, null, video.lengthSec())
                .map(w -> new YoutubePreviewResponse.Suggestion(w.startSec(), w.endSec(), SubtitleStudyPicker.pickTargets(w.lines())))
                .orElse(null);
        return new YoutubePreviewResponse(video.videoId(), video.title(), video.channel(), video.thumbnailUrl(),
                video.embeddable(), video.lengthSec(), video.captions() != null ? video.captions().kind() : null,
                lines, suggestion);
    }

    public YoutubeImportResponse importContent(YoutubeImportRequest request) {
        if (request.mode() != Mode.KPOP && request.mode() != Mode.KDRAMA) {
            throw invalid("mode", "유튜브 콘텐츠는 kpop 또는 kdrama만 만들 수 있어요");
        }
        if ((request.startSec() == null) != (request.endSec() == null)) {
            throw invalid("startSec", "startSec·endSec는 둘 다 주거나 둘 다 빼요");
        }
        if (request.startSec() != null && (request.endSec() <= request.startSec()
                || request.endSec() - request.startSec() > SubtitleStudyPicker.MAX_CLIP_SEC)) {
            throw invalid("endSec", "재생 구간은 끝 > 시작, 30초 이하예요");
        }

        if (request.lines() != null && request.startSec() == null) {
            throw invalid("startSec", "lines를 직접 넣으면 startSec·endSec도 같이 줘요");
        }
        if (request.lines() != null) {
            for (int i = 0; i < request.lines().size(); i++) {
                YoutubeImportRequest.Line l = request.lines().get(i);
                if (l.endSec() < l.startSec() || l.startSec() < request.startSec() || l.endSec() > request.endSec()) {
                    throw invalid("lines[" + i + "]", "줄 시간은 시작 ≤ 끝이고 재생 구간 안이어야 해요");
                }
                if (i > 0 && l.startSec() < request.lines().get(i - 1).startSec()) {
                    throw invalid("lines[" + i + "]", "줄은 시작 시간 순서대로 넣어요");
                }
            }
        }

        // 1. 유튜브 (트랜잭션 밖: 최대 수십 초)
        YoutubeVideo video = youtubeClient.fetch(YoutubeVideoId.parse(request.url()));
        SubtitleStudyPicker.Window window;
        List<String> translations = new ArrayList<>(); // 직접 준 lines의 번역 (같은 순서)
        if (request.lines() != null) {
            List<YoutubeVideo.Line> given = new ArrayList<>();
            for (YoutubeImportRequest.Line l : request.lines()) {
                given.add(new YoutubeVideo.Line(l.startSec(), l.endSec(), l.text().trim()));
            }
            window = new SubtitleStudyPicker.Window(request.startSec(), request.endSec(), given);
            request.lines().forEach(l -> translations.add(l.translation() != null ? l.translation().trim() : null));
        } else {
            List<YoutubeVideo.Line> all = video.captions() != null ? video.captions().lines() : List.of();
            window = SubtitleStudyPicker.window(all, request.startSec(), request.endSec(), video.lengthSec())
                    .orElseThrow(() -> invalid("startSec", "한국어 자막에서 쓸 구간을 못 찾았어요. startSec·endSec와 tasks(또는 lines)를 직접 넣어 주세요"));
        }
        if (video.lengthSec() != null && window.endSec() > video.lengthSec() + 1) {
            throw invalid("endSec", "영상 길이(" + video.lengthSec() + "초)를 넘어요");
        }

        // 2. 쓸 글: 직접 준 것 우선, 없으면 추천
        Map<Stage, String> picked = SubtitleStudyPicker.pickTargets(window.lines());
        Map<Stage, YoutubeImportResponse.Task> tasks = new EnumMap<>(Stage.class);
        for (Stage stage : Stage.values()) {
            YoutubeImportRequest.Task given = given(request.tasks(), stage);
            String target = given != null ? given.target().trim() : picked.get(stage);
            if (target == null) {
                throw invalid("tasks." + stage.getCode(), stage.getCode() + " 쓸 글을 자막에서 못 골랐어요. tasks로 직접 넣어 주세요");
            }
            try {
                ContentSeeder.checkTarget("유튜브 " + video.videoId(), stage, target);
            } catch (IllegalStateException e) {
                throw invalid("tasks." + stage.getCode(), e.getMessage());
            }
            String meaning = stage == Stage.BEGINNER && given != null ? given.meaning() : null;
            tasks.put(stage, new YoutubeImportResponse.Task(target, meaning, given != null ? "request" : "subtitle"));
        }

        String title = request.title() != null ? request.title().trim() : shortTitle(video.title());
        String tasteName = TasteService.normalizeName(request.taste());
        String source = request.source() != null ? request.source().trim()
                : request.mode() == Mode.KPOP ? tasteName + " - " + title : title;
        String seedKey = "yt:" + video.videoId() + ":" + (int) window.startSec();
        List<YoutubeImportResponse.Line> lines = new ArrayList<>();
        for (int i = 0; i < window.lines().size(); i++) {
            YoutubeVideo.Line l = window.lines().get(i);
            lines.add(new YoutubeImportResponse.Line(fitLine(l.text()), l.startSec(), l.endSec(),
                    i < translations.size() ? translations.get(i) : null));
        }

        // 3. 저장 (dryRun이면 안 함). 관리자 만들기는 한 번에 하나씩 (같은 key·취향 동시 생성, sortOrder 겹침 방지)
        return transactionTemplate.execute(status -> {
            if (!request.dryRun()) {
                jdbcTemplate.execute("select pg_advisory_xact_lock(hashtext('youtube-import'))");
            }
            TasteService.TasteRef taste = tasteService.findOrCreate(request.mode(), tasteName, request.dryRun());
            Long contentId = null;
            if (!request.dryRun()) {
                Content content = contentRepository.findBySeedKey(seedKey).orElse(null);
                int sortOrder = content != null ? content.getSortOrder()
                        : 1000 + (int) contentRepository.countBySeedKeyStartingWith("yt:");
                if (content == null) {
                    content = Content.builder().seedKey(seedKey).mode(request.mode()).kind(ContentKind.CLIP)
                            .title(title).sortOrder(sortOrder).build();
                }
                content.update(request.mode(), ContentKind.CLIP, title, source, video.thumbnailUrl(), video.videoId(),
                        window.startSec(), window.endSec(), video.embeddable(), request.situation(), sortOrder, taste.id());
                List<ContentLine> contentLines = new ArrayList<>();
                for (int i = 0; i < lines.size(); i++) {
                    YoutubeImportResponse.Line line = lines.get(i);
                    contentLines.add(ContentLine.builder().text(line.text()).startSec(line.startSec())
                            .endSec(line.endSec()).translation(line.translation()).sortOrder(i + 1).build());
                }
                content.replaceLines(contentLines);
                for (Map.Entry<Stage, YoutubeImportResponse.Task> e : tasks.entrySet()) {
                    content.putTask(e.getKey(), e.getValue().target(), e.getValue().meaning(), null);
                }
                content.retainTasks(tasks.keySet());
                contentId = contentRepository.save(content).getId();
            }
            return new YoutubeImportResponse(contentId, seedKey, request.dryRun(), request.mode(), title, source,
                    video.videoId(), window.startSec(), window.endSec(), video.embeddable(),
                    video.captions() != null ? video.captions().kind() : null, lines, tasks,
                    new YoutubeImportResponse.Taste(taste.id(), taste.name(), taste.seedKey(), taste.created()),
                    seedJson(seedKey, request, title, source, video, window, lines, tasks, taste));
        });
    }

    /** 목록에서 내린다 (쓰기 기록이 가리킬 수 있어서 지우지 않음). 시드 콘텐츠도 내릴 수 있지만 서버를 다시 켜면 시드대로 돌아온다. */
    public void deactivate(Long contentId) {
        transactionTemplate.executeWithoutResult(status -> contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "콘텐츠를 찾을 수 없어요"))
                .deactivate());
    }

    /** "NewJeans (뉴진스) 'Ditto' Official MV" → "Ditto". 따옴표가 없으면 40자까지. */
    static String shortTitle(String title) {
        Matcher m = QUOTED_TITLE.matcher(title);
        if (m.find()) {
            return m.group(1).trim();
        }
        return title.length() > 40 ? title.substring(0, 40).trim() : title.trim();
    }

    /** 자막 한 줄은 DB 200칸까지 (자동 자막은 줄이 합쳐져 길 수 있음). 어절 단위로 자른다. */
    static String fitLine(String text) {
        if (text.length() <= MAX_LINE) {
            return text;
        }
        String fitted = SubtitleStudyPicker.fit(text, MAX_LINE);
        return fitted.isEmpty() ? text.substring(0, MAX_LINE) : fitted;
    }

    private static YoutubeImportRequest.Task given(YoutubeImportRequest.Tasks tasks, Stage stage) {
        if (tasks == null) {
            return null;
        }
        return switch (stage) {
            case BEGINNER -> tasks.beginner();
            case INTERMEDIATE -> tasks.intermediate();
            case ADVANCED -> tasks.advanced();
        };
    }

    private ObjectNode seedJson(String seedKey, YoutubeImportRequest request, String title, String source, YoutubeVideo video,
                                SubtitleStudyPicker.Window window, List<YoutubeImportResponse.Line> lines,
                                Map<Stage, YoutubeImportResponse.Task> tasks, TasteService.TasteRef taste) {
        ObjectNode seed = objectMapper.createObjectNode();
        seed.put("key", seedKey);
        seed.put("mode", request.mode().getCode());
        seed.put("kind", "clip");
        seed.put("title", title);
        seed.put("source", source);
        seed.put("thumbnailUrl", video.thumbnailUrl());
        seed.put("youtubeVideoId", video.videoId());
        seed.put("startSec", window.startSec());
        seed.put("endSec", window.endSec());
        seed.put("embeddable", video.embeddable());
        if (request.situation() != null) {
            seed.put("situation", request.situation());
        }
        ArrayNode lineArray = seed.putArray("lines");
        lines.forEach(l -> {
            ObjectNode node = lineArray.addObject().put("text", l.text()).put("startSec", l.startSec()).put("endSec", l.endSec());
            if (l.translation() != null) {
                node.put("translation", l.translation());
            }
        });
        ArrayNode taskArray = seed.putArray("tasks");
        Map<Stage, YoutubeImportResponse.Task> ordered = new LinkedHashMap<>(tasks);
        ordered.forEach((stage, t) -> {
            ObjectNode node = taskArray.addObject().put("stage", stage.getCode()).put("target", t.target());
            if (t.meaning() != null) {
                node.put("meaning", t.meaning());
            }
        });
        seed.put("taste", taste.seedKey());
        return seed;
    }

    private static BusinessException invalid(String field, String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, List.of(new ErrorResponse.Detail(field, message)));
    }
}
