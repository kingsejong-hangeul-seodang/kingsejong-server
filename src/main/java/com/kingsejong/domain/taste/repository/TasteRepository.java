package com.kingsejong.domain.taste.repository;

import com.kingsejong.domain.taste.entity.Taste;
import com.kingsejong.global.common.Mode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TasteRepository extends JpaRepository<Taste, Long> {

    Optional<Taste> findBySeedKey(String seedKey);

    List<Taste> findAllByModeAndActiveTrueOrderBySortOrderAscIdAsc(Mode mode);

    /** 그 모드의 모든 취향 id (숨긴 것 포함). 저장 때 옛 선택까지 지우려고. */
    @Query("select t.id from Taste t where t.mode = :mode")
    List<Long> findAllIdsByMode(@Param("mode") Mode mode);

    Optional<Taste> findBySeedKeyAndActiveTrue(String seedKey);

    /** 시드에 없는 key의 취향 (숨길 대상). 유튜브 콘텐츠를 만들며 생긴 취향("yt:")은 시드가 건드리지 않는다. */
    @Query("""
            select t from Taste t
            where t.active = true and t.seedKey not in :seedKeys and t.seedKey not like 'yt:%'
            """)
    List<Taste> findBySeedKeyNotInAndActiveTrue(@Param("seedKeys") Collection<String> seedKeys);

    Optional<Taste> findFirstByModeAndNameAndActiveTrue(Mode mode, String name);
}
