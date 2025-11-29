package com.soi.worldtree.repository;

import com.soi.worldtree.entity.WorldTreeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorldTreeLevelRepository extends JpaRepository<WorldTreeLevel, Integer> {
    Optional<WorldTreeLevel> findByLevel(Integer level);
}

