package com.kingsejong.domain.writing.dto.response;

import com.kingsejong.domain.content.service.ContentSummary;
import com.kingsejong.domain.writing.repository.WritingRow;
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

        public static Item of(WritingRow row, ContentSummary content) {
            return new Item(
                    row.id(),
                    row.contentId(),
                    content != null ? content.title() : "",
                    content != null ? content.source() : null,
                    content != null ? content.mode() : null,
                    row.stage(),
                    row.target(),
                    row.correct(),
                    row.perfect(),
                    row.strokeErrorCount() != null ? row.strokeErrorCount() : 0, // 컬럼 생기기 전 기록은 0
                    row.createdAt()
            );
        }
    }
}
