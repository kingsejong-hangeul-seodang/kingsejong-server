package com.kingsejong.global.common;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * API에서 소문자 값("beginner", "kpop")으로 주고받는 enum.
 * JSON 응답은 code로 나가고, 쿼리 파라미터는 WebConfig의 변환기가 code로 읽는다.
 * DB에는 enum 이름(BEGINNER)으로 저장한다.
 */
public interface CodeEnum {

    @JsonValue
    String getCode();

    static <E extends Enum<E> & CodeEnum> E fromCode(Class<E> type, String code) {
        return Arrays.stream(type.getEnumConstants())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "'" + code + "'은(는) 올바른 값이 아니에요"));
    }
}
