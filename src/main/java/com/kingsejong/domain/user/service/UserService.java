package com.kingsejong.domain.user.service;

import com.kingsejong.domain.user.dto.request.LoginRequest;
import com.kingsejong.domain.user.dto.response.LoginResponse;
import com.kingsejong.domain.user.dto.response.MeResponse;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.user.repository.UserRepository;
import com.kingsejong.global.auth.JwtProvider;
import com.kingsejong.global.common.KstDate;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginCode(request.code().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CODE));
        return new LoginResponse(jwtProvider.create(user.getId()), toMe(user));
    }

    /** 다른 도메인에서 사용자 엔티티가 필요할 때. 토큰은 맞는데 계정이 지워졌으면 401. */
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private MeResponse toMe(User user) {
        // today.done(오늘 맞게 쓴 콘텐츠 수)은 쓰기 제출(#7)에서 채운다
        return MeResponse.of(user, KstDate.today(), 0);
    }
}
