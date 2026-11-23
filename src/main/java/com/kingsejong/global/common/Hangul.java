package com.kingsejong.global.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 한글 음절을 자모로 쪼갠다. 자모 도감의 40개(자음 19 · 모음 21) 기준이라
 * 겹받침은 낱자모로 나눈다 (ㄳ → ㄱ, ㅅ). 쌍자음과 겹모음(ㅘ 등)은 도감에 따로 있어서 그대로 둔다.
 */
public final class Hangul {

    private static final char BASE = '가';
    private static final char LAST = '힣';
    private static final String CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    private static final String JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ";
    // 종성: 0번은 받침 없음
    private static final String[] JONG = {"", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ",
            "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"};
    private static final Map<String, String> COMPOUND_FINAL = Map.ofEntries(
            Map.entry("ㄳ", "ㄱㅅ"), Map.entry("ㄵ", "ㄴㅈ"), Map.entry("ㄶ", "ㄴㅎ"), Map.entry("ㄺ", "ㄹㄱ"),
            Map.entry("ㄻ", "ㄹㅁ"), Map.entry("ㄼ", "ㄹㅂ"), Map.entry("ㄽ", "ㄹㅅ"), Map.entry("ㄾ", "ㄹㅌ"),
            Map.entry("ㄿ", "ㄹㅍ"), Map.entry("ㅀ", "ㄹㅎ"), Map.entry("ㅄ", "ㅂㅅ"));

    /** 도감 자음 19개, 모음 21개 (이 순서로 보여 준다). */
    public static final List<String> CONSONANTS = split(CHO);
    public static final List<String> VOWELS = split(JUNG);

    private Hangul() {
    }

    /**
     * 글자 하나를 도감 자모 목록으로 쪼갠다. 같은 자모가 두 번 나오면 두 번 들어간다 (밥 → ㅂ, ㅏ, ㅂ).
     * 자모 단독 글자는 그 자모 하나 (겹받침이면 나눈다). 한글이 아니면 빈 목록.
     */
    public static List<String> toJamo(String ch) {
        List<String> result = new ArrayList<>();
        if (ch == null || ch.length() != 1) {
            return result;
        }
        char c = ch.charAt(0);
        if (c >= BASE && c <= LAST) {
            int index = c - BASE;
            result.add(String.valueOf(CHO.charAt(index / (21 * 28))));
            result.add(String.valueOf(JUNG.charAt((index % (21 * 28)) / 28)));
            addFinal(result, JONG[index % 28]);
        } else if (CHO.indexOf(c) >= 0 || JUNG.indexOf(c) >= 0) {
            result.add(ch);
        } else {
            addFinal(result, ch);
        }
        return result;
    }

    /** 오류에 붙어 온 자모(겹받침일 수도 있음)를 도감 자모로. */
    public static List<String> normalize(String jamo) {
        List<String> result = new ArrayList<>();
        addFinal(result, jamo);
        return result;
    }

    public static boolean isDogamJamo(String jamo) {
        return CONSONANTS.contains(jamo) || VOWELS.contains(jamo);
    }

    private static void addFinal(List<String> result, String jong) {
        if (jong == null || jong.isEmpty()) {
            return;
        }
        String parts = COMPOUND_FINAL.getOrDefault(jong, jong);
        for (int i = 0; i < parts.length(); i++) {
            String part = String.valueOf(parts.charAt(i));
            if (isDogamJamoChar(part)) {
                result.add(part);
            }
        }
    }

    private static boolean isDogamJamoChar(String s) {
        return CHO.contains(s) || JUNG.contains(s);
    }

    private static List<String> split(String s) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            list.add(String.valueOf(s.charAt(i)));
        }
        return List.copyOf(list);
    }
}
