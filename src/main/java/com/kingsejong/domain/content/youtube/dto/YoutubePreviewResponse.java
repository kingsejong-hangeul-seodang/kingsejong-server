package com.kingsejong.domain.content.youtube.dto;

import com.kingsejong.global.common.Stage;
import com.kingsejong.infra.youtube.YoutubeVideo;

import java.util.List;
import java.util.Map;

/**
 * 영상 정보와 한국어 자막 전체, 그리고 추천 구간·쓸 글. 이걸 보고 import 요청의 startSec·endSec·tasks를 정하면 된다.
 * captionKind: manual(사람이 단 자막) | asr(자동 자막) | null(한국어 자막 없음)
 */
public record YoutubePreviewResponse(
        String videoId,
        String title,
        String channel,
        String thumbnailUrl,
        boolean embeddable,
        Integer lengthSec,
        String captionKind,
        List<YoutubeVideo.Line> lines,
        Suggestion suggestion
) {

    /** 자막이 없거나 쓸 수 있는 줄이 없으면 null. */
    public record Suggestion(double startSec, double endSec, Map<Stage, String> targets) {
    }
}
