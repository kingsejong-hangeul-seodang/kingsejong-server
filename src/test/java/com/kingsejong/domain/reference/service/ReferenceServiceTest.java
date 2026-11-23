package com.kingsejong.domain.reference.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsejong.infra.engine.EngineClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** "사전에 없음" 10분 만료. */
class ReferenceServiceTest {

    private final EngineClient engine = mock(EngineClient.class);
    private final ReferenceService service = new ReferenceService(engine);
    private Instant now = Instant.parse("2026-11-24T00:00:00Z");

    @BeforeEach
    void setUp() {
        service.clock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now;
            }
        };
    }

    @Test
    void 없음은_10분_동안만_기억하고_그_뒤_찾으면_계속_기억한다() throws Exception {
        when(engine.reference("뷁"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new ObjectMapper().readTree("[{\"order\":1,\"points\":[[0,0]]}]")));

        assertThat(service.getStrokes("뷁").complete()).isFalse();
        now = now.plus(ReferenceService.MISSING_TTL).minusSeconds(1);
        assertThat(service.getStrokes("뷁").response().items()).isEmpty();
        verify(engine, times(1)).reference("뷁");

        now = now.plusSeconds(2); // 10분 지남 → 다시 물음, 이번엔 있음
        assertThat(service.getStrokes("뷁").complete()).isTrue();
        now = now.plusSeconds(3600);
        assertThat(service.getStrokes("뷁").response().items()).hasSize(1);
        verify(engine, times(2)).reference("뷁");
    }
}
