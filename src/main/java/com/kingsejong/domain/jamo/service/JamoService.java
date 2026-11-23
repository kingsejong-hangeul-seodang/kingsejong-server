package com.kingsejong.domain.jamo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.domain.content.service.ContentService;
import com.kingsejong.domain.content.service.ContentSummary;
import com.kingsejong.domain.jamo.dto.JamoDetailResponse;
import com.kingsejong.domain.jamo.dto.JamoListResponse;
import com.kingsejong.domain.jamo.entity.UserJamoStat;
import com.kingsejong.domain.jamo.repository.UserJamoStatRepository;
import com.kingsejong.domain.writing.repository.RecentWritingRow;
import com.kingsejong.domain.writing.service.WritingQueryService;
import com.kingsejong.global.common.Hangul;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JamoService {

    static final int RECENT_LIMIT = 3;
    static final int RECENT_SCAN = 50;
    static final int EXAMPLE_LIMIT = 4;

    private final JamoCatalog jamoCatalog;
    private final UserJamoStatRepository userJamoStatRepository;
    private final ContentService contentService;
    private final WritingQueryService writingQueryService;

    public JamoListResponse getDogam(Long userId) {
        Map<String, UserJamoStat> stats = statsOf(userId);
        return new JamoListResponse(items(Hangul.CONSONANTS, stats), items(Hangul.VOWELS, stats));
    }

    public JamoDetailResponse getDetail(Long userId, String jamo) {
        JamoCatalog.JamoInfo info = jamoCatalog.find(jamo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "도감에 없는 자모예요"));
        UserJamoStat stat = userJamoStatRepository.findByUserIdAndJamo(userId, jamo).orElse(null);
        int writes = stat != null ? stat.getWriteCount() : 0;
        int errors = stat != null ? stat.getErrorCount() : 0;
        return new JamoDetailResponse(
                jamo,
                info.name(),
                info.type(),
                info.steps().size(),
                UserJamoStat.mastery(writes, errors),
                writes,
                errors,
                info.steps(),
                info.commonMistake(),
                contentService.findExampleChars(jamo, EXAMPLE_LIMIT),
                recentWritings(userId, jamo),
                contentService.findJamoPracticeContentId(jamo).orElse(null)
        );
    }

    private List<JamoListResponse.Item> items(List<String> jamos, Map<String, UserJamoStat> stats) {
        return jamos.stream().map(jamo -> {
            UserJamoStat stat = stats.get(jamo);
            int writes = stat != null ? stat.getWriteCount() : 0;
            int errors = stat != null ? stat.getErrorCount() : 0;
            return new JamoListResponse.Item(jamo, jamoCatalog.get(jamo).name(),
                    UserJamoStat.mastery(writes, errors), writes, errors);
        }).toList();
    }

    private Map<String, UserJamoStat> statsOf(Long userId) {
        return userJamoStatRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserJamoStat::getJamo, Function.identity()));
    }

    /** 이 자모가 정답 문장에 든 최근 기록 3개. 그 기록에서 이 자모가 든 칸들의 결과로 result·message를 정한다. */
    private List<JamoDetailResponse.RecentWriting> recentWritings(Long userId, String jamo) {
        List<RecentWritingRow> candidates = writingQueryService.recentGraded(userId, RECENT_SCAN).stream()
                .filter(w -> containsJamo(w.target(), jamo))
                .limit(RECENT_LIMIT)
                .toList();
        Map<String, ContentSummary> contents = contentService.getSummaries(
                candidates.stream().map(RecentWritingRow::contentId).distinct().toList(),
                candidates.stream().map(RecentWritingRow::stage).distinct().toList());
        List<JamoDetailResponse.RecentWriting> result = new ArrayList<>();
        for (RecentWritingRow w : candidates) {
            ContentSummary content = contents.get(w.contentId() + ":" + w.stage().getCode());
            String[] outcome = outcome(w.grade(), jamo);
            result.add(new JamoDetailResponse.RecentWriting(w.id(), w.target(),
                    content != null ? content.mode() : null, content != null ? content.source() : null,
                    w.createdAt(), outcome[0], outcome[1]));
        }
        return result;
    }

    private static boolean containsJamo(String target, String jamo) {
        for (int i = 0; i < target.length(); i++) {
            if (Hangul.toJamo(String.valueOf(target.charAt(i))).contains(jamo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * [result, message]. 이 자모가 든 칸 중 글자가 틀린 칸이 있으면 char, 이 자모에 획 오류(순서·방향·빠진 획·더 쓴 획)가
     * 있으면 stroke, 아니면 ok. message는 엔진이 사용자 언어로 준 설명(explanations[].text)을 그대로 쓰고,
     * 다 맞았거나 맞는 설명이 없으면 null (명세 3.13).
     */
    static String[] outcome(JsonNode grade, String jamo) {
        boolean stroke = false;
        for (JsonNode cell : grade.path("cells")) {
            String expected = cell.path("expected").asText(null);
            if (expected == null || !Hangul.toJamo(expected).contains(jamo)) {
                continue;
            }
            int index = cell.path("index").asInt(-1);
            if (!cell.path("charCorrect").asBoolean(false)) {
                return new String[]{"char", explanation(grade, "char", index, null)};
            }
            for (JsonNode error : cell.path("errors")) {
                if (JamoStatService.STROKE_ERRORS.contains(error.path("type").asText())
                        && Hangul.normalize(error.path("jamo").asText("")).contains(jamo)) {
                    stroke = true;
                }
            }
        }
        if (stroke) {
            return new String[]{"stroke", explanation(grade, "stroke", -1, jamo)};
        }
        return new String[]{"ok", null};
    }

    /** 엔진 설명 한 줄: char는 그 칸이 든 설명, stroke는 그 자모의 설명. */
    private static String explanation(JsonNode grade, String kind, int cellIndex, String jamo) {
        for (JsonNode e : grade.path("explanations")) {
            if (!kind.equals(e.path("kind").asText())) {
                continue;
            }
            boolean match = jamo != null
                    ? Hangul.normalize(e.path("jamo").asText("")).contains(jamo)
                    : containsIndex(e.path("cellIndexes"), cellIndex);
            if (match && e.path("text").isTextual()) {
                return e.get("text").asText();
            }
        }
        return null;
    }

    private static boolean containsIndex(JsonNode indexes, int index) {
        for (JsonNode i : indexes) {
            if (i.asInt() == index) {
                return true;
            }
        }
        return false;
    }
}
