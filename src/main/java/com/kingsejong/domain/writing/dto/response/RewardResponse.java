package com.kingsejong.domain.writing.dto.response;

import com.kingsejong.domain.user.dto.response.LevelResponse;
import com.kingsejong.domain.user.dto.response.TodayResponse;

import java.util.List;

/**
 * 쓰기 제출 보상 (API 명세 3.8 reward). 결과 화면의 "+10 EXP", "+5 EXP 오류 없음"과 레벨 업 화면에 쓴다.
 */
public record RewardResponse(
        int expGained,
        List<Item> items,
        LevelResponse level,
        boolean leveledUp,
        int previousLevelNo,
        int streakDays,
        TodayResponse today,
        boolean stageUpSuggested
) {

    /** 경험치 내역. reason = correct(다 맞음) | perfect(오류 없음). 0인 항목은 넣지 않는다. */
    public record Item(String reason, int exp) {
    }
}
