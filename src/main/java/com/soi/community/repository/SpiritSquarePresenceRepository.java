package com.soi.community.repository;

import com.soi.community.entity.SpiritSquarePresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpiritSquarePresenceRepository extends JpaRepository<SpiritSquarePresence, Long> {
    List<SpiritSquarePresence> findByChannelNumber(Integer channelNumber);
    Optional<SpiritSquarePresence> findByUserIdAndChannelNumber(Long userId, Integer channelNumber);
    
    @Query("SELECT ssp FROM SpiritSquarePresence ssp WHERE ssp.lastActivity > :since")
    List<SpiritSquarePresence> findActivePresences(@Param("since") LocalDateTime since);
    
    void deleteByUserIdAndChannelNumber(Long userId, Integer channelNumber);
}

