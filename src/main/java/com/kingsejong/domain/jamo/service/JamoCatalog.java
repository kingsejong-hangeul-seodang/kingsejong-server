package com.kingsejong.domain.jamo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsejong.global.common.Hangul;
import com.kingsejong.global.seed.SeedReader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 자모 40개의 이름·쓰는 순서 설명·자주 틀리는 점 (seed/jamo.json). 바뀌지 않는 글이라 DB가 아니라 메모리에 둔다.
 * 도감 자모(자음 19 · 모음 21)와 하나라도 다르면 서버가 켜지지 않는다.
 */
@Component
public class JamoCatalog {

    public record JamoInfo(String jamo, String name, String romanization, String type,
                           List<String> steps, String commonMistake) {
    }

    private final Map<String, JamoInfo> byJamo = new LinkedHashMap<>();

    public JamoCatalog(SeedReader seedReader) {
        try {
            List<JamoInfo> items = seedReader.read("seed/jamo.json", new TypeReference<>() {
            });
            items.forEach(item -> byJamo.put(item.jamo(), item));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        check(Hangul.CONSONANTS, "consonant");
        check(Hangul.VOWELS, "vowel");
        if (byJamo.size() != Hangul.CONSONANTS.size() + Hangul.VOWELS.size()) {
            throw new IllegalStateException("seed/jamo.json: 자모는 40개여야 해요 (지금 " + byJamo.size() + "개)");
        }
    }

    public Optional<JamoInfo> find(String jamo) {
        return Optional.ofNullable(byJamo.get(jamo));
    }

    public JamoInfo get(String jamo) {
        return byJamo.get(jamo);
    }

    private void check(List<String> expected, String type) {
        for (String jamo : expected) {
            JamoInfo info = byJamo.get(jamo);
            if (info == null || !type.equals(info.type()) || info.steps() == null || info.steps().isEmpty()) {
                throw new IllegalStateException("seed/jamo.json: '" + jamo + "' 항목이 없거나 type·steps가 틀려요");
            }
        }
    }
}
