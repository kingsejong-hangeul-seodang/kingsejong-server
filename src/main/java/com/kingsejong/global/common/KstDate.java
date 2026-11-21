package com.kingsejong.global.common;

import java.time.LocalDate;
import java.time.ZoneId;

/** 날짜 계산은 한국 시간 자정 기준 (API 명세 1번). */
public final class KstDate {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private KstDate() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
