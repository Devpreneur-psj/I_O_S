package com.soi.spirit.controller;

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

/**
 * 정령 진화 관련 컨트롤러
 */
@Controller
@RequestMapping("/evolution")
public class EvolutionController {

    private final SpiritService spiritService;
    private final UserRepository userRepository;

    @Autowired
    public EvolutionController(SpiritService spiritService, UserRepository userRepository) {
        this.spiritService = spiritService;
        this.userRepository = userRepository;
    }

    /**
     * 정령 연구소 페이지
     */
    @GetMapping("/lab")
    public String evolutionLab(Model model, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            
            // 진화 가능한 정령 목록 조회
            List<Spirit> evolvableSpirits = spiritService.getEvolvableSpirits(userId);
            // 진화 진행 중인 정령 목록 조회
            List<Spirit> evolvingSpirits = spiritService.getEvolvingSpirits(userId);
            
            model.addAttribute("evolvableSpirits", evolvableSpirits);
            model.addAttribute("evolvingSpirits", evolvingSpirits);
        } catch (Exception e) {
            model.addAttribute("error", "정령 연구소를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("evolvableSpirits", List.of());
            model.addAttribute("evolvingSpirits", List.of());
        }
        
        return "evolution-lab";
    }

    /**
     * 정령 진화 시작 API
     */
    @PostMapping("/api/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startEvolution(
            @RequestParam Long spiritId,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            spiritService.startEvolution(userId, spiritId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "진화가 시작되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 진화 진행 상태 조회 API
     */
    @GetMapping("/api/status/{spiritId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEvolutionStatus(
            @PathVariable Long spiritId,
            Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            java.util.Optional<Spirit> spiritOpt = spiritService.getSpirit(spiritId, userId);
            if (spiritOpt.isEmpty()) {
                throw new IllegalArgumentException("정령을 찾을 수 없습니다.");
            }
            Spirit spirit = spiritOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("evolutionInProgress", spirit.getEvolutionInProgress());
            response.put("evolutionStage", spirit.getEvolutionStage());
            response.put("evolutionTargetStage", spirit.getEvolutionTargetStage());
            response.put("evolutionStartTime", spirit.getEvolutionStartTime());
            
            // 남은 시간 계산
            if (spirit.getEvolutionInProgress() && spirit.getEvolutionStartTime() != null) {
                long hoursRequired = spirit.getEvolutionTargetStage() == 1 ? 1 : 24; // 1차 진화: 1시간, 2차 진화: 24시간
                java.time.LocalDateTime endTime = spirit.getEvolutionStartTime().plusHours(hoursRequired);
                java.time.Duration remaining = java.time.Duration.between(java.time.LocalDateTime.now(), endTime);
                
                response.put("remainingHours", Math.max(0, remaining.toHours()));
                response.put("remainingMinutes", Math.max(0, remaining.toMinutes() % 60));
                response.put("isComplete", java.time.LocalDateTime.now().isAfter(endTime));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 인증된 사용자 ID 가져오기
     */
    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            return user.getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}

