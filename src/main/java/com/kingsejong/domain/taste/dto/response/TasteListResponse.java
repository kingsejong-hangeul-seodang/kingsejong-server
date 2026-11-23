package com.kingsejong.domain.taste.dto.response;

import com.kingsejong.domain.taste.entity.Taste;
import com.kingsejong.global.common.Mode;

import java.util.List;

/** 취향 카드 목록 (API 명세 3.4). */
public record TasteListResponse(List<Item> items) {

    public record Item(Long id, Mode mode, String name, String imageUrl, boolean selected) {

        public static Item of(Taste taste, boolean selected) {
            return new Item(taste.getId(), taste.getMode(), taste.getName(), taste.getImageUrl(), selected);
        }
    }
}
