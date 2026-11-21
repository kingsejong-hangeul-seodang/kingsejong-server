package com.kingsejong.domain.writing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.global.common.BaseTimeEntity;
import com.kingsejong.global.common.Stage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * 쓰기 제출 한 번. 다시 쓰기도 새 기록이다.
 * 필기 원본(canvas, cells)은 사용자가 기록 수집에 동의했을 때만 저장한다.
 * 엔진 판정 응답(grade)은 바꾸지 않고 통째로 jsonb에 저장한다.
 */
@Getter
@Entity
@Table(name = "writings", indexes = {
        @Index(name = "idx_writings_user_date", columnList = "user_id, study_date"),
        @Index(name = "idx_writings_user_content_stage", columnList = "user_id, content_id, stage")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Writing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Stage stage;

    /** 제출 당시의 정답 문장 (콘텐츠가 나중에 바뀌어도 기록은 그대로). */
    @Column(nullable = false, length = 20)
    private String target;

    /** 제출한 날 (한국 날짜). 오늘 진행·하루 경험치 계산에 쓴다. */
    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode canvas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode cells;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WritingStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode grade;

    /** 글자 + 띄어쓰기가 모두 맞음. 판정 전·실패면 false. */
    @Column(nullable = false)
    private boolean correct;

    /** correct이고 획순 오류도 없음. */
    @Column(nullable = false)
    private boolean perfect;

    /** 이번 제출로 얻은 경험치. */
    @Column(name = "exp_gained", nullable = false)
    private int expGained;

    @Builder
    private Writing(Long userId, Long contentId, Stage stage, String target, LocalDate studyDate,
                    JsonNode canvas, JsonNode cells) {
        this.userId = userId;
        this.contentId = contentId;
        this.stage = stage;
        this.target = target;
        this.studyDate = studyDate;
        this.canvas = canvas;
        this.cells = cells;
        this.status = WritingStatus.PENDING;
    }

    public void graded(JsonNode grade) {
        this.grade = grade;
        this.correct = grade.path("correct").asBoolean(false);
        this.perfect = grade.path("perfect").asBoolean(false);
        this.status = WritingStatus.GRADED;
    }

    public void failed() {
        this.status = WritingStatus.FAILED;
    }

    public void gainExp(int expGained) {
        this.expGained = expGained;
    }
}
