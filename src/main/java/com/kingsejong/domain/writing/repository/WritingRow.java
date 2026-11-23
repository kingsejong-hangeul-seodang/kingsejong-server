package com.kingsejong.domain.writing.repository;

import com.kingsejong.global.common.Stage;

import java.time.OffsetDateTime;

/** 기록 목록용으로 필요한 칸만 (필기 원본·판정 jsonb는 읽지 않는다). */
public record WritingRow(Long id, Long contentId, Stage stage, String target, boolean correct, boolean perfect,
                         Integer strokeErrorCount, OffsetDateTime createdAt) {
}
