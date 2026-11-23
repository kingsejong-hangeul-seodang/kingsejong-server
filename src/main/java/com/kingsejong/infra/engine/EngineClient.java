package com.kingsejong.infra.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;

/**
 * 교정 엔진(FastAPI) 호출. 주소는 app.engine.base-url (환경 변수 ENGINE_BASE_URL).
 *
 * 오류 옮기기 (API 명세 5번):
 * - 엔진이 4xx·5xx를 돌려줌 → 502 ENGINE_ERROR (엔진 422는 서버가 요청을 잘못 만든 것이라 응답 본문을 로그로 남김)
 * - 연결 실패·연결 제한 시간 초과 → 503 ENGINE_UNAVAILABLE
 * - 응답 제한 시간 초과 → 504 ENGINE_TIMEOUT
 */
@Slf4j
@Component
public class EngineClient {

    /** 헬스체크는 짧게 기다린다 (엔진이 멈춰도 /health가 오래 걸리지 않게). */
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);

    private final RestClient gradeClient;
    private final RestClient healthClient;

    public EngineClient(@Value("${app.engine.base-url}") String baseUrl,
                        @Value("${app.engine.connect-timeout:2s}") Duration connectTimeout,
                        @Value("${app.engine.read-timeout:10s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.gradeClient = build(baseUrl, httpClient, readTimeout);
        this.healthClient = build(baseUrl, httpClient, HEALTH_TIMEOUT);
    }

    private static RestClient build(String baseUrl, HttpClient httpClient, Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** 판정 응답은 바꾸지 않고 그대로 프론트에 넘기므로 JsonNode로 받는다. */
    public JsonNode grade(EngineGradeRequest request) {
        try {
            JsonNode body = gradeClient.post()
                    .uri("/grade")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.path("correct").isBoolean() || !body.path("perfect").isBoolean()) {
                log.error("Engine returned an unexpected body: {}", body);
                throw new BusinessException(ErrorCode.ENGINE_ERROR);
            }
            return body;
        } catch (RestClientResponseException e) {
            log.error("Engine error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.ENGINE_ERROR);
        } catch (ResourceAccessException e) {
            throw toAccessError(e);
        } catch (RestClientException e) {
            log.error("Engine call failed", e);
            throw new BusinessException(ErrorCode.ENGINE_ERROR);
        }
    }

    /**
     * 글자 하나의 정답 획 (엔진 GET /reference/{글자}). 엔진 응답의 strokes 배열을 그대로 준다.
     * 정답 획 사전에 없는 글자면 Optional.empty(). 엔진에 사전이 없으면(503) ENGINE_UNAVAILABLE.
     */
    public Optional<JsonNode> reference(String ch) {
        try {
            JsonNode body = gradeClient.get()
                    .uri(builder -> builder.path("/reference/{char}").build(ch))
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.path("strokes").isArray()) {
                log.error("Engine returned an unexpected reference body: {}", body);
                throw new BusinessException(ErrorCode.ENGINE_ERROR);
            }
            return Optional.of(body.get("strokes"));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.error("Engine reference error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(e.getStatusCode().value() == 503
                    ? ErrorCode.ENGINE_UNAVAILABLE : ErrorCode.ENGINE_ERROR);
        } catch (ResourceAccessException e) {
            throw toAccessError(e);
        } catch (RestClientException e) {
            log.error("Engine reference call failed", e);
            throw new BusinessException(ErrorCode.ENGINE_ERROR);
        }
    }

    /** 엔진 상태 { status, engineVersion }. 연결이 안 되면 null (헬스체크용이라 예외를 던지지 않음). */
    public JsonNode health() {
        try {
            return healthClient.get().uri("/health").retrieve().body(JsonNode.class);
        } catch (RuntimeException e) {
            // 헬스체크는 어떤 이유로든 실패하면 down (명세상 /health는 항상 200)
            log.warn("Engine health check failed: {}", e.getMessage());
            return null;
        }
    }

    static BusinessException toAccessError(ResourceAccessException e) {
        Throwable cause = e.getCause();
        // HttpConnectTimeoutException은 HttpTimeoutException의 하위라 먼저 본다
        if (cause instanceof HttpConnectTimeoutException) {
            log.error("Engine connect timeout: {}", e.getMessage());
            return new BusinessException(ErrorCode.ENGINE_UNAVAILABLE);
        }
        if (cause instanceof HttpTimeoutException) {
            log.error("Engine read timeout: {}", e.getMessage());
            return new BusinessException(ErrorCode.ENGINE_TIMEOUT);
        }
        log.error("Engine unavailable: {}", e.getMessage());
        return new BusinessException(ErrorCode.ENGINE_UNAVAILABLE);
    }
}
