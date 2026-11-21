package com.kingsejong.global.error;

import lombok.Getter;

import java.util.List;

/**
 * 비즈니스 규칙 위반 시 던지는 공통 예외. GlobalExceptionHandler 가 오류 응답으로 바꾼다.
 *
 * 사용 예) throw new BusinessException(ErrorCode.NOT_FOUND, "콘텐츠를 찾을 수 없어요");
 *        throw new BusinessException(ErrorCode.INVALID_REQUEST, List.of(new ErrorResponse.Detail("cells", "...")));
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorResponse.Detail> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = List.of();
    }

    public BusinessException(ErrorCode errorCode, List<ErrorResponse.Detail> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}
