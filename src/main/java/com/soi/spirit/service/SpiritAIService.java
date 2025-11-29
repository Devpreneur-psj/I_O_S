package com.soi.spirit.service;

import com.soi.spirit.entity.Spirit;
import com.soi.game.entity.GameTime;
import com.soi.game.repository.GameTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

/**
 * 정령 AI 서비스
 * 정령들이 상황에 맞는 최적의 행동을 결정하는 AI 시스템
 */
@Service
@Transactional
public class SpiritAIService {

    private final GameTimeRepository gameTimeRepository;
    private final Random random = new Random();

    @Autowired
    public SpiritAIService(GameTimeRepository gameTimeRepository) {
        this.gameTimeRepository = gameTimeRepository;
    }

    /**
     * 정령의 현재 상황을 분석하고 최적의 행동을 결정합니다.
     */
    public String decideBestAction(Spirit spirit, Long userId) {
        // 상황 분석
        int hunger = spirit.getHunger() != null ? spirit.getHunger() : 50;
        int energy = spirit.getEnergy() != null ? spirit.getEnergy() : 100;
        int happiness = spirit.getHappiness() != null ? spirit.getHappiness() : 50;
        String healthStatus = spirit.getHealthStatus() != null ? spirit.getHealthStatus() : "건강";
        String personality = spirit.getPersonality();
        
        // 게임 시간 확인
        GameTime gameTime = gameTimeRepository.findByUserId(userId).orElse(null);
        String weather = (gameTime != null && gameTime.getCurrentWeather() != null) 
                ? gameTime.getCurrentWeather() : "맑음";

        // 우선순위 기반 행동 결정
        // 1. 건강 상태가 나쁘면 치료 요청
        if (!"건강".equals(healthStatus) && !"은퇴".equals(healthStatus)) {
            return "HEALTH_TREATMENT_REQUEST";
        }

        // 2. 배고픔이 70 이상이면 음식 요청
        if (hunger >= 70) {
            return "FOOD_REQUEST";
        }

        // 3. 에너지가 20 이하면 휴식
        if (energy <= 20) {
            return "REST";
        }

        // 4. 행복도가 30 이하면 놀이/활동 요청
        if (happiness <= 30) {
            return "PLAY_REQUEST";
        }

        // 5. 날씨에 따른 행동
        if ("비".equals(weather) || "눈".equals(weather)) {
            return "INDOOR_ACTIVITY";
        }

        // 6. 성격에 따른 특별 행동
        switch (personality) {
            case "고집":
                return hunger >= 50 ? "SOLITARY_REST" : "INDEPENDENT_ACTIVITY";
            case "조심":
                return energy > 50 ? "CAUTIOUS_EXPLORATION" : "SAFE_REST";
            case "장난꾸러기":
                return energy > 40 ? "PLAY" : "REST";
            case "온순":
                return "PEACEFUL_ACTIVITY";
            case "용감":
                return energy > 50 ? "ADVENTURE" : "REST";
            default:
                return "NORMAL_ACTIVITY";
        }
    }

    /**
     * 정령 간 상호작용을 결정합니다.
     * 친구 관계, 협력, 경쟁 등을 고려합니다.
     */
    public String decideInteraction(Spirit spirit1, Spirit spirit2) {
        if (spirit1.getId().equals(spirit2.getId())) {
            return "NONE";
        }

        String personality1 = spirit1.getPersonality();
        String personality2 = spirit2.getPersonality();

        // 성격 조합에 따른 상호작용
        // 온순 + 온순 = 친구
        if ("온순".equals(personality1) && "온순".equals(personality2)) {
            return "FRIENDLY_INTERACTION";
        }

        // 용감 + 용감 = 경쟁
        if ("용감".equals(personality1) && "용감".equals(personality2)) {
            return "COMPETITIVE_INTERACTION";
        }

        // 장난꾸러기 + 장난꾸러기 = 놀이
        if ("장난꾸러기".equals(personality1) && "장난꾸러기".equals(personality2)) {
            return "PLAY_TOGETHER";
        }

        // 고집 + 조심 = 갈등 가능
        if (("고집".equals(personality1) && "조심".equals(personality2)) ||
            ("조심".equals(personality1) && "고집".equals(personality2))) {
            return random.nextInt(100) < 30 ? "CONFLICT" : "NEUTRAL";
        }

        return "NEUTRAL";
    }

    /**
     * 정령이 주변 환경에 반응하는 행동을 결정합니다.
     */
    public String reactToEnvironment(Spirit spirit, String weather, List<Spirit> nearbySpirits) {
        // 날씨 반응
        switch (weather) {
            case "비":
            case "눈":
                return "SEEK_SHELTER";
            case "맑음":
                if (spirit.getEnergy() > 60 && random.nextInt(100) < 40) {
                    return "ENJOY_WEATHER";
                }
                break;
        }

        // 주변 정령과의 상호작용
        if (nearbySpirits != null && !nearbySpirits.isEmpty()) {
            Spirit nearbySpirit = nearbySpirits.get(random.nextInt(nearbySpirits.size()));
            return decideInteraction(spirit, nearbySpirit);
        }

        return "NORMAL";
    }
}

