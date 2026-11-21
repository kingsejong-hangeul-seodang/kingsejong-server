package com.kingsejong.domain.user.controller;

import com.kingsejong.domain.user.dto.request.LoginRequest;
import com.kingsejong.domain.user.dto.response.LoginResponse;
import com.kingsejong.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "로그인")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "로그인", description = "로그인 코드로 로그인하고 토큰과 내 정보를 받는다. 인증 없이 부른다.")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
