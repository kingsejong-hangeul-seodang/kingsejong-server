package com.kingsejong.domain.user.repository;

import com.kingsejong.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginCode(String loginCode);
}
