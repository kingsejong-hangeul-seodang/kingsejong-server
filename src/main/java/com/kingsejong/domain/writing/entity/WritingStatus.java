package com.kingsejong.domain.writing.entity;

/** 쓰기 기록 상태. 필기 원본을 먼저 저장하고 엔진을 부르므로 엔진이 실패하면 FAILED로 남는다. */
public enum WritingStatus {
    PENDING,  // 저장만 됨, 판정 전
    GRADED,   // 판정 끝
    FAILED    // 엔진 오류로 판정 못 함 (원본은 남음)
}
