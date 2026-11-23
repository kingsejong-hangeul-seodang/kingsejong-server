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
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 가짜 엔진(JDK HttpServer)을 띄워서 요청 모양과 오류 옮기기를 확인한다. */
class EngineClientTest {

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String response = "{}";
    private volatile long delayMs = 0;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastPath.set(exchange.getRequestURI().getPath());
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

    @Test
    void 엔진이_멈춰도_헬스체크는_2초대에_돌아온다() {
        delayMs = 3000;
        response = "{\"status\":\"ok\"}";
        long start = System.nanoTime();
        JsonNode result = client(Duration.ofSeconds(10)).health();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(result).isNull();
        assertThat(elapsedMs).isLessThan(3_000); // 엔진은 3초 뒤에 답하므로, 그 전에 돌아왔다는 뜻
    }

    @Test
    void 엔진_헬스가_500이면_null() {
        status = 500;
        assertThat(client(Duration.ofSeconds(2)).health()).isNull();
    }

    @Test
    void 상태는_200인데_JSON이_아니면_ENGINE_ERROR() {
        response = "<html>oops</html>";
        assertError(client(Duration.ofSeconds(2)), ErrorCode.ENGINE_ERROR);
    }

    @Test
    void 연결_제한_시간_초과는_503_응답_제한_시간_초과는_504() {
        assertThat(EngineClient.toAccessError(new ResourceAccessException("x",
                new HttpConnectTimeoutException("connect"))).getErrorCode()).isEqualTo(ErrorCode.ENGINE_UNAVAILABLE);
        assertThat(EngineClient.toAccessError(new ResourceAccessException("x",
                new HttpTimeoutException("read"))).getErrorCode()).isEqualTo(ErrorCode.ENGINE_TIMEOUT);
        assertThat(EngineClient.toAccessError(new ResourceAccessException("x",
                new java.net.ConnectException("refused"))).getErrorCode()).isEqualTo(ErrorCode.ENGINE_UNAVAILABLE);
    }

    @Test
    void 정답_획은_글자를_경로에_넣어_묻고_strokes를_준다() {
        response = "{\"char\":\"다\",\"strokes\":[{\"order\":1,\"points\":[[0.1,0.2],[0.5,0.2]]}],\"engineVersion\":\"t\"}";
        JsonNode strokes = client(Duration.ofSeconds(2)).reference("다").orElseThrow();
        assertThat(lastPath.get()).isEqualTo("/reference/다");
        assertThat(strokes.get(0).get("order").asInt()).isEqualTo(1);
    }

    @Test
    void 정답_획_사전에_없으면_빈_값() {
        status = 404;
        response = "{\"code\":\"NOT_FOUND\",\"message\":\"x\",\"details\":[],\"engineVersion\":\"t\"}";
        assertThat(client(Duration.ofSeconds(2)).reference("뷁")).isEmpty();
    }

    @Test
    void 엔진에_정답_획_사전이_없으면_ENGINE_UNAVAILABLE() {
        status = 503;
        response = "{\"code\":\"REFERENCE_UNAVAILABLE\",\"message\":\"x\",\"details\":[],\"engineVersion\":\"t\"}";
        assertThatThrownBy(() -> client(Duration.ofSeconds(2)).reference("다"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENGINE_UNAVAILABLE);
    }

    private void assertError(EngineClient client, ErrorCode expected) {
        assertThatThrownBy(() -> client.grade(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
