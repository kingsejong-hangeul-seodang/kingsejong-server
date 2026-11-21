package com.kingsejong.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 오류 코드. 노션 API 명세 "오류 응답" 표와 같게 맞춘다.
 * 새 코드가 필요하면 명세에 먼저 추가하고 여기에 넣는다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_JSON(HttpStatus.BAD_REQUEST, "본문이 JSON 형식이 아니에요"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요"),
    INVALID_LOGIN_CODE(HttpStatus.UNAUTHORIZED, "로그인 코드가 맞지 않아요"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근할 수 없어요"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없어요"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드예요"),
    INVALID_REQUEST(HttpStatus.UNPROCESSABLE_ENTITY, "요청 값이 규칙에 맞지 않아요"),
    ENGINE_ERROR(HttpStatus.BAD_GATEWAY, "교정 엔진이 오류를 돌려줬어요"),
    ENGINE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "교정 엔진에 연결할 수 없어요"),
    ENGINE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "교정 엔진이 제한 시간 안에 답하지 않았어요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했어요");

    private final HttpStatus status;
    private final String message;
}
