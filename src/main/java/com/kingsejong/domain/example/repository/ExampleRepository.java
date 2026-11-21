package com.kingsejong.domain.example.repository;

import com.kingsejong.domain.example.entity.Example;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleRepository extends JpaRepository<Example, Long> {
}
