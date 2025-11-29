package com.soi.spirit.entity;

import jakarta.persistence.*;

/**
 * 정령 종류 및 진화 정보를 담는 엔티티
 * 정령 종류별 기본 정보와 진화 단계를 정의
 */
@Entity
@Table(name = "spirit_types")
public class SpiritType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code", nullable = false, unique = true, length = 50)
    private String typeCode; // FIRE, WATER, WIND, LIGHT, DARK

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName; // 불의 정령, 물의 정령, 풀의 정령, 빛의 정령, 어둠의 정령

    @Column(name = "is_rare", nullable = false)
    private Boolean isRare = false; // 희귀 정령 여부

    @Column(name = "unlock_level")
    private Integer unlockLevel; // 해금 레벨 (희귀 정령의 경우 15)

    @Column(name = "base_stage_name", nullable = false, length = 50)
    private String baseStageName; // 기본 단계 이름

    @Column(name = "first_evolution_name", length = 50)
    private String firstEvolutionName; // 1차 진화 이름

    @Column(name = "second_evolution_name", length = 50)
    private String secondEvolutionName; // 2차 진화 이름

    @Column(name = "weak_against", length = 50)
    private String weakAgainst; // 상성상 불리한 정령 타입

    @Column(name = "strong_against", length = 50)
    private String strongAgainst; // 상성상 유리한 정령 타입

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Boolean getIsRare() {
        return isRare;
    }

    public void setIsRare(Boolean isRare) {
        this.isRare = isRare;
    }

    public Integer getUnlockLevel() {
        return unlockLevel;
    }

    public void setUnlockLevel(Integer unlockLevel) {
        this.unlockLevel = unlockLevel;
    }

    public String getBaseStageName() {
        return baseStageName;
    }

    public void setBaseStageName(String baseStageName) {
        this.baseStageName = baseStageName;
    }

    public String getFirstEvolutionName() {
        return firstEvolutionName;
    }

    public void setFirstEvolutionName(String firstEvolutionName) {
        this.firstEvolutionName = firstEvolutionName;
    }

    public String getSecondEvolutionName() {
        return secondEvolutionName;
    }

    public void setSecondEvolutionName(String secondEvolutionName) {
        this.secondEvolutionName = secondEvolutionName;
    }

    public String getWeakAgainst() {
        return weakAgainst;
    }

    public void setWeakAgainst(String weakAgainst) {
        this.weakAgainst = weakAgainst;
    }

    public String getStrongAgainst() {
        return strongAgainst;
    }

    public void setStrongAgainst(String strongAgainst) {
        this.strongAgainst = strongAgainst;
    }
}

