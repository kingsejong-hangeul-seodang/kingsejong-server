package com.kingsejong.domain.jamo.dto;

import com.kingsejong.global.common.Mode;

import java.time.OffsetDateTime;
import java.util.List;

/** 자모 상세 (API 명세 3.13). */
public record JamoDetailResponse(
        String jamo,
        String name,
        String type,
        int strokeCount,
        String mastery,
        int writeCount,
        int errorCount,
        List<String> steps,
        String commonMistake,
        List<String> examples,
        List<RecentWriting> recentWritings,
        Long practiceContentId
) {

    /** 이 자모가 든 최근 기록. result = ok(다 맞음) / char(글자가 다름) / stroke(획순이 다름). */
    public record RecentWriting(Long writingId, String target, Mode mode, String source,
                                OffsetDateTime createdAt, String result, String message) {
    }
}
