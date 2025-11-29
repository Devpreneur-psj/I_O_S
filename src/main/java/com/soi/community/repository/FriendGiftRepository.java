package com.soi.community.repository;

import com.soi.community.entity.FriendGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FriendGiftRepository extends JpaRepository<FriendGift, Long> {
    Optional<FriendGift> findBySenderIdAndReceiverIdAndGiftDate(Long senderId, Long receiverId, LocalDate giftDate);
    boolean existsBySenderIdAndReceiverIdAndGiftDate(Long senderId, Long receiverId, LocalDate giftDate);
}

