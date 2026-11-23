package com.kingsejong.domain.reference.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 정답 획 데이터 (API 명세 3.11). strokes는 엔진의 ReferenceStroke 배열 그대로
 * ({ order, jamo, role, strokeNo, points: [[x, y]...] 0~1, description }).
 * 사전에 없는 글자는 items에서 빠진다.
 */
public record StrokeReferenceResponse(String font, List<Item> items) {

    public record Item(@JsonProperty("char") String ch, JsonNode strokes) {
    }
}
