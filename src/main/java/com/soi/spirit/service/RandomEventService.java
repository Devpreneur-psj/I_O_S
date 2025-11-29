package com.soi.spirit.service;

import com.soi.spirit.entity.GameEvent;
import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.GameEventRepository;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.game.entity.GameTime;
import com.soi.game.repository.GameTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

/**
 * 랜덤 이벤트 서비스
 * 날씨 변화, 질병 발생, 정령 간 갈등 등을 처리
 */
@Service
@Transactional
public class RandomEventService {

    private final GameEventRepository gameEventRepository;
    private final SpiritRepository spiritRepository;
    private final GameTimeRepository gameTimeRepository;
    private final Random random = new Random();

    @Autowired
    public RandomEventService(GameEventRepository gameEventRepository,
                             SpiritRepository spiritRepository,
                             GameTimeRepository gameTimeRepository) {
        this.gameEventRepository = gameEventRepository;
        this.spiritRepository = spiritRepository;
        this.gameTimeRepository = gameTimeRepository;
    }

    /**
     * 랜덤 이벤트를 생성합니다.
     */
    public void generateRandomEvents(Long userId) {
        // 날씨 이벤트 (10% 확률)
        if (random.nextInt(100) < 10) {
            createWeatherEvent(userId);
        }
        
        // 질병 이벤트 (5% 확률)
        if (random.nextInt(100) < 5) {
            createDiseaseEvent(userId);
        }
        
        // 정령 간 갈등 이벤트 (8% 확률)
        if (random.nextInt(100) < 8) {
            createConflictEvent(userId);
        }
        
        // 특별 이벤트 (3% 확률)
        if (random.nextInt(100) < 3) {
            createSpecialEvent(userId);
        }
    }

    /**
     * 날씨 이벤트 생성
     */
    private void createWeatherEvent(Long userId) {
        GameTime gameTime = gameTimeRepository.findByUserId(userId).orElse(null);
        if (gameTime == null) {
            return;
        }
        
        String[] weathers = {"맑음", "흐림", "비", "눈", "바람"};
        String newWeather = weathers[random.nextInt(weathers.length)];
        
        gameTime.setCurrentWeather(newWeather);
        gameTimeRepository.save(gameTime);
        
        GameEvent event = new GameEvent();
        event.setUserId(userId);
        event.setEventType("WEATHER");
        event.setEventName("날씨 변화");
        event.setEventDescription("날씨가 " + newWeather + "로 변했습니다.");
        event.setEffect("정령들의 활동에 영향을 줄 수 있습니다.");
        gameEventRepository.save(event);
    }

    /**
     * 질병 이벤트 생성
     */
    private void createDiseaseEvent(Long userId) {
        List<Spirit> spirits = spiritRepository.findByUserId(userId);
        if (spirits.isEmpty()) {
            return;
        }
        
        Spirit spirit = spirits.get(random.nextInt(spirits.size()));
        if ("건강".equals(spirit.getHealthStatus())) {
            spirit.setHealthStatus("질병");
            spirit.setHappiness(Math.max(0, spirit.getHappiness() - 10));
            spiritRepository.save(spirit);
            
            GameEvent event = new GameEvent();
            event.setUserId(userId);
            event.setSpiritId(spirit.getId());
            event.setEventType("DISEASE");
            event.setEventName("질병 발생");
            event.setEventDescription(spirit.getName() + "이(가) 아프기 시작했습니다.");
            event.setEffect("치료가 필요합니다.");
            gameEventRepository.save(event);
        }
    }

    /**
     * 정령 간 갈등 이벤트 생성
     */
    private void createConflictEvent(Long userId) {
        List<Spirit> spirits = spiritRepository.findByUserId(userId);
        if (spirits.size() < 2) {
            return;
        }
        
        Spirit spirit1 = spirits.get(random.nextInt(spirits.size()));
        Spirit spirit2 = spirits.get(random.nextInt(spirits.size()));
        
        while (spirit1.getId().equals(spirit2.getId())) {
            spirit2 = spirits.get(random.nextInt(spirits.size()));
        }
        
        // 행복도 감소
        spirit1.setHappiness(Math.max(0, spirit1.getHappiness() - 5));
        spirit2.setHappiness(Math.max(0, spirit2.getHappiness() - 5));
        spiritRepository.save(spirit1);
        spiritRepository.save(spirit2);
        
        GameEvent event = new GameEvent();
        event.setUserId(userId);
        event.setEventType("CONFLICT");
        event.setEventName("정령 간 갈등");
        event.setEventDescription(spirit1.getName() + "과(와) " + spirit2.getName() + " 사이에 갈등이 발생했습니다.");
        event.setEffect("두 정령의 행복도가 감소했습니다.");
        gameEventRepository.save(event);
    }

    /**
     * 특별 이벤트 생성
     */
    private void createSpecialEvent(Long userId) {
        String[] specialEvents = {
            "행운의 날", "정령 축제", "특별 훈련 기회", "보물 발견"
        };
        
        String eventName = specialEvents[random.nextInt(specialEvents.length)];
        
        GameEvent event = new GameEvent();
        event.setUserId(userId);
        event.setEventType("SPECIAL");
        event.setEventName(eventName);
        event.setEventDescription("특별한 일이 발생했습니다!");
        event.setEffect("정령들에게 긍정적인 영향을 줄 수 있습니다.");
        gameEventRepository.save(event);
    }
}

