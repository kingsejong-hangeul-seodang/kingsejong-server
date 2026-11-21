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
        List<LevelInfo> levels,
        int streakDays,
        TodayResponse today
) {

    /** 레벨 한 단계 (마이페이지 "킹세종이 자라는 모습"). 레벨 정책 클래스와 API 모양을 분리한다. */
    public record LevelInfo(int no, String name, int minExp) {

        static LevelInfo from(LevelPolicy.LevelDef level) {
            return new LevelInfo(level.no(), level.name(), level.minExp());
        }
    }

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
                LevelPolicy.LEVELS.stream().map(LevelInfo::from).toList(),
                user.currentStreakDays(today),
                TodayResponse.of(todayDone)
        );
    }
}
