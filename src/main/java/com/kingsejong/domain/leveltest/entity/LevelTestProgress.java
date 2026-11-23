package com.kingsejong.domain.leveltest.entity;

import com.kingsejong.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 마지막 레벨 테스트의 단계 결과 (API 명세 3.15). null = 아직 안 푼 단계.
 * 1단계를 다시 제출하면 처음부터 다시 시작한다.
 */
@Getter
@Entity
@Table(name = "level_test_progress")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelTestProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "step1_passed")
    private Boolean step1Passed;

    @Column(name = "step2_passed")
    private Boolean step2Passed;

    @Column(name = "step3_passed")
    private Boolean step3Passed;

    public LevelTestProgress(Long userId) {
        this.userId = userId;
    }

    public Boolean passedAt(int step) {
        return switch (step) {
            case 1 -> step1Passed;
            case 2 -> step2Passed;
            case 3 -> step3Passed;
            default -> throw new IllegalArgumentException("step " + step);
        };
    }

    /** 이 단계의 결과를 남긴다. 1단계면 처음부터, 뒤 단계 결과는 지운다. */
    public void record(int step, boolean passed) {
        if (step == 1) {
            step2Passed = null;
            step3Passed = null;
            step1Passed = passed;
        } else if (step == 2) {
            step3Passed = null;
            step2Passed = passed;
        } else {
            step3Passed = passed;
        }
    }
}
