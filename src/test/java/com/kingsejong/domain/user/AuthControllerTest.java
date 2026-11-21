package com.kingsejong.domain.user;

import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegrationTest {

    @Test
    void 맞는_코드로_로그인하면_토큰과_내_정보를_준다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DGU-0001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.me.loginCode").value("DGU-0001"))
                .andExpect(jsonPath("$.me.stage").value("beginner"))
                .andExpect(jsonPath("$.me.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.me.level.no").value(1))
                .andExpect(jsonPath("$.me.levels.length()").value(5));
    }

    @Test
    void 온보딩_전_계정은_onboardingCompleted가_false이고_stage가_null이다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DGU-0004\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.me.stage").isEmpty());
    }

    @Test
    void 틀린_코드면_401_INVALID_LOGIN_CODE() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NOPE-9999\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_LOGIN_CODE"));
    }

    @Test
    void 코드가_비면_422() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("code"));
    }

    @Test
    void 토큰이_없으면_401_UNAUTHORIZED() throws Exception {
        mockMvc.perform(get("/api/v1/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 토큰이_틀리면_401_UNAUTHORIZED() throws Exception {
        mockMvc.perform(get("/api/v1/anything").header(HttpHeaders.AUTHORIZATION, "Bearer broken.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 맞는_토큰이면_인증을_통과한다() throws Exception {
        // 아직 없는 주소라 404지만, 인증은 통과했다는 뜻
        mockMvc.perform(get("/api/v1/anything").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isNotFound());
    }

    @Test
    void CORS_사전_요청은_토큰_없이_통과한다() throws Exception {
        mockMvc.perform(options("/api/v1/anything")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk());
    }
}
