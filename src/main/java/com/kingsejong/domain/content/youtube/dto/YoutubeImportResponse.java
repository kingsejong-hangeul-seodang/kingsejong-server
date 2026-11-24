package com.kingsejong.domain.content.youtube.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;

import java.util.List;
import java.util.Map;

/**
 * 만든(또는 dryRun이면 만들어질) 콘텐츠. seed는 contents.json에 그대로 붙여 넣을 수 있는 조각이라,
 * DB를 초기화해도 남기고 싶으면 시드 파일로 옮기면 된다 (취향도 tastes.json에 같이).
 */
public record YoutubeImportResponse(
        Long contentId,
        String seedKey,
        boolean dryRun,
        Mode mode,
        String title,
        String source,
        String videoId,
        double startSec,
        double endSec,
        boolean embeddable,
        String captionKind,
        List<Line> lines,
        Map<Stage, Task> tasks,
        Taste taste,
        JsonNode seed
) {

    public record Line(String text, double startSec, double endSec) {
    }

    public record Task(String target, String meaning, String source) {
    }

    /** created = 이번에 새로 만든 취향 (dryRun이면 만들 예정). */
    public record Taste(Long id, String name, String seedKey, boolean created) {
    }
}
