package com.kingsejong.domain.user.dto.response;

/** 로그인 응답 (API 명세 3.1). */
public record LoginResponse(String accessToken, MeResponse me) {
}
