package com.soi.spirit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 정령 행동 로그 엔티티
 * 정령의 자율 행동, 훈련, 아이템 사용 등을 기록
 */
@Entity
@Table(name = "spirit_actions")
public class SpiritAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spirit_id", nullable = false)
    private Long spiritId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // 행동 타입 (AUTONOMOUS, TRAINING, ITEM_USE, EVENT 등)

    @Column(name = "action_description", length = 500)
    private String actionDescription; // 행동 설명

    @Column(name = "result", length = 500)
    private String result; // 행동 결과

    @Column(name = "stat_changes", length = 500)
    private String statChanges; // 능력치 변화 (JSON 형식으로 저장 가능)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getStatChanges() {
        return statChanges;
    }

    public void setStatChanges(String statChanges) {
        this.statChanges = statChanges;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

