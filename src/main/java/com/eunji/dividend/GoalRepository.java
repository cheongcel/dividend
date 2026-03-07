package com.eunji.dividend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<GoalEntity, Long> {
    Optional<GoalEntity> findByUserId(Long userId);
}