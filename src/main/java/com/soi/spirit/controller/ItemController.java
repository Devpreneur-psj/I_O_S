package com.soi.spirit.controller;

import com.soi.spirit.entity.Item;
import com.soi.spirit.entity.UserItem;
import com.soi.spirit.service.ItemService;
import com.soi.spirit.service.LifecycleService;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 아이템 관련 컨트롤러
 */
@Controller
@RequestMapping("/item")
public class ItemController {

    private final ItemService itemService;
    private final LifecycleService lifecycleService;
    private final UserRepository userRepository;

    @Autowired
    public ItemController(ItemService itemService, LifecycleService lifecycleService, UserRepository userRepository) {
        this.itemService = itemService;
        this.lifecycleService = lifecycleService;
        this.userRepository = userRepository;
    }

    /**
     * 상점 페이지
     */
    @GetMapping("/shop")
    public String shopPage() {
        return "shop";
    }

    /**
     * 아이템 구매 API
     */
    @PostMapping("/api/purchase")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> purchaseItem(
            @RequestParam Long itemId,
            @RequestParam(defaultValue = "1") Integer quantity,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            Long userId = user.getId();
            itemService.purchaseItem(userId, itemId, quantity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "아이템을 구매했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 정령에게 아이템 사용 API
     */
    @PostMapping("/api/use")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> useItem(
            @RequestParam Long spiritId,
            @RequestParam Long itemId,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            Long userId = user.getId();
            
            // 아이템 정보 확인
            Item item = itemService.getAvailableItems().stream()
                    .filter(i -> i.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));
            
            // 수명 연장 아이템인 경우 특별 처리
            if ("LIFESPAN_EXTENSION".equals(item.getItemType()) && 
                "LIFESPAN".equals(item.getEffectType())) {
                lifecycleService.extendLifespan(userId, spiritId, item.getEffectValue());
                itemService.useItemOnSpirit(userId, spiritId, itemId); // 수량만 감소
            } else {
                itemService.useItemOnSpirit(userId, spiritId, itemId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "아이템을 사용했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 보유 아이템 목록 API
     */
    @GetMapping("/api/my-items")
    @ResponseBody
    public ResponseEntity<?> getMyItems(Authentication authentication) {
        System.out.println("========================================");
        System.out.println("=== getMyItems API 호출 시작 ===");
        System.out.println("Thread: " + Thread.currentThread().getName());
        System.out.println("Authentication: " + (authentication != null ? authentication.getName() : "null"));
        System.out.println("========================================");
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("인증 실패: authentication이 null이거나 인증되지 않음");
                return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
            }
            
            String username = authentication.getName();
            System.out.println("사용자 이름: " + username);
            if (username == null || username.isEmpty()) {
                System.out.println("사용자 이름이 null이거나 비어있음");
                return ResponseEntity.status(401).body(Map.of("error", "사용자 이름을 찾을 수 없습니다."));
            }
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            
            System.out.println("사용자 ID: " + user.getId());
            if (user == null || user.getId() == null) {
                System.out.println("사용자 정보가 올바르지 않음: user=" + user);
                return ResponseEntity.status(500).body(Map.of("error", "사용자 정보가 올바르지 않습니다."));
            }
            
            System.out.println("아이템 목록 조회 시작: userId=" + user.getId());
            List<UserItem> items = itemService.getUserItems(user.getId());
            System.out.println("조회된 아이템 개수: " + (items != null ? items.size() : "null"));
            
            // null 체크 및 안전한 변환
            if (items == null) {
                items = List.of();
            }
            
            // UserItem을 Map으로 변환하여 JSON 직렬화 문제 방지
            List<Map<String, Object>> result = items.stream()
                    .filter(ui -> ui != null) // null 항목 필터링
                    .map(ui -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ui.getId() != null ? ui.getId() : 0L);
                        map.put("userId", ui.getUserId() != null ? ui.getUserId() : 0L);
                        map.put("itemId", ui.getItemId() != null ? ui.getItemId() : 0L);
                        map.put("quantity", ui.getQuantity() != null ? ui.getQuantity() : 0);
                        return map;
                    })
                    .collect(Collectors.toList());
            
            System.out.println("변환된 결과 개수: " + result.size());
            System.out.println("=== getMyItems API 호출 성공 ===");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            System.err.println("=== getMyItems API 오류 발생 (IllegalArgument) ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(400)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(Map.of("error", e.getMessage()));
        } catch (org.springframework.dao.DataAccessException e) {
            System.err.println("=== getMyItems API 오류 발생 (DataAccess) ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "데이터베이스 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("exceptionType", e.getClass().getSimpleName());
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(errorResponse);
        } catch (Exception e) {
            System.err.println("=== getMyItems API 오류 발생 (Exception) ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Exception cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
            e.printStackTrace();
            
            // 더 자세한 오류 정보를 반환
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "아이템 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("exceptionType", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                errorResponse.put("cause", e.getCause().getMessage());
            }
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(errorResponse);
        } finally {
            System.out.println("=== getMyItems API 호출 종료 ===");
        }
    }

    /**
     * 판매 가능한 아이템 목록 API
     */
    @GetMapping("/api/available-items")
    @ResponseBody
    public ResponseEntity<?> getAvailableItems() {
        try {
            List<Item> items = itemService.getAvailableItems();
            return ResponseEntity.ok(items != null ? items : List.of());
        } catch (Exception e) {
            System.err.println("Error getting available items: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "아이템 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자 금액 조회 API
     */
    @GetMapping("/api/user-money")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserMoney(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            
            Map<String, Object> response = new HashMap<>();
            response.put("money", user.getMoney());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("money", 0);
            return ResponseEntity.ok(response);
        }
    }
}

