package com.soi.worldtree.dto;

/**
 * 정령의 축복 추가 요청 DTO
 * 정령이 죽었을 때 얻는 포인트를 추가하는 요청
 * 
 * 확장 가능성:
 * - spiritId: 정령 ID (추후 추가 예정)
 * - spiritGrade: 정령 등급 (COMMON, RARE, EPIC, LEGENDARY 등, 추후 추가 예정)
 * - spiritStatus: 정령 상태/친밀도 (NORMAL, INJURED, EXHAUSTED 등, 추후 추가 예정)
 */
public class EssencePulseRequest {
    private Integer amount;
    private String contentSource;
    
    // 정령 관련 필드 (추후 확장용, 현재는 nullable)
    private Long spiritId;           // 정령 ID
    private String spiritGrade;      // 정령 등급 (COMMON, RARE, EPIC, LEGENDARY 등)
    private String spiritStatus;     // 정령 상태 (NORMAL, INJURED, EXHAUSTED 등)

    public EssencePulseRequest() {
    }

    public EssencePulseRequest(Integer amount, String contentSource) {
        this.amount = amount;
        this.contentSource = contentSource;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getContentSource() {
        return contentSource;
    }

    public void setContentSource(String contentSource) {
        this.contentSource = contentSource;
    }

    public Long getSpiritId() {
        return spiritId;
    }

    public void setSpiritId(Long spiritId) {
        this.spiritId = spiritId;
    }

    public String getSpiritGrade() {
        return spiritGrade;
    }

    public void setSpiritGrade(String spiritGrade) {
        this.spiritGrade = spiritGrade;
    }

    public String getSpiritStatus() {
        return spiritStatus;
    }

    public void setSpiritStatus(String spiritStatus) {
        this.spiritStatus = spiritStatus;
    }
}

