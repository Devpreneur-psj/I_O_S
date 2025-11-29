package com.soi.worldtree.dto;

public class WorldTreeInfo {
    private Integer currentLevel;
    private Integer currentExp;
    private Integer requiredExp;
    private Integer cumulativeExp;
    private String growthEffect;
    private double expPercentage;
    private Long availableEssence; // 사용 가능한 정령의 축복 잔량
    private boolean spiritCreationUnlocked; // 정령 생성 기능 언락 여부

    public WorldTreeInfo() {
    }

    public WorldTreeInfo(Integer currentLevel, Integer currentExp, Integer requiredExp, Integer cumulativeExp, String growthEffect, Long availableEssence, boolean spiritCreationUnlocked) {
        this.currentLevel = currentLevel;
        this.currentExp = currentExp;
        this.requiredExp = requiredExp;
        this.cumulativeExp = cumulativeExp;
        this.growthEffect = growthEffect;
        this.expPercentage = requiredExp > 0 ? (double) currentExp / requiredExp * 100 : 0;
        this.availableEssence = availableEssence != null ? availableEssence : 0L;
        this.spiritCreationUnlocked = spiritCreationUnlocked;
    }

    // Getters and Setters
    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
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

    public Integer getCumulativeExp() {
        return cumulativeExp;
    }

    public void setCumulativeExp(Integer cumulativeExp) {
        this.cumulativeExp = cumulativeExp;
    }

    public String getGrowthEffect() {
        return growthEffect;
    }

    public void setGrowthEffect(String growthEffect) {
        this.growthEffect = growthEffect;
    }

    public double getExpPercentage() {
        return expPercentage;
    }

    public void setExpPercentage(double expPercentage) {
        this.expPercentage = expPercentage;
    }

    public Long getAvailableEssence() {
        return availableEssence;
    }

    public void setAvailableEssence(Long availableEssence) {
        this.availableEssence = availableEssence;
    }

    public boolean isSpiritCreationUnlocked() {
        return spiritCreationUnlocked;
    }

    public void setSpiritCreationUnlocked(boolean spiritCreationUnlocked) {
        this.spiritCreationUnlocked = spiritCreationUnlocked;
    }
}

