package com.kingsejong.infra.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 가짜 엔진(JDK HttpServer)을 띄워서 요청 모양과 오류 옮기기를 확인한다. */
class EngineClientTest {

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String response = "{}";
    private volatile long delayMs = 0;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private EngineClient client(Duration readTimeout) {
        return new EngineClient("http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(1), readTimeout);
    }

    private EngineGradeRequest request() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        return new EngineGradeRequest("1", "beginner", "바다", null,
                f.objectNode().put("w", 288).put("h", 288), f.arrayNode());
    }

    @Test
    void 판정_응답을_그대로_돌려주고_null_필드는_보내지_않는다() throws Exception {
        response = "{\"engineVersion\":\"0.1.0-stub\",\"correct\":true,\"perfect\":false,\"cells\":[]}";

        JsonNode grade = client(Duration.ofSeconds(2)).grade(request());

        assertThat(grade.get("engineVersion").asText()).isEqualTo("0.1.0-stub");
        assertThat(grade.get("perfect").asBoolean()).isFalse();
        JsonNode sent = new ObjectMapper().readTree(lastBody.get());
        assertThat(sent.has("locale")).isFalse();
        assertThat(sent.get("sessionId").asText()).isEqualTo("1");
        assertThat(sent.get("canvas").get("w").asInt()).isEqualTo(288);
    }

    @Test
    void 엔진이_422를_주면_ENGINE_ERROR() {
        status = 422;
        response = "{\"code\":\"INVALID_REQUEST\",\"message\":\"x\",\"details\":[],\"engineVersion\":\"0.1.0\"}";
        assertError(client(Duration.ofSeconds(2)), ErrorCode.ENGINE_ERROR);
    }

    @Test
    void 엔진이_500을_주면_ENGINE_ERROR() {
        status = 500;
        assertError(client(Duration.ofSeconds(2)), ErrorCode.ENGINE_ERROR);
    }

    @Test
    void 응답_모양이_이상하면_ENGINE_ERROR() {
        response = "{\"hello\":1}";
        assertError(client(Duration.ofSeconds(2)), ErrorCode.ENGINE_ERROR);
    }

    @Test
    void 응답이_늦으면_ENGINE_TIMEOUT() {
        delayMs = 1500;
        response = "{\"correct\":true,\"perfect\":true}";
        assertError(client(Duration.ofMillis(300)), ErrorCode.ENGINE_TIMEOUT);
    }

    @Test
    void 연결이_안_되면_ENGINE_UNAVAILABLE() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        EngineClient down = new EngineClient("http://127.0.0.1:" + closedPort, Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertThatThrownBy(() -> down.grade(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENGINE_UNAVAILABLE);
        assertThat(down.health()).isNull();
    }

    @Test
    void 헬스체크는_엔진_응답을_준다() {
        response = "{\"status\":\"ok\",\"engineVersion\":\"0.1.0-stub\"}";
        assertThat(client(Duration.ofSeconds(2)).health().get("engineVersion").asText()).isEqualTo("0.1.0-stub");
    }

    private void assertError(EngineClient client, ErrorCode expected) {
        assertThatThrownBy(() -> client.grade(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
