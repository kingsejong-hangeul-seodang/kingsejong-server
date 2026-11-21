package com.kingsejong.domain.content.entity;

import com.kingsejong.global.common.Stage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 한 콘텐츠의 한 난이도에서 쓸 글. 쓰기 제출 때 서버는 이 target을 정답으로 엔진에 보낸다.
 * target 규칙은 교정 엔진과 같다: 초보 = 띄어쓰기 없는 단어 하나(또는 자모 하나), 중수 = 어절 3개 이하, 고수 = 20칸 이하.
 */
@Getter
@Entity
@Table(name = "content_tasks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"content_id", "stage"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentTask {

    /** 고수 원고지는 정답 칸 수보다 2칸 넉넉하게 (API 명세 3.7). 엔진 상한은 30칸. */
    private static final int ADVANCED_EXTRA_CELLS = 2;
    private static final int MAX_CELLS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Stage stage;

    @Column(nullable = false, length = 20)
    private String target;

    /** 초보 단어 뜻 (B-11). 초보가 아니면 null. */
    @Column(length = 100)
    private String meaning;

    /**
     * 목록 카드 제목. null이면 콘텐츠 제목을 쓴다.
     * 기본 연습 초보는 쓸 단어가 곧 제목("바다"), 중수·고수는 주제("여행 표현 1")라 난이도마다 다를 수 있다.
     */
    @Column(length = 100)
    private String title;

    @Builder
    private ContentTask(Stage stage, String target, String meaning, String title) {
        this.stage = stage;
        this.target = target;
        this.meaning = meaning;
        this.title = title;
    }

    void assignContent(Content content) {
        this.content = content;
    }

    /** 원고지 칸 수. 초보·중수 = 공백 뺀 글자 수, 고수 = 정답 칸 수 + 2 (최대 30). */
    public int cellCount() {
        if (stage == Stage.ADVANCED) {
            return Math.min(target.length() + ADVANCED_EXTRA_CELLS, MAX_CELLS);
        }
        return target.replace(" ", "").length();
    }

    public int wordCount() {
        return target.split(" ").length;
    }

    /** 어절별 글자 수. "보고 싶었어" → [2, 3] */
    public List<Integer> wordGroups() {
        return Arrays.stream(target.split(" ")).map(String::length).toList();
    }
}
