package com.kingsejong.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "로그인 코드를 입력해 주세요")
        @Size(max = 30, message = "로그인 코드는 30자 이하예요")
        String code
) {
}
