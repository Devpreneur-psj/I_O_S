package com.soi.game.controller;

import com.soi.game.entity.GameTime;
import com.soi.game.service.GameTimeService;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 게임 시간 관련 컨트롤러
 */
@Controller
@RequestMapping("/game-time")
public class GameTimeController {

    private final GameTimeService gameTimeService;
    private final UserRepository userRepository;

    @Autowired
    public GameTimeController(GameTimeService gameTimeService, UserRepository userRepository) {
        this.gameTimeService = gameTimeService;
        this.userRepository = userRepository;
    }

    /**
     * 게임 시간 정보 조회 API
     */
    @GetMapping("/api/info")
    @ResponseBody
    public ResponseEntity<GameTime> getGameTimeInfo(Authentication authentication) {
        Long userId = getUserId(authentication);
        GameTime gameTime = gameTimeService.advanceGameTime(userId);
        return ResponseEntity.ok(gameTime);
    }

    /**
     * 게임 속도 설정 API
     */
    @PostMapping("/api/set-speed")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setGameSpeed(
            @RequestParam Integer speed,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            gameTimeService.setGameSpeed(userId, speed);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "게임 속도가 설정되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
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

