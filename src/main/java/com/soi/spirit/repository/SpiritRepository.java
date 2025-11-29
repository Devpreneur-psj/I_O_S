package com.soi.spirit.repository;

import com.soi.spirit.entity.Spirit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpiritRepository extends JpaRepository<Spirit, Long> {
    List<Spirit> findByUserId(Long userId);
    Long countByUserId(Long userId); // 사용자가 소유한 정령 수
    List<Spirit> findByEvolutionInProgressTrue(); // 진화 진행 중인 정령 목록
}

