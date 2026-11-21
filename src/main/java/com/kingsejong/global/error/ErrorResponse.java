package com.kingsejong.global.error;

import java.util.List;

/**
 * 오류 응답. 성공이 아니면 항상 이 모양 (API 명세 "오류 응답").
 *
 * { "code": "INVALID_REQUEST", "message": "...", "details": [{ "field": "name", "message": "..." }] }
 */
public record ErrorResponse(
        String code,
        String message,
        List<Detail> details
) {

    public record Detail(String field, String message) {
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<Detail> details) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), details);
    }
}
