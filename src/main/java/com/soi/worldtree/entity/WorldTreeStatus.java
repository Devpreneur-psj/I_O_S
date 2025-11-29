package com.soi.worldtree.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "world_tree_status")
public class WorldTreeStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "current_level", nullable = false)
    private Integer currentLevel = 1;

    @Column(name = "current_exp", nullable = false)
    private Integer currentExp = 0;

    @Column(name = "available_essence", nullable = false)
    private Long availableEssence = 0L; // 사용 가능한 정령의 축복 잔량

    @Column(name = "rare_spirit_received", nullable = false)
    private Boolean rareSpiritReceived = false; // 15레벨 달성 시 희귀 정령 수령 여부

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

    public WorldTreeStatus() {
    }

    public WorldTreeStatus(Long userId) {
        this.userId = userId;
        this.currentLevel = 1;
        this.currentExp = 0;
        this.availableEssence = 0L;
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

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Integer getCurrentExp() {
        return currentExp;
    }

    public void setCurrentExp(Integer currentExp) {
        this.currentExp = currentExp;
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

    public Long getAvailableEssence() {
        return availableEssence;
    }

    public void setAvailableEssence(Long availableEssence) {
        this.availableEssence = availableEssence;
    }

    public Boolean getRareSpiritReceived() {
        return rareSpiritReceived != null ? rareSpiritReceived : false;
    }

    public void setRareSpiritReceived(Boolean rareSpiritReceived) {
        this.rareSpiritReceived = rareSpiritReceived != null ? rareSpiritReceived : false;
    }
}

