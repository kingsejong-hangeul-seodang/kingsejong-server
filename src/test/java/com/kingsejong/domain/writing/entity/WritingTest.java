package com.kingsejong.domain.writing.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsejong.global.common.Stage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** 판정 저장 때 획순 오류 칸 수 계산. */
class WritingTest {

    private final ObjectMapper om = new ObjectMapper();

    private Writing writing() {
        return Writing.builder().userId(1L).contentId(1L).stage(Stage.BEGINNER).target("바다").studyDate(LocalDate.now()).build();
    }

    @Test
    void mark가_stroke인_칸만_센다() throws Exception {
        Writing w = writing();
        w.graded(om.readTree("{\"correct\":true,\"perfect\":false,\"cells\":["
                + "{\"mark\":\"stroke\"},{\"mark\":\"char\"},{\"mark\":\"stroke\"},{\"mark\":\"spacing\"},{\"mark\":\"none\"}]}"));
        assertThat(w.getStrokeErrorCount()).isEqualTo(2);
        assertThat(w.getStatus()).isEqualTo(WritingStatus.GRADED);
    }

    @Test
    void cells가_없으면_0() throws Exception {
        Writing w = writing();
        w.graded(om.readTree("{\"correct\":false,\"perfect\":false}"));
        assertThat(w.getStrokeErrorCount()).isZero();
    }
}
