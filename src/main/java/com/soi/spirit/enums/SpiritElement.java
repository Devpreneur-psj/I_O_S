package com.soi.spirit.enums;

/**
 * 정령 속성(원소) 열거형
 */
public enum SpiritElement {
    FIRE("불의 정령", "FIRE"),
    WATER("물의 정령", "WATER"),
    WIND("풀의 정령", "WIND"),
    LIGHT("빛의 정령", "LIGHT"),
    DARK("어둠의 정령", "DARK");
    
    private final String koreanName;
    private final String code;
    
    SpiritElement(String koreanName, String code) {
        this.koreanName = koreanName;
        this.code = code;
    }
    
    public String getKoreanName() {
        return koreanName;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 한국어 이름으로 찾기
     */
    public static SpiritElement fromKoreanName(String koreanName) {
        for (SpiritElement element : values()) {
            if (element.koreanName.equals(koreanName)) {
                return element;
            }
        }
        return FIRE; // 기본값
    }
    
    /**
     * 코드로 찾기
     */
    public static SpiritElement fromCode(String code) {
        for (SpiritElement element : values()) {
            if (element.code.equals(code)) {
                return element;
            }
        }
        return FIRE; // 기본값
    }
    
    /**
     * 속성 상성 계산
     * @param attacker 공격자 속성
     * @param defender 방어자 속성
     * @return 상성 배율
     */
    public static double getTypeEffectiveness(SpiritElement attacker, SpiritElement defender) {
        if (attacker == null || defender == null) {
            return 1.0;
        }
        
        // 같은 속성
        if (attacker == defender) {
            return 0.75;
        }
        
        // 삼각 상성: 불 > 풀 > 물 > 불
        if (attacker == FIRE && defender == WIND) {
            return 2.0; // 효과가 굉장함
        }
        if (attacker == WIND && defender == WATER) {
            return 2.0;
        }
        if (attacker == WATER && defender == FIRE) {
            return 2.0;
        }
        
        // 약함 관계
        if (attacker == FIRE && defender == WATER) {
            return 0.5;
        }
        if (attacker == WIND && defender == FIRE) {
            return 0.5;
        }
        if (attacker == WATER && defender == WIND) {
            return 0.5;
        }
        
        // 빛 <-> 어둠 상호 강함
        if (attacker == LIGHT && defender == DARK) {
            return 2.0;
        }
        if (attacker == DARK && defender == LIGHT) {
            return 2.0;
        }
        
        return 1.0; // 일반
    }
    
    /**
     * 문자열(속성 이름)로부터 속성 열거형 찾기
     */
    public static SpiritElement fromTypeName(String typeName) {
        if (typeName == null) {
            return FIRE;
        }
        
        if (typeName.contains("불")) {
            return FIRE;
        } else if (typeName.contains("물")) {
            return WATER;
        } else if (typeName.contains("풀") || typeName.contains("바람")) {
            return WIND;
        } else if (typeName.contains("빛")) {
            return LIGHT;
        } else if (typeName.contains("어둠")) {
            return DARK;
        }
        
        return FIRE; // 기본값
    }
}

