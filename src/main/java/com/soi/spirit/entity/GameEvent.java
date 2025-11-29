package com.soi.spirit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 게임 이벤트 엔티티
 * 랜덤 이벤트, 날씨 변화, 질병 발생 등을 기록
 */
@Entity
@Table(name = "game_events")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spirit_id")
    private Long spiritId; // 특정 정령과 관련된 이벤트인 경우

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // 이벤트 타입 (WEATHER, DISEASE, CONFLICT, SPECIAL 등)

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName; // 이벤트 이름

    @Column(name = "event_description", length = 1000)
    private String eventDescription; // 이벤트 설명

    @Column(name = "effect", length = 500)
    private String effect; // 이벤트 효과

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false; // 해결 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSpiritId() {
        return spiritId;
    }

    public void setSpiritId(Long spiritId) {
        this.spiritId = spiritId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
        if (isResolved && resolvedAt == null) {
            this.resolvedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

