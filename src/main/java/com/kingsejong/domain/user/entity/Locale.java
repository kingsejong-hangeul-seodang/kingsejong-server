package com.kingsejong.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.kingsejong.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 화면·설명 문장 언어. 값은 교정 엔진의 locale과 같다. */
@Getter
@RequiredArgsConstructor
public enum Locale implements CodeEnum {

    KO("ko"),
    EN("en");

    private final String code;

    @JsonCreator
    public static Locale from(String code) {
        return CodeEnum.fromCode(Locale.class, code);
    }
}
