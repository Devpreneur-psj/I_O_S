package com.soi.spirit.repository;

import com.soi.spirit.entity.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    List<GameEvent> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT ge FROM GameEvent ge WHERE ge.userId = :userId AND ge.isResolved = false ORDER BY ge.createdAt DESC")
    List<GameEvent> findActiveEventsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ge FROM GameEvent ge WHERE ge.userId = :userId AND ge.createdAt >= :since ORDER BY ge.createdAt DESC")
    List<GameEvent> findRecentEventsByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}

