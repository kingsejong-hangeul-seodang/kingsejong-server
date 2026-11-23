package com.kingsejong.domain.leveltest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsejong.domain.leveltest.dto.LevelStepRequest;
import com.kingsejong.domain.leveltest.dto.LevelStepResponse;
import com.kingsejong.domain.leveltest.dto.LevelTestResponse;
import com.kingsejong.domain.leveltest.entity.LevelTestProgress;
import com.kingsejong.domain.leveltest.repository.LevelTestProgressRepository;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.user.service.UserService;
import com.kingsejong.domain.writing.service.WritingService;
import com.kingsejong.global.common.Stage;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;
import com.kingsejong.infra.engine.EngineClient;
import com.kingsejong.infra.engine.EngineGradeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 레벨 테스트 (API 명세 3.14, 3.15). 단계마다 엔진을 한 번 부르고, 쓰기 기록·경험치에는 넣지 않는다.
 * 통과 = 엔진 correct (획순은 안 봄). 순서대로만 풀 수 있다 (1단계는 언제든 처음부터 다시).
 * 추천 난이도 (명세 3.15): 1단계에서 멈춤 → 초보, 2·3단계에서 멈춤 → 중수, 3단계 통과 → 고수.
 */
@Service
@RequiredArgsConstructor
public class LevelTestService {

    private final LevelTestProgressRepository progressRepository;
    private final UserService userService;
    private final EngineClient engineClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public LevelTestResponse getSteps() {
        return new LevelTestResponse(LevelTestCatalog.STEPS.stream()
                .map(s -> new LevelTestResponse.Step(s.step(), s.title(), s.stage(), s.target(), s.cellCount(), s.wordGroups()))
                .toList());
    }

    public LevelStepResponse submit(Long userId, int step, LevelStepRequest request) {
        if (step < 1 || step > LevelTestCatalog.STEPS.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "없는 단계예요");
        }
        User user = userService.getUser(userId);
        LevelTestCatalog.Step def = LevelTestCatalog.get(step);
        checkOrder(userId, step);
        WritingService.checkCells(def.stage(), def.target(), request.cells());

        // 엔진 호출은 트랜잭션 밖 (쓰기 제출과 같은 이유)
        JsonNode grade = engineClient.grade(new EngineGradeRequest(
                "level-" + userId + "-" + step,
                def.stage().getCode(),
                def.target(),
                user.getLocale().getCode(),
                objectMapper.valueToTree(request.canvas()),
                objectMapper.valueToTree(request.cells())));
        boolean passed = grade.path("correct").asBoolean(false);

        return transactionTemplate.execute(status -> {
            LevelTestProgress progress = progressRepository.findByUserId(userId)
                    .orElseGet(() -> progressRepository.save(new LevelTestProgress(userId)));
            progress.record(step, passed);
            boolean finished = !passed || step == LevelTestCatalog.STEPS.size();
            return new LevelStepResponse(step, passed, finished, finished ? recommend(progress) : null, results(progress));
        });
    }

    /** 2·3단계는 바로 앞 단계를 통과했을 때만. 같은 단계를 다시 내는 건 된다 (오류 뒤 재시도). */
    private void checkOrder(Long userId, int step) {
        if (step == 1) {
            return;
        }
        Boolean previous = progressRepository.findByUserId(userId).map(p -> p.passedAt(step - 1)).orElse(null);
        if (!Boolean.TRUE.equals(previous)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, List.of(new ErrorResponse.Detail(
                    "step", (step - 1) + "단계를 통과해야 " + step + "단계를 풀 수 있어요")));
        }
    }

    private static Stage recommend(LevelTestProgress progress) {
        int highestPassed = 0;
        for (int s = 1; s <= LevelTestCatalog.STEPS.size(); s++) {
            if (Boolean.TRUE.equals(progress.passedAt(s))) {
                highestPassed = s;
            } else {
                break;
            }
        }
        return switch (highestPassed) {
            case 3 -> Stage.ADVANCED;
            case 0 -> Stage.BEGINNER;
            default -> Stage.INTERMEDIATE;
        };
    }

    private static List<LevelStepResponse.StepResult> results(LevelTestProgress progress) {
        List<LevelStepResponse.StepResult> list = new ArrayList<>();
        for (LevelTestCatalog.Step s : LevelTestCatalog.STEPS) {
            Boolean passed = progress.passedAt(s.step());
            if (passed != null) {
                list.add(new LevelStepResponse.StepResult(s.step(), s.title(), passed));
            }
        }
        return list;
    }
}
