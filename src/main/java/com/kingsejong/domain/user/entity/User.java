package com.kingsejong.domain.user.entity;

import com.kingsejong.global.common.BaseTimeEntity;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import org.hibernate.annotations.DynamicUpdate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 사용자. 회원가입은 없고, 계정은 시드(seed/users.json)나 DB에 직접 넣는다. 로그인은 loginCode 하나로.
 */
@Getter
@Entity
@DynamicUpdate // 바뀐 컬럼만 UPDATE (내 정보 수정이 보상으로 바뀐 경험치를 옛 값으로 덮어쓰지 않게)
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "login_code", nullable = false, unique = true, length = 30)
    private String loginCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Locale locale;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Experience experience;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Stage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_mode", length = 10)
    private Mode lastMode;

    @Column(name = "consent_agreed", nullable = false)
    private boolean consentAgreed;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    /** 누적 경험치. 레벨은 이 값으로 계산한다 (LevelPolicy). */
    @Column(nullable = false)
    private int exp;

    /** 연속 학습 일수와 마지막으로 제출한 날 (한국 날짜). */
    @Column(name = "streak_days", nullable = false)
    private int streakDays;

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate;

    /** 같은 난이도에서 연속으로 맞은 횟수 (난이도 올리기 추천용). 난이도를 바꾸면 0. */
    @Column(name = "correct_streak", nullable = false)
    private int correctStreak;

    @Builder
    private User(String name, String loginCode, Locale locale, Experience experience, Stage stage,
                 Mode lastMode, boolean consentAgreed, boolean onboardingCompleted) {
        this.name = name;
        this.loginCode = loginCode;
        this.locale = locale != null ? locale : Locale.KO;
        this.experience = experience;
        this.stage = stage;
        this.lastMode = lastMode;
        this.consentAgreed = consentAgreed;
        this.onboardingCompleted = onboardingCompleted;
    }

    // ===== 내 정보 수정 (PATCH /me) =====

    public void changeLocale(Locale locale) {
        this.locale = locale;
    }

    public void changeConsent(boolean consentAgreed) {
        this.consentAgreed = consentAgreed;
    }

    /** "처음 배워요"면 난이도를 초보로 같이 정한다. */
    public void changeExperience(Experience experience) {
        this.experience = experience;
        if (experience == Experience.FIRST) {
            changeStage(Stage.BEGINNER);
        }
    }

    /** 난이도를 바꾸면 연속 정답 카운트를 0으로 되돌린다. */
    public void changeStage(Stage stage) {
        if (this.stage != stage) {
            this.correctStreak = 0;
        }
        this.stage = stage;
    }

    public void changeLastMode(Mode lastMode) {
        this.lastMode = lastMode;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    // ===== 쓰기 제출 뒤 보상 =====

    public void addExp(int amount) {
        this.exp += amount;
    }

    /** 그날 처음 제출하면 연속 학습을 이어 가거나(어제 했으면) 1로 다시 시작한다. */
    public void recordStudy(LocalDate today) {
        // 같은 날이거나 더 이전 날짜면 그대로 (자정을 걸친 제출의 보상이 늦게 와도 되돌아가지 않게)
        if (lastStudyDate != null && !today.isAfter(lastStudyDate)) {
            return;
        }
        this.streakDays = today.minusDays(1).equals(lastStudyDate) ? streakDays + 1 : 1;
        this.lastStudyDate = today;
    }

    /** 오늘 기준으로 보이는 연속 학습 일수. 어제도 오늘도 안 했으면 끊긴 것이라 0. */
    public int currentStreakDays(LocalDate today) {
        if (lastStudyDate == null || lastStudyDate.isBefore(today.minusDays(1))) {
            return 0;
        }
        return streakDays;
    }

    /**
     * 연속 정답 카운트를 갱신하고, 이번 제출로 기준(threshold)을 채웠으면 true를 돌려주며 0으로 되돌린다.
     */
    public boolean countCorrect(boolean correct, int threshold) {
        if (!correct) {
            this.correctStreak = 0;
            return false;
        }
        this.correctStreak++;
        if (correctStreak >= threshold) {
            this.correctStreak = 0;
            return true;
        }
        return false;
    }
}
