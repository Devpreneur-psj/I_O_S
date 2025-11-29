package com.soi.community.repository;

import com.soi.community.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChannelNumberOrderByCreatedAtDesc(Integer channelNumber);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.channelNumber = :channelNumber AND cm.createdAt > :since ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesByChannel(@Param("channelNumber") Integer channelNumber, @Param("since") LocalDateTime since);
}

