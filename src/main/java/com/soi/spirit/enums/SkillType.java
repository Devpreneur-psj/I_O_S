package com.soi.spirit.enums;

/**
 * 스킬 타입 열거형
 */
public enum SkillType {
    RANGED_ATTACK("원거리 공격"),
    MELEE_ATTACK("근거리 공격"),
    SUPPORT("지원 기술");
    
    private final String description;
    
    SkillType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

