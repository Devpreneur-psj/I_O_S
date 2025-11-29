package com.soi.explorer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 사용자의 던전 진행 상태 엔티티
 */
@Entity
@Table(name = "dungeon_progress")
public class DungeonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stage_number", nullable = false)
    private Integer stageNumber;

    @Column(name = "is_cleared", nullable = false)
    private Boolean isCleared = false;

    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;

    @Column(name = "best_clear_time")
    private Integer bestClearTime; // 최단 클리어 시간 (초)

    @Column(name = "clear_count", nullable = false)
    private Integer clearCount = 0; // 클리어 횟수

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

    public Integer getStageNumber() {
        return stageNumber;
    }

    public void setStageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
    }

    public Boolean getIsCleared() {
        return isCleared;
    }

    public void setIsCleared(Boolean isCleared) {
        this.isCleared = isCleared;
    }

    public LocalDateTime getClearedAt() {
        return clearedAt;
    }

    public void setClearedAt(LocalDateTime clearedAt) {
        this.clearedAt = clearedAt;
    }

    public Integer getBestClearTime() {
        return bestClearTime;
    }

    public void setBestClearTime(Integer bestClearTime) {
        this.bestClearTime = bestClearTime;
    }

    public Integer getClearCount() {
        return clearCount;
    }

    public void setClearCount(Integer clearCount) {
        this.clearCount = clearCount;
    }
}

