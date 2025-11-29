package com.soi.community.controller;

import com.soi.community.service.FriendService;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/friend")
public class FriendController {
    
    private final FriendService friendService;
    private final UserRepository userRepository;
    
    @Autowired
    public FriendController(FriendService friendService, UserRepository userRepository) {
        this.friendService = friendService;
        this.userRepository = userRepository;
    }
    
    /**
     * 친구 목록 페이지
     */
    @GetMapping("/list")
    public String friendList(Model model, Authentication authentication) {
        Long userId = getUserId(authentication);
        if (userId == null) {
            return "redirect:/login";
        }
        
        List<User> friends = friendService.getFriends(userId);
        List<User> recommendedFriends = friendService.getRecommendedFriends(userId, 10);
        List<User> pendingRequests = friendService.getPendingFriendRequests(userId);
        
        model.addAttribute("friends", friends);
        model.addAttribute("recommendedFriends", recommendedFriends);
        model.addAttribute("pendingRequests", pendingRequests);
        
        return "friend-list";
    }
    
    /**
     * 친구 요청 보내기 API
     */
    @PostMapping("/api/send-request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendFriendRequest(
            @RequestParam(required = false) Long friendId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (friendId == null) {
                response.put("success", false);
                response.put("message", "친구 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            friendService.sendFriendRequest(userId, friendId);
            
            response.put("success", true);
            response.put("message", "친구 요청을 보냈습니다.");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            System.err.println("친구 요청 보내기 오류: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "친구 요청 전송 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 친구 요청 수락 API
     */
    @PostMapping("/api/accept-request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> acceptFriendRequest(
            @RequestParam Long friendId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            friendService.acceptFriendRequest(userId, friendId);
            
            response.put("success", true);
            response.put("message", "친구 요청을 수락했습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 친구 요청 거절 API
     */
    @PostMapping("/api/reject-request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectFriendRequest(
            @RequestParam Long friendId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            friendService.rejectFriendRequest(userId, friendId);
            
            response.put("success", true);
            response.put("message", "친구 요청을 거절했습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 유저 검색 API
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<User> users = friendService.searchUsers(keyword);
            response.put("success", true);
            response.put("users", users);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 친구에게 선물 보내기 API
     */
    @PostMapping("/api/send-gift")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendGift(
            @RequestParam Long friendId,
            @RequestParam Long itemId,
            @RequestParam Integer quantity,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            friendService.sendGift(userId, friendId, itemId, quantity);
            
            response.put("success", true);
            response.put("message", "선물을 보냈습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 친구의 정령 마을 방문
     */
    @GetMapping("/visit/{friendId}")
    public String visitFriendVillage(@PathVariable Long friendId, Model model, Authentication authentication) {
        Long userId = getUserId(authentication);
        if (userId == null) {
            return "redirect:/login";
        }
        
        // 친구 관계 확인
        List<User> friends = friendService.getFriends(userId);
        boolean isFriend = friends.stream().anyMatch(f -> f.getId().equals(friendId));
        
        if (!isFriend) {
            model.addAttribute("error", "친구만 방문할 수 있습니다.");
            return "error";
        }
        
        model.addAttribute("friendId", friendId);
        return "friend-village";
    }
    
    private Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

