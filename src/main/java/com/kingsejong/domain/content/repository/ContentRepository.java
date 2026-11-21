package com.kingsejong.domain.content.repository;

import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    /** 그 모드에서 이 난이도로 쓸 글이 있는 콘텐츠, 순서대로. */
    @Query("""
            select distinct c from Content c join c.tasks t
            where c.mode = :mode and t.stage = :stage
            order by c.sortOrder asc, c.id asc
            """)
    List<Content> findAllByModeAndStage(@Param("mode") Mode mode, @Param("stage") Stage stage);
}
