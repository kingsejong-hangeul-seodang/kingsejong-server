package com.kingsejong.domain.taste.entity;

import com.kingsejong.global.common.BaseTimeEntity;
import com.kingsejong.global.common.Mode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 취향 카드 하나: K팝은 가수, K드라마는 작품 (온보딩 A-08). seed/tastes.json이 원본.
 */
@Getter
@Entity
@Table(name = "tastes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Taste extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seed_key", nullable = false, unique = true, length = 50)
    private String seedKey;

    /** KPOP(가수) 또는 KDRAMA(작품). 기본 연습에는 취향이 없다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Mode mode;

    @Column(nullable = false, length = 50)
    private String name;

    /** 없으면 null → 프론트가 킹세종 기본 그림. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    private Taste(String seedKey, Mode mode, String name, String imageUrl, int sortOrder) {
        this.seedKey = seedKey;
        update(mode, name, imageUrl, sortOrder);
    }

    public void update(Mode mode, String name, String imageUrl, int sortOrder) {
        this.mode = mode;
        this.name = name;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }
}
