package com.soi.spirit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 정령이 배운 기술 엔티티
 */
@Entity
@Table(name = "spirit_skills")
public class SpiritSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spirit_id", nullable = false)
    private Long spiritId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "learned_at", updatable = false)
    private LocalDateTime learnedAt; // 학습 완료 시간

    @Column(name = "learning_start_time")
    private LocalDateTime learningStartTime; // 학습 시작 시간

    @Column(name = "learning_completion_time")
    private LocalDateTime learningCompletionTime; // 학습 완료 예정 시간

    @Column(name = "is_learning", nullable = false)
    private Boolean isLearning = false; // 학습 진행 중 여부

    @Column(name = "mastery_level", nullable = false)
    private Integer masteryLevel = 1; // 숙련도 (1-5)

    @Column(name = "last_used_time")
    private LocalDateTime lastUsedTime; // 마지막 사용 시간 (쿨타임 관리용)

    @PrePersist
    protected void onCreate() {
        // 학습 완료 시에만 learnedAt 설정
        if (!isLearning && learnedAt == null) {
            learnedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSpiritId() {
        return spiritId;
    }

    public void setSpiritId(Long spiritId) {
        this.spiritId = spiritId;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public LocalDateTime getLearnedAt() {
        return learnedAt;
    }

    public void setLearnedAt(LocalDateTime learnedAt) {
        this.learnedAt = learnedAt;
    }

    public Integer getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(Integer masteryLevel) {
        this.masteryLevel = Math.max(1, Math.min(5, masteryLevel)); // 1-5 범위 제한
    }

    public LocalDateTime getLearningStartTime() {
        return learningStartTime;
    }

    public void setLearningStartTime(LocalDateTime learningStartTime) {
        this.learningStartTime = learningStartTime;
    }

    public LocalDateTime getLearningCompletionTime() {
        return learningCompletionTime;
    }

    public void setLearningCompletionTime(LocalDateTime learningCompletionTime) {
        this.learningCompletionTime = learningCompletionTime;
    }

    public Boolean getIsLearning() {
        return isLearning != null ? isLearning : false;
    }

    public void setIsLearning(Boolean isLearning) {
        this.isLearning = isLearning != null ? isLearning : false;
    }

    public LocalDateTime getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(LocalDateTime lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }
}

