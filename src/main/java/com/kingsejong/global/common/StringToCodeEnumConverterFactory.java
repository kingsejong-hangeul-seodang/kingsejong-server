package com.kingsejong.global.common;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/** 쿼리 파라미터 "?stage=beginner" → Stage.BEGINNER. 틀린 값이면 422 (타입 불일치). */
@SuppressWarnings({"unchecked", "rawtypes"})
public class StringToCodeEnumConverterFactory implements ConverterFactory<String, CodeEnum> {

    @Override
    public <T extends CodeEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> (T) CodeEnum.fromCode((Class) targetType, source.trim());
    }
}
