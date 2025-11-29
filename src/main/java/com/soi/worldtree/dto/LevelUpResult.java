package com.soi.worldtree.dto;

public class LevelUpResult {
    private boolean leveledUp;
    private Integer newLevel;
    private Integer currentExp;
    private Integer requiredExp;
    private String growthEffect;
    private boolean spiritCreationUnlocked; // 정령 생성 기능 언락 여부
    private boolean rareSpiritSelectionRequired; // 15레벨 달성 시 희귀 정령 선택 필요 여부

    public LevelUpResult() {
    }

    public LevelUpResult(boolean leveledUp, Integer newLevel, Integer currentExp, Integer requiredExp, String growthEffect, boolean spiritCreationUnlocked) {
        this.leveledUp = leveledUp;
        this.newLevel = newLevel;
        this.currentExp = currentExp;
        this.requiredExp = requiredExp;
        this.growthEffect = growthEffect;
        this.spiritCreationUnlocked = spiritCreationUnlocked;
        this.rareSpiritSelectionRequired = false;
    }

    public LevelUpResult(boolean leveledUp, Integer newLevel, Integer currentExp, Integer requiredExp, String growthEffect, boolean spiritCreationUnlocked, boolean rareSpiritSelectionRequired) {
        this.leveledUp = leveledUp;
        this.newLevel = newLevel;
        this.currentExp = currentExp;
        this.requiredExp = requiredExp;
        this.growthEffect = growthEffect;
        this.spiritCreationUnlocked = spiritCreationUnlocked;
        this.rareSpiritSelectionRequired = rareSpiritSelectionRequired;
    }

    // Getters and Setters
    public boolean isLeveledUp() {
        return leveledUp;
    }

    public void setLeveledUp(boolean leveledUp) {
        this.leveledUp = leveledUp;
    }

    public Integer getNewLevel() {
        return newLevel;
    }

    public void setNewLevel(Integer newLevel) {
        this.newLevel = newLevel;
    }

    public Integer getCurrentExp() {
        return currentExp;
    }

    public void setCurrentExp(Integer currentExp) {
        this.currentExp = currentExp;
    }

    public Integer getRequiredExp() {
        return requiredExp;
    }

    public void setRequiredExp(Integer requiredExp) {
        this.requiredExp = requiredExp;
    }

    public String getGrowthEffect() {
        return growthEffect;
    }

    public void setGrowthEffect(String growthEffect) {
        this.growthEffect = growthEffect;
    }

    public boolean isSpiritCreationUnlocked() {
        return spiritCreationUnlocked;
    }

    public void setSpiritCreationUnlocked(boolean spiritCreationUnlocked) {
        this.spiritCreationUnlocked = spiritCreationUnlocked;
    }

    public boolean isRareSpiritSelectionRequired() {
        return rareSpiritSelectionRequired;
    }

    public void setRareSpiritSelectionRequired(boolean rareSpiritSelectionRequired) {
        this.rareSpiritSelectionRequired = rareSpiritSelectionRequired;
    }
}

