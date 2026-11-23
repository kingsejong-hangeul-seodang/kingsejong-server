package com.kingsejong.domain.jamo.dto;

import java.util.List;

/** 자모 도감 (API 명세 3.12). 자음 19개, 모음 21개. */
public record JamoListResponse(List<Item> consonants, List<Item> vowels) {

    /** mastery: none(안 써 봄) / practice(연습) / familiar(익숙). */
    public record Item(String jamo, String name, String mastery, int writeCount, int errorCount) {
    }
}
