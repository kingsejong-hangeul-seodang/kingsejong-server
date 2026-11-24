package com.kingsejong.domain.content.repository;

import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    Optional<Content> findBySeedKey(String seedKey);

    /** 시드에 없는 key의 콘텐츠 (숨길 대상). 관리자가 유튜브에서 만든 콘텐츠("yt:")는 시드가 건드리지 않는다. */
    @Query("""
            select c from Content c
            where c.active = true and c.seedKey not in :seedKeys and c.seedKey not like 'yt:%'
            """)
    List<Content> findBySeedKeyNotInAndActiveTrue(@Param("seedKeys") Collection<String> seedKeys);

    long countBySeedKeyStartingWith(String prefix);

    /** 그 모드에서 이 난이도로 쓸 글이 있는 보이는(active) 콘텐츠, 순서대로. */
    @Query("""
            select distinct c from Content c join c.tasks t
            where c.mode = :mode and t.stage = :stage and c.active = true
            order by c.sortOrder asc, c.id asc
            """)
    List<Content> findAllByModeAndStage(@Param("mode") Mode mode, @Param("stage") Stage stage);
}
