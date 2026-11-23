package com.kingsejong.domain.leveltest.repository;

import com.kingsejong.domain.leveltest.entity.LevelTestProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LevelTestProgressRepository extends JpaRepository<LevelTestProgress, Long> {

    Optional<LevelTestProgress> findByUserId(Long userId);
}
