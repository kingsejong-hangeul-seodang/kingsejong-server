package com.kingsejong.domain.example.entity;

import com.kingsejong.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구조 예시용 엔티티 (실제 도메인 개발을 시작하면 example 패키지를 통째로 지운다).
 */
@Getter
@Entity
@Table(name = "examples")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Example extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    private Example(String name) {
        this.name = name;
    }

    public static Example create(String name) {
        return new Example(name);
    }
}
