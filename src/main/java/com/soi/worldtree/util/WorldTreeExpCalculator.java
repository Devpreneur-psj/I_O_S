package com.soi.worldtree.util;

/**
 * 세계수 경험치 계산 유틸리티
 * 점진적 증가 방식: 초반 완만, 후반 급격한 성장
 */
public class WorldTreeExpCalculator {
    
    /** 세계수 최대 레벨 */
    public static final int MAX_WORLD_TREE_LEVEL = 30;
    
    /**
     * 레벨에 필요한 누적 경험치 계산
     * 점진적 증가: 초반 800-1200, 중반 1200-2000, 후반 2000-3000
     * 
     * 공식: 800 + (level - 1) * 40 + (level - 1)² * 2
     */
    public static int getRequiredExpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        
        // 점진적 증가 공식
        double baseExp = 800;
        double linearGrowth = (level - 1) * 40;
        double quadraticGrowth = Math.pow(level - 1, 2) * 2;
        
        return (int) Math.round(baseExp + linearGrowth + quadraticGrowth);
    }
    
    /**
     * 특정 레벨까지의 누적 경험치
     */
    public static int getCumulativeExp(int level) {
        if (level <= 1) {
            return 1000; // 레벨 1 시작점
        }
        
        int cumulative = 1000; // 레벨 1 시작
        
        for (int i = 2; i <= level; i++) {
            cumulative += getRequiredExpForLevel(i) - getRequiredExpForLevel(i - 1);
        }
        
        return cumulative;
    }
    
    /**
     * 레벨업에 필요한 경험치 (현재 레벨 → 다음 레벨)
     */
    public static int getExpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_WORLD_TREE_LEVEL) {
            return Integer.MAX_VALUE;
        }
        
        int currentExp = getCumulativeExp(currentLevel);
        int nextExp = getCumulativeExp(currentLevel + 1);
        
        return nextExp - currentExp;
    }
    
    /**
     * 경험치 테이블 생성 (레벨 1-30)
     */
    public static void printExpTable() {
        System.out.println("=== 세계수 경험치 테이블 (레벨 1-30) ===");
        System.out.println("레벨 | 누적 경험치 | 레벨업 필요 경험치");
        System.out.println("----------------------------------------");
        
        for (int level = 1; level <= MAX_WORLD_TREE_LEVEL; level++) {
            int cumulative = getCumulativeExp(level);
            int needed = level > 1 ? getExpForNextLevel(level - 1) : cumulative;
            System.out.printf("%2d   | %10d | %10d%n", level, cumulative, needed);
        }
    }
    
    /**
     * 실제 사용을 위한 더 부드러운 곡선 생성
     * 레벨 1-30까지의 경험치를 생성
     */
    public static int[] generateExpTable() {
        int[] expTable = new int[MAX_WORLD_TREE_LEVEL + 1];
        
        // 레벨 1: 1000 (시작점)
        expTable[1] = 1000;
        
        // 레벨 2-30: 점진적 증가
        for (int level = 2; level <= MAX_WORLD_TREE_LEVEL; level++) {
            int prevCumulative = expTable[level - 1];
            int requiredExp = calculateRequiredExpForLevel(level);
            expTable[level] = prevCumulative + requiredExp;
        }
        
        return expTable;
    }
    
    /**
     * 레벨별 필요 경험치 계산
     * 초반: 완만하게, 후반: 급격하게
     */
    private static int calculateRequiredExpForLevel(int level) {
        if (level <= 10) {
            // 초반: 800 ~ 1200
            return 800 + (level - 1) * 40;
        } else if (level <= 20) {
            // 중반: 1200 ~ 2000
            int base = 1200;
            int growth = (level - 10) * 80;
            return base + growth;
        } else {
            // 후반: 2000 ~ 3000
            int base = 2000;
            int growth = (level - 20) * 100;
            return base + growth;
        }
    }
}

