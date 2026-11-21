package com.kingsejong.domain.content.entity;

import com.kingsejong.global.common.BaseTimeEntity;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 콘텐츠 하나 (K팝 곡의 한 구간, K드라마 한 장면, 기본 연습 한 주제).
 * 난이도마다 쓸 글은 ContentTask에, 자막은 ContentLine에 있다.
 * 영상이 없는 기본 연습은 youtubeVideoId가 null.
 */
@Getter
@Entity
@Table(name = "contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Mode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContentKind kind;

    @Column(nullable = false, length = 100)
    private String title;

    /** 출처 표기 (곡명·작품명·회차). 기본 연습은 null. */
    @Column(length = 200)
    private String source;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    // ===== 클립 (영상 없는 콘텐츠는 전부 null) =====

    @Column(name = "youtube_video_id", length = 20)
    private String youtubeVideoId;

    @Column(name = "start_sec")
    private Double startSec;

    @Column(name = "end_sec")
    private Double endSec;

    @Column(nullable = false)
    private boolean embeddable;

    /** 장면 설명 (B-05). */
    @Column(length = 300)
    private String situation;

    /** 같은 모드 안에서의 순서. 목록 정렬과 "다음 글"에 쓴다. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OrderBy("sortOrder ASC")
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentTask> tasks = new ArrayList<>();

    @Builder
    private Content(Mode mode, ContentKind kind, String title, String source, String thumbnailUrl,
                    String youtubeVideoId, Double startSec, Double endSec, boolean embeddable,
                    String situation, int sortOrder) {
        this.mode = mode;
        this.kind = kind;
        this.title = title;
        this.source = source;
        this.thumbnailUrl = thumbnailUrl;
        this.youtubeVideoId = youtubeVideoId;
        this.startSec = startSec;
        this.endSec = endSec;
        this.embeddable = embeddable;
        this.situation = situation;
        this.sortOrder = sortOrder;
    }

    public void addLine(ContentLine line) {
        lines.add(line);
        line.assignContent(this);
    }

    public void addTask(ContentTask task) {
        tasks.add(task);
        task.assignContent(this);
    }

    public boolean hasClip() {
        return youtubeVideoId != null;
    }

    /** 카드의 "0:24". 영상이 없으면 null. */
    public Integer durationSec() {
        if (!hasClip() || startSec == null || endSec == null) {
            return null;
        }
        return (int) Math.round(endSec - startSec);
    }

    public Optional<ContentTask> taskOf(Stage stage) {
        return tasks.stream().filter(task -> task.getStage() == stage).findFirst();
    }
}
