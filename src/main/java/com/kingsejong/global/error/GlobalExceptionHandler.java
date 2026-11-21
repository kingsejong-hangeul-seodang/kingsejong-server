package com.kingsejong.global.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.kingsejong.global.common.CodeEnum;
import org.springframework.web.HttpMediaTypeNotSupportedException;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        ErrorResponse body = e.getDetails().isEmpty()
                ? ErrorResponse.of(code, e.getMessage())
                : ErrorResponse.of(code, e.getDetails());
        return respond(code, body);
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

    /**
     * 읽을 수 없는 본문.
     * - JSON 문법은 맞는데 값이 틀림 (정해진 값이 아닌 "stage": "expert", 숫자 자리에 문자 등) → 422, details에 위치
     * - JSON 문법 자체가 깨짐 → 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidDefinitionException definition) {
            // DTO 정의 실수 = 서버 버그. 클라이언트 탓 422로 보내지 않는다
            log.error("Request DTO definition error", definition);
            return respond(ErrorCode.INTERNAL_ERROR,
                    ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage()));
        }
        if (e.getCause() instanceof JsonMappingException mapping) {
            String field = mapping.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining(".")).replace(".[", "[");
            List<ErrorResponse.Detail> details = List.of(new ErrorResponse.Detail(
                    field.isEmpty() ? null : field, describe(mapping)));
            return respond(ErrorCode.INVALID_REQUEST, ErrorResponse.of(ErrorCode.INVALID_REQUEST, details));
        }
        log.warn("Malformed request body: {}", e.getMessage());
        return respond(ErrorCode.INVALID_JSON, ErrorResponse.of(ErrorCode.INVALID_JSON, ErrorCode.INVALID_JSON.getMessage()));
    }

    /** 값 오류 설명. 정해진 값(enum)이면 쓸 수 있는 값을 알려 준다. */
    private static String describe(JsonMappingException mapping) {
        if (mapping instanceof UnrecognizedPropertyException) {
            return "없는 필드예요";
        }
        Class<?> target = null;
        if (mapping instanceof MismatchedInputException mismatched) {
            target = mismatched.getTargetType();
        } else if (mapping instanceof ValueInstantiationException instantiation && instantiation.getType() != null) {
            target = instantiation.getType().getRawClass();
        }
        if (target != null && target.isEnum() && CodeEnum.class.isAssignableFrom(target)) {
            String allowed = Arrays.stream(target.getEnumConstants())
                    .map(constant -> ((CodeEnum) constant).getCode())
                    .collect(Collectors.joining(", "));
            return "다음 중 하나여야 해요: " + allowed;
        }
        return "올바른 값이 아니에요";
    }

    /** Content-Type이 application/json이 아님 → 400 (명세에 415가 없어서 "JSON이 아님"으로 본다) */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return respond(ErrorCode.INVALID_JSON,
                ErrorResponse.of(ErrorCode.INVALID_JSON, "Content-Type: application/json 으로 보내 주세요"));
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

    /**
     * 그 밖의 모든 예외 → 500 (자세한 내용은 로그로만).
     * 단, Spring이 상태 코드를 정해 둔 요청 오류(406, 필수 헤더 누락 등 4xx)는 서버 버그가 아니므로 400.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse springError
                && springError.getStatusCode().is4xxClientError()) {
            log.warn("Client request error: {}", e.getMessage());
            return respond(ErrorCode.INVALID_JSON, ErrorResponse.of(ErrorCode.INVALID_JSON, "요청 형식이 올바르지 않아요"));
        }
        log.error("Unhandled exception", e);
        return respond(ErrorCode.INTERNAL_ERROR,
                ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, ErrorResponse body) {
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
