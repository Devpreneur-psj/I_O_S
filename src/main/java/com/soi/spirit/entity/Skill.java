package com.soi.spirit.entity;

import jakarta.persistence.*;

/**
 * 기술 엔티티
 */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName; // 기술 이름

    @Column(name = "skill_type", nullable = false, length = 20)
    private String skillType; // RANGED_ATTACK, MELEE_ATTACK

    @Column(name = "element_type", nullable = false, length = 20)
    private String elementType; // FIRE, WATER, WIND, LIGHT, DARK

    @Column(name = "base_power", nullable = false)
    private Integer basePower; // 기본 위력

    @Column(name = "description", length = 500)
    private String description; // 기술 설명

    @Column(name = "unlock_evolution_stage", nullable = false)
    private Integer unlockEvolutionStage; // 해금 진화 단계 (0: 기본, 1: 1차, 2: 2차)

    @Column(name = "required_level")
    private Integer requiredLevel; // 필요 레벨

    @Column(name = "learn_time_minutes", nullable = false)
    private Integer learnTimeMinutes = 30; // 학습 시간 (분 단위, 기본값 30분)

    @Column(name = "cooldown_seconds", nullable = false)
    private Integer cooldownSeconds = 0; // 쿨타임 (초 단위, 기본값 0초)

    @Column(name = "effect_type", length = 50)
    private String effectType; // 효과 타입 (ATTACK, BUFF, DEBUFF 등)
    
    @Column(name = "effect_value")
    private Integer effectValue; // 효과 값 (버프/디버프 수치)

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillType() {
        return skillType;
    }

    public void setSkillType(String skillType) {
        this.skillType = skillType;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public Integer getBasePower() {
        return basePower;
    }

    public void setBasePower(Integer basePower) {
        this.basePower = basePower;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUnlockEvolutionStage() {
        return unlockEvolutionStage;
    }

    public void setUnlockEvolutionStage(Integer unlockEvolutionStage) {
        this.unlockEvolutionStage = unlockEvolutionStage;
    }

    public Integer getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(Integer requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public Integer getLearnTimeMinutes() {
        return learnTimeMinutes;
    }

    public void setLearnTimeMinutes(Integer learnTimeMinutes) {
        this.learnTimeMinutes = learnTimeMinutes != null ? learnTimeMinutes : 30;
    }

    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Integer cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds != null ? cooldownSeconds : 0;
    }

    public String getEffectType() {
        return effectType;
    }

    public void setEffectType(String effectType) {
        this.effectType = effectType;
    }

    public Integer getEffectValue() {
        return effectValue;
    }

    public void setEffectValue(Integer effectValue) {
        this.effectValue = effectValue;
    }
}

