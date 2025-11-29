package com.soi.spirit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 정령 엔티티
 */
@Entity
@Table(name = "spirits")
public class Spirit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spirit_type", nullable = false, length = 50)
    private String spiritType; // 불의 정령, 물의 정령, 풀의 정령, 빛의 정령, 어둠의 정령

    @Column(name = "evolution_stage", nullable = false)
    private Integer evolutionStage = 0; // 0: 기본, 1: 1차 진화, 2: 2차 진화

    @Column(name = "name", length = 50)
    private String name; // 정령 이름 (사용자가 지을 수 있음)

    @Column(name = "level", nullable = false)
    private Integer level = 1; // 정령 레벨 (최대 30)

    @Column(name = "experience", nullable = false)
    private Integer experience = 0; // 경험치

    @Column(name = "intimacy", nullable = false)
    private Integer intimacy = 1; // 친밀도 (1-10)

    @Column(name = "personality", nullable = false, length = 20)
    private String personality; // 고집, 조심, 장난꾸러기, 온순, 용감

    // 능력치
    @Column(name = "ranged_attack", nullable = false)
    private Integer rangedAttack = 0; // 원거리 공격력

    @Column(name = "melee_attack", nullable = false)
    private Integer meleeAttack = 0; // 근거리 공격력

    @Column(name = "ranged_defense", nullable = false)
    private Integer rangedDefense = 0; // 원거리 방어력

    @Column(name = "melee_defense", nullable = false)
    private Integer meleeDefense = 0; // 근거리 방어력

    @Column(name = "speed", nullable = false)
    private Integer speed = 0; // 스피드

    // 수명 관련
    @Column(name = "max_level_reached", nullable = false)
    private Boolean maxLevelReached = false; // 50레벨 달성 여부

    @Column(name = "lifespan_countdown")
    private LocalDateTime lifespanCountdown; // 수명 카운트다운 시작일 (50레벨 달성 후 10일)

    @Column(name = "lifespan_extended")
    private Integer lifespanExtended = 0; // 생명연장 아이템으로 연장된 일수

    // 희귀 정령 고치 돌봐주기 게이지
    @Column(name = "cocoon_care_gauge")
    private Integer cocoonCareGauge = 0; // 돌봐주기 게이지 (0 ~ MAX_COCOON_CARE_GAUGE)
    
    @Column(name = "last_care_date")
    private java.time.LocalDate lastCareDate; // 마지막 돌봐주기 날짜
    
    @Column(name = "daily_care_count")
    private Integer dailyCareCount = 0; // 오늘 돌봐주기 횟수 (하루 최대 5회)

    // 진화 관련
    @Column(name = "evolution_in_progress", nullable = false)
    private Boolean evolutionInProgress = false; // 진화 진행 중 여부

    @Column(name = "evolution_start_time")
    private LocalDateTime evolutionStartTime; // 진화 시작 시간

    @Column(name = "evolution_target_stage")
    private Integer evolutionTargetStage; // 진화 목표 단계

    // 상태 관리
    @Column(name = "health_status", length = 50)
    private String healthStatus = "건강"; // 건강 상태 (건강, 아픔, 질병 등)

    @Column(name = "happiness", nullable = false)
    private Integer happiness = 50; // 행복도 (0-100)

    @Column(name = "mood", length = 50)
    private String mood = "보통"; // 기분 (좋음, 보통, 나쁨 등)

    @Column(name = "hunger", nullable = false)
    private Integer hunger = 50; // 배고픔 (0-100, 0이면 배부름, 100이면 매우 배고픔)

    @Column(name = "energy", nullable = false)
    private Integer energy = 100; // 에너지 (0-100)
    
    @Column(name = "last_status_update_time")
    private LocalDateTime lastStatusUpdateTime; // 마지막 상태 업데이트 시간 (배고픔, 행복도 등)
    
    @Column(name = "last_energy_value")
    private Integer lastEnergyValue = 100; // 마지막 에너지 값 (배고픔 계산용)
    
    @Column(name = "happiness_zero_since")
    private LocalDateTime happinessZeroSince; // 행복도가 0이 된 시점 (마을 떠남 판단용)

    @Column(name = "age", nullable = false)
    private Integer age = 0; // 나이 (일 단위)

    @Column(name = "diet_habit", length = 50)
    private String dietHabit; // 식습관

    @Column(name = "is_retired", nullable = false)
    private Boolean isRetired = false; // 은퇴 여부

    @Column(name = "retired_at")
    private LocalDateTime retiredAt; // 은퇴 일시

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSpiritType() {
        return spiritType;
    }

    public void setSpiritType(String spiritType) {
        this.spiritType = spiritType;
    }

    public Integer getEvolutionStage() {
        return evolutionStage;
    }

    public void setEvolutionStage(Integer evolutionStage) {
        this.evolutionStage = evolutionStage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public Integer getIntimacy() {
        return intimacy;
    }

    public void setIntimacy(Integer intimacy) {
        this.intimacy = Math.max(1, Math.min(10, intimacy)); // 1-10 범위 제한
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public Integer getRangedAttack() {
        return rangedAttack;
    }

    public void setRangedAttack(Integer rangedAttack) {
        this.rangedAttack = rangedAttack;
    }

    public Integer getMeleeAttack() {
        return meleeAttack;
    }

    public void setMeleeAttack(Integer meleeAttack) {
        this.meleeAttack = meleeAttack;
    }

    public Integer getRangedDefense() {
        return rangedDefense;
    }

    public void setRangedDefense(Integer rangedDefense) {
        this.rangedDefense = rangedDefense;
    }

    public Integer getMeleeDefense() {
        return meleeDefense;
    }

    public void setMeleeDefense(Integer meleeDefense) {
        this.meleeDefense = meleeDefense;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Boolean getMaxLevelReached() {
        return maxLevelReached;
    }

    public void setMaxLevelReached(Boolean maxLevelReached) {
        this.maxLevelReached = maxLevelReached;
    }

    public LocalDateTime getLifespanCountdown() {
        return lifespanCountdown;
    }

    public void setLifespanCountdown(LocalDateTime lifespanCountdown) {
        this.lifespanCountdown = lifespanCountdown;
    }

    public Integer getLifespanExtended() {
        return lifespanExtended;
    }

    public void setLifespanExtended(Integer lifespanExtended) {
        this.lifespanExtended = lifespanExtended;
    }

    public Integer getCocoonCareGauge() {
        return cocoonCareGauge != null ? cocoonCareGauge : 0;
    }

    public void setCocoonCareGauge(Integer cocoonCareGauge) {
        this.cocoonCareGauge = cocoonCareGauge != null ? Math.max(0, cocoonCareGauge) : 0;
    }

    public java.time.LocalDate getLastCareDate() {
        return lastCareDate;
    }

    public void setLastCareDate(java.time.LocalDate lastCareDate) {
        this.lastCareDate = lastCareDate;
    }

    public Integer getDailyCareCount() {
        return dailyCareCount != null ? dailyCareCount : 0;
    }

    public void setDailyCareCount(Integer dailyCareCount) {
        this.dailyCareCount = dailyCareCount != null ? Math.max(0, dailyCareCount) : 0;
    }

    public Boolean getEvolutionInProgress() {
        return evolutionInProgress;
    }

    public void setEvolutionInProgress(Boolean evolutionInProgress) {
        this.evolutionInProgress = evolutionInProgress;
    }

    public LocalDateTime getEvolutionStartTime() {
        return evolutionStartTime;
    }

    public void setEvolutionStartTime(LocalDateTime evolutionStartTime) {
        this.evolutionStartTime = evolutionStartTime;
    }

    public Integer getEvolutionTargetStage() {
        return evolutionTargetStage;
    }

    public void setEvolutionTargetStage(Integer evolutionTargetStage) {
        this.evolutionTargetStage = evolutionTargetStage;
    }

    public String getHealthStatus() {
        return healthStatus != null ? healthStatus : "건강";
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getDietHabit() {
        return dietHabit;
    }

    public void setDietHabit(String dietHabit) {
        this.dietHabit = dietHabit;
    }

    public Integer getHappiness() {
        return happiness != null ? happiness : 50;
    }

    public void setHappiness(Integer happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness)); // 0-100 범위 제한
    }

    public String getMood() {
        return mood != null ? mood : "보통";
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public Integer getHunger() {
        return hunger != null ? hunger : 50;
    }

    public void setHunger(Integer hunger) {
        this.hunger = Math.max(0, Math.min(100, hunger)); // 0-100 범위 제한
    }

    public Integer getEnergy() {
        return energy != null ? energy : 100;
    }

    public void setEnergy(Integer energy) {
        this.energy = Math.max(0, Math.min(100, energy)); // 0-100 범위 제한
    }

    public LocalDateTime getLastStatusUpdateTime() {
        return lastStatusUpdateTime;
    }

    public void setLastStatusUpdateTime(LocalDateTime lastStatusUpdateTime) {
        this.lastStatusUpdateTime = lastStatusUpdateTime;
    }

    public Integer getLastEnergyValue() {
        return lastEnergyValue != null ? lastEnergyValue : 100;
    }

    public void setLastEnergyValue(Integer lastEnergyValue) {
        this.lastEnergyValue = lastEnergyValue != null ? Math.max(0, Math.min(100, lastEnergyValue)) : 100;
    }

    public LocalDateTime getHappinessZeroSince() {
        return happinessZeroSince;
    }

    public void setHappinessZeroSince(LocalDateTime happinessZeroSince) {
        this.happinessZeroSince = happinessZeroSince;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getIsRetired() {
        return isRetired;
    }

    public void setIsRetired(Boolean isRetired) {
        this.isRetired = isRetired;
        if (isRetired && retiredAt == null) {
            this.retiredAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(LocalDateTime retiredAt) {
        this.retiredAt = retiredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

