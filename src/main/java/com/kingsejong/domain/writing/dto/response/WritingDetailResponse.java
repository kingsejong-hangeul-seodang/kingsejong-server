package com.kingsejong.domain.writing.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.global.common.Stage;

import java.time.OffsetDateTime;

/**
 * 쓰기 기록 하나 (API 명세 3.10). 제출 응답에서 reward를 빼고 필기 원본을 더한 모양.
 * 기록 수집에 동의하지 않은 기록은 canvas·cells가 null.
 */
public record WritingDetailResponse(
        Long writingId,
        Long contentId,
        Stage stage,
        String target,
        OffsetDateTime createdAt,
        JsonNode canvas,
        JsonNode cells,
        JsonNode grade
) {

    public static WritingDetailResponse from(Writing writing) {
        return new WritingDetailResponse(
                writing.getId(),
                writing.getContentId(),
                writing.getStage(),
                writing.getTarget(),
                writing.getCreatedAt(),
                writing.getCanvas(),
                writing.getCells(),
                writing.getGrade()
        );
    }
}
