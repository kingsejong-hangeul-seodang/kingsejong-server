package com.kingsejong.domain.user.controller;

import com.kingsejong.domain.user.dto.request.MeUpdateRequest;
import com.kingsejong.domain.user.dto.response.MeResponse;
import com.kingsejong.domain.user.service.UserService;
import com.kingsejong.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Me", description = "내 정보")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserService userService;

    @Operation(summary = "내 정보", description = "프로필, 레벨, 연속 학습, 오늘 진행")
    @GetMapping
    public MeResponse getMe(@Parameter(hidden = true) @LoginUser Long userId) {
        return userService.getMe(userId);
    }

    @Operation(summary = "내 정보 수정", description = "바꿀 필드만 보낸다. 응답은 내 정보 전체.")
    @PatchMapping
    public MeResponse updateMe(@Parameter(hidden = true) @LoginUser Long userId,
                               @Valid @RequestBody MeUpdateRequest request) {
        return userService.updateMe(userId, request);
    }
}
