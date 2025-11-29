package com.soi.community.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "spirit_square_presence", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "channel_number"})
})
public class SpiritSquarePresence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "username", nullable = false, length = 50)
    private String username;
    
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;
    
    @Column(name = "channel_number", nullable = false)
    private Integer channelNumber; // 1-5
    
    @Column(name = "spirit_id", nullable = false)
    private Long spiritId; // 대표 정령 ID
    
    @Column(name = "spirit_name", nullable = false, length = 100)
    private String spiritName;
    
    @Column(name = "spirit_type", nullable = false, length = 50)
    private String spiritType;
    
    @Column(name = "spirit_evolution_stage", nullable = false)
    private Integer spiritEvolutionStage;
    
    @Column(name = "position_x", nullable = false)
    private Double positionX;
    
    @Column(name = "position_y", nullable = false)
    private Double positionY;
    
    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastActivity = LocalDateTime.now();
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
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public Integer getChannelNumber() {
        return channelNumber;
    }
    
    public void setChannelNumber(Integer channelNumber) {
        this.channelNumber = channelNumber;
    }
    
    public Long getSpiritId() {
        return spiritId;
    }
    
    public void setSpiritId(Long spiritId) {
        this.spiritId = spiritId;
    }
    
    public String getSpiritName() {
        return spiritName;
    }
    
    public void setSpiritName(String spiritName) {
        this.spiritName = spiritName;
    }
    
    public String getSpiritType() {
        return spiritType;
    }
    
    public void setSpiritType(String spiritType) {
        this.spiritType = spiritType;
    }
    
    public Integer getSpiritEvolutionStage() {
        return spiritEvolutionStage;
    }
    
    public void setSpiritEvolutionStage(Integer spiritEvolutionStage) {
        this.spiritEvolutionStage = spiritEvolutionStage;
    }
    
    public Double getPositionX() {
        return positionX;
    }
    
    public void setPositionX(Double positionX) {
        this.positionX = positionX;
    }
    
    public Double getPositionY() {
        return positionY;
    }
    
    public void setPositionY(Double positionY) {
        this.positionY = positionY;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
}

