package com.kingsejong.domain.user.dto.request;

import com.kingsejong.domain.user.entity.Experience;
import com.kingsejong.domain.user.entity.Locale;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import jakarta.validation.constraints.AssertTrue;

/** 내 정보 수정 (API 명세 3.3). 바꿀 필드만 보낸다 (없는 필드는 null = 안 바꿈). */
public record MeUpdateRequest(
        Locale locale,
        Boolean consentAgreed,
        Experience experience,
        Stage stage,
        Mode lastMode,
        @AssertTrue(message = "onboardingCompleted는 true만 보낼 수 있어요")
        Boolean onboardingCompleted
) {
}
