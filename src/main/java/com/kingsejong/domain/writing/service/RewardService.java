package com.kingsejong.domain.writing.service;

import com.kingsejong.domain.user.dto.response.LevelResponse;
import com.kingsejong.domain.user.dto.response.TodayResponse;
import com.kingsejong.domain.user.entity.LevelPolicy;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.user.service.UserService;
import com.kingsejong.domain.writing.dto.response.RewardResponse;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.domain.writing.repository.WritingRepository;
import com.kingsejong.global.common.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 쓰기 제출 보상 계산 (API 명세 3.8 "경험치 규칙", 숫자는 임시).
 *
 * - correct +10, perfect +5 더. 같은 콘텐츠·난이도로 하루 최대 15
 * - 오늘 같은 콘텐츠·난이도로 이미 받은 만큼을 빼고 차액만 준다
 *   (처음 correct로 10, 다시 써서 perfect면 5를 더 받음. 틀린 뒤 다시 써도 손해 없음)
 * - 연속 학습: 판정을 받은 제출이 그날 하나라도 있으면 하루로 센다
 * - 난이도 올리기 추천: 같은 난이도에서 10번 연속 correct인 그 제출에서만 true, 그 뒤 카운트 0. 고수는 항상 false
 */
@Service
@RequiredArgsConstructor
public class RewardService {

    static final int CORRECT_EXP = 10;
    static final int PERFECT_EXP = 5;
    static final int STAGE_UP_STREAK = 10;

    private final WritingRepository writingRepository;
    private final WritingStatsService writingStatsService;
    private final UserService userService;

    /** 판정이 끝난 기록에 보상을 준다. 쓰기 제출의 "판정 저장" 트랜잭션 안에서 부른다. */
    @Transactional
    public RewardResponse reward(Writing writing) {
        User user = userService.getUser(writing.getUserId());
        int previousLevelNo = LevelPolicy.levelOf(user.getExp()).no();

        int alreadyToday = writingRepository.sumExpGainedOn(
                writing.getUserId(), writing.getContentId(), writing.getStage(), writing.getStudyDate());
        int earnedTotal = (writing.isCorrect() ? CORRECT_EXP : 0) + (writing.isPerfect() ? PERFECT_EXP : 0);
        int gained = Math.max(0, earnedTotal - alreadyToday);
        // correct 몫(최대 10)을 먼저 채우고 남는 것이 perfect 몫
        int correctPart = Math.max(0, Math.min(CORRECT_EXP, earnedTotal) - Math.min(CORRECT_EXP, alreadyToday));
        int perfectPart = gained - correctPart;

        List<RewardResponse.Item> items = new ArrayList<>();
        if (correctPart > 0) {
            items.add(new RewardResponse.Item("correct", correctPart));
        }
        if (perfectPart > 0) {
            items.add(new RewardResponse.Item("perfect", perfectPart));
        }

        writing.gainExp(gained);
        user.addExp(gained);
        user.recordStudy(writing.getStudyDate());
        boolean streakReached = user.countCorrect(writing.isCorrect(), STAGE_UP_STREAK);
        boolean stageUpSuggested = streakReached && writing.getStage() != Stage.ADVANCED;

        LevelResponse level = LevelResponse.of(user.getExp());
        int todayDone = writingStatsService.countTodayCorrectContents(user.getId(), writing.getStudyDate());

        return new RewardResponse(
                gained,
                items,
                level,
                level.no() > previousLevelNo,
                previousLevelNo,
                user.currentStreakDays(writing.getStudyDate()),
                TodayResponse.of(todayDone),
                stageUpSuggested
        );
    }
}
