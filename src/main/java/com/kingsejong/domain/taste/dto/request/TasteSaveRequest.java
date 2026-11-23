package com.kingsejong.domain.taste.dto.request;

import com.kingsejong.global.common.Mode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 그 모드의 취향 선택을 통째로 바꾼다 (API 명세 3.5). 빈 목록이면 그 모드 선택을 모두 지운다. */
public record TasteSaveRequest(
        @NotNull(message = "필수 값이에요")
        Mode mode,

        @NotNull(message = "필수 값이에요")
        @Size(max = 50, message = "50개까지 고를 수 있어요")
        List<@NotNull Long> tasteIds
) {
}
