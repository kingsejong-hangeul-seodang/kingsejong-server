package com.kingsejong.domain.taste;

import com.kingsejong.domain.taste.repository.TasteRepository;
import com.kingsejong.domain.user.repository.UserRepository;
import com.kingsejong.global.common.Mode;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** 같은 사용자의 취향 저장이 동시에 와도 500 없이 하나씩 처리된다 (실제 커밋, DGU-0004만 쓰고 끝나면 지움). */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TasteConcurrencyTest extends IntegrationTest {

    @Autowired
    TasteRepository tasteRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        Long userId = userRepository.findByLoginCode("DGU-0004").orElseThrow().getId();
        jdbcTemplate.update("delete from user_tastes where user_id = ?", userId);
    }

    @Test
    void 동시에_여러_번_저장해도_모두_204이고_선택이_섞이지_않는다() throws Exception {
        String token = bearer("DGU-0004");
        List<Long> ids = tasteRepository.findAllByModeAndActiveTrueOrderBySortOrderAscIdAsc(Mode.KPOP)
                .stream().map(t -> t.getId()).toList();
        String body = "{\"mode\":\"kpop\",\"tasteIds\":[" + ids.get(0) + "," + ids.get(1) + "]}";

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return mockMvc.perform(put("/api/v1/me/tastes").header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertThat(result.get(20, TimeUnit.SECONDS)).isEqualTo(204);
            }
        } finally {
            pool.shutdownNow();
        }
        Long userId = userRepository.findByLoginCode("DGU-0004").orElseThrow().getId();
        assertThat(jdbcTemplate.queryForList("select taste_id from user_tastes where user_id = ?", Long.class, userId))
                .containsExactlyInAnyOrder(ids.get(0), ids.get(1));
    }
}
