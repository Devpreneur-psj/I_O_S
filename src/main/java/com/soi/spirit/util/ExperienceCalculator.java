package com.soi.spirit.util;

import com.soi.spirit.constants.SpiritConstants;

/**
 * 경험치 계산 유틸리티
 * Medium Slow 타입: 초반 완만, 후반 급격한 성장
 */
public class ExperienceCalculator {
    
    /**
     * 레벨에 필요한 누적 경험치 계산
     * 공식: level³ * 0.8 + level * 50 (더 부드러운 곡선)
     * 
     * @param level 목표 레벨
     * @return 누적 경험치
     */
    public static int getRequiredExpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        
        // Medium Slow 타입 공식
        double baseExp = Math.pow(level, 3) * 0.8;
        double linearExp = level * 50;
        
        return (int) Math.round(baseExp + linearExp);
    }
    
    /**
     * 특정 레벨까지의 누적 경험치를 반환
     * 레벨 1: 0
     * 레벨 2: ~100
     * 레벨 10: ~8,500
     * 레벨 20: ~66,000
     * 레벨 30: ~218,700
     */
    public static int getCumulativeExp(int level) {
        return getRequiredExpForLevel(level);
    }
    
    /**
     * 레벨업에 필요한 경험치 (현재 레벨 → 다음 레벨)
     */
    public static int getExpForNextLevel(int currentLevel) {
        if (currentLevel >= SpiritConstants.MAX_SPIRIT_LEVEL) {
            return Integer.MAX_VALUE; // 최대 레벨
        }
        
        int currentExp = getRequiredExpForLevel(currentLevel);
        int nextExp = getRequiredExpForLevel(currentLevel + 1);
        
        return nextExp - currentExp;
    }
    
    /**
     * 현재 경험치로부터 다음 레벨까지 필요한 경험치
     */
    public static int getExpNeededForNextLevel(int currentLevel, int currentExp) {
        if (currentLevel >= SpiritConstants.MAX_SPIRIT_LEVEL) {
            return 0;
        }
        
        int requiredExp = getRequiredExpForLevel(currentLevel + 1);
        return Math.max(0, requiredExp - currentExp);
    }
    
    /**
     * 경험치 테이블 생성 (레벨 1-30)
     */
    public static void printExpTable() {
        System.out.println("=== 정령 경험치 테이블 (레벨 1-30) ===");
        System.out.println("레벨 | 누적 경험치 | 레벨업 필요 경험치");
        System.out.println("----------------------------------------");
        
        for (int level = 1; level <= SpiritConstants.MAX_SPIRIT_LEVEL; level++) {
            int cumulative = getCumulativeExp(level);
            int needed = level > 1 ? cumulative - getCumulativeExp(level - 1) : cumulative;
            System.out.printf("%2d   | %10d | %10d%n", level, cumulative, needed);
        }
    }
}

