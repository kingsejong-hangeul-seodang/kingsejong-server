package com.kingsejong.domain.writing.dto.request;

import com.kingsejong.global.common.Stage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 쓰기 제출 (API 명세 3.8). canvas·cells는 엔진 요청과 같은 모양이라 서버가 그대로 넘긴다.
 * 정답 문장은 보내지 않는다 (서버가 contentId + stage로 찾는다).
 * 개수 상한은 엔진이 검사하지 않아서 서버가 막는다 (칸 30, 칸당 획 60, 획당 점 2,000).
 */
public record WritingCreateRequest(
        @NotNull(message = "필수 값이에요")
        Long contentId,

        @NotNull(message = "필수 값이에요")
        Stage stage,

        @NotNull(message = "필수 값이에요")
        @Valid
        Canvas canvas,

        @NotNull(message = "필수 값이에요")
        @Size(min = 1, max = 30, message = "칸은 1~30개예요")
        List<@NotNull @Valid Cell> cells
) {

    /** 칸 하나의 크기. 점 좌표와 같은 단위 (지금 프론트는 288). */
    public record Canvas(
            @NotNull @Positive Double w,
            @NotNull @Positive Double h
    ) {
    }

    /** 원고지 칸 하나. 빈 칸이면 strokes = []. */
    public record Cell(
            @NotNull @PositiveOrZero Integer index,
            @NotNull @Size(max = 60, message = "칸 하나에 획은 60개까지예요")
            List<@NotNull @Valid Stroke> strokes
    ) {
    }

    /** 획 하나. 점은 [x, y, t, p]. */
    public record Stroke(
            @NotNull
            @Size(min = 1, max = 2000, message = "획 하나에 점은 1~2,000개예요")
            List<@NotNull @Size(min = 4, max = 4, message = "점은 [x, y, t, p] 숫자 4개예요") List<@NotNull Double>> points
    ) {
    }
}
