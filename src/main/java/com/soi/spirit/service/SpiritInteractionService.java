package com.soi.spirit.service;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.entity.SpiritAction;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.spirit.repository.SpiritActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 정령 간 상호작용 서비스
 * 정령들 간의 친구 관계, 협력, 경쟁, 갈등 등을 처리합니다.
 */
@Service
@Transactional
public class SpiritInteractionService {

    private final SpiritRepository spiritRepository;
    private final SpiritActionRepository spiritActionRepository;
    private final SpiritAIService spiritAIService;
    private final Random random = new Random();

    @Autowired
    public SpiritInteractionService(SpiritRepository spiritRepository,
                                   SpiritActionRepository spiritActionRepository,
                                   SpiritAIService spiritAIService) {
        this.spiritRepository = spiritRepository;
        this.spiritActionRepository = spiritActionRepository;
        this.spiritAIService = spiritAIService;
    }

    /**
     * 정령들 간의 상호작용을 처리합니다.
     */
    public void processInteractions(Long userId) {
        List<Spirit> spirits = spiritRepository.findByUserId(userId);
        
        if (spirits.size() < 2) {
            return; // 정령이 2마리 미만이면 상호작용 불가
        }

        // 은퇴하거나 진화 중인 정령 제외
        List<Spirit> activeSpirits = spirits.stream()
                .filter(spirit -> !(spirit.getIsRetired() != null && spirit.getIsRetired()))
                .filter(spirit -> !(spirit.getEvolutionInProgress() != null && spirit.getEvolutionInProgress()))
                .toList();

        if (activeSpirits.size() < 2) {
            return;
        }

        // 랜덤하게 두 정령을 선택하여 상호작용
        if (random.nextInt(100) < 30) { // 30% 확률로 상호작용 발생
            Spirit spirit1 = activeSpirits.get(random.nextInt(activeSpirits.size()));
            Spirit spirit2 = activeSpirits.get(random.nextInt(activeSpirits.size()));
            
            while (spirit1.getId().equals(spirit2.getId())) {
                spirit2 = activeSpirits.get(random.nextInt(activeSpirits.size()));
            }

            String interactionType = spiritAIService.decideInteraction(spirit1, spirit2);
            applyInteraction(spirit1, spirit2, interactionType);
        }
    }

    /**
     * 상호작용을 적용하고 결과를 처리합니다.
     */
    private void applyInteraction(Spirit spirit1, Spirit spirit2, String interactionType) {
        switch (interactionType) {
            case "FRIENDLY_INTERACTION":
                handleFriendlyInteraction(spirit1, spirit2);
                break;
            case "COMPETITIVE_INTERACTION":
                handleCompetitiveInteraction(spirit1, spirit2);
                break;
            case "PLAY_TOGETHER":
                handlePlayTogether(spirit1, spirit2);
                break;
            case "CONFLICT":
                handleConflict(spirit1, spirit2);
                break;
            case "NEUTRAL":
                // 상호작용 없음
                break;
            default:
                break;
        }
    }

    /**
     * 친근한 상호작용 처리
     */
    private void handleFriendlyInteraction(Spirit spirit1, Spirit spirit2) {
        spirit1.setHappiness(Math.min(100, spirit1.getHappiness() + 3));
        spirit2.setHappiness(Math.min(100, spirit2.getHappiness() + 3));
        
        logInteraction(spirit1, spirit2, "친근한 상호작용", 
                spirit1.getName() + "과(와) " + spirit2.getName() + "이(가) 친근하게 대화했습니다.",
                "두 정령의 행복도가 +3 증가했습니다.");
        
        spiritRepository.save(spirit1);
        spiritRepository.save(spirit2);
    }

    /**
     * 경쟁적인 상호작용 처리
     */
    private void handleCompetitiveInteraction(Spirit spirit1, Spirit spirit2) {
        // 능력치가 높은 정령이 승리
        int totalStats1 = spirit1.getMeleeAttack() + spirit1.getRangedAttack() + 
                         spirit1.getSpeed() + spirit1.getMeleeDefense() + spirit1.getRangedDefense();
        int totalStats2 = spirit2.getMeleeAttack() + spirit2.getRangedAttack() + 
                         spirit2.getSpeed() + spirit2.getMeleeDefense() + spirit2.getRangedDefense();

        if (totalStats1 > totalStats2) {
            spirit1.setHappiness(Math.min(100, spirit1.getHappiness() + 5));
            spirit2.setHappiness(Math.max(0, spirit2.getHappiness() - 2));
            logInteraction(spirit1, spirit2, "경쟁", 
                    spirit1.getName() + "이(가) " + spirit2.getName() + "과(와)의 경쟁에서 승리했습니다.",
                    spirit1.getName() + "의 행복도 +5, " + spirit2.getName() + "의 행복도 -2");
        } else if (totalStats2 > totalStats1) {
            spirit2.setHappiness(Math.min(100, spirit2.getHappiness() + 5));
            spirit1.setHappiness(Math.max(0, spirit1.getHappiness() - 2));
            logInteraction(spirit1, spirit2, "경쟁", 
                    spirit2.getName() + "이(가) " + spirit1.getName() + "과(와)의 경쟁에서 승리했습니다.",
                    spirit2.getName() + "의 행복도 +5, " + spirit1.getName() + "의 행복도 -2");
        } else {
            // 무승부
            spirit1.setHappiness(Math.min(100, spirit1.getHappiness() + 2));
            spirit2.setHappiness(Math.min(100, spirit2.getHappiness() + 2));
            logInteraction(spirit1, spirit2, "경쟁", 
                    spirit1.getName() + "과(와) " + spirit2.getName() + "이(가) 무승부를 기록했습니다.",
                    "두 정령의 행복도가 +2 증가했습니다.");
        }
        
        spiritRepository.save(spirit1);
        spiritRepository.save(spirit2);
    }

    /**
     * 함께 놀기 처리
     */
    private void handlePlayTogether(Spirit spirit1, Spirit spirit2) {
        spirit1.setHappiness(Math.min(100, spirit1.getHappiness() + 4));
        spirit2.setHappiness(Math.min(100, spirit2.getHappiness() + 4));
        spirit1.setEnergy(Math.max(0, spirit1.getEnergy() - 5));
        spirit2.setEnergy(Math.max(0, spirit2.getEnergy() - 5));
        
        logInteraction(spirit1, spirit2, "함께 놀기", 
                spirit1.getName() + "과(와) " + spirit2.getName() + "이(가) 함께 놀았습니다.",
                "두 정령의 행복도 +4, 에너지 -5");
        
        spiritRepository.save(spirit1);
        spiritRepository.save(spirit2);
    }

    /**
     * 갈등 처리
     */
    private void handleConflict(Spirit spirit1, Spirit spirit2) {
        spirit1.setHappiness(Math.max(0, spirit1.getHappiness() - 5));
        spirit2.setHappiness(Math.max(0, spirit2.getHappiness() - 5));
        
        logInteraction(spirit1, spirit2, "갈등", 
                spirit1.getName() + "과(와) " + spirit2.getName() + " 사이에 갈등이 발생했습니다.",
                "두 정령의 행복도가 -5 감소했습니다.");
        
        spiritRepository.save(spirit1);
        spiritRepository.save(spirit2);
    }

    /**
     * 상호작용 로그 기록
     */
    private void logInteraction(Spirit spirit1, Spirit spirit2, String interactionType, 
                               String description, String result) {
        SpiritAction action = new SpiritAction();
        action.setSpiritId(spirit1.getId());
        action.setActionType("INTERACTION_" + interactionType);
        action.setActionDescription(description);
        action.setResult(result);
        action.setStatChanges(String.format("{\"spirit2_id\": %d}", spirit2.getId()));
        spiritActionRepository.save(action);
    }
}

