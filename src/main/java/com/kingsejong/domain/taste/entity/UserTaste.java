package com.kingsejong.domain.taste.entity;

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

/** 사용자가 고른 취향 하나. */
@Getter
@Entity
@Table(name = "user_tastes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "taste_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "taste_id", nullable = false)
    private Long tasteId;

    public UserTaste(Long userId, Long tasteId) {
        this.userId = userId;
        this.tasteId = tasteId;
    }
}
