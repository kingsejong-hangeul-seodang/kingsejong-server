package com.kingsejong.domain.content.dto.response;

import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.domain.content.entity.ContentKind;
import com.kingsejong.domain.content.entity.ContentTask;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;

import java.util.List;

/** 콘텐츠 목록 (API 명세 3.6). */
public record ContentListResponse(List<Item> items) {

    /**
     * 카드 하나. 고수(듣고 쓰기)는 답과 길이가 목록에 보이면 안 되므로 target·cellCount·wordCount가 null.
     */
    public record Item(
            Long id,
            Mode mode,
            ContentKind kind,
            String title,
            String source,
            String thumbnailUrl,
            String writeMode,
            Integer durationSec,
            boolean tasteMatched,
            String target,
            Integer cellCount,
            Integer wordCount,
            boolean completed,
            boolean perfect
    ) {

        public static Item of(Content content, ContentTask task, boolean completed, boolean perfect) {
            boolean hidden = task.getStage() == Stage.ADVANCED;
            return new Item(
                    content.getId(),
                    content.getMode(),
                    content.getKind(),
                    task.getTitle() != null ? task.getTitle() : content.getTitle(),
                    content.getSource(),
                    content.getThumbnailUrl(),
                    task.getStage().getWriteMode(),
                    content.durationSec(),
                    false, // 취향(tastes)은 아직 없음
                    hidden ? null : task.getTarget(),
                    hidden ? null : task.cellCount(),
                    hidden ? null : task.wordCount(),
                    completed,
                    perfect
            );
        }
    }
}
