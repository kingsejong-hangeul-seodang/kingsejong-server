package com.kingsejong.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 공통. 실제 PostgreSQL(Testcontainers) + 시드 데이터(seed/*.json).
 * 테스트마다 롤백되므로 시드 계정을 마음대로 바꿔도 된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected EntityManager entityManager;

    /**
     * 지금까지의 변경을 DB에 쓰고 영속성 컨텍스트를 비운다.
     * 테스트가 한 트랜잭션이라 "저장이 안 되는" 버그(@Transactional 누락 등)를 놓치지 않으려면,
     * 변경 후 이걸 부르고 다시 조회해서 DB 값으로 확인한다.
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /** 시드 계정으로 로그인하고 "Bearer <토큰>"을 돌려준다. */
    protected String bearer(String loginCode) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + loginCode + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }
}
