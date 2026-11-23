package com.kingsejong.domain.taste.repository;

import com.kingsejong.domain.taste.entity.UserTaste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserTasteRepository extends JpaRepository<UserTaste, Long> {

    /** 내가 고른 취향 중 지금 보이는(active) 것의 id. */
    @Query("""
            select ut.tasteId from UserTaste ut, Taste t
            where ut.tasteId = t.id and ut.userId = :userId and t.active = true
            """)
    List<Long> findActiveTasteIds(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserTaste ut where ut.userId = :userId and ut.tasteId in :tasteIds")
    void deleteByUserIdAndTasteIdIn(@Param("userId") Long userId, @Param("tasteIds") Collection<Long> tasteIds);
}
