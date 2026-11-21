package com.kingsejong.global.error;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 공통 예외. GlobalExceptionHandler 가 오류 응답으로 바꾼다.
 *
 * 사용 예) throw new BusinessException(ErrorCode.NOT_FOUND, "콘텐츠를 찾을 수 없어요");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
