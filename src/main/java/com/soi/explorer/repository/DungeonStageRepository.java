package com.soi.explorer.repository;

import com.soi.explorer.entity.DungeonStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DungeonStageRepository extends JpaRepository<DungeonStage, Long> {
    Optional<DungeonStage> findByStageNumber(Integer stageNumber);
    List<DungeonStage> findAllByOrderByStageNumberAsc();
}

