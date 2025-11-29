package com.soi.spirit.repository;

import com.soi.spirit.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByElementType(String elementType);
    List<Skill> findBySkillType(String skillType);
    List<Skill> findByUnlockEvolutionStageLessThanEqual(Integer evolutionStage);
    List<Skill> findByElementTypeAndUnlockEvolutionStageLessThanEqual(String elementType, Integer evolutionStage);
}

