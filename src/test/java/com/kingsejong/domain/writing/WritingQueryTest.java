package com.kingsejong.domain.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 쓰기 기록 목록·하나 (API 명세 3.9, 3.10). */
class WritingQueryTest extends IntegrationTest {

    @MockitoBean
    EngineClient engineClient;

    @org.springframework.beans.factory.annotation.Autowired
    com.kingsejong.domain.content.repository.ContentRepository contentRepository;

    private static final String GRADE_WITH_STROKE = "{\"engineVersion\":\"test\",\"correct\":true,\"perfect\":false,"
            + "\"cells\":[{\"index\":0,\"mark\":\"stroke\"},{\"index\":1,\"mark\":\"none\"}],\"sejong\":{\"text\":\"t\"}}";

    @Test
    void 최신순으로_커서_페이지를_준다() throws Exception {
        String token = bearer("DGU-0001");
        long first = submit(token, "kpop", "beginner", 0, GRADE_WITH_STROKE);
        long second = submit(token, "kpop", "beginner", 1, GRADE_WITH_STROKE);
        long third = submit(token, "basic", "beginner", 0, GRADE_WITH_STROKE);

        String body = mockMvc.perform(get("/api/v1/writings").param("size", "2").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].writingId").value(third))
                .andExpect(jsonPath("$.items[0].title").value("밥"))
                .andExpect(jsonPath("$.items[0].mode").value("basic"))
                .andExpect(jsonPath("$.items[0].source").isEmpty())
                .andExpect(jsonPath("$.items[1].writingId").value(second))
                .andExpect(jsonPath("$.items[1].title").value("보고 싶었어"))
                .andExpect(jsonPath("$.items[1].source").value("달무리 - 보고 싶었어"))
                .andExpect(jsonPath("$.items[1].stage").value("beginner"))
                .andExpect(jsonPath("$.items[1].target").value("오늘"))
                .andExpect(jsonPath("$.items[1].correct").value(true))
                .andExpect(jsonPath("$.items[1].perfect").value(false))
                .andExpect(jsonPath("$.items[1].strokeErrorCount").value(1))
                .andExpect(jsonPath("$.items[1].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.nextCursor").value(String.valueOf(second)))
                .andReturn().getResponse().getContentAsString();

        String cursor = objectMapper.readTree(body).get("nextCursor").asText();
        mockMvc.perform(get("/api/v1/writings").param("size", "2").param("cursor", cursor)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].writingId").value(first))
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }

    @Test
    void 엔진이_실패한_기록은_목록에_없고_하나로도_못_본다() throws Exception {
        String token = bearer("DGU-0001");
        doThrow(new BusinessException(ErrorCode.ENGINE_UNAVAILABLE)).when(engineClient).grade(any());
        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(submission(contentId(token, "kpop", "beginner", 0))))
                .andExpect(status().isServiceUnavailable());
        flushAndClear();

        mockMvc.perform(get("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }

    @Test
    void 하나는_판정과_필기_원본을_준다() throws Exception {
        String token = bearer("DGU-0001");
        long id = submit(token, "kpop", "beginner", 0, GRADE_WITH_STROKE);
        flushAndClear();

        mockMvc.perform(get("/api/v1/writings/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.writingId").value(id))
                .andExpect(jsonPath("$.stage").value("beginner"))
                .andExpect(jsonPath("$.target").value("바다"))
                .andExpect(jsonPath("$.canvas.w").value(288.0))
                .andExpect(jsonPath("$.cells.length()").value(2))
                .andExpect(jsonPath("$.cells[0].strokes[0].points[0].length()").value(4))
                .andExpect(jsonPath("$.grade.correct").value(true))
                .andExpect(jsonPath("$.grade.cells[0].mark").value("stroke"))
                .andExpect(jsonPath("$.reward").doesNotExist());
    }

    @Test
    void 기록_수집에_동의하지_않은_기록은_canvas와_cells가_null() throws Exception {
        String token = bearer("DGU-0003");
        long id = submit(token, "basic", "advanced", 0, GRADE_WITH_STROKE);
        flushAndClear();

        mockMvc.perform(get("/api/v1/writings/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canvas").isEmpty())
                .andExpect(jsonPath("$.cells").isEmpty())
                .andExpect(jsonPath("$.grade.correct").value(true));
    }

    @Test
    void 남의_기록은_403_없는_기록은_404() throws Exception {
        long id = submit(bearer("DGU-0002"), "kpop", "intermediate", 0, GRADE_WITH_STROKE);
        String me = bearer("DGU-0001");

        mockMvc.perform(get("/api/v1/writings/{id}", id).header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/writings/{id}", 999_999).header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void 커서나_size가_틀리면_422() throws Exception {
        String token = bearer("DGU-0001");
        mockMvc.perform(get("/api/v1/writings").param("cursor", "abc").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("cursor"));
        mockMvc.perform(get("/api/v1/writings").param("size", "0").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("size"));
        mockMvc.perform(get("/api/v1/writings").param("size", "51").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 획순_오류가_없으면_0이고_제목은_그_난이도의_카드_제목이다() throws Exception {
        String token = bearer("DGU-0002"); // 중수
        submit(token, "basic", "intermediate", 1, "{\"correct\":true,\"perfect\":true,\"cells\":[{\"index\":0,\"mark\":\"none\"}]}");
        mockMvc.perform(get("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].title").value("여행 표현 1"))
                .andExpect(jsonPath("$.items[0].strokeErrorCount").value(0));
    }

    @Test
    void 숨긴_콘텐츠의_기록도_목록과_하나에서_계속_보인다() throws Exception {
        String token = bearer("DGU-0001");
        long id = submit(token, "kpop", "beginner", 0, GRADE_WITH_STROKE);
        long contentId = contentId(token, "kpop", "beginner", 0);
        contentRepository.findById(contentId).orElseThrow().deactivate();
        flushAndClear();

        mockMvc.perform(get("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].writingId").value(id))
                .andExpect(jsonPath("$.items[0].title").value("별빛 아래"));
        mockMvc.perform(get("/api/v1/writings/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void 다른_사람의_기록은_내_목록에_안_섞인다() throws Exception {
        submit(bearer("DGU-0002"), "kpop", "intermediate", 0, GRADE_WITH_STROKE);
        String me = bearer("DGU-0001");
        long mine = submit(me, "kpop", "beginner", 0, GRADE_WITH_STROKE);
        mockMvc.perform(get("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].writingId").value(mine));
    }

    @Test
    void size_경계와_커서_0_음수() throws Exception {
        String token = bearer("DGU-0001");
        mockMvc.perform(get("/api/v1/writings").param("size", "1").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/writings").param("size", "50").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/writings").param("cursor", "").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        for (String cursor : new String[]{"0", "-5"}) {
            mockMvc.perform(get("/api/v1/writings").param("cursor", cursor).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/writings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/writings/{id}", 1)).andExpect(status().isUnauthorized());
    }

    // ===== 도우미 =====

    private long submit(String token, String mode, String stage, int index, String grade) throws Exception {
        doReturn(objectMapper.readTree(grade)).when(engineClient).grade(any());
        long contentId = contentId(token, mode, stage, index);
        JsonNode detail = objectMapper.readTree(mockMvc.perform(get("/api/v1/contents/{id}", contentId)
                .param("stage", stage).header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getContentAsString());
        int cells = detail.get("task").get("cellCount").asInt();
        String body = mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(submission(contentId, stage, cells)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("writingId").asLong();
    }

    private String submission(long contentId) {
        return submission(contentId, "beginner", 2);
    }

    private String submission(long contentId, String stage, int cellCount) {
        StringBuilder cells = new StringBuilder();
        for (int i = 0; i < cellCount; i++) {
            cells.append(i > 0 ? "," : "").append("{\"index\":").append(i).append(",\"strokes\":[")
                    .append(i == 0 ? "{\"points\":[[30,28,0,0.5],[30,70,80,0.5]]}" : "").append("]}");
        }
        return "{\"contentId\":" + contentId + ",\"stage\":\"" + stage + "\",\"canvas\":{\"w\":288,\"h\":288},\"cells\":[" + cells + "]}";
    }

    private long contentId(String token, String mode, String stage, int index) throws Exception {
        String body = mockMvc.perform(get("/api/v1/contents").param("mode", mode).param("stage", stage)
                .header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("items").get(index).get("id").asLong();
    }
}
