package com.soi.worldtree.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 정령의 축복 획득 로그 엔티티
 * 정령이 죽었을 때 얻는 포인트를 기록
 * 
 * 확장 가능성:
 * - spiritId: 정령 ID (추후 추가 예정)
 * - spiritGrade: 정령 등급 (추후 추가 예정)
 * - spiritStatus: 정령 상태/친밀도 (추후 추가 예정)
 */
@Entity
@Table(name = "essence_pulse_logs")
public class EssencePulseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "content_source", nullable = false, length = 100)
    private String contentSource;

    // 정령 관련 필드 (추후 확장용, nullable)
    @Column(name = "spirit_id", nullable = true)
    private Long spiritId;           // 정령 ID

    @Column(name = "spirit_grade", nullable = true, length = 50)
    private String spiritGrade;      // 정령 등급 (COMMON, RARE, EPIC, LEGENDARY 등)

    @Column(name = "spirit_status", nullable = true, length = 50)
    private String spiritStatus;     // 정령 상태 (NORMAL, INJURED, EXHAUSTED 등)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public EssencePulseLog() {
    }

    public EssencePulseLog(Long userId, Integer amount, String contentSource) {
        this.userId = userId;
        this.amount = amount;
        this.contentSource = contentSource;
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

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getContentSource() {
        return contentSource;
    }

    public void setContentSource(String contentSource) {
        this.contentSource = contentSource;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSpiritId() {
        return spiritId;
    }

    public void setSpiritId(Long spiritId) {
        this.spiritId = spiritId;
    }

    public String getSpiritGrade() {
        return spiritGrade;
    }

    public void setSpiritGrade(String spiritGrade) {
        this.spiritGrade = spiritGrade;
    }

    public String getSpiritStatus() {
        return spiritStatus;
    }

    public void setSpiritStatus(String spiritStatus) {
        this.spiritStatus = spiritStatus;
    }
}

