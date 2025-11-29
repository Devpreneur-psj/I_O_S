package com.soi.spirit.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 아이템 엔티티
 * 정령에게 사용할 수 있는 다양한 아이템들
 */
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, unique = true, length = 50)
    private String itemCode; // 아이템 코드

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName; // 아이템 이름

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType; // 아이템 타입 (FOOD, MEDICINE, TOY, VITAMIN, LIFESPAN_EXTENSION 등)

    @Column(name = "description", length = 500)
    private String description; // 아이템 설명

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // 가격

    @Column(name = "effect_type", length = 50)
    private String effectType; // 효과 타입 (HEALTH, HAPPINESS, ENERGY, STAT_BOOST 등)

    @Column(name = "effect_value")
    private Integer effectValue; // 효과 값

    @Column(name = "target_stat", length = 50)
    private String targetStat; // 대상 능력치 (RANGED_ATTACK, MELEE_ATTACK, SPEED 등)

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true; // 판매 가능 여부

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getEffectType() {
        return effectType;
    }

    public void setEffectType(String effectType) {
        this.effectType = effectType;
    }

    public Integer getEffectValue() {
        return effectValue;
    }

    public void setEffectValue(Integer effectValue) {
        this.effectValue = effectValue;
    }

    public String getTargetStat() {
        return targetStat;
    }

    public void setTargetStat(String targetStat) {
        this.targetStat = targetStat;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}

