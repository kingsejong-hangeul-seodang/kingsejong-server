package com.kingsejong.global.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 배포 확인용 헬스체크 (인증 없음).
 * 교정 엔진 상태(engine)는 엔진 연동 이슈에서 추가한다. DB 포함 상세 상태는 /actuator/health.
 */
@Tag(name = "Health", description = "서버 상태 확인")
@RestController
public class HealthController {

    @Operation(summary = "헬스체크", description = "서버가 살아 있는지 확인한다.")
    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
