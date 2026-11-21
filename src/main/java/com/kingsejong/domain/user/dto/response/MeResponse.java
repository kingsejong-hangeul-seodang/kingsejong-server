package com.kingsejong.domain.user.dto.response;

import com.kingsejong.domain.user.entity.Experience;
import com.kingsejong.domain.user.entity.LevelPolicy;
import com.kingsejong.domain.user.entity.Locale;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;

import java.time.LocalDate;
import java.util.List;

/** 내 정보 (API 명세 3.2). */
public record MeResponse(
        Long id,
        String name,
        Locale locale,
        Experience experience,
        Stage stage,
        Mode lastMode,
        boolean consentAgreed,
        boolean onboardingCompleted,
        String loginCode,
        LevelResponse level,
        List<LevelPolicy.LevelDef> levels,
        int streakDays,
        TodayResponse today
) {

    public static MeResponse of(User user, LocalDate today, int todayDone) {
        return new MeResponse(
                user.getId(),
                user.getName(),
                user.getLocale(),
                user.getExperience(),
                user.getStage(),
                user.getLastMode(),
                user.isConsentAgreed(),
                user.isOnboardingCompleted(),
                user.getLoginCode(),
                LevelResponse.of(user.getExp()),
                LevelPolicy.LEVELS,
                user.currentStreakDays(today),
                TodayResponse.of(todayDone)
        );
    }
}
