package com.soi.worldtree.dto;

/**
 * 정령의 축복 부여 요청 DTO
 * 보유 중인 정령의 축복을 경험치로 부여하는 요청
 */
public class BlessingGrantRequest {
    private Integer amount; // 부여할 축복 양 (null이면 전체 부여)

    public BlessingGrantRequest() {
    }

    public BlessingGrantRequest(Integer amount) {
        this.amount = amount;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}

