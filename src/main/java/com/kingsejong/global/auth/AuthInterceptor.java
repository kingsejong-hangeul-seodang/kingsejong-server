package com.kingsejong.global.auth;

import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * "Authorization: Bearer <토큰>"을 확인하고 사용자 id를 요청에 담는다.
 * 컨트롤러에서는 @LoginUser Long userId 로 꺼낸다. 어느 주소에 붙는지는 WebConfig.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "loginUserId";
    private static final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        // 스킴은 대소문자를 가리지 않는다 ("bearer xxx"도 허용)
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = header.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = jwtProvider.parseUserId(token);
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
