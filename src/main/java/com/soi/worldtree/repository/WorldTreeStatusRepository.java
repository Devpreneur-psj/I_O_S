package com.soi.worldtree.repository;

import com.soi.worldtree.entity.WorldTreeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorldTreeStatusRepository extends JpaRepository<WorldTreeStatus, Long> {
    Optional<WorldTreeStatus> findByUserId(Long userId);
}

