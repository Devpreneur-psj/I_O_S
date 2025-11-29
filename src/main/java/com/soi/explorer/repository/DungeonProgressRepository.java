package com.soi.explorer.repository;

import com.soi.explorer.entity.DungeonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DungeonProgressRepository extends JpaRepository<DungeonProgress, Long> {
    Optional<DungeonProgress> findByUserIdAndStageNumber(Long userId, Integer stageNumber);
    List<DungeonProgress> findByUserIdOrderByStageNumberAsc(Long userId);
    List<DungeonProgress> findByUserIdAndIsClearedTrue(Long userId);
}

