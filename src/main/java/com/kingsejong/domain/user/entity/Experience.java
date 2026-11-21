package com.kingsejong.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.kingsejong.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 한글 경험 (온보딩). */
@Getter
@RequiredArgsConstructor
public enum Experience implements CodeEnum {

    FIRST("first"),      // 처음 배워요
    STUDIED("studied");  // 공부해 봤어요

    private final String code;

    @JsonCreator
    public static Experience from(String code) {
        return CodeEnum.fromCode(Experience.class, code);
    }
}
