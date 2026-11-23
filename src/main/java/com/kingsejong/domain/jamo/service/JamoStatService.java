package com.kingsejong.domain.jamo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.domain.jamo.entity.UserJamoStat;
import com.kingsejong.domain.jamo.repository.UserJamoStatRepository;
import com.kingsejong.global.common.Hangul;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 쓰기 제출 판정으로 자모 숙련도를 쌓는다 (API 명세 3.8 "서버가 하는 일" 6번).
 * - 쓴 횟수: 글자가 맞은 칸(charCorrect)의 정답 글자를 자모로 쪼개 센다 (엔진 오류는 틀린 자모만 알려 주므로)
 * - 오류 횟수: 그 칸의 획 오류(순서·방향·빠진 획·더 쓴 획)의 jamo. 한 칸에서 같은 자모는 한 번만.
 *   균형(BALANCE)은 화면에서 틀린 것으로 치지 않기로 해서(명세 9번 9) 세지 않는다
 * - 글자가 틀린 칸은 세지 않는다. 겹받침은 낱자모로 나눠 센다
 */
@Service
@RequiredArgsConstructor
public class JamoStatService {

    /** 숙련도·자모 상세에서 "획 오류"로 보는 엔진 오류 종류. */
    public static final Set<String> STROKE_ERRORS = Set.of("ORDER", "DIRECTION", "MISSING", "EXTRA");

    private final UserJamoStatRepository userJamoStatRepository;

    /** 판정 저장 트랜잭션 안에서 부른다 (보상 계산이 잡은 사용자 행 잠금 안에서 돌아 같은 사용자 동시 갱신이 없다). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long userId, JsonNode grade) {
        Map<String, int[]> delta = new HashMap<>(); // jamo → [writes, errors]
        for (JsonNode cell : grade.path("cells")) {
            if (!cell.path("charCorrect").asBoolean(false) || !cell.path("expected").isTextual()) {
                continue;
            }
            for (String jamo : Hangul.toJamo(cell.get("expected").asText())) {
                delta.computeIfAbsent(jamo, k -> new int[2])[0]++;
            }
            Set<String> erred = new HashSet<>();
            for (JsonNode error : cell.path("errors")) {
                if (STROKE_ERRORS.contains(error.path("type").asText()) && error.path("jamo").isTextual()) {
                    erred.addAll(Hangul.normalize(error.get("jamo").asText()));
                }
            }
            for (String jamo : erred) {
                delta.computeIfAbsent(jamo, k -> new int[2])[1]++;
            }
        }
        delta.forEach((jamo, counts) -> {
            if (!Hangul.isDogamJamo(jamo)) {
                return;
            }
            UserJamoStat stat = userJamoStatRepository.findByUserIdAndJamo(userId, jamo)
                    .orElseGet(() -> userJamoStatRepository.save(new UserJamoStat(userId, jamo)));
            stat.add(counts[0], counts[1]);
        });
    }
}
