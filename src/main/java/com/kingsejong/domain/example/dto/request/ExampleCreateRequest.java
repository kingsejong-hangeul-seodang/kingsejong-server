package com.kingsejong.domain.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 요청 DTO 는 record 로 쓴다 (불변, 코드 짧음). 검증은 jakarta validation.
 */
public record ExampleCreateRequest(

        @NotBlank(message = "name은 필수예요")
        @Size(max = 20, message = "name은 20자 이하여야 해요")
        String name
) {
}
