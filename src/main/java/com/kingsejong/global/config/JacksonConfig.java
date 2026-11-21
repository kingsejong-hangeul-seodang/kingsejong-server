package com.kingsejong.global.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** JSON 문서 하나의 최대 크기. Content-Length 없이 오는 큰 본문도 파싱 중에 멈춘다 (RequestSizeLimitFilter와 같은 2MB). */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer streamReadLimit() {
        return builder -> builder.postConfigurer(mapper -> mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxDocumentLength(RequestSizeLimitFilter.MAX_BODY_BYTES).build()));
    }
}
