package com.kingsejong.domain.user;

import com.kingsejong.domain.user.entity.User;
import com.kingsejong.global.common.Stage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** 연속 학습과 연속 정답 (날짜 경계). */
class UserTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 22);

    private User user() {
        return User.builder().name("t").loginCode("T").stage(Stage.BEGINNER).build();
    }

    @Test
    void 처음_제출하면_연속_학습_1일() {
        User user = user();
        user.recordStudy(DAY);
        assertThat(user.currentStreakDays(DAY)).isEqualTo(1);
    }

    @Test
    void 같은_날_여러_번_제출해도_1일() {
        User user = user();
        user.recordStudy(DAY);
        user.recordStudy(DAY);
        assertThat(user.currentStreakDays(DAY)).isEqualTo(1);
    }

    @Test
    void 어제에_이어서_하면_늘어난다() {
        User user = user();
        user.recordStudy(DAY.minusDays(1));
        user.recordStudy(DAY);
        assertThat(user.currentStreakDays(DAY)).isEqualTo(2);
    }

    @Test
    void 하루를_건너뛰면_1부터_다시() {
        User user = user();
        user.recordStudy(DAY.minusDays(3));
        user.recordStudy(DAY.minusDays(2));
        user.recordStudy(DAY);
        assertThat(user.currentStreakDays(DAY)).isEqualTo(1);
    }

    @Test
    void 오늘_아직_안_했어도_어제_했으면_이어진_것으로_보인다() {
        User user = user();
        user.recordStudy(DAY.minusDays(2));
        user.recordStudy(DAY.minusDays(1));
        assertThat(user.currentStreakDays(DAY)).isEqualTo(2);
    }

    @Test
    void 어제도_오늘도_안_했으면_0으로_보인다() {
        User user = user();
        user.recordStudy(DAY.minusDays(2));
        assertThat(user.currentStreakDays(DAY)).isZero();
    }

    @Test
    void 연속_정답이_기준에_닿으면_true를_주고_0으로_되돌린다() {
        User user = user();
        assertThat(user.countCorrect(true, 3)).isFalse();
        assertThat(user.countCorrect(true, 3)).isFalse();
        assertThat(user.countCorrect(true, 3)).isTrue();
        assertThat(user.getCorrectStreak()).isZero();
    }

    @Test
    void 난이도를_바꾸면_연속_정답이_0() {
        User user = user();
        user.countCorrect(true, 10);
        user.changeStage(Stage.INTERMEDIATE);
        assertThat(user.getCorrectStreak()).isZero();
    }
}
