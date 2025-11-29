package com.soi.spirit.repository;

import com.soi.spirit.entity.SpiritAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpiritActionRepository extends JpaRepository<SpiritAction, Long> {
    List<SpiritAction> findBySpiritIdOrderByCreatedAtDesc(Long spiritId);
    
    @Query("SELECT sa FROM SpiritAction sa WHERE sa.spiritId = :spiritId AND sa.createdAt >= :since ORDER BY sa.createdAt DESC")
    List<SpiritAction> findRecentActionsBySpiritId(@Param("spiritId") Long spiritId, @Param("since") LocalDateTime since);
}

