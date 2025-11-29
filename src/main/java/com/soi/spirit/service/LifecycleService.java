package com.soi.spirit.service;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.worldtree.service.WorldTreeService;
import com.soi.worldtree.dto.EssencePulseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 생애 주기 관리 서비스
 * 정령의 나이, 은퇴, 사망 등을 관리
 */
@Service
@Transactional
public class LifecycleService {

    private final SpiritRepository spiritRepository;
    private final WorldTreeService worldTreeService;

    @Autowired
    public LifecycleService(SpiritRepository spiritRepository, WorldTreeService worldTreeService) {
        this.spiritRepository = spiritRepository;
        this.worldTreeService = worldTreeService;
    }

    /**
     * 정령들의 나이를 증가시키고 생애 주기를 관리합니다.
     */
    public void processLifecycle(Long userId) {
        List<Spirit> spirits = spiritRepository.findByUserId(userId);
        
        for (Spirit spirit : spirits) {
            if (spirit.getIsRetired()) {
                continue; // 이미 은퇴한 정령은 제외
            }
            
            // 나이 증가 (하루에 1씩)
            spirit.setAge(spirit.getAge() + 1);
            
            // 50레벨 달성 후 10일 경과 시 수명 카운트다운 시작
            if (spirit.getMaxLevelReached() && spirit.getLifespanCountdown() == null) {
                spirit.setLifespanCountdown(LocalDateTime.now());
            }
            
            // 수명 관리
            if (spirit.getLifespanCountdown() != null) {
                LocalDateTime countdownStart = spirit.getLifespanCountdown();
                long daysSinceMaxLevel = java.time.Duration.between(countdownStart, LocalDateTime.now()).toDays();
                int remainingDays = 10 + spirit.getLifespanExtended() - (int)daysSinceMaxLevel;
                
                if (remainingDays <= 0 && !spirit.getIsRetired()) {
                    // 은퇴 처리 및 정령의 축복 추가
                    int blessing = retireSpirit(spirit);
                    // 정령의 축복 추가: 친밀도 * 10
                    try {
                        EssencePulseRequest request = new EssencePulseRequest();
                        request.setAmount(blessing);
                        worldTreeService.addBlessing(spirit.getUserId(), request);
                    } catch (Exception e) {
                        System.err.println("Error adding blessing for retired spirit: " + e.getMessage());
                    }
                } else if (remainingDays <= 3) {
                    // 수명이 얼마 남지 않음 (경고)
                    spirit.setHealthStatus("노쇠");
                    spirit.setHappiness(Math.max(0, spirit.getHappiness() - 5));
                }
            }
            
            // 나이가 많아지면 건강 상태 악화 가능
            if (spirit.getAge() > 30 && Math.random() < 0.1) {
                if ("건강".equals(spirit.getHealthStatus())) {
                    spirit.setHealthStatus("아픔");
                }
            }
            
            spiritRepository.save(spirit);
        }
    }

    /**
     * 정령을 은퇴시킵니다.
     * 정령의 축복 = 친밀도 * 10
     */
    public int retireSpirit(Spirit spirit) {
        spirit.setIsRetired(true);
        spirit.setRetiredAt(LocalDateTime.now());
        spirit.setHealthStatus("은퇴");
        spirit.setHappiness(50); // 은퇴 후 행복도 초기화
        spiritRepository.save(spirit);
        
        // 정령의 축복 계산: 친밀도 * 10
        int blessing = spirit.getIntimacy() * 10;
        return blessing;
    }

    /**
     * 정령의 수명을 연장합니다.
     */
    public void extendLifespan(Long userId, Long spiritId, int days) {
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        if (!spirit.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 정령만 수명을 연장할 수 있습니다.");
        }
        
        if (spirit.getIsRetired()) {
            throw new IllegalArgumentException("이미 은퇴한 정령입니다.");
        }
        
        spirit.setLifespanExtended(spirit.getLifespanExtended() + days);
        spiritRepository.save(spirit);
    }

    /**
     * 정령의 생애 정보를 조회합니다.
     */
    public LifecycleInfo getLifecycleInfo(Spirit spirit) {
        int remainingDays = -1;
        
        if (spirit.getLifespanCountdown() != null) {
            LocalDateTime countdownStart = spirit.getLifespanCountdown();
            long daysSinceMaxLevel = java.time.Duration.between(countdownStart, LocalDateTime.now()).toDays();
            remainingDays = 10 + spirit.getLifespanExtended() - (int)daysSinceMaxLevel;
            remainingDays = Math.max(0, remainingDays);
        }
        
        return new LifecycleInfo(
            spirit.getAge(),
            spirit.getIsRetired(),
            spirit.getRetiredAt(),
            remainingDays,
            spirit.getLifespanExtended()
        );
    }

    /**
     * 생애 정보 클래스
     */
    public static class LifecycleInfo {
        private final int age;
        private final boolean isRetired;
        private final LocalDateTime retiredAt;
        private final int remainingDays;
        private final int lifespanExtended;

        public LifecycleInfo(int age, boolean isRetired, LocalDateTime retiredAt, 
                           int remainingDays, int lifespanExtended) {
            this.age = age;
            this.isRetired = isRetired;
            this.retiredAt = retiredAt;
            this.remainingDays = remainingDays;
            this.lifespanExtended = lifespanExtended;
        }

        public int getAge() {
            return age;
        }

        public boolean isRetired() {
            return isRetired;
        }

        public LocalDateTime getRetiredAt() {
            return retiredAt;
        }

        public int getRemainingDays() {
            return remainingDays;
        }

        public int getLifespanExtended() {
            return lifespanExtended;
        }
    }
}

