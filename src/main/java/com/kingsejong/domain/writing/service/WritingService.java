package com.kingsejong.domain.writing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsejong.domain.content.service.ContentService;
import com.kingsejong.domain.content.service.WritingTarget;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.user.service.UserService;
import com.kingsejong.domain.writing.dto.request.WritingCreateRequest;
import com.kingsejong.domain.writing.dto.response.RewardResponse;
import com.kingsejong.domain.writing.dto.response.WritingResponse;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.domain.writing.repository.WritingRepository;
import com.kingsejong.global.common.KstDate;
import com.kingsejong.global.common.Stage;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;
import com.kingsejong.global.validation.CellRules;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.infra.engine.EngineGradeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 쓰기 제출 (API 명세 3.8 "서버가 하는 일").
 *
 * 엔진 호출(최대 10초) 동안 DB 트랜잭션을 잡고 있지 않도록 세 단계로 나눈다.
 * 1. 저장 (트랜잭션): 필기 원본을 먼저 저장 → 엔진이 실패해도 원본이 남는다
 * 2. 엔진 호출 (트랜잭션 밖)
 * 3. 판정 저장 + 보상 (트랜잭션)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WritingService {

    private final WritingRepository writingRepository;
    private final UserService userService;
    private final ContentService contentService;
    private final EngineClient engineClient;
    private final RewardService rewardService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public WritingResponse submit(Long userId, WritingCreateRequest request) {
        User user = userService.getUser(userId);
        WritingTarget target = contentService.getWritingTarget(request.contentId(), request.stage());
        CellRules.check(request.stage(), target.target(), request.cells().stream().map(WritingCreateRequest.Cell::index).toList());

        JsonNode canvas = objectMapper.valueToTree(request.canvas());
        JsonNode cells = objectMapper.valueToTree(request.cells());

        // 1. 저장. 기록 수집에 동의하지 않았으면 필기 원본은 남기지 않는다
        Writing saved = transactionTemplate.execute(status -> writingRepository.save(Writing.builder()
                .userId(userId)
                .contentId(target.contentId())
                .stage(request.stage())
                .target(target.target())
                .studyDate(KstDate.today())
                .canvas(user.isConsentAgreed() ? canvas : null)
                .cells(user.isConsentAgreed() ? cells : null)
                .build()));

        // 2. 엔진 호출. sessionId에 writingId를 넣어 엔진 로그와 맞춘다
        JsonNode grade;
        try {
            grade = engineClient.grade(new EngineGradeRequest(
                    String.valueOf(saved.getId()),
                    request.stage().getCode(),
                    target.target(),
                    user.getLocale().getCode(),
                    canvas,
                    cells));
        } catch (RuntimeException e) {
            // 엔진 오류든 예상 밖 오류든 원본은 남기고 FAILED로 표시한다.
            // (서버가 엔진 호출 중에 꺼지면 PENDING으로 남는다. 오래된 PENDING은 판정 전 기록으로 보고 무시한다)
            markFailed(saved.getId(), e);
            throw e;
        }

        // 3. 판정 저장 + 보상 (경험치·레벨·연속 학습·난이도 추천). 둘은 함께 커밋되거나 함께 롤백된다
        try {
            return transactionTemplate.execute(status -> {
                Writing writing = writingRepository.findById(saved.getId()).orElseThrow();
                writing.graded(grade);
                RewardResponse reward = rewardService.reward(writing);
                return WritingResponse.of(writing, reward, target.nextContentId());
            });
        } catch (RuntimeException e) {
            markFailed(saved.getId(), e);
            throw e;
        }
    }

    /** 기록을 FAILED로 표시한다. 표시가 실패해도 원래 오류(503·504 등)를 그대로 돌려주도록 예외는 삼킨다. */
    private void markFailed(Long writingId, RuntimeException cause) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    writingRepository.findById(writingId).ifPresent(Writing::failed));
        } catch (RuntimeException markFailed) {
            log.error("Failed to mark writing {} as FAILED", writingId, markFailed);
            cause.addSuppressed(markFailed);
        }
    }

}
