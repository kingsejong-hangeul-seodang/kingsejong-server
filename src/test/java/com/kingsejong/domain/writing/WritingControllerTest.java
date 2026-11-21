package com.kingsejong.domain.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.domain.writing.entity.WritingStatus;
import com.kingsejong.domain.writing.repository.WritingRepository;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.infra.engine.EngineGradeRequest;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 엔진은 가짜(MockitoBean)로 바꾸고, 서버가 엔진에 무엇을 보내고 결과를 어떻게 저장·응답하는지 본다. */
class WritingControllerTest extends IntegrationTest {

    @MockitoBean
    EngineClient engineClient;

    @Autowired
    WritingRepository writingRepository;

    @Autowired
    com.kingsejong.domain.user.repository.UserRepository userRepository;

    @Autowired
    com.kingsejong.domain.content.repository.ContentRepository contentRepository;

    private static final String STROKE = "{\"points\":[[30,28,0,0.5],[30,70,80,0.5]]}";

    @Test
    void 제출하면_엔진_판정을_그대로_돌려주고_저장한다() throws Exception {
        when(engineClient.grade(any())).thenReturn(grade(true, true));
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner"); // 바다 (2칸)

        String body = mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(contentId, "beginner", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentId").value(contentId))
                .andExpect(jsonPath("$.stage").value("beginner"))
                .andExpect(jsonPath("$.target").value("바다"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.grade.correct").value(true))
                .andExpect(jsonPath("$.grade.engineVersion").value("test"))
                .andExpect(jsonPath("$.grade.sejong.text").value("잘했어요"))
                .andExpect(jsonPath("$.nextContentId").isNumber())
                .andReturn().getResponse().getContentAsString();
        long writingId = objectMapper.readTree(body).get("writingId").asLong();

        ArgumentCaptor<EngineGradeRequest> sent = ArgumentCaptor.forClass(EngineGradeRequest.class);
        verify(engineClient).grade(sent.capture());
        assertThat(sent.getValue().sessionId()).isEqualTo(String.valueOf(writingId));
        assertThat(sent.getValue().stage()).isEqualTo("beginner");
        assertThat(sent.getValue().target()).isEqualTo("바다");
        assertThat(sent.getValue().locale()).isEqualTo("ko");
        assertThat(sent.getValue().cells().get(0).get("strokes").get(0).get("points").get(1).get(2).asDouble()).isEqualTo(80);

        flushAndClear(); // 영속성 컨텍스트가 아니라 DB에 저장된 값으로 확인
        Writing saved = writingRepository.findById(writingId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(WritingStatus.GRADED);
        assertThat(saved.getGrade().get("sejong").get("text").asText()).isEqualTo("잘했어요");
        assertThat(saved.isPerfect()).isTrue();
        assertThat(saved.getCells()).isNotNull(); // DGU-0001은 기록 수집 동의
    }

    @Test
    void 맞게_쓰면_목록의_completed와_오늘_진행에_반영된다() throws Exception {
        when(engineClient.grade(any())).thenReturn(grade(true, false));
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");

        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(contentId, "beginner", 2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].completed").value(true))
                .andExpect(jsonPath("$.items[0].perfect").value(false))
                .andExpect(jsonPath("$.items[1].completed").value(false));
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.today.done").value(1));
    }

    @Test
    void 틀리게_쓰면_completed가_아니다() throws Exception {
        when(engineClient.grade(any())).thenReturn(grade(false, false));
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");

        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(contentId, "beginner", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grade.correct").value(false));

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.today.done").value(0));
    }

    @Test
    void 기록_수집에_동의하지_않았으면_필기_원본을_저장하지_않는다() throws Exception {
        when(engineClient.grade(any())).thenReturn(grade(true, true));
        String token = bearer("DGU-0003"); // 고수, 동의 안 함, 영어
        long contentId = firstContentId(token, "basic", "advanced"); // 잘 먹겠습니다 (7칸)

        String body = mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(contentId, "advanced", 9)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        flushAndClear();
        Writing saved = writingRepository.findById(objectMapper.readTree(body).get("writingId").asLong()).orElseThrow();
        assertThat(saved.getCanvas()).isNull();
        assertThat(saved.getCells()).isNull();
        assertThat(saved.getGrade()).isNotNull();

        ArgumentCaptor<EngineGradeRequest> sent = ArgumentCaptor.forClass(EngineGradeRequest.class);
        verify(engineClient).grade(sent.capture());
        assertThat(sent.getValue().locale()).isEqualTo("en");
        assertThat(sent.getValue().cells().size()).isEqualTo(9);
    }

    @Test
    void 초보_칸_수가_정답_글자_수와_다르면_422이고_엔진을_부르지_않는다() throws Exception {
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");

        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(contentId, "beginner", 3)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details[0].field").value("cells"));
        verify(engineClient, never()).grade(any());
    }

    @Test
    void 고수_칸이_정답보다_적으면_422() throws Exception {
        String token = bearer("DGU-0003");
        long contentId = firstContentId(token, "basic", "advanced"); // 7칸

        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(contentId, "advanced", 6)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 칸_번호가_순서대로가_아니면_422() throws Exception {
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");
        String body = "{\"contentId\":" + contentId + ",\"stage\":\"beginner\",\"canvas\":{\"w\":288,\"h\":288},"
                + "\"cells\":[{\"index\":1,\"strokes\":[]},{\"index\":0,\"strokes\":[]}]}";

        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("cells[0].index"));
    }

    @Test
    void 점이_숫자_4개가_아니면_422() throws Exception {
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");
        String body = "{\"contentId\":" + contentId + ",\"stage\":\"beginner\",\"canvas\":{\"w\":288,\"h\":288},"
                + "\"cells\":[{\"index\":0,\"strokes\":[{\"points\":[[1,2,3]]}]},{\"index\":1,\"strokes\":[]}]}";

        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
        verify(engineClient, never()).grade(any());
    }

    @Test
    void 없는_콘텐츠면_404() throws Exception {
        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(999_999, "beginner", 2)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 엔진에_연결이_안_되면_503이고_원본은_FAILED로_남는다() throws Exception {
        when(engineClient.grade(any())).thenThrow(new BusinessException(ErrorCode.ENGINE_UNAVAILABLE));
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");
        long before = writingRepository.count();

        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(contentId, "beginner", 2)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ENGINE_UNAVAILABLE"));

        assertThat(writingRepository.count()).isEqualTo(before + 1);
        Writing failed = writingRepository.findAll().stream()
                .max((a, b) -> Long.compare(a.getId(), b.getId())).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(WritingStatus.FAILED);
        assertThat(failed.getCells()).isNotNull();
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/writings").contentType(MediaType.APPLICATION_JSON)
                        .content(submission(1, "beginner", 2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 개수_상한을_넘거나_필수_값이_없으면_422() throws Exception {
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");
        String point = "[1,2,3,0.5]";
        String manyPoints = String.join(",", java.util.Collections.nCopies(2001, point));
        String manyStrokes = String.join(",", java.util.Collections.nCopies(61, "{\"points\":[" + point + "]}"));

        String[][] cases = {
                {"cells", submission(contentId, "advanced", 31)},
                {"cells[0].strokes", "{\"contentId\":" + contentId + ",\"stage\":\"beginner\",\"canvas\":{\"w\":288,\"h\":288},"
                        + "\"cells\":[{\"index\":0,\"strokes\":[" + manyStrokes + "]},{\"index\":1,\"strokes\":[]}]}"},
                {"cells[0].strokes[0].points", "{\"contentId\":" + contentId + ",\"stage\":\"beginner\",\"canvas\":{\"w\":288,\"h\":288},"
                        + "\"cells\":[{\"index\":0,\"strokes\":[{\"points\":[" + manyPoints + "]}]},{\"index\":1,\"strokes\":[]}]}"},
                {"canvas.w", "{\"contentId\":" + contentId + ",\"stage\":\"beginner\",\"canvas\":{\"w\":0,\"h\":288},"
                        + "\"cells\":[{\"index\":0,\"strokes\":[]},{\"index\":1,\"strokes\":[]}]}"},
                {"contentId", "{\"stage\":\"beginner\",\"canvas\":{\"w\":288,\"h\":288},"
                        + "\"cells\":[{\"index\":0,\"strokes\":[]},{\"index\":1,\"strokes\":[]}]}"},
        };
        for (String[] c : cases) {
            mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON).content(c[1]))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.details[0].field").value(c[0]));
        }
        verify(engineClient, never()).grade(any());
    }

    @Test
    void 본문이_2MB를_넘으면_읽지_않고_422() throws Exception {
        String huge = "{\"pad\":\"" + "x".repeat(2 * 1024 * 1024) + "\"}";
        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON).content(huge))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verify(engineClient, never()).grade(any());
    }

    @Test
    void 엔진이_502나_504를_주면_그대로_응답하고_FAILED로_남는다() throws Exception {
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");
        for (ErrorCode code : new ErrorCode[]{ErrorCode.ENGINE_ERROR, ErrorCode.ENGINE_TIMEOUT}) {
            doThrow(new BusinessException(code)).when(engineClient).grade(any());
            mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(submission(contentId, "beginner", 2)))
                    .andExpect(status().is(code.getStatus().value()))
                    .andExpect(jsonPath("$.code").value(code.name()));
        }
        flushAndClear();
        Long userId = userRepository.findByLoginCode("DGU-0001").orElseThrow().getId();
        assertThat(writingRepository.findAll())
                .filteredOn(w -> w.getUserId().equals(userId) && w.getStatus() == WritingStatus.FAILED).hasSize(2);
    }

    @Test
    void 중수는_공백_뺀_글자_수_고수는_정답_칸_수와_같으면_통과한다() throws Exception {
        when(engineClient.grade(any())).thenReturn(grade(true, true));
        String mid = bearer("DGU-0002");
        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, mid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(firstContentId(mid, "kpop", "intermediate"), "intermediate", 4))) // 별빛 아래
                .andExpect(status().isCreated());

        String pro = bearer("DGU-0003");
        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, pro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(firstContentId(pro, "basic", "advanced"), "advanced", 7))) // 잘 먹겠습니다
                .andExpect(status().isCreated());
    }

    @Test
    void 다른_사람의_기록은_내_오늘_진행과_완료에_섞이지_않는다() throws Exception {
        when(engineClient.grade(any())).thenReturn(grade(true, true));
        String other = bearer("DGU-0002");
        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(firstContentId(other, "kpop", "beginner"), "beginner", 2)))
                .andExpect(status().isCreated());

        String me = bearer("DGU-0001");
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(jsonPath("$.today.done").value(0));
        mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, me))
                .andExpect(jsonPath("$.items[0].completed").value(false));
    }

    @Test
    void 숨긴_콘텐츠는_제출할_수_없다() throws Exception {
        String token = bearer("DGU-0001");
        long contentId = firstContentId(token, "kpop", "beginner");
        contentRepository.findById(contentId).orElseThrow().deactivate();
        flushAndClear();

        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submission(contentId, "beginner", 2)))
                .andExpect(status().isNotFound());
        verify(engineClient, never()).grade(any());
    }

    // ===== 도우미 =====

    private JsonNode grade(boolean correct, boolean perfect) throws Exception {
        return objectMapper.readTree("{\"engineVersion\":\"test\",\"stage\":\"beginner\",\"correct\":" + correct
                + ",\"perfect\":" + perfect + ",\"cells\":[],\"alignment\":[],\"words\":[],\"explanations\":[],"
                + "\"sejong\":{\"text\":\"잘했어요\",\"expression\":\"praise\",\"source\":\"template\"}}");
    }

    /** 첫 칸에만 획 하나, 나머지는 빈 칸. */
    private String submission(long contentId, String stage, int cellCount) {
        StringBuilder cells = new StringBuilder();
        for (int i = 0; i < cellCount; i++) {
            if (i > 0) {
                cells.append(',');
            }
            cells.append("{\"index\":").append(i).append(",\"strokes\":[").append(i == 0 ? STROKE : "").append("]}");
        }
        return "{\"contentId\":" + contentId + ",\"stage\":\"" + stage + "\",\"canvas\":{\"w\":288,\"h\":288},"
                + "\"cells\":[" + cells + "]}";
    }

    private long firstContentId(String token, String mode, String stage) throws Exception {
        String body = mockMvc.perform(get("/api/v1/contents").param("mode", mode).param("stage", stage)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("items").get(0).get("id").asLong();
    }
}
