package com.kingsejong.domain.leveltest.dto;

import com.kingsejong.global.common.Stage;

import java.util.List;

/**
 * 한 단계 결과 (API 명세 3.15). finished면 결과 화면으로, recommendedStage는 finished일 때만.
 * steps는 지금까지 푼 단계 (안 푼 단계는 빠짐).
 */
public record LevelStepResponse(int step, boolean passed, boolean finished, Stage recommendedStage, List<StepResult> steps) {

    public record StepResult(int step, String title, boolean passed) {
    }
}
