package com.soi.community.controller;

import com.soi.community.entity.ChatMessage;
import com.soi.community.entity.SpiritSquarePresence;
import com.soi.community.service.SpiritSquareService;
import com.soi.spirit.entity.Spirit;
import com.soi.spirit.service.SpiritService;
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
@RequestMapping("/spirit-square")
public class SpiritSquareController {
    
    private final SpiritSquareService spiritSquareService;
    private final SpiritService spiritService;
    private final UserRepository userRepository;
    
    @Autowired
    public SpiritSquareController(SpiritSquareService spiritSquareService, 
                                 SpiritService spiritService,
                                 UserRepository userRepository) {
        this.spiritSquareService = spiritSquareService;
        this.spiritService = spiritService;
        this.userRepository = userRepository;
    }
    
    /**
     * 정령 광장 메인 페이지
     */
    @GetMapping("/plaza")
    public String plaza(Model model, Authentication authentication) {
        Long userId = getUserId(authentication);
        if (userId == null) {
            return "redirect:/login";
        }
        
        // 사용자의 정령 목록 가져오기
        List<Spirit> userSpirits = spiritService.getUserSpirits(userId);
        model.addAttribute("spirits", userSpirits);
        
        return "spirit-square";
    }
    
    /**
     * 채널 입장 API
     */
    @PostMapping("/api/enter-channel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enterChannel(
            @RequestParam Integer channelNumber,
            @RequestParam Long spiritId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            SpiritSquarePresence presence = spiritSquareService.enterSquare(userId, channelNumber, spiritId);
            
            response.put("success", true);
            response.put("presence", presence);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 채널 퇴장 API
     */
    @PostMapping("/api/exit-channel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> exitChannel(
            @RequestParam Integer channelNumber,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            spiritSquareService.exitSquare(userId, channelNumber);
            
            response.put("success", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 채널 유저 목록 조회 API
     */
    @GetMapping("/api/channel-users")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChannelUsers(@RequestParam Integer channelNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<SpiritSquarePresence> users = spiritSquareService.getChannelUsers(channelNumber);
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
     * 위치 업데이트 API
     */
    @PostMapping("/api/update-position")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePosition(
            @RequestParam Integer channelNumber,
            @RequestParam Double x,
            @RequestParam Double y,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            spiritSquareService.updatePosition(userId, channelNumber, x, y);
            
            response.put("success", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 채팅 메시지 전송 API
     */
    @PostMapping("/api/send-message")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam Integer channelNumber,
            @RequestParam String message,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            ChatMessage chatMessage = spiritSquareService.sendMessage(userId, channelNumber, message);
            
            response.put("success", true);
            response.put("message", chatMessage);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 채팅 메시지 조회 API
     */
    @GetMapping("/api/messages")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMessages(@RequestParam Integer channelNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ChatMessage> messages = spiritSquareService.getRecentMessages(channelNumber, 100);
            response.put("success", true);
            response.put("messages", messages);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
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

