package com.kingsejong.global.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 학습 모드 (홈 탭). */
@Getter
@RequiredArgsConstructor
public enum Mode implements CodeEnum {

    KPOP("kpop"),
    KDRAMA("kdrama"),
    BASIC("basic");

    private final String code;

    @JsonCreator
    public static Mode from(String code) {
        return CodeEnum.fromCode(Mode.class, code);
    }
}
