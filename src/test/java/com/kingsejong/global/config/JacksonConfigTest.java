package com.kingsejong.global.config;

import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Boot의 ObjectMapper에 문서 크기 상한(4MB)이 실제로 걸렸는지. Content-Length 없는 큰 본문을 막는 쪽. */
class JacksonConfigTest extends IntegrationTest {

    @Test
    void 상한을_넘는_JSON은_읽지_않는다() {
        // 길이 검사는 읽기 버퍼 단위라 상한을 넉넉히 넘겨서 확인한다
        String huge = "{\"pad\":\"" + "x".repeat((int) JacksonConfig.MAX_DOCUMENT_BYTES * 2) + "\"}";
        assertThatThrownBy(() -> objectMapper.readTree(huge)).isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void 요청_상한_2MB_근처는_읽는다() {
        String nearLimit = "{\"pad\":\"" + "x".repeat((int) (RequestSizeLimitFilter.MAX_BODY_BYTES - 100)) + "\"}";
        assertThatCode(() -> objectMapper.readTree(nearLimit)).doesNotThrowAnyException();
    }
}
