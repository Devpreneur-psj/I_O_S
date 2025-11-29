package com.soi.worldtree.repository;

import com.soi.worldtree.entity.EssencePulseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EssencePulseLogRepository extends JpaRepository<EssencePulseLog, Long> {
    
    /**
     * 사용자가 획득한 정령의 축복 총량을 계산합니다.
     * @param userId 사용자 ID
     * @return 총 정령의 축복 양
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0L) FROM EssencePulseLog e WHERE e.userId = :userId")
    Long getTotalEssenceAmountByUserId(@Param("userId") Long userId);
}

