package com.kingsejong.global.auth;

import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 관리자 주소(/api/v1/admin/**) 확인. 관리자 화면이 없어서 Swagger·curl로 부르며, "X-Admin-Key" 헤더가 설정값과 같아야 한다.
 * 키가 없으면 401, 틀리면 403. 키는 환경 변수 ADMIN_KEY (local·test에만 기본값).
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Admin-Key";
    static final int MIN_LENGTH = 16;

    private final byte[] adminKey;

    public AdminInterceptor(@Value("${app.admin.key}") String adminKey) {
        if (adminKey == null || adminKey.length() < MIN_LENGTH) {
            throw new IllegalStateException("app.admin.key(ADMIN_KEY)는 " + MIN_LENGTH + "자 이상이어야 해요");
        }
        this.adminKey = adminKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "관리자 키(X-Admin-Key)가 필요해요");
        }
        if (!MessageDigest.isEqual(adminKey, key.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 키가 맞지 않아요");
        }
        return true;
    }
}
