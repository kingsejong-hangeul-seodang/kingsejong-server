package com.kingsejong.domain.jamo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 × 자모 숙련도 집계. 쓰기 제출의 판정 저장 때 함께 늘어난다. */
@Getter
@Entity
@Table(name = "user_jamo_stats", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "jamo"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJamoStat {

    /** 숙련(familiar): 5번 이상 쓰고 오류율 20% 이하 (API 명세 3.12, 가정). */
    public static final int FAMILIAR_MIN_WRITES = 5;
    public static final double FAMILIAR_MAX_ERROR_RATE = 0.2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 2)
    private String jamo;

    /** 맞게 쓴 글자 안에서 이 자모를 쓴 횟수. */
    @Column(name = "write_count", nullable = false)
    private int writeCount;

    /** 그중 획순·방향·빠진 획·균형 오류가 난 횟수. */
    @Column(name = "error_count", nullable = false)
    private int errorCount;

    public UserJamoStat(Long userId, String jamo) {
        this.userId = userId;
        this.jamo = jamo;
    }

    public void add(int writes, int errors) {
        this.writeCount += writes;
        this.errorCount += errors;
    }

    public static String mastery(int writeCount, int errorCount) {
        if (writeCount == 0) {
            return "none";
        }
        if (writeCount >= FAMILIAR_MIN_WRITES && (double) errorCount / writeCount <= FAMILIAR_MAX_ERROR_RATE) {
            return "familiar";
        }
        return "practice";
    }
}
