package com.soi.explorer.entity;

import jakarta.persistence.*;

/**
 * 던전 스테이지 엔티티
 */
@Entity
@Table(name = "dungeon_stages")
public class DungeonStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_number", nullable = false, unique = true)
    private Integer stageNumber; // 스테이지 번호 (1, 2, 3...)

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName; // 스테이지 이름

    @Column(name = "description", length = 500)
    private String description; // 스테이지 설명

    @Column(name = "difficulty", nullable = false)
    private Integer difficulty; // 난이도 (1-5)

    @Column(name = "required_level")
    private Integer requiredLevel; // 필요 레벨 (null이면 제한 없음)

    @Column(name = "exp_reward", nullable = false)
    private Integer expReward; // 경험치 보상

    @Column(name = "gold_reward", nullable = false)
    private Integer goldReward; // 골드 보상

    @Column(name = "enemy_count", nullable = false)
    private Integer enemyCount = 3; // 적 정령 수

    @Column(name = "enemy_level_base", nullable = false)
    private Integer enemyLevelBase; // 적 정령 기본 레벨

    @Column(name = "enemy_type", length = 50)
    private String enemyType; // 적 정령 타입 (랜덤이면 null)

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStageNumber() {
        return stageNumber;
    }

    public void setStageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(Integer requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public Integer getExpReward() {
        return expReward;
    }

    public void setExpReward(Integer expReward) {
        this.expReward = expReward;
    }

    public Integer getGoldReward() {
        return goldReward;
    }

    public void setGoldReward(Integer goldReward) {
        this.goldReward = goldReward;
    }

    public Integer getEnemyCount() {
        return enemyCount;
    }

    public void setEnemyCount(Integer enemyCount) {
        this.enemyCount = enemyCount;
    }

    public Integer getEnemyLevelBase() {
        return enemyLevelBase;
    }

    public void setEnemyLevelBase(Integer enemyLevelBase) {
        this.enemyLevelBase = enemyLevelBase;
    }

    public String getEnemyType() {
        return enemyType;
    }

    public void setEnemyType(String enemyType) {
        this.enemyType = enemyType;
    }
}

