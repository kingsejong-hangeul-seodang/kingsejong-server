package com.kingsejong.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 요청 본문 2MB 상한 (API 명세 3.8). 본문을 다 읽기 전에 Content-Length로 막는다.
 * Content-Length가 없는 chunked 요청은 JacksonConfig의 maxDocumentLength가 막는다.
 * 명세 오류 표에 413이 없어서 422 INVALID_REQUEST로 보낸다.
 */
@Component
@RequiredArgsConstructor
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    public static final long MAX_BODY_BYTES = 2L * 1024 * 1024;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            ErrorResponse body = ErrorResponse.of(ErrorCode.INVALID_REQUEST,
                    List.of(new ErrorResponse.Detail(null, "요청 본문은 2MB까지예요")));
            response.setStatus(ErrorCode.INVALID_REQUEST.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }
        chain.doFilter(request, response);
    }
}
