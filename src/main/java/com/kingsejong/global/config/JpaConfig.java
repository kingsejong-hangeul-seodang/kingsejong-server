package com.kingsejong.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * JPA 설정.
 *
 * - @EnableJpaAuditing: BaseTimeEntity 의 created_at / updated_at 자동 기록
 * - 감사 시각 공급자를 OffsetDateTime 으로 지정한다. 기본 공급자는 LocalDateTime 을 주는데
 *   BaseTimeEntity 필드가 OffsetDateTime 이라 변환에 실패해 저장이 터진다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
