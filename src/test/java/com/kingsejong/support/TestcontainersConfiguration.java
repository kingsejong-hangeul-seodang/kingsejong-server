package com.kingsejong.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 테스트용 PostgreSQL. 테스트할 때 컨테이너를 띄우고 datasource 를 자동으로 연결한다.
 * docker-compose 의 로컬 DB 와 별개라서, 로컬 DB 를 안 띄워도 테스트는 돈다 (Docker 는 필요).
 *
 * 사용: @Import(TestcontainersConfiguration.class)
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
