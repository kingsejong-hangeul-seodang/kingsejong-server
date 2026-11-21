package com.kingsejong.global.health;

import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthEngineUpTest extends IntegrationTest {

    @MockitoBean
    EngineClient engineClient;

    @Test
    void 엔진이_살아_있으면_버전을_주고_5초_동안은_다시_부르지_않는다() throws Exception {
        when(engineClient.health()).thenReturn(objectMapper.readTree("{\"status\":\"ok\",\"engineVersion\":\"0.1.0-stub\"}"));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.engine.status").value("ok"))
                    .andExpect(jsonPath("$.engine.engineVersion").value("0.1.0-stub"));
        }
        verify(engineClient, times(1)).health();
    }
}
