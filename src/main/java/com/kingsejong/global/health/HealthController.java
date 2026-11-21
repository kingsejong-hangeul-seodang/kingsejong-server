package com.kingsejong.global.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.infra.engine.EngineClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배포 확인용 헬스체크 (인증 없음, API 명세 3.16). DB 포함 상세 상태는 /actuator/health.
 * 엔진에 연결이 안 돼도 HTTP는 200이고 engine.status만 "down".
 */
@Tag(name = "Health", description = "서버 상태 확인")
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final EngineClient engineClient;

    public record HealthResponse(String status, EngineStatus engine) {
    }

    /** engineVersion이 "-stub"으로 끝나면 엔진이 임시 응답(모든 칸 맞음)을 주는 중. */
    public record EngineStatus(String status, String engineVersion) {
    }

    @Operation(summary = "헬스체크", description = "서버와 교정 엔진이 살아 있는지 확인한다.")
    @GetMapping("/api/v1/health")
    public HealthResponse health() {
        JsonNode engine = engineClient.health();
        EngineStatus engineStatus = engine != null
                ? new EngineStatus("ok", engine.path("engineVersion").asText(null))
                : new EngineStatus("down", null);
        return new HealthResponse("ok", engineStatus);
    }
}
