package com.kingsejong.domain.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자막 한 줄과 표시 시간. 재생 시점에 프론트가 강조한다. */
@Getter
@Entity
@Table(name = "content_lines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @Column(nullable = false, length = 200)
    private String text;

    @Column(name = "start_sec")
    private Double startSec;

    @Column(name = "end_sec")
    private Double endSec;

    /** 영어 번역 (B-05). */
    @Column(length = 300)
    private String translation;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private ContentLine(String text, Double startSec, Double endSec, String translation, int sortOrder) {
        this.text = text;
        this.startSec = startSec;
        this.endSec = endSec;
        this.translation = translation;
        this.sortOrder = sortOrder;
    }

    void assignContent(Content content) {
        this.content = content;
    }
}
