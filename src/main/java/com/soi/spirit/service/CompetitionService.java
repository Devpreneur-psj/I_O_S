package com.soi.spirit.service;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

/**
 * 대회/경쟁 서비스
 * 정령들의 능력치 기반 경쟁 및 상금 시스템
 */
@Service
@Transactional
public class CompetitionService {

    private final SpiritRepository spiritRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Autowired
    public CompetitionService(SpiritRepository spiritRepository, UserRepository userRepository) {
        this.spiritRepository = spiritRepository;
        this.userRepository = userRepository;
    }

    /**
     * 대회에 참가합니다.
     */
    public CompetitionResult participateInCompetition(Long userId, Long spiritId, String competitionType) {
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        if (!spirit.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 정령만 대회에 참가할 수 있습니다.");
        }
        
        if (spirit.getEnergy() < 30) {
            throw new IllegalArgumentException("에너지가 부족합니다.");
        }
        
        // 대회 타입에 따른 난이도 및 상금 설정
        int difficulty = getCompetitionDifficulty(competitionType);
        long prizeMoney = getPrizeMoney(competitionType);
        
        // 정령의 종합 능력치 계산
        int totalStats = spirit.getMeleeAttack() + spirit.getRangedAttack() +
                        spirit.getMeleeDefense() + spirit.getRangedDefense() + spirit.getSpeed();
        
        // 승률 계산 (능력치가 높을수록 승률 증가, 난이도가 높을수록 승률 감소)
        int winChance = Math.max(10, Math.min(90, (totalStats / 5) - difficulty));
        
        // 승패 결정
        boolean won = random.nextInt(100) < winChance;
        
        // 에너지 소모
        spirit.setEnergy(Math.max(0, spirit.getEnergy() - 30));
        
        if (won) {
            // 승리 시 상금 지급
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            user.setMoney(user.getMoney() + prizeMoney);
            userRepository.save(user);
            
            // 경험치 획득
            spirit.setExperience(spirit.getExperience() + 100);
            
            // 행복도 증가
            spirit.setHappiness(Math.min(100, spirit.getHappiness() + 10));
            
            // 친밀도 증가
            spirit.setIntimacy(Math.min(10, spirit.getIntimacy() + 1));
        } else {
            // 패배 시 경험치 약간 획득
            spirit.setExperience(spirit.getExperience() + 30);
            
            // 행복도 감소
            spirit.setHappiness(Math.max(0, spirit.getHappiness() - 5));
        }
        
        spiritRepository.save(spirit);
        
        return new CompetitionResult(won, prizeMoney, winChance);
    }

    /**
     * 대회 난이도 반환
     */
    private int getCompetitionDifficulty(String competitionType) {
        switch (competitionType) {
            case "EASY": return 10;
            case "NORMAL": return 20;
            case "HARD": return 30;
            case "EXPERT": return 40;
            default: return 20;
        }
    }

    /**
     * 대회 상금 반환
     */
    private long getPrizeMoney(String competitionType) {
        switch (competitionType) {
            case "EASY": return 100;
            case "NORMAL": return 300;
            case "HARD": return 500;
            case "EXPERT": return 1000;
            default: return 300;
        }
    }

    /**
     * 대회 결과 클래스
     */
    public static class CompetitionResult {
        private final boolean won;
        private final long prizeMoney;
        private final int winChance;

        public CompetitionResult(boolean won, long prizeMoney, int winChance) {
            this.won = won;
            this.prizeMoney = prizeMoney;
            this.winChance = winChance;
        }

        public boolean isWon() {
            return won;
        }

        public long getPrizeMoney() {
            return prizeMoney;
        }

        public int getWinChance() {
            return winChance;
        }
    }
}

