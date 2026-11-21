package com.kingsejong.domain.user.dto.response;

/** 일일 챌린지: 오늘 맞게 쓴 서로 다른 콘텐츠 수 / 목표. */
public record TodayResponse(int done, int goal) {

    public static final int GOAL = 3;

    public static TodayResponse of(int done) {
        return new TodayResponse(done, GOAL);
    }
}
