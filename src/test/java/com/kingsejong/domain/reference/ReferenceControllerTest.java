package com.kingsejong.domain.reference;

import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 정답 획 (API 명세 3.11). 서버는 기억(캐시)이 컨텍스트 동안 남으므로 테스트마다 다른 글자를 쓴다. */
class ReferenceControllerTest extends IntegrationTest {

    @MockitoBean
    EngineClient engineClient;

    @Test
    void 글자마다_엔진에_한_번만_묻고_기억해_둔다() throws Exception {
        when(engineClient.reference(anyString())).thenAnswer(inv -> Optional.of(objectMapper.readTree(
                "[{\"order\":1,\"jamo\":\"ㅂ\",\"role\":\"cho\",\"strokeNo\":1,\"points\":[[0.1,0.2],[0.1,0.8]],\"description\":null}]")));
        String token = bearer("DGU-0001");

        mockMvc.perform(get("/api/v1/reference/strokes").param("chars", "바다바").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=86400, private"))
                .andExpect(jsonPath("$.font").value("DONGGUK-UNIVERSITY"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].char").value("바"))
                .andExpect(jsonPath("$.items[1].char").value("다"))
                .andExpect(jsonPath("$.items[0].strokes[0].order").value(1))
                .andExpect(jsonPath("$.items[0].strokes[0].points[0][1]").value(0.2));
        mockMvc.perform(get("/api/v1/reference/strokes").param("chars", "다바").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items.length()").value(2));

        verify(engineClient, times(1)).reference(eq("바"));
        verify(engineClient, times(1)).reference(eq("다"));
    }

    @Test
    void 사전에_없는_글자는_빠진다() throws Exception {
        when(engineClient.reference(eq("뷁"))).thenReturn(Optional.empty());
        when(engineClient.reference(eq("ㄷ"))).thenAnswer(inv -> Optional.of(objectMapper.readTree("[{\"order\":1,\"points\":[[0,0]]}]")));

        String token = bearer("DGU-0001");
        mockMvc.perform(get("/api/v1/reference/strokes").param("chars", "뷁ㄷ").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].char").value("ㄷ"));
        // "없음"도 기억해서 다시 묻지 않는다 (10분)
        mockMvc.perform(get("/api/v1/reference/strokes").param("chars", "뷁").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items.length()").value(0));
        verify(engineClient, times(1)).reference(eq("뷁"));
    }

    @Test
    void 엔진_오류는_기억하지_않아서_엔진이_돌아오면_바로_된다() throws Exception {
        String token = bearer("DGU-0001");
        when(engineClient.reference(eq("꿳"))).thenThrow(new BusinessException(ErrorCode.ENGINE_TIMEOUT))
                .thenAnswer(inv -> Optional.of(objectMapper.readTree("[{\"order\":1,\"points\":[[0,0]]}]")));
        mockMvc.perform(get("/api/v1/reference/strokes").param("chars", "꿳").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isGatewayTimeout());
        mockMvc.perform(get("/api/v1/reference/strokes").param("chars", "꿳").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].char").value("꿳"));
    }

    @Test
    void 엔진에_연결할_수_없고_기억에도_없으면_503() throws Exception {
        when(engineClient.reference(eq("쀍"))).thenThrow(new BusinessException(ErrorCode.ENGINE_UNAVAILABLE));

        mockMvc.perform(get("/api/v1/reference/strokes").param("chars", "쀍").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ENGINE_UNAVAILABLE"));
    }

    @Test
    void 한글이_아니거나_띄어쓰기가_있거나_30자를_넘으면_422() throws Exception {
        String token = bearer("DGU-0001");
        for (String chars : new String[]{"바 다", "abc", "가".repeat(31), "바다!"}) {
            mockMvc.perform(get("/api/v1/reference/strokes").param("chars", chars).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.details[0].field").value("chars"));
        }
        mockMvc.perform(get("/api/v1/reference/strokes").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity());
        verify(engineClient, never()).reference(anyString());
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/reference/strokes").param("chars", "가")).andExpect(status().isUnauthorized());
    }
}
