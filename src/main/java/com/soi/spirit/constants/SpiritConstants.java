package com.soi.spirit.constants;

/**
 * 정령 관련 상수 정의
 */
public class SpiritConstants {
    
    // ========== 정령 레벨 관련 ==========
    /** 정령 최소 레벨 */
    public static final int MIN_SPIRIT_LEVEL = 1;
    
    /** 정령 최대 레벨 */
    public static final int MAX_SPIRIT_LEVEL = 30;
    
    /** 정령 기본 에너지 */
    public static final int DEFAULT_ENERGY = 100;
    
    /** 정령 최대 에너지 */
    public static final int MAX_ENERGY = 100;
    
    // ========== 정령 능력치 관련 ==========
    /** 기본 능력치 값 */
    public static final int BASE_STAT = 50;
    
    /** 능력치 최소값 */
    public static final int MIN_STAT = 1;
    
    /** 능력치 최대값 */
    public static final int MAX_STAT = 150;
    
    // ========== 정령 친밀도 관련 ==========
    /** 친밀도 최소값 */
    public static final int MIN_INTIMACY = 1;
    
    /** 친밀도 최대값 */
    public static final int MAX_INTIMACY = 10;
    
    // ========== 성격 보정 배율 ==========
    /** 성격 보정 배율 (증가) */
    public static final double PERSONALITY_BOOST_MULTIPLIER = 1.1; // +10%
    
    /** 성격 보정 배율 (감소) */
    public static final double PERSONALITY_REDUCE_MULTIPLIER = 0.9; // -10%
    
    // ========== 레벨업 성장치 ==========
    /** 레벨업 시 기본 성장치 */
    public static final int BASE_GROWTH = 2;
    
    /** 주요 성장치 (성격에 따라) */
    public static final int MAJOR_GROWTH = 3;
    
    /** 보조 성장치 */
    public static final int MINOR_GROWTH = 1;
    
    // ========== 진화 관련 ==========
    /** 기본 단계 (0단계) */
    public static final int EVOLUTION_STAGE_BASE = 0;
    
    /** 1차 진화 (1단계) */
    public static final int EVOLUTION_STAGE_FIRST = 1;
    
    /** 2차 진화 (2단계) */
    public static final int EVOLUTION_STAGE_SECOND = 2;
    
    // ========== 정령 생성 관련 ==========
    /** 정령 생성 필요 세계수 레벨 */
    public static final int SPIRIT_CREATION_UNLOCK_LEVEL = 2;
    
    /** 희귀 정령 언락 레벨 */
    public static final int RARE_SPIRIT_UNLOCK_LEVEL = 15;
    
    // ========== 속성 상성 배율 ==========
    /** 효과가 굉장함 (강함) */
    public static final double TYPE_EFFECTIVENESS_SUPER = 2.0;
    
    /** 효과가 별로 (약함) */
    public static final double TYPE_EFFECTIVENESS_NOT_VERY = 0.5;
    
    /** 효과가 약간 약함 (같은 속성) */
    public static final double TYPE_EFFECTIVENESS_SAME = 0.75;
    
    /** 일반 (상성 관계 없음) */
    public static final double TYPE_EFFECTIVENESS_NORMAL = 1.0;
    
    // ========== 전투 관련 ==========
    /** 전투 최대 라운드 */
    public static final int MAX_COMBAT_ROUNDS = 50;
    
    /** 전투 데미지 랜덤 최소값 (백분율) */
    public static final double DAMAGE_RANDOM_MIN = 0.85;
    
    /** 전투 데미지 랜덤 최대값 (백분율) */
    public static final double DAMAGE_RANDOM_MAX = 1.0;
    
    /** 레벨 차이 보정 최소 배율 */
    public static final double LEVEL_DIFF_MULTIPLIER_MIN = 0.5;
    
    /** 레벨 차이 보정 최대 배율 */
    public static final double LEVEL_DIFF_MULTIPLIER_MAX = 1.5;
    
    /** 레벨 차이 보정 배율 (퍼 레벨) */
    public static final double LEVEL_DIFF_MULTIPLIER_PER_LEVEL = 0.05; // 5% per level
    
    private SpiritConstants() {
        // 인스턴스화 방지
    }
}

