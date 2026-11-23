package com.kingsejong.domain.writing.repository;

import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.global.common.Stage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface WritingRepository extends JpaRepository<Writing, Long> {

    /** 판정이 끝난 내 기록, 최신순. cursor(writingId)보다 오래된 것만. 첫 페이지는 cursor = Long.MAX_VALUE. */
    @Query("""
            select w from Writing w
            where w.userId = :userId and w.status = com.kingsejong.domain.writing.entity.WritingStatus.GRADED
              and w.id < :cursor
            order by w.id desc
            """)
    List<Writing> findGradedPage(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    /** 그날 맞게 쓴 서로 다른 콘텐츠 수 (일일 챌린지). */
    @Query("""
            select count(distinct w.contentId) from Writing w
            where w.userId = :userId and w.studyDate = :date and w.correct = true
            """)
    long countCorrectContentsOn(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 그날 같은 콘텐츠·난이도로 이미 받은 경험치 합 (하루 최대 15 계산용). */
    @Query("""
            select coalesce(sum(w.expGained), 0) from Writing w
            where w.userId = :userId and w.contentId = :contentId and w.stage = :stage and w.studyDate = :date
            """)
    int sumExpGainedOn(@Param("userId") Long userId, @Param("contentId") Long contentId,
                       @Param("stage") Stage stage, @Param("date") LocalDate date);

    /** 이 난이도로 맞게 쓴 적이 있는 콘텐츠와, 그중 perfect가 한 번이라도 있었는지. [contentId, perfect] */
    @Query("""
            select w.contentId, max(case when w.perfect = true then 1 else 0 end) from Writing w
            where w.userId = :userId and w.stage = :stage and w.correct = true and w.contentId in :contentIds
            group by w.contentId
            """)
    List<Object[]> findCompletion(@Param("userId") Long userId, @Param("stage") Stage stage,
                                  @Param("contentIds") Collection<Long> contentIds);
}
