package com.soi.spirit.repository;

import com.soi.spirit.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    List<UserItem> findByUserId(Long userId);
    Optional<UserItem> findByUserIdAndItemId(Long userId, Long itemId);
    
    @Query("SELECT ui FROM UserItem ui WHERE ui.userId = :userId AND ui.quantity > 0")
    List<UserItem> findAvailableItemsByUserId(@Param("userId") Long userId);
}

