package com.kingsejong.domain.jamo.repository;

import com.kingsejong.domain.jamo.entity.UserJamoStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJamoStatRepository extends JpaRepository<UserJamoStat, Long> {

    List<UserJamoStat> findAllByUserId(Long userId);

    Optional<UserJamoStat> findByUserIdAndJamo(Long userId, String jamo);
}
