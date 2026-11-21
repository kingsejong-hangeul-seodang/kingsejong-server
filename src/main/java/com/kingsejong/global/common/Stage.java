package com.kingsejong.global.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 난이도. 값은 교정 엔진의 stage와 같다. 쓰는 방식(writeMode)은 난이도로 정해진다. */
@Getter
@RequiredArgsConstructor
public enum Stage implements CodeEnum {

    BEGINNER("beginner", "trace"),         // 초보: 회색 글자를 따라 씀
    INTERMEDIATE("intermediate", "copy"),  // 중수: 글을 보고 씀
    ADVANCED("advanced", "dictation");     // 고수: 듣고 받아씀

    private final String code;
    private final String writeMode;

    @JsonCreator
    public static Stage from(String code) {
        return CodeEnum.fromCode(Stage.class, code);
    }
}
