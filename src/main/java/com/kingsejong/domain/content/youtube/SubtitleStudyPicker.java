package com.kingsejong.domain.content.youtube;

import com.kingsejong.domain.content.seed.ContentSeeder;
import com.kingsejong.global.common.Stage;
import com.kingsejong.infra.youtube.YoutubeVideo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 자막에서 학습 구간과 난이도별 쓸 글을 고른다 (사람이 고치기 전의 추천값).
 *
 * - 쓸 수 있는 줄: 영어·숫자가 없고, 정리하면 한글 2글자 이상 남는 줄 (괄호 안 [음악] 같은 설명은 지움)
 * - 고수: 쓸 수 있는 줄 중 20칸 이하에서 가장 긴 것 (넘으면 어절 단위로 20칸까지)
 * - 중수: 고수 문장의 앞 2어절 (1어절이면 그대로)
 * - 초보: 고수 문장에서 조사를 뗀 2~3글자 낱말 (2글자 우선)
 * - 구간: 시작·끝을 안 주면, 쓸 수 있는 줄이 가장 많이 들어가는 30초 (엔진·명세의 클립 30초 제한)
 */
public final class SubtitleStudyPicker {

    static final double MAX_CLIP_SEC = 30;
    private static final Pattern NOT_ALLOWED = Pattern.compile("[^가-힣 ?!.,]");
    private static final Pattern BRACKETS = Pattern.compile("\\[[^]]*]|\\([^)]*\\)|<[^>]*>");
    private static final Pattern LATIN_OR_DIGIT = Pattern.compile("[A-Za-z0-9]");
    private static final Pattern HANGUL = Pattern.compile("[가-힣]");
    private static final String PARTICLES = "은는이가을를에의도요야아";

    private SubtitleStudyPicker() {
    }

    public record Window(double startSec, double endSec, List<YoutubeVideo.Line> lines) {
    }

    /** 자막 한 줄을 엔진이 받는 글자만 남기고 정리한다. */
    static String clean(String raw) {
        String s = BRACKETS.matcher(raw).replaceAll(" ");
        s = NOT_ALLOWED.matcher(s).replaceAll(" ");
        // 한글이 없는 조각(문장부호만 남은 것)은 뗀다
        return Arrays.stream(s.trim().split("\\s+"))
                .filter(word -> HANGUL.matcher(word).find())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    static boolean usable(String raw) {
        return !LATIN_OR_DIGIT.matcher(BRACKETS.matcher(raw).replaceAll("")).find()
                && HANGUL.matcher(clean(raw)).results().count() >= 2;
    }

    /** 시작·끝을 받았으면 그 구간, 아니면 쓸 수 있는 줄이 가장 많은 30초. 자막이 없으면 empty. */
    public static Optional<Window> window(List<YoutubeVideo.Line> lines, Double startSec, Double endSec, Integer lengthSec) {
        if (startSec != null && endSec != null) {
            return Optional.of(new Window(startSec, endSec, inside(lines, startSec, endSec)));
        }
        Window best = null;
        long bestCount = 0;
        for (YoutubeVideo.Line line : lines) {
            if (!usable(line.text())) {
                continue;
            }
            double start = Math.max(0, Math.floor(line.startSec() - 1));
            double end = start + MAX_CLIP_SEC;
            if (lengthSec != null) {
                end = Math.min(end, lengthSec);
            }
            List<YoutubeVideo.Line> in = inside(lines, start, end);
            long count = in.stream().filter(l -> usable(l.text())).count();
            if (count > bestCount) {
                bestCount = count;
                double lastEnd = in.get(in.size() - 1).endSec();
                best = new Window(start, Math.min(end, Math.ceil(lastEnd + 1)), in);
            }
        }
        return Optional.ofNullable(best);
    }

    /** 구간 안에 다 들어가는 줄만. */
    static List<YoutubeVideo.Line> inside(List<YoutubeVideo.Line> lines, double start, double end) {
        return lines.stream().filter(l -> l.startSec() >= start && l.endSec() <= end).toList();
    }

    /** 난이도별 쓸 글 추천. 고를 줄이 없으면 빈 맵. 엔진 규칙(ContentSeeder.checkTarget)을 통과한 것만 넣는다. */
    public static Map<Stage, String> pickTargets(List<YoutubeVideo.Line> lines) {
        Map<Stage, String> result = new EnumMap<>(Stage.class);
        String advanced = null;
        for (YoutubeVideo.Line line : lines) {
            if (!usable(line.text())) {
                continue;
            }
            String candidate = fit(clean(line.text()), 20);
            if (candidate.length() >= 2 && (advanced == null || candidate.length() > advanced.length())) {
                advanced = candidate;
            }
        }
        if (advanced == null) {
            return result;
        }
        String[] words = advanced.split(" ");
        String intermediate = String.join(" ", Arrays.copyOf(words, Math.min(2, words.length)));
        String beginner = beginnerWord(words);
        put(result, Stage.ADVANCED, advanced);
        put(result, Stage.INTERMEDIATE, intermediate);
        if (beginner != null) {
            put(result, Stage.BEGINNER, beginner);
        }
        return result;
    }

    private static void put(Map<Stage, String> result, Stage stage, String target) {
        try {
            ContentSeeder.checkTarget("유튜브 추천", stage, target);
            result.put(stage, target);
        } catch (IllegalStateException ignored) {
            // 엔진 규칙에 안 맞으면 추천하지 않는다 (사람이 직접 넣음)
        }
    }

    /** 어절 단위로 max칸까지. */
    static String fit(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (String word : text.split(" ")) {
            if (sb.length() + (sb.isEmpty() ? 0 : 1) + word.length() > max) {
                break;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(word);
        }
        return sb.toString();
    }

    static String beginnerWord(String[] words) {
        List<String> candidates = new ArrayList<>();
        for (String w : words) {
            String word = w.replaceAll("[?!.,]", "");
            if (word.length() >= 3 && PARTICLES.indexOf(word.charAt(word.length() - 1)) >= 0) {
                word = word.substring(0, word.length() - 1);
            }
            if (word.length() >= 2 && word.length() <= 3 && word.chars().allMatch(c -> c >= '가' && c <= '힣')) {
                candidates.add(word);
            }
        }
        return candidates.stream().filter(w -> w.length() == 2).findFirst()
                .orElse(candidates.isEmpty() ? null : candidates.get(0));
    }
}
