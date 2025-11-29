package com.soi.game.repository;

import com.soi.game.entity.GameTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameTimeRepository extends JpaRepository<GameTime, Long> {
    Optional<GameTime> findByUserId(Long userId);
}

