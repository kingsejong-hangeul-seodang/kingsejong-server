package com.kingsejong.global.health;

import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest extends IntegrationTest {

    @Test
    void 엔진이_꺼져_있어도_200이고_engine_status만_down() throws Exception {
        // test 프로필의 엔진 주소는 닫힌 포트 (application.yml)
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.engine.status").value("down"))
                .andExpect(jsonPath("$.engine.engineVersion").isEmpty());
    }
}
