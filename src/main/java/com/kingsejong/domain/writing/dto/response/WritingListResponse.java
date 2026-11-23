package com.kingsejong.domain.writing.dto.response;

import com.kingsejong.domain.content.service.ContentSummary;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;

import java.time.OffsetDateTime;
import java.util.List;

/** 쓰기 기록 목록 (API 명세 3.9). nextCursor가 null이면 마지막 페이지. */
public record WritingListResponse(List<Item> items, String nextCursor) {

    public record Item(
            Long writingId,
            Long contentId,
            String title,
            String source,
            Mode mode,
            Stage stage,
            String target,
            boolean correct,
            boolean perfect,
            int strokeErrorCount,
            OffsetDateTime createdAt
    ) {

        public static Item of(Writing writing, ContentSummary content) {
            return new Item(
                    writing.getId(),
                    writing.getContentId(),
                    content != null ? content.title() : "",
                    content != null ? content.source() : null,
                    content != null ? content.mode() : null,
                    writing.getStage(),
                    writing.getTarget(),
                    writing.isCorrect(),
                    writing.isPerfect(),
                    strokeErrorCount(writing),
                    writing.getCreatedAt()
            );
        }

        /** 획순 오류 칸 수 = 엔진 판정에서 mark가 stroke인 칸 수 ("획순 1곳" 표시). */
        static int strokeErrorCount(Writing writing) {
            if (writing.getGrade() == null || !writing.getGrade().path("cells").isArray()) {
                return 0;
            }
            int count = 0;
            for (var cell : writing.getGrade().get("cells")) {
                if ("stroke".equals(cell.path("mark").asText())) {
                    count++;
                }
            }
            return count;
        }
    }
}
