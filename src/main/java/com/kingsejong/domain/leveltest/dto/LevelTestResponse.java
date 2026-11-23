package com.kingsejong.domain.leveltest.dto;

import com.kingsejong.global.common.Stage;

import java.util.List;

/** 레벨 테스트 문항 (API 명세 3.14). */
public record LevelTestResponse(List<Step> steps) {

    public record Step(int step, String title, Stage stage, String target, int cellCount, List<Integer> wordGroups) {
    }
}
