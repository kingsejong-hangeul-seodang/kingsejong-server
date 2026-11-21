package com.kingsejong.domain.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentControllerTest extends IntegrationTest {

    @Test
    void 초보_K팝_목록은_쓸_글과_칸_수를_준다() throws Exception {
        mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].title").value("별빛 아래"))
                .andExpect(jsonPath("$.items[0].kind").value("clip"))
                .andExpect(jsonPath("$.items[0].writeMode").value("trace"))
                .andExpect(jsonPath("$.items[0].target").value("바다"))
                .andExpect(jsonPath("$.items[0].cellCount").value(2))
                .andExpect(jsonPath("$.items[0].wordCount").value(1))
                .andExpect(jsonPath("$.items[0].durationSec").value(26))
                .andExpect(jsonPath("$.items[0].completed").value(false));
    }

    @Test
    void stage를_빼면_내_난이도로_준다() throws Exception {
        mockMvc.perform(get("/api/v1/contents").param("mode", "kpop")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DGU-0002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].writeMode").value("copy"))
                .andExpect(jsonPath("$.items[0].target").value("별빛 아래"))
                .andExpect(jsonPath("$.items[0].cellCount").value(4))
                .andExpect(jsonPath("$.items[0].wordCount").value(2));
    }

    @Test
    void 고수_목록은_답과_길이를_숨긴다() throws Exception {
        mockMvc.perform(get("/api/v1/contents").param("mode", "basic").param("stage", "advanced")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].title").value("식사 표현 1"))
                .andExpect(jsonPath("$.items[0].writeMode").value("dictation"))
                .andExpect(jsonPath("$.items[0].target").isEmpty())
                .andExpect(jsonPath("$.items[0].cellCount").isEmpty())
                .andExpect(jsonPath("$.items[0].wordCount").isEmpty());
    }

    @Test
    void 초보_기본_연습은_단어가_제목이고_자모_연습도_있다() throws Exception {
        mockMvc.perform(get("/api/v1/contents").param("mode", "basic").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.items[0].title").value("밥"))
                .andExpect(jsonPath("$.items[3].kind").value("jamo"))
                .andExpect(jsonPath("$.items[3].target").value("ㄷ"));
    }

    @Test
    void 클립_상세는_재생_구간과_자막을_주고_speech는_없다() throws Exception {
        String token = bearer("DGU-0001");
        long id = firstContentId(token, "kpop", "beginner");

        mockMvc.perform(get("/api/v1/contents/{id}", id).param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clip.youtubeVideoId").value("VIDEO_ID_01"))
                .andExpect(jsonPath("$.clip.startSec").value(62.0))
                .andExpect(jsonPath("$.clip.embeddable").value(true))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].translation").value("Under the starlight above the sea"))
                .andExpect(jsonPath("$.task.target").value("바다"))
                .andExpect(jsonPath("$.task.meaning").value("sea"))
                .andExpect(jsonPath("$.task.wordGroups[0]").value(2))
                .andExpect(jsonPath("$.task.speech").isEmpty())
                .andExpect(jsonPath("$.nextContentId").isNumber());
    }

    @Test
    void 기본_연습_상세는_clip이_없고_speech가_있다() throws Exception {
        String token = bearer("DGU-0001");
        long id = firstContentId(token, "basic", "advanced");

        mockMvc.perform(get("/api/v1/contents/{id}", id).param("stage", "advanced")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clip").isEmpty())
                .andExpect(jsonPath("$.task.writeMode").value("dictation"))
                .andExpect(jsonPath("$.task.target").value("잘 먹겠습니다"))
                .andExpect(jsonPath("$.task.cellCount").value(9))
                .andExpect(jsonPath("$.task.meaning").isEmpty())
                .andExpect(jsonPath("$.task.speech.text").value("잘 먹겠습니다"))
                .andExpect(jsonPath("$.task.speech.lang").value("ko-KR"));
    }

    @Test
    void 마지막_콘텐츠는_nextContentId가_null이다() throws Exception {
        String token = bearer("DGU-0001");
        String body = mockMvc.perform(get("/api/v1/contents").param("mode", "kdrama").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("items").get(0).get("id").asLong();

        mockMvc.perform(get("/api/v1/contents/{id}", id).param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextContentId").isEmpty());
    }

    @Test
    void 그_난이도로_쓸_글이_없으면_404() throws Exception {
        String token = bearer("DGU-0001");
        JsonNode items = objectMapper.readTree(mockMvc.perform(get("/api/v1/contents")
                        .param("mode", "basic").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString()).get("items");
        long jamoId = items.get(3).get("id").asLong();

        mockMvc.perform(get("/api/v1/contents/{id}", jamoId).param("stage", "intermediate")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 없는_콘텐츠면_404() throws Exception {
        mockMvc.perform(get("/api/v1/contents/{id}", 999_999).header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isNotFound());
    }

    @Test
    void 정해진_값이_아닌_쿼리는_422() throws Exception {
        mockMvc.perform(get("/api/v1/contents").param("mode", "anime")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("mode"));
    }

    @Test
    void mode가_없으면_422() throws Exception {
        mockMvc.perform(get("/api/v1/contents").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isUnprocessableEntity());
    }

    private long firstContentId(String token, String mode, String stage) throws Exception {
        String body = mockMvc.perform(get("/api/v1/contents").param("mode", mode).param("stage", stage)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("items").get(0).get("id").asLong();
    }
}
