package com.kingsejong.domain.leveltest.controller;

import com.kingsejong.domain.leveltest.dto.LevelStepRequest;
import com.kingsejong.domain.leveltest.dto.LevelStepResponse;
import com.kingsejong.domain.leveltest.dto.LevelTestResponse;
import com.kingsejong.domain.leveltest.service.LevelTestService;
import com.kingsejong.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LevelTest", description = "레벨 테스트 (온보딩)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/level-test")
public class LevelTestController {

    private final LevelTestService levelTestService;

    @Operation(summary = "레벨 테스트 문항", description = "3단계, 단계마다 원고지 한 장")
    @GetMapping
    public LevelTestResponse getSteps(@Parameter(hidden = true) @LoginUser Long userId) {
        return levelTestService.getSteps();
    }

    @Operation(summary = "한 단계 제출", description = "엔진으로 채점. 통과 못 했거나 3단계면 finished + recommendedStage. 순서대로만 (1단계는 처음부터)")
    @PostMapping("/steps/{step}")
    public LevelStepResponse submit(@Parameter(hidden = true) @LoginUser Long userId,
                                    @PathVariable int step,
                                    @Valid @RequestBody LevelStepRequest request) {
        return levelTestService.submit(userId, step, request);
    }
}
