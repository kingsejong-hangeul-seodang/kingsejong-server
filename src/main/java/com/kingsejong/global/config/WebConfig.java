package com.kingsejong.global.config;

import com.kingsejong.global.auth.AdminInterceptor;
import com.kingsejong.global.auth.AuthInterceptor;
import com.kingsejong.global.auth.LoginUserArgumentResolver;
import com.kingsejong.global.common.StringToCodeEnumConverterFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS — 프론트 주소만 허용한다 (API 명세). 주소는 app.cors.allowed-origins (쉼표로 여러 개).
 * 인증 — /api/v1/** 전체에 토큰 확인. 로그인과 헬스체크만 뺀다. 관리자 주소(/api/v1/admin/**)는 관리자 키로 따로 확인.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/login", "/api/v1/health", "/api/v1/admin/**");
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/v1/admin/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToCodeEnumConverterFactory());
    }
}
