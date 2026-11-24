package com.kingsejong.infra.youtube;

import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 유튜브 자막(timedtext XML) 읽기. 두 가지 모양을 받는다.
 * - format 3: {@code <p t="18918" d="677">희수야</p>} (ms)
 * - format 1: {@code <text start="18.9" dur="0.67">희수야</text>} (초)
 */
final class CaptionParser {

    private static final Pattern P = Pattern.compile("<p\\s+t=\"(\\d+)\"\\s+d=\"(\\d+)\"[^>]*>(.*?)</p>", Pattern.DOTALL);
    private static final Pattern TEXT = Pattern.compile(
            "<text\\s+start=\"([\\d.]+)\"\\s+dur=\"([\\d.]+)\"[^>]*>(.*?)</text>", Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    private CaptionParser() {
    }

    static List<YoutubeVideo.Line> parse(String xml) {
        List<YoutubeVideo.Line> lines = new ArrayList<>();
        if (xml == null) {
            return lines;
        }
        Matcher m = P.matcher(xml);
        while (m.find()) {
            add(lines, Long.parseLong(m.group(1)) / 1000.0, Long.parseLong(m.group(2)) / 1000.0, m.group(3));
        }
        if (lines.isEmpty()) {
            Matcher t = TEXT.matcher(xml);
            while (t.find()) {
                add(lines, Double.parseDouble(t.group(1)), Double.parseDouble(t.group(2)), t.group(3));
            }
        }
        return lines;
    }

    private static void add(List<YoutubeVideo.Line> lines, double start, double dur, String raw) {
        // 태그를 걷고 HTML 기호를 두 번 푼다 (자막이 &amp;#39; 처럼 두 번 감싸여 오는 경우가 있음)
        String text = HtmlUtils.htmlUnescape(HtmlUtils.htmlUnescape(TAG.matcher(raw).replaceAll("")))
                .replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (!text.isEmpty()) {
            lines.add(new YoutubeVideo.Line(round(start), round(start + dur), text));
        }
    }

    private static double round(double sec) {
        return Math.round(sec * 100) / 100.0;
    }

    /**
     * 자동 자막(asr)은 같은 말이 겹쳐서 다시 나오는 모양(roll-up)이 흔하다.
     * 앞 줄과 같거나 앞 줄을 포함하면 합치고(끝 시간 늘림), 앞 줄에 포함되면 버린다.
     */
    static List<YoutubeVideo.Line> mergeRollUp(List<YoutubeVideo.Line> lines) {
        List<YoutubeVideo.Line> merged = new ArrayList<>();
        for (YoutubeVideo.Line line : lines) {
            if (!merged.isEmpty()) {
                YoutubeVideo.Line prev = merged.get(merged.size() - 1);
                if (line.text().contains(prev.text())) {
                    merged.set(merged.size() - 1, new YoutubeVideo.Line(prev.startSec(), Math.max(prev.endSec(), line.endSec()), line.text()));
                    continue;
                }
                if (prev.text().contains(line.text())) {
                    merged.set(merged.size() - 1, new YoutubeVideo.Line(prev.startSec(), Math.max(prev.endSec(), line.endSec()), prev.text()));
                    continue;
                }
            }
            merged.add(line);
        }
        return merged;
    }
}
