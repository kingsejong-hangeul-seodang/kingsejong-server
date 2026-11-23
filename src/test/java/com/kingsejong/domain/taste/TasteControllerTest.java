package com.kingsejong.domain.taste;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TasteControllerTest extends IntegrationTest {

    @Test
    void 모드별_취향_카드를_순서대로_준다() throws Exception {
        String token = bearer("DGU-0001");
        mockMvc.perform(get("/api/v1/tastes").param("mode", "kpop").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.items[0].name").value("하늘빛"))
                .andExpect(jsonPath("$.items[0].mode").value("kpop"))
                .andExpect(jsonPath("$.items[0].imageUrl").isEmpty())
                .andExpect(jsonPath("$.items[0].selected").value(false));
        mockMvc.perform(get("/api/v1/tastes").param("mode", "kdrama").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].name").value("서당의 봄"));
    }

    @Test
    void 기본_연습이나_없는_모드는_422() throws Exception {
        String token = bearer("DGU-0001");
        mockMvc.perform(get("/api/v1/tastes").param("mode", "basic").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("mode"));
        mockMvc.perform(get("/api/v1/tastes").param("mode", "anime").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 저장하면_그_모드만_통째로_바뀌고_DB에_남는다() throws Exception {
        String token = bearer("DGU-0001");
        long haneul = tasteId(token, "kpop", 0), dalmuri = tasteId(token, "kpop", 1), drama = tasteId(token, "kdrama", 0);

        save(token, "kpop", "[" + haneul + "," + dalmuri + "]").andExpect(status().isNoContent());
        save(token, "kdrama", "[" + drama + "]").andExpect(status().isNoContent());
        save(token, "kpop", "[" + dalmuri + "]").andExpect(status().isNoContent());
        flushAndClear();

        mockMvc.perform(get("/api/v1/tastes").param("mode", "kpop").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].selected").value(false))
                .andExpect(jsonPath("$.items[1].selected").value(true));
        mockMvc.perform(get("/api/v1/tastes").param("mode", "kdrama").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].selected").value(true));

        save(token, "kpop", "[]").andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/tastes").param("mode", "kpop").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[1].selected").value(false));
    }

    @Test
    void 다른_모드의_취향이나_없는_id는_422() throws Exception {
        String token = bearer("DGU-0001");
        long drama = tasteId(token, "kdrama", 0);
        save(token, "kpop", "[" + drama + "]").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("tasteIds"));
        save(token, "kpop", "[999999]").andExpect(status().isUnprocessableEntity());
        save(token, "basic", "[]").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 다른_사람의_선택은_보이지_않는다() throws Exception {
        String other = bearer("DGU-0002");
        save(other, "kpop", "[" + tasteId(other, "kpop", 0) + "]").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tastes").param("mode", "kpop").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(jsonPath("$.items[0].selected").value(false));
    }

    @Test
    void 고른_가수의_콘텐츠가_목록_맨_앞에_오고_배지가_붙는다() throws Exception {
        String token = bearer("DGU-0001");
        mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].title").value("별빛 아래"))
                .andExpect(jsonPath("$.items[0].tasteMatched").value(false));

        save(token, "kpop", "[" + tasteId(token, "kpop", 1) + "]").andExpect(status().isNoContent()); // 달무리

        mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].title").value("보고 싶었어"))
                .andExpect(jsonPath("$.items[0].tasteMatched").value(true))
                .andExpect(jsonPath("$.items[1].tasteMatched").value(false));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/tastes").param("mode", "kpop")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/me/tastes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"kpop\",\"tasteIds\":[]}")).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions save(String token, String mode, String ids) throws Exception {
        return mockMvc.perform(put("/api/v1/me/tastes").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"" + mode + "\",\"tasteIds\":" + ids + "}"));
    }

    private long tasteId(String token, String mode, int index) throws Exception {
        JsonNode items = objectMapper.readTree(mockMvc.perform(get("/api/v1/tastes").param("mode", mode)
                .header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getContentAsString()).get("items");
        return items.get(index).get("id").asLong();
    }
}
