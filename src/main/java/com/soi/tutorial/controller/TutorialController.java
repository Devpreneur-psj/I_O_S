package com.soi.tutorial.controller;

import com.soi.tutorial.service.TutorialService;
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
 * 튜토리얼 컨트롤러
 */
@Controller
@RequestMapping("/tutorial")
public class TutorialController {

    private final TutorialService tutorialService;
    private final UserRepository userRepository;

    @Autowired
    public TutorialController(TutorialService tutorialService, UserRepository userRepository) {
        this.tutorialService = tutorialService;
        this.userRepository = userRepository;
    }

    /**
     * 튜토리얼 완료 API
     */
    @PostMapping("/api/complete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completeTutorial(Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            tutorialService.completeTutorial(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "튜토리얼이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 튜토리얼 상태 조회 API
     */
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTutorialStatus(Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            boolean completed = tutorialService.isTutorialCompleted(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("completed", completed);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("completed", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return user.getId();
    }
}

