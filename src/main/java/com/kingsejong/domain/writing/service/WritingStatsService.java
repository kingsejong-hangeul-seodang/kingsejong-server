package com.kingsejong.domain.writing.service;

import com.kingsejong.domain.writing.repository.WritingRepository;
import com.kingsejong.global.common.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 쓰기 기록으로 계산하는 값들. 내 정보(today)와 콘텐츠 목록(completed·perfect)이 쓴다.
 * 쓰기 제출(WritingService)은 이 서비스만 거꾸로 부르지 않으므로 서비스 간 순환이 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WritingStatsService {

    private final WritingRepository writingRepository;

    public int countTodayCorrectContents(Long userId, LocalDate today) {
        return (int) writingRepository.countCorrectContentsOn(userId, today);
    }

    /** contentId → perfect 여부. 키가 있으면 이 난이도로 한 번이라도 맞게 쓴 것. */
    public Map<Long, Boolean> findCompletion(Long userId, Stage stage, Collection<Long> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Boolean> result = new HashMap<>();
        List<Object[]> rows = writingRepository.findCompletion(userId, stage, contentIds);
        for (Object[] row : rows) {
            result.put((Long) row[0], ((Number) row[1]).intValue() == 1);
        }
        return result;
    }
}
