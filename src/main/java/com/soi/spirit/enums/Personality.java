package com.soi.spirit.enums;

/**
 * 정령 성격 열거형
 */
public enum Personality {
    STUBBORN("고집", "근거리공격 중심"),
    CAUTIOUS("조심", "원거리공격 중심"),
    PLAYFUL("장난꾸러기", "스피드 중심"),
    GENTLE("온순", "방어력 중심"),
    BRAVE("용감", "공격력 균형");
    
    private final String koreanName;
    private final String description;
    
    Personality(String koreanName, String description) {
        this.koreanName = koreanName;
        this.description = description;
    }
    
    public String getKoreanName() {
        return koreanName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 한국어 이름으로 찾기
     */
    public static Personality fromKoreanName(String koreanName) {
        for (Personality p : values()) {
            if (p.koreanName.equals(koreanName)) {
                return p;
            }
        }
        return STUBBORN; // 기본값
    }
}

