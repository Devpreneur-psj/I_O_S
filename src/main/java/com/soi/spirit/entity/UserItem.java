package com.soi.spirit.entity;

import jakarta.persistence.*;

/**
 * 사용자 아이템 인벤토리 엔티티
 * 사용자가 소유한 아이템과 수량을 관리
 */
@Entity
@Table(name = "user_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "item_id"})
})
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0; // 보유 수량

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = Math.max(0, quantity);
    }
}

