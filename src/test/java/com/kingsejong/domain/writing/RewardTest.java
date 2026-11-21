package com.kingsejong.domain.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.user.repository.UserRepository;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.domain.writing.repository.WritingRepository;
import com.kingsejong.global.common.KstDate;
import com.kingsejong.global.common.Stage;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 쓰기 제출 응답의 reward (API 명세 3.8 경험치 규칙). */
class RewardTest extends IntegrationTest {

    @MockitoBean
    EngineClient engineClient;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WritingRepository writingRepository;

    @Test
    void 처음_perfect면_10과_5를_받는다() throws Exception {
        String token = bearer("DGU-0001");
        JsonNode reward = submit(token, true, true);

        assertThat(reward.get("expGained").asInt()).isEqualTo(15);
        assertThat(reward.get("items").size()).isEqualTo(2);
        assertThat(reward.get("items").get(0).get("reason").asText()).isEqualTo("correct");
        assertThat(reward.get("items").get(0).get("exp").asInt()).isEqualTo(10);
        assertThat(reward.get("items").get(1).get("reason").asText()).isEqualTo("perfect");
        assertThat(reward.get("level").get("exp").asInt()).isEqualTo(15);
        assertThat(reward.get("streakDays").asInt()).isEqualTo(1);
        assertThat(reward.get("today").get("done").asInt()).isEqualTo(1);
        assertThat(reward.get("leveledUp").asBoolean()).isFalse();
        assertThat(reward.get("stageUpSuggested").asBoolean()).isFalse();
    }

    @Test
    void 같은_날_같은_콘텐츠는_15까지만_받는다() throws Exception {
        String token = bearer("DGU-0001");
        submit(token, true, true);
        JsonNode again = submit(token, true, true);

        assertThat(again.get("expGained").asInt()).isZero();
        assertThat(again.get("items").size()).isZero();
        assertThat(again.get("level").get("exp").asInt()).isEqualTo(15);
    }

    @Test
    void correct_뒤에_perfect가_되면_5만_더_받는다() throws Exception {
        String token = bearer("DGU-0001");
        JsonNode first = submit(token, true, false);
        JsonNode second = submit(token, true, true);

        assertThat(first.get("expGained").asInt()).isEqualTo(10);
        assertThat(second.get("expGained").asInt()).isEqualTo(5);
        assertThat(second.get("items").size()).isEqualTo(1);
        assertThat(second.get("items").get(0).get("reason").asText()).isEqualTo("perfect");
    }

    @Test
    void 틀리면_경험치는_없지만_연속_학습은_센다() throws Exception {
        String token = bearer("DGU-0001");
        JsonNode reward = submit(token, false, false);

        assertThat(reward.get("expGained").asInt()).isZero();
        assertThat(reward.get("items").size()).isZero();
        assertThat(reward.get("streakDays").asInt()).isEqualTo(1);
        assertThat(reward.get("today").get("done").asInt()).isZero();
    }

    @Test
    void 기준_경험치를_넘으면_leveledUp() throws Exception {
        userRepository.findByLoginCode("DGU-0001").orElseThrow().addExp(295);
        JsonNode reward = submit(bearer("DGU-0001"), true, false);

        assertThat(reward.get("leveledUp").asBoolean()).isTrue();
        assertThat(reward.get("previousLevelNo").asInt()).isEqualTo(1);
        assertThat(reward.get("level").get("no").asInt()).isEqualTo(2);
        assertThat(reward.get("level").get("name").asText()).isEqualTo("성균관 유생");
    }

    @Test
    void 같은_난이도에서_10번_연속_맞으면_그_제출에서만_난이도_올리기를_추천한다() throws Exception {
        String token = bearer("DGU-0001");
        for (int i = 1; i <= 9; i++) {
            assertThat(submit(token, true, false).get("stageUpSuggested").asBoolean()).isFalse();
        }
        assertThat(submit(token, true, false).get("stageUpSuggested").asBoolean()).isTrue();
        assertThat(submit(token, true, false).get("stageUpSuggested").asBoolean()).isFalse();
    }

    @Test
    void 중간에_틀리거나_난이도를_바꾸면_연속_정답이_0이_된다() throws Exception {
        String token = bearer("DGU-0001");
        for (int i = 0; i < 9; i++) {
            submit(token, true, false);
        }
        submit(token, false, false);
        for (int i = 0; i < 9; i++) {
            assertThat(submit(token, true, false).get("stageUpSuggested").asBoolean()).isFalse();
        }
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"intermediate\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"beginner\"}"))
                .andExpect(status().isOk());
        assertThat(submit(token, true, false).get("stageUpSuggested").asBoolean()).isFalse();
    }

    @Test
    void 받은_경험치가_내_정보에_반영된다() throws Exception {
        String token = bearer("DGU-0001");
        submit(token, true, true);

        String me = mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(me);
        assertThat(body.get("level").get("exp").asInt()).isEqualTo(15);
        assertThat(body.get("streakDays").asInt()).isEqualTo(1);
    }

    @Test
    void 내_난이도가_아닌_콘텐츠는_난이도_올리기_카운트에_안_들어간다() throws Exception {
        String token = bearer("DGU-0001"); // 초보
        for (int i = 0; i < 10; i++) {
            JsonNode reward = submit(token, "kpop", "intermediate", 4, true, false);
            assertThat(reward.get("stageUpSuggested").asBoolean()).isFalse();
        }
    }

    @Test
    void 어제_받은_경험치와_상관없이_오늘_다시_받고_연속_학습이_이어진다() throws Exception {
        String token = bearer("DGU-0001");
        User user = userRepository.findByLoginCode("DGU-0001").orElseThrow();
        LocalDate yesterday = KstDate.today().minusDays(1);
        user.recordStudy(yesterday);
        long contentId = firstContentId(token, "kpop", "beginner");
        Writing old = Writing.builder().userId(user.getId()).contentId(contentId).stage(Stage.BEGINNER)
                .target("바다").studyDate(yesterday).build();
        old.gainExp(15);
        writingRepository.save(old);
        flushAndClear();

        JsonNode reward = submit(token, true, true);

        assertThat(reward.get("expGained").asInt()).isEqualTo(15);
        assertThat(reward.get("streakDays").asInt()).isEqualTo(2);
    }

    @Test
    void 정확히_기준_경험치가_되면_레벨이_오른다() throws Exception {
        userRepository.findByLoginCode("DGU-0001").orElseThrow().addExp(285);
        JsonNode reward = submit(bearer("DGU-0001"), true, true); // 285 + 15 = 300

        assertThat(reward.get("level").get("exp").asInt()).isEqualTo(300);
        assertThat(reward.get("level").get("no").asInt()).isEqualTo(2);
        assertThat(reward.get("leveledUp").asBoolean()).isTrue();
    }

    /** 초보 K팝 첫 콘텐츠(바다, 2칸)를 제출하고 reward를 돌려준다. */
    private JsonNode submit(String token, boolean correct, boolean perfect) throws Exception {
        return submit(token, "kpop", "beginner", 2, correct, perfect);
    }

    private long firstContentId(String token, String mode, String stage) throws Exception {
        String list = mockMvc.perform(get("/api/v1/contents").param("mode", mode).param("stage", stage)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(list).get("items").get(0).get("id").asLong();
    }

    private JsonNode submit(String token, String mode, String stage, int cellCount, boolean correct, boolean perfect)
            throws Exception {
        when(engineClient.grade(any())).thenReturn(objectMapper.readTree(
                "{\"engineVersion\":\"test\",\"correct\":" + correct + ",\"perfect\":" + perfect + ",\"cells\":[]}"));
        long contentId = firstContentId(token, mode, stage);
        StringBuilder cells = new StringBuilder();
        for (int i = 0; i < cellCount; i++) {
            cells.append(i > 0 ? "," : "").append("{\"index\":").append(i).append(",\"strokes\":[]}");
        }
        String body = mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentId\":" + contentId + ",\"stage\":\"" + stage + "\",\"canvas\":{\"w\":288,\"h\":288},"
                                + "\"cells\":[" + cells + "]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("reward");
    }
}
