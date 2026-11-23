package com.kingsejong.domain.writing.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.global.common.Stage;

import java.time.OffsetDateTime;

/** 자모 상세의 최근 기록용: 필기 원본(canvas·cells)은 읽지 않는다. */
public record RecentWritingRow(Long id, Long contentId, Stage stage, String target, OffsetDateTime createdAt, JsonNode grade) {
}
