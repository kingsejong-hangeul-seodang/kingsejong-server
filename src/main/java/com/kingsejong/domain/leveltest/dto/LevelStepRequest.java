package com.kingsejong.domain.leveltest.dto;

import com.kingsejong.domain.writing.dto.request.WritingCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 레벨 테스트 한 단계 제출 (API 명세 3.15). 원고지 모양과 상한은 쓰기 제출과 같다. */
public record LevelStepRequest(
        @NotNull(message = "필수 값이에요")
        @Valid
        WritingCreateRequest.Canvas canvas,

        @NotNull(message = "필수 값이에요")
        @Size(min = 1, max = 30, message = "칸은 1~30개예요")
        List<WritingCreateRequest.@NotNull @Valid Cell> cells
) {
}
