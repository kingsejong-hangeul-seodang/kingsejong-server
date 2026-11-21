package com.kingsejong.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 전역 예외 처리. 컨트롤러에서 try-catch 하지 말고 예외를 던진다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 예외 → ErrorCode 의 상태코드 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("BusinessException: {} - {}", code, e.getMessage());
        return respond(code, ErrorResponse.of(code, e.getMessage()));
    }

    /** @Valid 검증 실패 → 422, details 에 필드와 이유 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.Detail> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.Detail(error.getField(),
                        error.getDefaultMessage() != null ? error.getDefaultMessage() : "올바르지 않은 값이에요"))
                .toList();
        return respond(ErrorCode.INVALID_REQUEST, ErrorResponse.of(ErrorCode.INVALID_REQUEST, details));
    }

    /** 경로 변수·쿼리 타입 불일치 (예: id 자리에 문자열) → 422 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(e.getName(), "'" + e.getValue() + "'은(는) 올바른 형식이 아니에요"));
        return respond(ErrorCode.INVALID_REQUEST, ErrorResponse.of(ErrorCode.INVALID_REQUEST, details));
    }

    /** 필수 쿼리 파라미터 없음 → 422 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(e.getParameterName(), "필수 값이에요"));
        return respond(ErrorCode.INVALID_REQUEST, ErrorResponse.of(ErrorCode.INVALID_REQUEST, details));
    }

    /** 깨진 JSON 등 읽을 수 없는 본문 → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return respond(ErrorCode.INVALID_JSON, ErrorResponse.of(ErrorCode.INVALID_JSON, ErrorCode.INVALID_JSON.getMessage()));
    }

    /** 지원하지 않는 HTTP 메서드 → 405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED,
                ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage()));
    }

    /** 없는 주소 → 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND, ErrorResponse.of(ErrorCode.NOT_FOUND, "없는 주소예요"));
    }

    /** 그 밖의 모든 예외 → 500 (자세한 내용은 로그로만) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return respond(ErrorCode.INTERNAL_ERROR,
                ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, ErrorResponse body) {
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
