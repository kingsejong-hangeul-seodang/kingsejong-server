package com.kingsejong.global.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.infra.engine.EngineClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 배포 확인용 헬스체크 (인증 없음, API 명세 3.16). DB 포함 상세 상태는 /actuator/health.
 * 엔진에 연결이 안 돼도 HTTP는 200이고 engine.status만 "down".
 *
 * 인증 없는 주소라 호출마다 엔진을 부르면, 엔진이 멈췄을 때 요청마다 스레드를 붙잡고 엔진에도 부하가 간다.
 * 그래서 엔진 상태는 5초 동안 기억해 두고 그 뒤에만 다시 확인한다.
 */
@Tag(name = "Health", description = "서버 상태 확인")
@RestController
@RequiredArgsConstructor
public class HealthController {

    static final Duration ENGINE_CHECK_INTERVAL = Duration.ofSeconds(5);

    private final EngineClient engineClient;

    private record Cached(EngineStatus status, long checkedAtNanos) {
    }

    private volatile Cached cached;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    public record HealthResponse(String status, EngineStatus engine) {
    }

    /** engineVersion이 "-stub"으로 끝나면 엔진이 임시 응답(모든 칸 맞음)을 주는 중. */
    public record EngineStatus(String status, String engineVersion) {
    }

    @Operation(summary = "헬스체크", description = "서버와 교정 엔진이 살아 있는지 확인한다. 엔진 상태는 최대 5초 전 값.")
    @GetMapping("/api/v1/health")
    public HealthResponse health() {
        return new HealthResponse("ok", engineStatus());
    }

    /**
     * 5초 안에 확인한 값이 있으면 그대로 준다. 지나면 요청 하나만 엔진을 확인하고,
     * 그동안 들어온 다른 요청은 기다리지 않고 마지막 값(처음이면 down)을 받는다.
     */
    private EngineStatus engineStatus() {
        Cached current = cached;
        if (current != null && System.nanoTime() - current.checkedAtNanos() < ENGINE_CHECK_INTERVAL.toNanos()) {
            return current.status();
        }
        if (!refreshing.compareAndSet(false, true)) {
            return current != null ? current.status() : new EngineStatus("down", null);
        }
        try {
            JsonNode engine = engineClient.health();
            EngineStatus status = engine != null
                    ? new EngineStatus("ok", engine.path("engineVersion").asText(null))
                    : new EngineStatus("down", null);
            cached = new Cached(status, System.nanoTime());
            return status;
        } finally {
            refreshing.set(false);
        }
    }
}
