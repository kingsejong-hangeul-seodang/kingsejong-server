package com.kingsejong.domain.taste.repository;

import com.kingsejong.domain.taste.entity.Taste;
import com.kingsejong.global.common.Mode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TasteRepository extends JpaRepository<Taste, Long> {

    Optional<Taste> findBySeedKey(String seedKey);

    List<Taste> findAllByModeAndActiveTrueOrderBySortOrderAscIdAsc(Mode mode);

    List<Taste> findBySeedKeyNotInAndActiveTrue(Collection<String> seedKeys);
}
