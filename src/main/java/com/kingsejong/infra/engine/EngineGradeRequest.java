package com.kingsejong.infra.engine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 교정 엔진 판정 요청 POST /grade (API 명세 6.2, 엔진 app/schemas.py GradeRequest).
 * 엔진은 모르는 필드와 null 필드를 422로 거절하므로 null은 빼고 보낸다.
 * canvas·cells는 프론트가 보낸 모양 그대로 넘긴다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EngineGradeRequest(
        String sessionId,
        String stage,
        String target,
        String locale,
        JsonNode canvas,
        JsonNode cells
) {
}
