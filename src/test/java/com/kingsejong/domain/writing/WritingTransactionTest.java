package com.kingsejong.domain.writing;

import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.user.repository.UserRepository;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.domain.writing.entity.WritingStatus;
import com.kingsejong.domain.writing.repository.WritingRepository;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 쓰기 제출의 트랜잭션 보증을 실제 커밋으로 확인한다 (테스트 트랜잭션 없음).
 * - 엔진을 부르는 동안 DB 트랜잭션이 열려 있지 않다
 * - 엔진이 실패해도 필기 원본은 커밋되어 FAILED로 남는다
 * 다른 테스트에 영향이 없도록 DGU-0004 계정만 쓰고, 끝나면 그 계정의 기록을 지운다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WritingTransactionTest extends IntegrationTest {

    private static final String LOGIN = "DGU-0004";

    @MockitoBean
    EngineClient engineClient;

    @Autowired
    WritingRepository writingRepository;

    @Autowired
    UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        User user = userRepository.findByLoginCode(LOGIN).orElseThrow();
        user.changeConsent(false); // 시드 값으로 되돌림
        userRepository.save(user);
        Long userId = user.getId();
        writingRepository.deleteAll(writingRepository.findAll().stream()
                .filter(w -> w.getUserId().equals(userId)).toList());
    }

    @Test
    void 엔진을_부르는_동안에는_트랜잭션이_없다() throws Exception {
        AtomicBoolean txActiveDuringEngineCall = new AtomicBoolean(true);
        when(engineClient.grade(any())).thenAnswer(invocation -> {
            txActiveDuringEngineCall.set(TransactionSynchronizationManager.isActualTransactionActive());
            return objectMapper.readTree("{\"correct\":true,\"perfect\":true,\"cells\":[]}");
        });

        submit().andExpect(status().isCreated());

        assertThat(txActiveDuringEngineCall.get()).isFalse();
    }

    @Test
    void 엔진이_실패해도_원본은_커밋되어_FAILED로_남는다() throws Exception {
        User user = userRepository.findByLoginCode(LOGIN).orElseThrow();
        user.changeConsent(true); // 원본(canvas·cells)이 저장되는 계정으로
        userRepository.save(user);
        when(engineClient.grade(any())).thenThrow(new BusinessException(ErrorCode.ENGINE_TIMEOUT));

        submit().andExpect(status().isGatewayTimeout());

        Long userId = userRepository.findByLoginCode(LOGIN).orElseThrow().getId();
        List<Writing> mine = writingRepository.findAll().stream().filter(w -> w.getUserId().equals(userId)).toList();
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getStatus()).isEqualTo(WritingStatus.FAILED);
        assertThat(mine.get(0).getGrade()).isNull();
        assertThat(mine.get(0).getCells()).isNotNull();
        assertThat(mine.get(0).getCanvas()).isNotNull();
    }

    @Test
    void 예상_밖_오류도_FAILED로_남긴다() throws Exception {
        when(engineClient.grade(any())).thenThrow(new IllegalStateException("boom"));

        submit().andExpect(status().isInternalServerError());

        Long userId = userRepository.findByLoginCode(LOGIN).orElseThrow().getId();
        assertThat(writingRepository.findAll().stream().filter(w -> w.getUserId().equals(userId)))
                .singleElement().extracting(Writing::getStatus).isEqualTo(WritingStatus.FAILED);
    }

    private org.springframework.test.web.servlet.ResultActions submit() throws Exception {
        String token = bearer(LOGIN);
        String list = mockMvc.perform(get("/api/v1/contents").param("mode", "kpop").param("stage", "beginner")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        long contentId = objectMapper.readTree(list).get("items").get(0).get("id").asLong();
        return mockMvc.perform(post("/api/v1/writings").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentId\":" + contentId + ",\"stage\":\"beginner\",\"canvas\":{\"w\":288,\"h\":288},"
                        + "\"cells\":[{\"index\":0,\"strokes\":[{\"points\":[[1,2,0,0.5]]}]},{\"index\":1,\"strokes\":[]}]}"));
    }
}
