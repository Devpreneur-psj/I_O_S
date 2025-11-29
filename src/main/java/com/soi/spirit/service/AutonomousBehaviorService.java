package com.soi.spirit.service;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.SpiritRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

/**
 * 정령 자율 행동 서비스
 * 정령들이 주인의 관리 없이 자율적으로 행동하는 시스템
 * 배고픔, 행복도 등을 시간 기반으로 관리합니다.
 */
@Service
@Transactional
public class AutonomousBehaviorService {

    private final SpiritRepository spiritRepository;
    private final Random random = new Random();
    
    // 서버 시간대 (GameTimeService와 동일)
    private static final ZoneId SERVER_TIMEZONE = ZoneId.systemDefault();

    @Autowired
    public AutonomousBehaviorService(SpiritRepository spiritRepository) {
        this.spiritRepository = spiritRepository;
    }
    
    /**
     * 서버의 현재 시간을 가져옵니다.
     */
    private LocalDateTime getServerNow() {
        return ZonedDateTime.now(SERVER_TIMEZONE).toLocalDateTime();
    }

    /**
     * 정령들의 자율 행동을 처리합니다.
     * 시간이 지나면 정령들이 자동으로 행동합니다.
     */
    public void processAutonomousBehaviors(Long userId) {
        List<Spirit> spirits = spiritRepository.findByUserId(userId);
        LocalDateTime serverNow = getServerNow();
        
        for (Spirit spirit : spirits) {
            if (spirit.getIsRetired() != null && spirit.getIsRetired()) {
                continue; // 은퇴한 정령은 제외
            }
            if (spirit.getEvolutionInProgress() != null && spirit.getEvolutionInProgress()) {
                continue; // 진화 중인 정령은 제외
            }
            
            // 시간 기반 상태 업데이트 (배고픔, 행복도)
            updateStatusBasedOnTime(spirit, serverNow);
            
            // 행복도가 0인 상태로 하루가 지났는지 확인
            checkAndHandleSpiritLeaving(spirit, serverNow);
            
            // 성격에 따른 자율 행동
            performAutonomousAction(spirit);
        }
    }
    
    /**
     * 시간 기반으로 배고픔과 행복도를 업데이트합니다.
     */
    private void updateStatusBasedOnTime(Spirit spirit, LocalDateTime serverNow) {
        LocalDateTime lastUpdate = spirit.getLastStatusUpdateTime();
        
        if (lastUpdate == null) {
            // 처음 업데이트하는 경우
            spirit.setLastStatusUpdateTime(serverNow);
            spirit.setLastEnergyValue(spirit.getEnergy() != null ? spirit.getEnergy() : 100);
            spiritRepository.save(spirit);
            return;
        }
        
        // 경과 시간 계산 (시간 단위)
        long hoursPassed = java.time.Duration.between(lastUpdate, serverNow).toHours();
        
        if (hoursPassed <= 0) {
            return; // 시간이 지나지 않았으면 업데이트하지 않음
        }
        
        int currentEnergy = spirit.getEnergy() != null ? spirit.getEnergy() : 100;
        int lastEnergy = spirit.getLastEnergyValue() != null ? spirit.getLastEnergyValue() : 100;
        int currentHunger = spirit.getHunger() != null ? spirit.getHunger() : 50;
        int currentHappiness = spirit.getHappiness() != null ? spirit.getHappiness() : 50;
        
        // 1. 배고픔 업데이트: 사용한 에너지와 지난 시간을 기준으로 증가
        // 에너지가 감소하면 배고픔 증가, 시간이 지나도 배고픔 증가
        int energyUsed = Math.max(0, lastEnergy - currentEnergy);
        int hungerIncrease = (int)(hoursPassed * 2) + (energyUsed / 5); // 시간당 2씩 증가, 에너지 5당 1씩 증가
        int newHunger = Math.min(100, currentHunger + hungerIncrease);
        spirit.setHunger(newHunger);
        
        // 2. 행복도 업데이트
        // 배고픔이 70 이상(배고픔 상태)이 오래 지속되면 행복도 감소
        // 배고픔이 30 이하(배부름 상태)이면 행복도 회복
        if (newHunger >= 70) {
            // 배고픔 상태: 시간이 지날수록 행복도 감소
            int happinessDecrease = (int)(hoursPassed * 1); // 시간당 1씩 감소
            int newHappiness = Math.max(0, currentHappiness - happinessDecrease);
            spirit.setHappiness(newHappiness);
            
            // 행복도가 0이 되면 시점 기록
            if (newHappiness == 0 && spirit.getHappinessZeroSince() == null) {
                spirit.setHappinessZeroSince(serverNow);
            }
        } else if (newHunger <= 30) {
            // 배부름 상태: 시간이 지날수록 행복도 회복
            int happinessIncrease = (int)(hoursPassed * 1); // 시간당 1씩 회복
            int newHappiness = Math.min(100, currentHappiness + happinessIncrease);
            spirit.setHappiness(newHappiness);
            
            // 행복도가 0보다 크면 happinessZeroSince 초기화
            if (newHappiness > 0 && spirit.getHappinessZeroSince() != null) {
                spirit.setHappinessZeroSince(null);
            }
        }
        
        // 마지막 업데이트 시간 및 에너지 값 저장
        spirit.setLastStatusUpdateTime(serverNow);
        spirit.setLastEnergyValue(currentEnergy);
        
        spiritRepository.save(spirit);
    }
    
    /**
     * 행복도가 0인 상태로 하루가 지났는지 확인하고, 정령이 마을을 떠나도록 처리합니다.
     */
    private void checkAndHandleSpiritLeaving(Spirit spirit, LocalDateTime serverNow) {
        if (spirit.getHappiness() != null && spirit.getHappiness() == 0) {
            LocalDateTime happinessZeroSince = spirit.getHappinessZeroSince();
            
            if (happinessZeroSince != null) {
                // 행복도가 0인 상태로 경과한 시간 계산
                long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(
                    happinessZeroSince.toLocalDate(), 
                    serverNow.toLocalDate()
                );
                
                // 하루가 지났으면 정령이 마을을 떠남 (은퇴 처리)
                if (daysPassed >= 1) {
                    spirit.setIsRetired(true);
                    spirit.setRetiredAt(serverNow);
                    spirit.setHappiness(0);
                    // 정령이 마을을 떠났다는 메시지 로그
                    System.out.println("정령 ID " + spirit.getId() + " (" + spirit.getName() + 
                                     ")이(가) 행복도가 0인 상태로 하루가 지나 마을을 떠났습니다.");
                }
            } else {
                // 행복도가 0인데 시점이 기록되지 않았으면 지금 기록
                spirit.setHappinessZeroSince(serverNow);
            }
        } else {
            // 행복도가 0이 아니면 happinessZeroSince 초기화
            if (spirit.getHappinessZeroSince() != null) {
                spirit.setHappinessZeroSince(null);
            }
        }
        
        spiritRepository.save(spirit);
    }

    /**
     * 정령의 자율 행동을 수행합니다.
     * 시간 기반 상태 업데이트는 updateStatusBasedOnTime에서 처리되므로,
     * 여기서는 성격별 특별 행동만 처리합니다.
     */
    private void performAutonomousAction(Spirit spirit) {
        String personality = spirit.getPersonality();
        
        // 에너지가 낮으면 휴식 (시간 기반 업데이트와 별개로 즉시 회복)
        if (spirit.getEnergy() != null && spirit.getEnergy() < 30) {
            spirit.setEnergy(Math.min(100, spirit.getEnergy() + 5));
        }
        
        // 건강 상태가 나쁘면 치료 요청 (행복도 감소)
        if (spirit.getHealthStatus() != null && !"건강".equals(spirit.getHealthStatus())) {
            int currentHappiness = spirit.getHappiness() != null ? spirit.getHappiness() : 50;
            spirit.setHappiness(Math.max(0, currentHappiness - 2));
        }
        
        // 성격별 특별 행동
        switch (personality) {
            case "고집":
                // 고집스러운 정령: 혼자 시간 보내기 (에너지 소모 적음, 행복도 유지)
                if (spirit.getEnergy() > 50) {
                    spirit.setEnergy(Math.max(0, spirit.getEnergy() - 1));
                }
                break;
            case "조심":
                // 조심스러운 정령: 안전한 곳에서 휴식 (에너지 회복, 행복도 약간 증가)
                spirit.setEnergy(Math.min(100, spirit.getEnergy() + 3));
                spirit.setHappiness(Math.min(100, spirit.getHappiness() + 1));
                break;
            case "장난꾸러기":
                // 장난꾸러기: 놀이 활동 (에너지 소모, 행복도 증가)
                if (spirit.getEnergy() > 30) {
                    spirit.setEnergy(Math.max(0, spirit.getEnergy() - 3));
                    spirit.setHappiness(Math.min(100, spirit.getHappiness() + 2));
                }
                break;
            case "온순":
                // 온순한 정령: 평화롭게 시간 보내기 (행복도 유지)
                spirit.setHappiness(Math.min(100, spirit.getHappiness() + 1));
                break;
            case "용감":
                // 용감한 정령: 활동적인 행동 (에너지 소모, 능력치 약간 향상 가능)
                if (spirit.getEnergy() > 40) {
                    spirit.setEnergy(Math.max(0, spirit.getEnergy() - 4));
                    // 가끔 능력치 약간 향상
                    if (random.nextInt(100) < 5) {
                        int stat = random.nextInt(5);
                        switch (stat) {
                            case 0: spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 1)); break;
                            case 1: spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 1)); break;
                            case 2: spirit.setSpeed(Math.min(100, spirit.getSpeed() + 1)); break;
                            case 3: spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 1)); break;
                            case 4: spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 1)); break;
                        }
                    }
                }
                break;
        }
        
        // 나이 증가 (하루에 한 번)
        // TODO: 시간 시스템과 연동 필요
        
        spiritRepository.save(spirit);
    }
}

