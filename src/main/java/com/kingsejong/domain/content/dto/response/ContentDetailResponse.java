package com.kingsejong.domain.content.dto.response;

import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.domain.content.entity.ContentKind;
import com.kingsejong.domain.content.entity.ContentLine;
import com.kingsejong.domain.content.entity.ContentTask;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;

import java.util.List;

/**
 * 콘텐츠 상세 (API 명세 3.7). 쓰기 화면 6가지는 clip이 있는지와 task.writeMode로 정한다.
 */
public record ContentDetailResponse(
        Long id,
        Mode mode,
        ContentKind kind,
        String title,
        String source,
        Clip clip,
        List<Line> lines,
        String situation,
        Task task,
        Long nextContentId
) {

    /** 영상 재생 구간. 영상 없는 콘텐츠(기본 연습)는 clip 자체가 null. */
    public record Clip(String youtubeVideoId, Double startSec, Double endSec, boolean embeddable) {
    }

    public record Line(String text, Double startSec, Double endSec, String translation) {

        static Line from(ContentLine line) {
            return new Line(line.getText(), line.getStartSec(), line.getEndSec(), line.getTranslation());
        }
    }

    /** 읽어 줄 글. 프론트가 브라우저 음성 합성(ko-KR)으로 읽는다. */
    public record Speech(String text, String lang) {
    }

    /** 이 난이도에서 쓸 것. dictation(고수)이면 프론트는 target을 화면에 보여 주지 않는다. */
    public record Task(
            Stage stage,
            String writeMode,
            String target,
            String meaning,
            int cellCount,
            List<Integer> wordGroups,
            Speech speech
    ) {

        static Task of(Content content, ContentTask task) {
            // 클립 콘텐츠는 영상 구간을 다시 재생하므로 speech가 없다
            Speech speech = content.hasClip() ? null : new Speech(task.getTarget(), "ko-KR");
            return new Task(
                    task.getStage(),
                    task.getStage().getWriteMode(),
                    task.getTarget(),
                    task.getStage() == Stage.BEGINNER ? task.getMeaning() : null,
                    task.cellCount(),
                    task.wordGroups(),
                    speech
            );
        }
    }

    public static ContentDetailResponse of(Content content, ContentTask task, Long nextContentId) {
        Clip clip = content.hasClip()
                ? new Clip(content.getYoutubeVideoId(), content.getStartSec(), content.getEndSec(), content.isEmbeddable())
                : null;
        return new ContentDetailResponse(
                content.getId(),
                content.getMode(),
                content.getKind(),
                task.getTitle() != null ? task.getTitle() : content.getTitle(),
                content.getSource(),
                clip,
                content.getLines().stream().map(Line::from).toList(),
                content.getSituation(),
                Task.of(content, task),
                nextContentId
        );
    }
}
