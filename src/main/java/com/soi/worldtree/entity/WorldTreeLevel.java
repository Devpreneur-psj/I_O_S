package com.soi.worldtree.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "world_tree_levels")
public class WorldTreeLevel {

    @Id
    @Column(name = "level")
    private Integer level;

    @Column(name = "required_exp", nullable = false)
    private Integer requiredExp;

    @Column(name = "cumulative_exp", nullable = false)
    private Integer cumulativeExp;

    @Column(name = "growth_effect", nullable = false, length = 500)
    private String growthEffectDescription;

    public WorldTreeLevel() {
    }

    public WorldTreeLevel(Integer level, Integer requiredExp, Integer cumulativeExp, String growthEffectDescription) {
        this.level = level;
        this.requiredExp = requiredExp;
        this.cumulativeExp = cumulativeExp;
        this.growthEffectDescription = growthEffectDescription;
    }

    // Getters and Setters
    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
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

    public String getGrowthEffectDescription() {
        return growthEffectDescription;
    }

    public void setGrowthEffectDescription(String growthEffectDescription) {
        this.growthEffectDescription = growthEffectDescription;
    }
}

