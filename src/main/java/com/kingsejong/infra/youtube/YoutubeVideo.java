package com.kingsejong.infra.youtube;

import java.util.List;

/**
 * 유튜브에서 가져온 영상 하나.
 * @param embeddable  다른 사이트에 퍼갈 수 있는지 (oEmbed가 막혀 있으면 false → 프론트는 "문장만 써요")
 * @param lengthSec   영상 길이(초). 모르면 null
 * @param captions    한국어 자막 (사람이 단 자막 우선, 없으면 자동 자막). 없으면 null
 */
public record YoutubeVideo(String videoId, String title, String channel, String thumbnailUrl,
                           boolean embeddable, Integer lengthSec, Captions captions) {

    /** kind = manual(사람이 단 자막) | asr(자동 자막). */
    public record Captions(String language, String kind, List<Line> lines) {
    }

    /** 자막 한 줄. 시간은 영상 처음부터의 초. text는 HTML 기호를 풀어 둔 원문 그대로. */
    public record Line(double startSec, double endSec, String text) {
    }
}
