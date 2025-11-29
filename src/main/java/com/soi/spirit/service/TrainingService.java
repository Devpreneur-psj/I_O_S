package com.soi.spirit.service;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.SpiritRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

/**
 * 훈련 서비스
 * 정령의 능력치 향상 및 성격 변화를 관리
 */
@Service
@Transactional
public class TrainingService {

    private final SpiritRepository spiritRepository;
    private final Random random = new Random();

    @Autowired
    public TrainingService(SpiritRepository spiritRepository) {
        this.spiritRepository = spiritRepository;
    }

    /**
     * 정령을 훈련시킵니다.
     */
    public void trainSpirit(Long userId, Long spiritId, String trainingType) {
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        if (!spirit.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 정령만 훈련시킬 수 있습니다.");
        }
        
        if (spirit.getEnergy() < 20) {
            throw new IllegalArgumentException("에너지가 부족합니다.");
        }
        
        // 훈련 타입에 따른 효과
        switch (trainingType) {
            case "ATTACK":
                trainAttack(spirit);
                break;
            case "DEFENSE":
                trainDefense(spirit);
                break;
            case "SPEED":
                trainSpeed(spirit);
                break;
            case "BALANCED":
                trainBalanced(spirit);
                break;
            default:
                throw new IllegalArgumentException("알 수 없는 훈련 타입입니다.");
        }
        
        // 에너지 소모
        spirit.setEnergy(Math.max(0, spirit.getEnergy() - 20));
        
        // 친밀도 증가
        spirit.setIntimacy(Math.min(10, spirit.getIntimacy() + 1));
        
        // 가끔 성격 변화 (10% 확률)
        if (random.nextInt(100) < 10) {
            adjustPersonality(spirit, trainingType);
        }
        
        spiritRepository.save(spirit);
    }

    /**
     * 공격 훈련
     */
    private void trainAttack(Spirit spirit) {
        if (random.nextBoolean()) {
            spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 2));
        } else {
            spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 2));
        }
    }

    /**
     * 방어 훈련
     */
    private void trainDefense(Spirit spirit) {
        if (random.nextBoolean()) {
            spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 2));
        } else {
            spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 2));
        }
    }

    /**
     * 스피드 훈련
     */
    private void trainSpeed(Spirit spirit) {
        spirit.setSpeed(Math.min(100, spirit.getSpeed() + 2));
    }

    /**
     * 균형 훈련
     */
    private void trainBalanced(Spirit spirit) {
        int stat = random.nextInt(5);
        switch (stat) {
            case 0: spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 1)); break;
            case 1: spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 1)); break;
            case 2: spirit.setSpeed(Math.min(100, spirit.getSpeed() + 1)); break;
            case 3: spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 1)); break;
            case 4: spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 1)); break;
        }
    }

    /**
     * 훈련에 따른 성격 조정
     */
    private void adjustPersonality(Spirit spirit, String trainingType) {
        String currentPersonality = spirit.getPersonality();
        
        // 훈련 타입에 따라 성격이 변화할 수 있음
        // 예: 공격 훈련을 많이 하면 "용감" 성격으로 변화 가능
        if ("ATTACK".equals(trainingType) && random.nextInt(100) < 30) {
            if (!"용감".equals(currentPersonality)) {
                spirit.setPersonality("용감");
            }
        } else if ("DEFENSE".equals(trainingType) && random.nextInt(100) < 30) {
            if (!"온순".equals(currentPersonality)) {
                spirit.setPersonality("온순");
            }
        }
    }
}

