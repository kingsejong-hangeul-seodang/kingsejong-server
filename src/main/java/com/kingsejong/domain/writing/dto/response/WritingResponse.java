package com.kingsejong.domain.writing.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.global.common.Stage;

import java.time.OffsetDateTime;

/**
 * 쓰기 제출 응답 (API 명세 3.8). grade는 엔진 판정 응답을 바꾸지 않고 그대로 넣는다.
 */
public record WritingResponse(
        Long writingId,
        Long contentId,
        Stage stage,
        String target,
        OffsetDateTime createdAt,
        JsonNode grade,
        Long nextContentId
) {

    public static WritingResponse of(Writing writing, Long nextContentId) {
        return new WritingResponse(
                writing.getId(),
                writing.getContentId(),
                writing.getStage(),
                writing.getTarget(),
                writing.getCreatedAt(),
                writing.getGrade(),
                nextContentId
        );
    }
}
