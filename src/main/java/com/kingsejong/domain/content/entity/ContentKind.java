package com.kingsejong.domain.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.kingsejong.global.common.CodeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 콘텐츠 종류. 기본 연습은 clip이 아님. */
@Getter
@RequiredArgsConstructor
public enum ContentKind implements CodeEnum {

    CLIP("clip"),          // 영상 있음 (K팝·K드라마)
    JAMO("jamo"),          // 자모 연습
    WORD("word"),          // 단어
    SENTENCE("sentence");  // 문장

    private final String code;

    @JsonCreator
    public static ContentKind from(String code) {
        return CodeEnum.fromCode(ContentKind.class, code);
    }
}
