package com.kingsejong.domain.leveltest.service;

import com.kingsejong.global.common.Stage;

import java.util.Arrays;
import java.util.List;

/**
 * 레벨 테스트 문항 3단계 (API 명세 3.14). 단계마다 원고지 한 장, 가이드 글자 없음.
 * stage는 그 단계를 엔진에 채점시킬 때 쓰는 난이도. 3단계 칸 수는 피그마(leveltest-3)의 9칸.
 */
public final class LevelTestCatalog {

    public record Step(int step, String title, Stage stage, String target, int cellCount) {

        public List<Integer> wordGroups() {
            return Arrays.stream(target.split(" ")).map(String::length).toList();
        }
    }

    public static final List<Step> STEPS = List.of(
            new Step(1, "가나다 쓰기", Stage.BEGINNER, "가나다", 3),
            new Step(2, "단어 쓰기", Stage.INTERMEDIATE, "바다", 2),
            new Step(3, "짧은 문장 쓰기", Stage.ADVANCED, "바다 보러 가요", 9)
    );

    private LevelTestCatalog() {
    }

    public static Step get(int step) {
        return STEPS.get(step - 1);
    }
}
