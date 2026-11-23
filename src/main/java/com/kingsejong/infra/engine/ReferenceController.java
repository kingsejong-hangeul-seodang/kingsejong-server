package com.kingsejong.infra.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.global.auth.LoginUser;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReferenceController {

    private final EngineClient engineClient;

    @GetMapping("/api/v1/reference/strokes")
    public JsonNode strokes(@LoginUser Long userId, @RequestParam @Size(min = 1, max = 30) String chars) {
        return engineClient.reference(chars);
    }
}
