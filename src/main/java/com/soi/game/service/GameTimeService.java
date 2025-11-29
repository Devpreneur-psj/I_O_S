package com.soi.game.service;

import com.soi.game.entity.GameTime;
import com.soi.game.repository.GameTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;

/**
 * 게임 시간 관리 서비스
 * 서버 시간을 기준으로 게임 시간을 현실 시간과 동기화합니다.
 */
@Service
@Transactional
public class GameTimeService {

    private final GameTimeRepository gameTimeRepository;
    private final Random random = new Random();
    
    // 서버 시간대 (배포 환경에 맞게 설정 가능)
    // 기본값은 시스템 기본 시간대를 사용하지만, 필요시 명시적으로 설정 가능
    private static final ZoneId SERVER_TIMEZONE = ZoneId.systemDefault();

    @Autowired
    public GameTimeService(GameTimeRepository gameTimeRepository) {
        this.gameTimeRepository = gameTimeRepository;
    }

    /**
     * 서버의 현재 시간을 가져옵니다.
     * 배포 환경에서도 일관된 서버 시간을 사용합니다.
     */
    private LocalDateTime getServerNow() {
        return ZonedDateTime.now(SERVER_TIMEZONE).toLocalDateTime();
    }

    /**
     * 사용자의 게임 시간을 초기화합니다.
     * 게임 시간은 서버 시간과 동기화됩니다.
     */
    public GameTime initializeGameTime(Long userId) {
        GameTime gameTime = gameTimeRepository.findByUserId(userId)
                .orElse(new GameTime());
        
        LocalDateTime serverNow = getServerNow();
        
        gameTime.setUserId(userId);
        // 게임 시간을 서버 시간과 동기화 (현실 시간 기준)
        gameTime.setCurrentDay(1);
        gameTime.setCurrentHour(serverNow.getHour()); // 서버 시간의 현재 시간으로 초기화
        gameTime.setCurrentWeather("맑음");
        gameTime.setGameSpeed(1);
        gameTime.setLastUpdateTime(serverNow); // 서버 시간 저장
        
        return gameTimeRepository.save(gameTime);
    }

    /**
     * 게임 시간을 가져오거나 초기화합니다.
     */
    public GameTime getOrInitializeGameTime(Long userId) {
        return gameTimeRepository.findByUserId(userId)
                .orElseGet(() -> initializeGameTime(userId));
    }

    /**
     * 게임 시간을 진행시킵니다.
     * 서버 시간을 기준으로 현실 시간과 1:1로 동기화됩니다.
     * (1실제 시간 = 1게임 시간)
     */
    public GameTime advanceGameTime(Long userId) {
        GameTime gameTime = getOrInitializeGameTime(userId);
        LocalDateTime serverNow = getServerNow(); // 서버 시간 사용
        LocalDateTime lastUpdate = gameTime.getLastUpdateTime();
        
        if (lastUpdate == null) {
            // lastUpdateTime이 없으면 현재 서버 시간으로 초기화
            lastUpdate = serverNow;
            gameTime.setLastUpdateTime(serverNow);
            gameTime.setCurrentHour(serverNow.getHour());
            gameTimeRepository.save(gameTime);
            return gameTime;
        }
        
        // 서버 시간과 게임 시간의 차이 계산 (1실제 시간 = 1게임 시간)
        long hoursPassed = java.time.Duration.between(lastUpdate, serverNow).toHours();
        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(lastUpdate.toLocalDate(), serverNow.toLocalDate());
        
        if (hoursPassed > 0 || daysPassed > 0) {
            // 게임 시간을 서버 시간과 동기화
            // 서버 시간의 현재 시간을 게임 시간으로 사용
            int serverHour = serverNow.getHour();
            int newDay = gameTime.getCurrentDay() + (int)daysPassed;
            int newHour = serverHour;
            
            // 게임 속도 적용 (1보다 크면 추가 시간 진행)
            if (gameTime.getGameSpeed() > 1 && hoursPassed > 0) {
                int additionalHours = (int)hoursPassed * (gameTime.getGameSpeed() - 1);
                newHour += additionalHours;
                
                // 24시간이 지나면 다음 날
                while (newHour >= 24) {
                    newHour -= 24;
                    newDay++;
                }
            }
            
            gameTime.setCurrentHour(newHour);
            gameTime.setCurrentDay(newDay);
            gameTime.setLastUpdateTime(serverNow); // 서버 시간으로 업데이트
            
            // 날씨 랜덤 변경 (하루가 지났고 새벽 6시일 때)
            if (daysPassed > 0 && newHour == 6) {
                gameTime.setCurrentWeather(getRandomWeather());
            }
            
            gameTimeRepository.save(gameTime);
        } else {
            // 시간이 지나지 않았어도 서버 시간과 동기화 (정확도 향상)
            // 게임 시간을 서버 시간의 현재 시간으로 설정
            int serverHour = serverNow.getHour();
            if (gameTime.getCurrentHour() != serverHour) {
                gameTime.setCurrentHour(serverHour);
                gameTime.setLastUpdateTime(serverNow);
                gameTimeRepository.save(gameTime);
            }
        }
        
        return gameTime;
    }

    /**
     * 랜덤 날씨 생성
     */
    private String getRandomWeather() {
        String[] weathers = {"맑음", "흐림", "비", "눈", "바람"};
        return weathers[random.nextInt(weathers.length)];
    }

    /**
     * 게임 속도 설정
     */
    public void setGameSpeed(Long userId, Integer speed) {
        GameTime gameTime = getOrInitializeGameTime(userId);
        gameTime.setGameSpeed(Math.max(1, Math.min(10, speed)));
        gameTimeRepository.save(gameTime);
    }
}

