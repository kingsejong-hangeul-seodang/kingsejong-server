package com.kingsejong.domain.jamo;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 자모 숙련도·도감·상세 (API 명세 3.8의 6번, 3.12, 3.13). */
class JamoControllerTest extends IntegrationTest {

    @MockitoBean
    EngineClient engineClient;

    /** 바다: '바'는 맞았지만 ㅂ 획순이 다름, '다'는 다른 글자. */
    private static final String GRADE_BA_STROKE_DA_WRONG = "{\"correct\":false,\"perfect\":false,\"cells\":["
            + "{\"index\":0,\"expected\":\"바\",\"charCorrect\":true,\"mark\":\"stroke\",\"errors\":[{\"type\":\"ORDER\",\"jamo\":\"ㅂ\",\"role\":\"cho\"}]},"
            + "{\"index\":1,\"expected\":\"다\",\"charCorrect\":false,\"mark\":\"char\",\"errors\":[{\"type\":\"CHAR_MISMATCH\"}]}],"
            + "\"explanations\":[{\"kind\":\"stroke\",\"jamo\":\"ㅂ\",\"right\":\"바\",\"cellIndexes\":[0],\"text\":\"바의 ㅂ은 세로획부터 써요.\"},"
            + "{\"kind\":\"char\",\"wrong\":\"타\",\"right\":\"다\",\"cellIndexes\":[1],\"text\":\"타라고 쓰셨는데 다라고 써야 해요.\"}]}";
    private static final String GRADE_ALL_OK = "{\"correct\":true,\"perfect\":true,\"cells\":["
            + "{\"index\":0,\"expected\":\"바\",\"charCorrect\":true,\"mark\":\"none\",\"errors\":[]},"
            + "{\"index\":1,\"expected\":\"다\",\"charCorrect\":true,\"mark\":\"none\",\"errors\":[]}]}";

    @Test
    void 처음에는_40개_모두_안_써_봄() throws Exception {
        mockMvc.perform(get("/api/v1/jamo").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consonants.length()").value(19))
                .andExpect(jsonPath("$.vowels.length()").value(21))
                .andExpect(jsonPath("$.consonants[0].jamo").value("ㄱ"))
                .andExpect(jsonPath("$.consonants[0].name").value("기역"))
                .andExpect(jsonPath("$.consonants[0].mastery").value("none"))
                .andExpect(jsonPath("$.vowels[20].jamo").value("ㅣ"));
    }

    @Test
    void 맞은_칸의_자모는_쓴_횟수_획_오류는_오류_횟수로_쌓이고_틀린_칸은_안_센다() throws Exception {
        String token = bearer("DGU-0001");
        submitBada(token, GRADE_BA_STROKE_DA_WRONG);
        flushAndClear();

        JsonNode dogam = objectMapper.readTree(mockMvc.perform(get("/api/v1/jamo").header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString());
        JsonNode bieup = find(dogam.get("consonants"), "ㅂ");
        JsonNode a = find(dogam.get("vowels"), "ㅏ");
        JsonNode digeut = find(dogam.get("consonants"), "ㄷ");
        org.assertj.core.api.Assertions.assertThat(bieup.get("writeCount").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(bieup.get("errorCount").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(bieup.get("mastery").asText()).isEqualTo("practice");
        org.assertj.core.api.Assertions.assertThat(a.get("writeCount").asInt()).isEqualTo(1); // '다'의 ㅏ는 안 셈
        org.assertj.core.api.Assertions.assertThat(digeut.get("mastery").asText()).isEqualTo("none");
    }

    @Test
    void 다섯_번_이상_오류_없이_쓰면_익숙() throws Exception {
        String token = bearer("DGU-0001");
        for (int i = 0; i < 5; i++) {
            submitBada(token, GRADE_ALL_OK);
        }
        mockMvc.perform(get("/api/v1/jamo/{j}", "ㄷ").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.mastery").value("familiar"))
                .andExpect(jsonPath("$.writeCount").value(5))
                .andExpect(jsonPath("$.errorCount").value(0));
    }

    @Test
    void 상세는_단계_설명_예시_최근_기록_연습_콘텐츠를_준다() throws Exception {
        String token = bearer("DGU-0001");
        submitBada(token, GRADE_BA_STROKE_DA_WRONG);

        mockMvc.perform(get("/api/v1/jamo/{j}", "ㅂ").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jamo").value("ㅂ"))
                .andExpect(jsonPath("$.name").value("비읍"))
                .andExpect(jsonPath("$.type").value("consonant"))
                .andExpect(jsonPath("$.strokeCount").value(4))
                .andExpect(jsonPath("$.steps.length()").value(4))
                .andExpect(jsonPath("$.commonMistake").isNotEmpty())
                .andExpect(jsonPath("$.examples", hasItem("바")))
                .andExpect(jsonPath("$.recentWritings.length()").value(1))
                .andExpect(jsonPath("$.recentWritings[0].target").value("바다"))
                .andExpect(jsonPath("$.recentWritings[0].mode").value("kpop"))
                .andExpect(jsonPath("$.recentWritings[0].source").value("하늘빛 - 별빛 아래"))
                .andExpect(jsonPath("$.recentWritings[0].result").value("stroke"))
                .andExpect(jsonPath("$.recentWritings[0].message").value("바의 ㅂ은 세로획부터 써요."))
                .andExpect(jsonPath("$.practiceContentId").isNumber());

        mockMvc.perform(get("/api/v1/jamo/{j}", "ㄷ").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.recentWritings[0].result").value("char"))
                .andExpect(jsonPath("$.recentWritings[0].message").value("타라고 쓰셨는데 다라고 써야 해요."));
        mockMvc.perform(get("/api/v1/jamo/{j}", "ㅢ").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.type").value("vowel"))
                .andExpect(jsonPath("$.recentWritings.length()").value(0))
                .andExpect(jsonPath("$.commonMistake").isEmpty());
    }

    @Test
    void 연습하기는_그_자모의_초보_자모_연습_콘텐츠를_연다() throws Exception {
        String token = bearer("DGU-0001");
        long practiceId = objectMapper.readTree(mockMvc.perform(get("/api/v1/jamo/{j}", "ㄷ")
                .header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getContentAsString())
                .get("practiceContentId").asLong();

        mockMvc.perform(get("/api/v1/contents/{id}", practiceId).param("stage", "beginner").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.kind").value("jamo"))
                .andExpect(jsonPath("$.task.target").value("ㄷ"));
    }

    @Test
    void 다_맞은_기록은_ok이고_message는_null() throws Exception {
        String token = bearer("DGU-0001");
        submitBada(token, GRADE_ALL_OK);
        mockMvc.perform(get("/api/v1/jamo/{j}", "ㄷ").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.recentWritings[0].result").value("ok"))
                .andExpect(jsonPath("$.recentWritings[0].message").isEmpty());
    }

    @Test
    void 균형_오류는_안_세고_한_칸의_같은_자모_오류는_한_번만_겹받침은_나눠_센다() throws Exception {
        String token = bearer("DGU-0001");
        submitBada(token, "{\"correct\":true,\"perfect\":false,\"cells\":["
                + "{\"index\":0,\"expected\":\"닭\",\"charCorrect\":true,\"errors\":["
                + "{\"type\":\"ORDER\",\"jamo\":\"ㄷ\"},{\"type\":\"DIRECTION\",\"jamo\":\"ㄷ\"},{\"type\":\"BALANCE\",\"jamo\":\"ㅏ\"}]},"
                + "{\"index\":1,\"expected\":\"다\",\"charCorrect\":true,\"errors\":[]}]}");
        flushAndClear();

        JsonNode dogam = objectMapper.readTree(mockMvc.perform(get("/api/v1/jamo").header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(find(dogam.get("consonants"), "ㄷ").get("writeCount").asInt()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(find(dogam.get("consonants"), "ㄷ").get("errorCount").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(find(dogam.get("vowels"), "ㅏ").get("errorCount").asInt()).isZero();
        org.assertj.core.api.Assertions.assertThat(find(dogam.get("consonants"), "ㄹ").get("writeCount").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(find(dogam.get("consonants"), "ㄱ").get("writeCount").asInt()).isEqualTo(1);
    }

    @Test
    void 도감은_자음_19_모음_21_순서와_종류() throws Exception {
        JsonNode dogam = objectMapper.readTree(mockMvc.perform(get("/api/v1/jamo")
                .header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))).andReturn().getResponse().getContentAsString());
        String consonants = "", vowels = "";
        for (JsonNode n : dogam.get("consonants")) consonants += n.get("jamo").asText();
        for (JsonNode n : dogam.get("vowels")) vowels += n.get("jamo").asText();
        org.assertj.core.api.Assertions.assertThat(consonants).isEqualTo("ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ");
        org.assertj.core.api.Assertions.assertThat(vowels).isEqualTo("ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ");
        mockMvc.perform(get("/api/v1/jamo/{j}", "ㅊ").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(jsonPath("$.strokeCount").value(4)); // 엔진 정답 획 수와 같게
    }

    @Test
    void 최근_기록은_3개까지_내_것만() throws Exception {
        String token = bearer("DGU-0001");
        for (int i = 0; i < 4; i++) {
            submitBada(token, GRADE_ALL_OK);
        }
        submitBada(bearer("DGU-0002"), GRADE_ALL_OK);
        mockMvc.perform(get("/api/v1/jamo/{j}", "ㄷ").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.recentWritings.length()").value(3));
    }

    @Test
    void 도감에_없는_자모는_404() throws Exception {
        String token = bearer("DGU-0001");
        for (String j : new String[]{"ㄳ", "a", "가"}) {
            mockMvc.perform(get("/api/v1/jamo/{j}", j).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/jamo")).andExpect(status().isUnauthorized());
    }

    private void submitBada(String token, String grade) throws Exception {
        doReturn(objectMapper.readTree(grade)).when(engineClient).grade(any());
        long contentId = objectMapper.readTree(mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "beginner")
                .header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getContentAsString()).get("items").get(0).get("id").asLong();
        mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentId\":" + contentId + ",\"stage\":\"beginner\",\"canvas\":{\"w\":288,\"h\":288},"
                                + "\"cells\":[{\"index\":0,\"strokes\":[]},{\"index\":1,\"strokes\":[]}]}"))
                .andExpect(status().isCreated());
    }

    private static JsonNode find(JsonNode items, String jamo) {
        for (JsonNode item : items) {
            if (item.get("jamo").asText().equals(jamo)) {
                return item;
            }
        }
        throw new AssertionError(jamo);
    }
}
