package com.kingsejong.domain.user;

import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeControllerTest extends IntegrationTest {

    @Test
    void 내_정보를_준다() throws Exception {
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("중수"))
                .andExpect(jsonPath("$.stage").value("intermediate"))
                .andExpect(jsonPath("$.lastMode").value("kpop"))
                .andExpect(jsonPath("$.level.no").value(1))
                .andExpect(jsonPath("$.level.name").value("서당 입문"))
                .andExpect(jsonPath("$.level.nextLevelExp").value(300))
                .andExpect(jsonPath("$.levels[4].name").value("킹세종"))
                .andExpect(jsonPath("$.streakDays").value(0))
                .andExpect(jsonPath("$.today.goal").value(3));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 온보딩_값을_보낸_필드만_바꾼다() throws Exception {
        String token = bearer("DGU-0004");

        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locale\":\"en\",\"consentAgreed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("en"))
                .andExpect(jsonPath("$.consentAgreed").value(true))
                .andExpect(jsonPath("$.stage").isEmpty())
                .andExpect(jsonPath("$.onboardingCompleted").value(false));

        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastMode\":\"basic\",\"onboardingCompleted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("en"))
                .andExpect(jsonPath("$.lastMode").value("basic"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }

    @Test
    void 처음_배워요를_고르면_난이도가_초보로_같이_정해진다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"experience\":\"first\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experience").value("first"))
                .andExpect(jsonPath("$.stage").value("beginner"));
    }

    @Test
    void 난이도를_바꾼다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"advanced\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("advanced"));
    }

    @Test
    void 정해진_값이_아니면_422와_위치를_준다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"expert\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details[0].field").value("stage"));
    }

    @Test
    void onboardingCompleted에_false를_보내면_422() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onboardingCompleted\":false}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("onboardingCompleted"));
    }

    @Test
    void JSON이_깨지면_400_INVALID_JSON() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void 수정한_값이_DB에_저장된다() throws Exception {
        String token = bearer("DGU-0001");
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"advanced\",\"locale\":\"en\"}"))
                .andExpect(status().isOk());
        flushAndClear();

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.stage").value("advanced"))
                .andExpect(jsonPath("$.locale").value("en"));
    }

    @Test
    void 처음_배워요와_난이도를_같이_보내면_보낸_난이도가_이긴다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"experience\":\"first\",\"stage\":\"intermediate\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experience").value("first"))
                .andExpect(jsonPath("$.stage").value("intermediate"));
    }

    @Test
    void 빈_객체를_보내면_아무것도_안_바뀐다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0002"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("intermediate"))
                .andExpect(jsonPath("$.locale").value("ko"));
    }

    @Test
    void 정해진_값이_틀리면_쓸_수_있는_값을_알려_준다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"expert\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].message").value(containsString("beginner, intermediate, advanced")));
    }

    @Test
    void 없는_필드를_보내면_422와_그_필드를_준다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stagee\":\"advanced\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("stagee"));
    }

    @Test
    void 타입이_틀리면_422와_그_필드를_준다() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"consentAgreed\":\"yes\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("consentAgreed"));
    }

    @Test
    void 객체가_아닌_JSON이면_422() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").isEmpty());
    }

    @Test
    void 본문이_없으면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void Content_Type이_JSON이_아니면_500이_아니라_400() throws Exception {
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.TEXT_PLAIN).content("{\"stage\":\"advanced\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void PATCH도_토큰이_없으면_401() throws Exception {
        mockMvc.perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
