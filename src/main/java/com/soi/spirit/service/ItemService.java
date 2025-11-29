package com.soi.spirit.service;

import com.soi.spirit.entity.*;
import com.soi.spirit.repository.*;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 아이템 관리 서비스
 */
@Service
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final SpiritRepository spiritRepository;
    private final UserRepository userRepository;
    private final LifecycleService lifecycleService;

    @Autowired
    public ItemService(ItemRepository itemRepository,
                      UserItemRepository userItemRepository,
                      SpiritRepository spiritRepository,
                      UserRepository userRepository,
                      LifecycleService lifecycleService) {
        this.itemRepository = itemRepository;
        this.userItemRepository = userItemRepository;
        this.spiritRepository = spiritRepository;
        this.userRepository = userRepository;
        this.lifecycleService = lifecycleService;
    }

    /**
     * 정령에게 아이템을 사용합니다.
     */
    public void useItemOnSpirit(Long userId, Long spiritId, Long itemId) {
        // 정령 확인
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        if (!spirit.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 정령만 아이템을 사용할 수 있습니다.");
        }
        
        // 아이템 확인
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));
        
        // 보유 아이템 확인
        UserItem userItem = userItemRepository.findByUserIdAndItemId(userId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("보유한 아이템이 없습니다."));
        
        if (userItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("아이템 수량이 부족합니다.");
        }
        
        // 아이템 효과 적용
        applyItemEffect(spirit, item);
        
        // 아이템 수량 감소
        userItem.setQuantity(userItem.getQuantity() - 1);
        if (userItem.getQuantity() <= 0) {
            userItemRepository.delete(userItem);
        } else {
            userItemRepository.save(userItem);
        }
        
        spiritRepository.save(spirit);
    }

    /**
     * 아이템 효과를 적용합니다.
     */
    private void applyItemEffect(Spirit spirit, Item item) {
        String effectType = item.getEffectType();
        Integer effectValue = item.getEffectValue() != null ? item.getEffectValue() : 0;
        
        switch (effectType) {
            case "HEALTH":
                // 건강 회복
                if ("질병".equals(spirit.getHealthStatus()) || "아픔".equals(spirit.getHealthStatus())) {
                    spirit.setHealthStatus("건강");
                }
                break;
            case "HAPPINESS":
                // 행복도 증가
                spirit.setHappiness(Math.min(100, spirit.getHappiness() + effectValue));
                break;
            case "ENERGY":
                // 에너지 회복
                spirit.setEnergy(Math.min(100, spirit.getEnergy() + effectValue));
                break;
            case "STAT_BOOST":
                // 능력치 향상
                String targetStat = item.getTargetStat();
                if (targetStat != null) {
                    switch (targetStat) {
                        case "RANGED_ATTACK":
                            spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + effectValue));
                            break;
                        case "MELEE_ATTACK":
                            spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + effectValue));
                            break;
                        case "SPEED":
                            spirit.setSpeed(Math.min(100, spirit.getSpeed() + effectValue));
                            break;
                        case "RANGED_DEFENSE":
                            spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + effectValue));
                            break;
                        case "MELEE_DEFENSE":
                            spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + effectValue));
                            break;
                    }
                }
                break;
            case "HUNGER":
                // 배고픔 감소
                spirit.setHunger(Math.max(0, spirit.getHunger() - effectValue));
                break;
            case "LIFESPAN":
            case "LIFESPAN_EXTENSION":
                // 수명 연장 (LifecycleService 사용)
                if (effectValue > 0) {
                    lifecycleService.extendLifespan(spirit.getUserId(), spirit.getId(), effectValue);
                }
                break;
        }
    }

    /**
     * 상점에서 아이템을 구매합니다.
     */
    public void purchaseItem(Long userId, Long itemId, Integer quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));
        
        if (!item.getIsAvailable()) {
            throw new IllegalArgumentException("판매 중인 아이템이 아닙니다.");
        }
        
        BigDecimal totalPrice = item.getPrice().multiply(BigDecimal.valueOf(quantity));
        if (user.getMoney() < totalPrice.longValue()) {
            throw new IllegalArgumentException("금액이 부족합니다.");
        }
        
        // 금액 차감
        user.setMoney(user.getMoney() - totalPrice.longValue());
        userRepository.save(user);
        
        // 아이템 추가
        UserItem userItem = userItemRepository.findByUserIdAndItemId(userId, itemId)
                .orElse(new UserItem());
        
        if (userItem.getId() == null) {
            userItem.setUserId(userId);
            userItem.setItemId(itemId);
            userItem.setQuantity(0);
        }
        
        userItem.setQuantity(userItem.getQuantity() + quantity);
        userItemRepository.save(userItem);
    }

    /**
     * 사용자가 보유한 아이템 목록을 조회합니다.
     */
    public List<UserItem> getUserItems(Long userId) {
        System.out.println("=== ItemService.getUserItems 호출 ===");
        System.out.println("userId: " + userId);
        try {
            if (userId == null) {
                System.out.println("userId가 null이므로 빈 리스트 반환");
                return List.of();
            }
            System.out.println("userItemRepository.findByUserId 호출 전");
            List<UserItem> items = userItemRepository.findByUserId(userId);
            System.out.println("조회된 아이템 개수: " + (items != null ? items.size() : "null"));
            return items != null ? items : List.of();
        } catch (org.springframework.dao.DataAccessException e) {
            System.err.println("=== ItemService.getUserItems 데이터베이스 오류 발생 ===");
            System.err.println("Error getting user items (DataAccessException): " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Exception cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
            e.printStackTrace();
            // 데이터베이스 오류는 빈 리스트 반환
            return List.of();
        } catch (Exception e) {
            System.err.println("=== ItemService.getUserItems 오류 발생 ===");
            System.err.println("Error getting user items: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Exception cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
            e.printStackTrace();
            // 모든 예외는 빈 리스트 반환 (안전한 처리)
            return List.of();
        }
    }

    /**
     * 판매 가능한 아이템 목록을 조회합니다.
     */
    public List<Item> getAvailableItems() {
        try {
            List<Item> items = itemRepository.findByIsAvailableTrue();
            return items != null ? items : List.of();
        } catch (Exception e) {
            System.err.println("Error getting available items: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}

