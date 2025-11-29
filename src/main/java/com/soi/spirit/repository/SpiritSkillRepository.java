package com.soi.spirit.repository;

import com.soi.spirit.entity.SpiritSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpiritSkillRepository extends JpaRepository<SpiritSkill, Long> {
    List<SpiritSkill> findBySpiritId(Long spiritId);
    boolean existsBySpiritIdAndSkillId(Long spiritId, Long skillId);
    Optional<SpiritSkill> findBySpiritIdAndSkillId(Long spiritId, Long skillId);
}

