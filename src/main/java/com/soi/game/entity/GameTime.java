package com.soi.game.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 게임 시간 엔티티
 * 각 사용자의 게임 시간 진행 상태를 관리
 */
@Entity
@Table(name = "game_time")
public class GameTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "current_day", nullable = false)
    private Integer currentDay = 1; // 현재 게임 일수

    @Column(name = "current_hour", nullable = false)
    private Integer currentHour = 6; // 현재 게임 시간 (0-23)

    @Column(name = "last_update_time", nullable = false)
    private LocalDateTime lastUpdateTime; // 마지막 업데이트 시간 (실제 시간)

    @Column(name = "game_speed", nullable = false)
    private Integer gameSpeed = 1; // 게임 속도 배율 (1 = 정상, 2 = 2배속 등)

    @Column(name = "current_weather", length = 50)
    private String currentWeather = "맑음"; // 현재 날씨

    @PrePersist
    protected void onCreate() {
        if (lastUpdateTime == null) {
            // 서버 시간 사용 (시간대 고려)
            lastUpdateTime = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // 서버 시간 사용 (시간대 고려)
        // 주의: 이 메서드는 자동으로 호출되므로, GameTimeService에서 명시적으로 설정한 시간이 우선됩니다.
        // 하지만 안전을 위해 서버 시간으로 설정
        if (lastUpdateTime == null) {
            lastUpdateTime = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }
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

    public Integer getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(Integer currentDay) {
        this.currentDay = currentDay;
    }

    public Integer getCurrentHour() {
        return currentHour;
    }

    public void setCurrentHour(Integer currentHour) {
        this.currentHour = Math.max(0, Math.min(23, currentHour));
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Integer getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(Integer gameSpeed) {
        this.gameSpeed = Math.max(1, Math.min(10, gameSpeed));
    }

    public String getCurrentWeather() {
        return currentWeather != null ? currentWeather : "맑음";
    }

    public void setCurrentWeather(String currentWeather) {
        this.currentWeather = currentWeather;
    }
}

