package com.kingsejong.domain.content.youtube;

import com.kingsejong.infra.youtube.YoutubeClient;
import com.kingsejong.infra.youtube.YoutubeVideo;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 같은 영상·새 취향을 동시에 만들어도 500 없이 하나만 생긴다 (실제 커밋, 끝나면 "yt:"를 지움). */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class YoutubeImportConcurrencyTest extends IntegrationTest {

    @MockitoBean
    YoutubeClient youtubeClient;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        String yt = "(select id from contents where seed_key like 'yt:%')";
        jdbcTemplate.update("delete from content_lines where content_id in " + yt);
        jdbcTemplate.update("delete from content_tasks where content_id in " + yt);
        jdbcTemplate.update("delete from contents where seed_key like 'yt:%'");
        jdbcTemplate.update("delete from tastes where seed_key like 'yt:%'");
    }

    @Test
    void 동시에_만들어도_모두_201이고_콘텐츠와_취향은_하나() throws Exception {
        when(youtubeClient.fetch(anyString())).thenReturn(new YoutubeVideo("pSUydWEqKwE", "영상", "채널", null, true, 100,
                new YoutubeVideo.Captions("ko", "manual", List.of(new YoutubeVideo.Line(10, 14, "오늘도 너를 기다렸어")))));
        String body = "{\"url\":\"pSUydWEqKwE\",\"mode\":\"kpop\",\"taste\":\"동시 가수\",\"tasks\":{\"beginner\":{\"target\":\"오늘\"}}}";

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return mockMvc.perform(post("/api/v1/admin/contents/youtube").header("X-Admin-Key", "kingsejong-test-admin-key")
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> r : results) {
                assertThat(r.get(30, TimeUnit.SECONDS)).isEqualTo(201);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from contents where seed_key like 'yt:%'", Long.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from tastes where name = '동시 가수'", Long.class)).isEqualTo(1);
    }
}
