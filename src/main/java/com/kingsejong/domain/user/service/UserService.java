package com.kingsejong.domain.user.service;

import com.kingsejong.domain.user.dto.request.LoginRequest;
import com.kingsejong.domain.user.dto.request.MeUpdateRequest;
import com.kingsejong.domain.user.dto.response.LoginResponse;
import com.kingsejong.domain.user.dto.response.MeResponse;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.user.repository.UserRepository;
import com.kingsejong.domain.writing.service.WritingStatsService;
import com.kingsejong.global.auth.JwtProvider;
import com.kingsejong.global.common.KstDate;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final WritingStatsService writingStatsService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginCode(request.code().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CODE));
        return new LoginResponse(jwtProvider.create(user.getId()), toMe(user));
    }

    public MeResponse getMe(Long userId) {
        return toMe(getUser(userId));
    }

    /** 보낸 필드만 바꾼다 (null = 안 바꿈). */
    @Transactional
    public MeResponse updateMe(Long userId, MeUpdateRequest request) {
        User user = getUser(userId);
        if (request.locale() != null) {
            user.changeLocale(request.locale());
        }
        if (request.consentAgreed() != null) {
            user.changeConsent(request.consentAgreed());
        }
        if (request.experience() != null) {
            user.changeExperience(request.experience());
        }
        if (request.stage() != null) {
            user.changeStage(request.stage());
        }
        if (request.lastMode() != null) {
            user.changeLastMode(request.lastMode());
        }
        if (Boolean.TRUE.equals(request.onboardingCompleted())) {
            user.completeOnboarding();
        }
        return toMe(user);
    }

    /**
     * 사용자 행을 잠그고 읽는다. 호출한 트랜잭션이 끝날 때까지 같은 사용자의 다른 보상 계산은 기다린다.
     * 그 트랜잭션에서 사용자를 처음 읽는 호출이어야 잠금 후의 최신 값을 본다.
     */
    @Transactional
    public User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /** 다른 도메인에서 사용자 엔티티가 필요할 때. 토큰은 맞는데 계정이 지워졌으면 401. */
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private MeResponse toMe(User user) {
        LocalDate today = KstDate.today();
        return MeResponse.of(user, today, writingStatsService.countTodayCorrectContents(user.getId(), today));
    }
}
