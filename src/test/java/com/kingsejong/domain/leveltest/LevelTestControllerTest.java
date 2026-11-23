package com.kingsejong.domain.leveltest;

import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.infra.engine.EngineGradeRequest;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 레벨 테스트 (API 명세 3.14, 3.15). */
class LevelTestControllerTest extends IntegrationTest {

    @MockitoBean
    EngineClient engineClient;

    @org.springframework.beans.factory.annotation.Autowired
    com.kingsejong.domain.leveltest.repository.LevelTestProgressRepository progressRepository;

    @org.springframework.beans.factory.annotation.Autowired
    com.kingsejong.domain.user.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    com.kingsejong.domain.writing.repository.WritingRepository writingRepository;

    private static final int[] CELLS = {0, 3, 2, 9};

    @Test
    void 문항_3단계를_준다() throws Exception {
        mockMvc.perform(get("/api/v1/level-test").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.length()").value(3))
                .andExpect(jsonPath("$.steps[0].step").value(1))
                .andExpect(jsonPath("$.steps[0].stage").value("beginner"))
                .andExpect(jsonPath("$.steps[0].target").value("가나다"))
                .andExpect(jsonPath("$.steps[0].cellCount").value(3))
                .andExpect(jsonPath("$.steps[2].stage").value("advanced"))
                .andExpect(jsonPath("$.steps[2].cellCount").value(9))
                .andExpect(jsonPath("$.steps[2].wordGroups[0]").value(2));
    }

    @Test
    void 세_단계를_모두_통과하면_고수를_추천한다() throws Exception {
        String token = bearer("DGU-0004");
        submit(token, 1, true).andExpect(jsonPath("$.passed").value(true)).andExpect(jsonPath("$.finished").value(false))
                .andExpect(jsonPath("$.recommendedStage").isEmpty()).andExpect(jsonPath("$.steps.length()").value(1));
        submit(token, 2, true).andExpect(jsonPath("$.finished").value(false)).andExpect(jsonPath("$.steps.length()").value(2));
        submit(token, 3, true)
                .andExpect(jsonPath("$.step").value(3))
                .andExpect(jsonPath("$.finished").value(true))
                .andExpect(jsonPath("$.recommendedStage").value("advanced"))
                .andExpect(jsonPath("$.steps[2].title").value("짧은 문장 쓰기"))
                .andExpect(jsonPath("$.steps[2].passed").value(true));

        ArgumentCaptor<EngineGradeRequest> sent = ArgumentCaptor.forClass(EngineGradeRequest.class);
        verify(engineClient, org.mockito.Mockito.times(3)).grade(sent.capture());
        assertThat(sent.getAllValues().get(2).stage()).isEqualTo("advanced");
        assertThat(sent.getAllValues().get(2).target()).isEqualTo("바다 보러 가요");
    }

    @Test
    void 멈춘_단계에_따라_추천이_다르다() throws Exception {
        String token = bearer("DGU-0004");
        submit(token, 1, false).andExpect(jsonPath("$.finished").value(true))
                .andExpect(jsonPath("$.recommendedStage").value("beginner"))
                .andExpect(jsonPath("$.steps[0].passed").value(false));

        submit(token, 1, true);
        submit(token, 2, false).andExpect(jsonPath("$.finished").value(true))
                .andExpect(jsonPath("$.recommendedStage").value("intermediate"));

        submit(token, 1, true);
        submit(token, 2, true);
        submit(token, 3, false).andExpect(jsonPath("$.finished").value(true))
                .andExpect(jsonPath("$.recommendedStage").value("intermediate"))
                .andExpect(jsonPath("$.steps.length()").value(3));
    }

    @Test
    void 앞_단계를_통과하지_않으면_다음_단계는_422_엔진도_안_부른다() throws Exception {
        String token = bearer("DGU-0004");
        submit(token, 2, true).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("step"));
        submit(token, 1, false);
        submit(token, 2, true).andExpect(status().isUnprocessableEntity());
        verify(engineClient, org.mockito.Mockito.times(1)).grade(any());
    }

    @Test
    void 일단계를_다시_내면_처음부터() throws Exception {
        String token = bearer("DGU-0004");
        submit(token, 1, true);
        submit(token, 2, true);
        submit(token, 1, true).andExpect(jsonPath("$.steps.length()").value(1));
        submit(token, 3, true).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 기록과_경험치에는_넣지_않는다() throws Exception {
        String token = bearer("DGU-0004");
        long writingsBefore = writingRepository.count();
        submit(token, 1, true);
        assertThat(writingRepository.count()).isEqualTo(writingsBefore);
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.level.exp").value(0))
                .andExpect(jsonPath("$.streakDays").value(0));
    }

    @Test
    void 칸_수가_틀리거나_없는_단계면_오류() throws Exception {
        String token = bearer("DGU-0004");
        mockMvc.perform(post("/api/v1/level-test/steps/{s}", 1).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("cells"));
        mockMvc.perform(post("/api/v1/level-test/steps/{s}", 4).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(3)))
                .andExpect(status().isNotFound());
        verify(engineClient, never()).grade(any());
    }

    @Test
    void 엔진_오류는_그대로_돌려주고_결과는_안_남긴다() throws Exception {
        String token = bearer("DGU-0004");
        doThrow(new BusinessException(ErrorCode.ENGINE_TIMEOUT)).when(engineClient).grade(any());
        mockMvc.perform(post("/api/v1/level-test/steps/{s}", 1).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(3)))
                .andExpect(status().isGatewayTimeout());
        submit(token, 2, true).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 실패한_단계를_다시_내서_통과하면_다음으로_간다() throws Exception {
        String token = bearer("DGU-0004");
        submit(token, 1, true);
        submit(token, 2, false).andExpect(jsonPath("$.finished").value(true));
        submit(token, 2, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.finished").value(false))
                .andExpect(jsonPath("$.steps.length()").value(2));
        submit(token, 3, true).andExpect(jsonPath("$.recommendedStage").value("advanced"));
        // 다 끝난 뒤 3단계를 다시 내도 된다 (2단계 통과 상태)
        submit(token, 3, false).andExpect(jsonPath("$.recommendedStage").value("intermediate"));
    }

    @Test
    void 다른_사람의_진행은_섞이지_않는다() throws Exception {
        submit(bearer("DGU-0001"), 1, true);
        submit(bearer("DGU-0004"), 2, true).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 삼단계_칸_수_경계와_단계_값() throws Exception {
        String token = bearer("DGU-0004");
        submit(token, 1, true);
        submit(token, 2, true);
        doReturn(objectMapper.readTree("{\"correct\":true,\"cells\":[]}")).when(engineClient).grade(any());
        for (int[] c : new int[][]{{7, 422}, {8, 200}, {9, 200}, {30, 200}, {31, 422}}) {
            mockMvc.perform(post("/api/v1/level-test/steps/{s}", 3).header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON).content(body(c[0])))
                    .andExpect(status().is(c[1]));
        }
        mockMvc.perform(post("/api/v1/level-test/steps/{s}", 0).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(3)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/level-test/steps/{s}", "abc").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(3)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void 엔진을_기다리는_사이_앞_단계가_초기화되면_422이고_결과를_남기지_않는다() throws Exception {
        String token = bearer("DGU-0004");
        submit(token, 1, true);
        Long userId = userRepository.findByLoginCode("DGU-0004").orElseThrow().getId();
        org.mockito.Mockito.doAnswer(invocation -> {
            progressRepository.findByUserId(userId).orElseThrow().record(1, false); // 그사이 1단계를 다시 내서 실패한 셈
            return objectMapper.readTree("{\"correct\":true,\"cells\":[]}");
        }).when(engineClient).grade(any());

        mockMvc.perform(post("/api/v1/level-test/steps/{s}", 2).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("step"));
        assertThat(progressRepository.findByUserId(userId).orElseThrow().getStep2Passed()).isNull();
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/level-test")).andExpect(status().isUnauthorized());
    }

    private ResultActions submit(String token, int step, boolean correct) throws Exception {
        doReturn(objectMapper.readTree("{\"correct\":" + correct + ",\"perfect\":" + correct + ",\"cells\":[]}"))
                .when(engineClient).grade(any());
        return mockMvc.perform(post("/api/v1/level-test/steps/{s}", step).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body(CELLS[step])));
    }

    private static String body(int cellCount) {
        StringBuilder cells = new StringBuilder();
        for (int i = 0; i < cellCount; i++) {
            cells.append(i > 0 ? "," : "").append("{\"index\":").append(i).append(",\"strokes\":[{\"points\":[[10,10,").append(i * 100).append(",0.5]]}]}");
        }
        return "{\"canvas\":{\"w\":288,\"h\":288},\"cells\":[" + cells + "]}";
    }
}
