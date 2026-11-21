package com.kingsejong.global.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 문서 하나의 최대 크기. Content-Length 없이 오는 큰 본문도 파싱 중에 멈춘다.
 * 요청 상한(2MB)보다 넉넉하게 4MB: 다시 직렬화하면 숫자가 30 → 30.0처럼 커져서, 저장한 값을 다시 읽을 때 걸리지 않게.
 */
@Configuration
public class JacksonConfig {

    public static final long MAX_DOCUMENT_BYTES = 4L * 1024 * 1024;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer streamReadLimit() {
        return builder -> builder.postConfigurer(mapper -> mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxDocumentLength(MAX_DOCUMENT_BYTES).build()));
    }
}
