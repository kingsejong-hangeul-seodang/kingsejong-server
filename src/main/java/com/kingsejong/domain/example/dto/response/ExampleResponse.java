package com.kingsejong.domain.example.dto.response;

import com.kingsejong.domain.example.entity.Example;

import java.time.OffsetDateTime;

/**
 * 응답 DTO. 엔티티를 그대로 내보내지 않고 DTO 로 바꿔서 응답한다.
 */
public record ExampleResponse(
        Long id,
        String name,
        OffsetDateTime createdAt
) {

    public static ExampleResponse from(Example example) {
        return new ExampleResponse(example.getId(), example.getName(), example.getCreatedAt());
    }
}
