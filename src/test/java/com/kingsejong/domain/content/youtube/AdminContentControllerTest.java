package com.kingsejong.domain.content.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.domain.content.repository.ContentRepository;
import com.kingsejong.domain.content.seed.ContentSeeder;
import com.kingsejong.domain.taste.seed.TasteSeeder;
import com.kingsejong.infra.youtube.YoutubeClient;
import com.kingsejong.infra.youtube.YoutubeVideo;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 관리자: 유튜브로 콘텐츠 만들기. 유튜브는 가짜(MockitoBean). */
class AdminContentControllerTest extends IntegrationTest {

    private static final String KEY = "kingsejong-test-admin-key";

    @MockitoBean
    YoutubeClient youtubeClient;

    @Autowired
    ContentRepository contentRepository;

    @Autowired
    ContentSeeder contentSeeder;

    @Autowired
    TasteSeeder tasteSeeder;

    @BeforeEach
    void video() {
        when(youtubeClient.fetch(anyString())).thenReturn(new YoutubeVideo("pSUydWEqKwE", "NewJeans 'Ditto' Official MV",
                "HYBE LABELS", "https://i.ytimg.com/vi/pSUydWEqKwE/hqdefault.jpg", true, 333,
                new YoutubeVideo.Captions("ko", "manual", List.of(
                        new YoutubeVideo.Line(5, 7, "Okay, Let's Go"),
                        new YoutubeVideo.Line(40, 43, "오늘도 너를 기다렸어"),
                        new YoutubeVideo.Line(44, 47, "보고 싶었어 정말로")))));
    }

    @Test
    void 관리자_키가_없으면_401_틀리면_403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/youtube/preview").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/admin/youtube/preview").header("X-Admin-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"x\"}"))
                .andExpect(status().isForbidden());
        // 사용자 토큰으로도 안 된다
        mockMvc.perform(post("/api/v1/admin/youtube/preview").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 미리_보기는_자막과_추천을_준다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/youtube/preview").header("X-Admin-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://youtu.be/pSUydWEqKwE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captionKind").value("manual"))
                .andExpect(jsonPath("$.lines.length()").value(3))
                .andExpect(jsonPath("$.suggestion.startSec").value(39.0))
                .andExpect(jsonPath("$.suggestion.targets.advanced").value("오늘도 너를 기다렸어"));
    }

    @Test
    void 만들면_목록_상세_취향에_이어진다() throws Exception {
        JsonNode created = objectMapper.readTree(importVideo("{\"url\":\"https://youtu.be/pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\","
                + "\"tasks\":{\"beginner\":{\"target\":\"고양이\",\"meaning\":\"cat\"}}}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Ditto"))
                .andExpect(jsonPath("$.source").value("뉴진스 - Ditto"))
                .andExpect(jsonPath("$.seedKey").value("yt:pSUydWEqKwE:39"))
                .andExpect(jsonPath("$.tasks.beginner.target").value("고양이"))
                .andExpect(jsonPath("$.tasks.beginner.source").value("request"))
                .andExpect(jsonPath("$.tasks.advanced.source").value("subtitle"))
                .andExpect(jsonPath("$.taste.created").value(true))
                .andExpect(jsonPath("$.seed.taste").value(org.hamcrest.Matchers.startsWith("yt:kpop:")))
                .andReturn().getResponse().getContentAsString());
        long contentId = created.get("contentId").asLong();
        flushAndClear();

        String token = bearer("DGU-0001");
        mockMvc.perform(get("/api/v1/contents/{id}", contentId).param("stage", "beginner").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.kind").value("clip"))
                .andExpect(jsonPath("$.clip.youtubeVideoId").value("pSUydWEqKwE"))
                .andExpect(jsonPath("$.clip.startSec").value(39.0))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.task.target").value("고양이"))
                .andExpect(jsonPath("$.task.meaning").value("cat"));
        mockMvc.perform(get("/api/v1/tastes").param("mode", "kpop").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[?(@.name == '뉴진스')]").exists());
        mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "advanced").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[?(@.id == " + contentId + ")]").exists());
    }

    @Test
    void dryRun은_저장하지_않는다() throws Exception {
        long before = contentRepository.count();
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"dryRun\":true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentId").isEmpty())
                .andExpect(jsonPath("$.taste.id").isEmpty());
        assertThat(contentRepository.count()).isEqualTo(before);
    }

    @Test
    void 같은_영상_같은_구간은_덮어쓰고_시드가_다시_돌아도_남는다() throws Exception {
        String body = "{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\"}";
        long first = objectMapper.readTree(importVideo(body).andReturn().getResponse().getContentAsString()).get("contentId").asLong();
        long count = contentRepository.count();
        long second = objectMapper.readTree(importVideo(body.replace("}", ",\"title\":\"디토\"}"))
                .andReturn().getResponse().getContentAsString()).get("contentId").asLong();
        assertThat(second).isEqualTo(first);
        assertThat(contentRepository.count()).isEqualTo(count);

        tasteSeeder.run(null);
        contentSeeder.run(null);
        flushAndClear();
        assertThat(contentRepository.findById(first).orElseThrow().isActive()).isTrue();
        assertThat(contentRepository.findById(first).orElseThrow().getTitle()).isEqualTo("디토");
    }

    @Test
    void 기본_연습_모드나_틀린_구간이나_규칙에_안_맞는_쓸_글은_422() throws Exception {
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"basic\",\"taste\":\"x\"}").andExpect(status().isUnprocessableEntity());
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"x\",\"startSec\":10}").andExpect(status().isUnprocessableEntity());
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"x\",\"startSec\":10,\"endSec\":50}")
                .andExpect(status().isUnprocessableEntity());
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"x\",\"tasks\":{\"beginner\":{\"target\":\"두 단어\"}}}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("tasks.beginner"));
        importVideo("{\"url\":\"https://example.com\",\"mode\":\"kpop\",\"taste\":\"x\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("url"));
    }

    @Test
    void 자막이_없으면_구간과_쓸_글을_직접_줘야_한다() throws Exception {
        when(youtubeClient.fetch(anyString())).thenReturn(new YoutubeVideo("abcdefghijk", "영상", "채널", null, true, 100, null));
        importVideo("{\"url\":\"abcdefghijk\",\"mode\":\"kdrama\",\"taste\":\"작품\"}").andExpect(status().isUnprocessableEntity());
        importVideo("{\"url\":\"abcdefghijk\",\"mode\":\"kdrama\",\"taste\":\"작품\",\"startSec\":10,\"endSec\":30,"
                + "\"tasks\":{\"beginner\":{\"target\":\"친구\"},\"intermediate\":{\"target\":\"우리는 친구\"},\"advanced\":{\"target\":\"오늘부터 우리는 친구\"}}}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("영상"))
                .andExpect(jsonPath("$.lines.length()").value(0));
    }

    @Test
    void 내리면_목록에서_빠진다() throws Exception {
        long id = objectMapper.readTree(importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\"}")
                .andReturn().getResponse().getContentAsString()).get("contentId").asLong();
        mockMvc.perform(delete("/api/v1/admin/contents/{id}", id).header("X-Admin-Key", KEY)).andExpect(status().isNoContent());
        flushAndClear();
        mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").doesNotExist());
        mockMvc.perform(delete("/api/v1/admin/contents/{id}", 999_999).header("X-Admin-Key", KEY)).andExpect(status().isNotFound());
    }

    @Test
    void 취향_이름은_정규화해서_같은_이름이면_재사용하고_시드_취향도_재사용한다() throws Exception {
        String longName = "가".repeat(50);
        String key = objectMapper.readTree(importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kdrama\",\"taste\":\"" + longName + "\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).at("/taste/seedKey").asText();
        assertThat(key).startsWith("yt:kdrama:").hasSizeLessThanOrEqualTo(50);

        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\" 뉴진스 \",\"startSec\":40,\"endSec\":48}")
                .andExpect(jsonPath("$.taste.created").value(true));
        String nfd = java.text.Normalizer.normalize("뉴진스", java.text.Normalizer.Form.NFD);
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"" + nfd + "\"}")
                .andExpect(jsonPath("$.taste.created").value(false))
                .andExpect(jsonPath("$.taste.name").value("뉴진스"));
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"하늘빛\"}")
                .andExpect(jsonPath("$.taste.created").value(false))
                .andExpect(jsonPath("$.taste.seedKey").value("artist-haneulbit"));
    }

    @Test
    void 시작_초가_다르면_새_콘텐츠_소수점만_다르면_덮어쓴다_내린_뒤_다시_만들면_다시_보인다() throws Exception {
        long a = id(importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"startSec\":40,\"endSec\":48}"));
        long b = id(importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"startSec\":40.5,\"endSec\":48}"));
        long c = id(importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"startSec\":39,\"endSec\":48}"));
        assertThat(b).isEqualTo(a);
        assertThat(c).isNotEqualTo(a);

        mockMvc.perform(delete("/api/v1/admin/contents/{id}", a).header("X-Admin-Key", KEY)).andExpect(status().isNoContent());
        flushAndClear();
        assertThat(contentRepository.findById(a).orElseThrow().isActive()).isFalse();
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"startSec\":40,\"endSec\":48}");
        flushAndClear();
        assertThat(contentRepository.findById(a).orElseThrow().isActive()).isTrue();
    }

    @Test
    void 자막_한_줄이_200자를_넘으면_어절_단위로_자른다() throws Exception {
        String longLine = "가나다라 ".repeat(50).trim(); // 249자
        when(youtubeClient.fetch(anyString())).thenReturn(new YoutubeVideo("pSUydWEqKwE", "영상", "채널", null, true, 100,
                new YoutubeVideo.Captions("ko", "asr", List.of(new YoutubeVideo.Line(10, 15, longLine)))));
        String text = objectMapper.readTree(importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kdrama\",\"taste\":\"작품\","
                        + "\"tasks\":{\"beginner\":{\"target\":\"가나\"}}}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).at("/lines/0/text").asText();
        assertThat(text).hasSizeLessThanOrEqualTo(200).endsWith("가나다라");
    }

    @Test
    void 자막을_직접_넣으면_번역과_함께_저장한다() throws Exception {
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"lines\":[{\"text\":\"안녕\",\"startSec\":1,\"endSec\":2}]}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.details[0].field").value("startSec"));
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"startSec\":10,\"endSec\":20,"
                + "\"lines\":[{\"text\":\"안녕\",\"startSec\":1,\"endSec\":2}]}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.details[0].field").value("lines[0]"));
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"startSec\":10,\"endSec\":20,"
                + "\"lines\":[{\"text\":\"안녕\",\"startSec\":15,\"endSec\":16},{\"text\":\"반가워\",\"startSec\":11,\"endSec\":12}]}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.details[0].field").value("lines[1]"));
        importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"startSec\":10,\"endSec\":20,\"lines\":[],"
                + "\"tasks\":{\"beginner\":{\"target\":\"친구\"},\"intermediate\":{\"target\":\"우리는 친구\"},\"advanced\":{\"target\":\"우리는 친구\"}}}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lines.length()").value(0));
        long id = id(importVideo("{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"뉴진스\",\"startSec\":10,\"endSec\":20,"
                + "\"lines\":[{\"text\":\"오늘도 너를 기다렸어\",\"startSec\":11,\"endSec\":14,\"translation\":\"I waited for you again today\"}]}")
                .andExpect(jsonPath("$.tasks.advanced.target").value("오늘도 너를 기다렸어"))
                .andExpect(jsonPath("$.seed.lines[0].translation").value("I waited for you again today")));
        flushAndClear();
        mockMvc.perform(get("/api/v1/contents/{id}", id).param("stage", "advanced").header(HttpHeaders.AUTHORIZATION, bearer("DGU-0001")))
                .andExpect(jsonPath("$.lines[0].translation").value("I waited for you again today"));
    }

    @Test
    void 관리자_키는_16자_이상이어야_서버가_뜬다() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new com.kingsejong.global.auth.AdminInterceptor("short-key-123"))
                .isInstanceOf(IllegalStateException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new com.kingsejong.global.auth.AdminInterceptor(null))
                .isInstanceOf(IllegalStateException.class);
    }

    private long id(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("contentId").asLong();
    }

    private ResultActions importVideo(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/contents/youtube").header("X-Admin-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
