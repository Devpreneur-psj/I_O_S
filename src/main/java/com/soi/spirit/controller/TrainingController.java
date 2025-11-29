package com.soi.spirit.controller;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.service.SpiritService;
import com.soi.spirit.service.TrainingService;
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
import java.util.stream.Collectors;

/**
 * 훈련 관련 컨트롤러
 */
@Controller
@RequestMapping("/training")
public class TrainingController {

    private final TrainingService trainingService;
    private final UserRepository userRepository;
    private final SpiritService spiritService;

    @Autowired
    public TrainingController(TrainingService trainingService, UserRepository userRepository, SpiritService spiritService) {
        this.trainingService = trainingService;
        this.userRepository = userRepository;
        this.spiritService = spiritService;
    }

    /**
     * 정령 수련장 페이지
     */
    @GetMapping("/grounds")
    public String trainingGrounds(Model model, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            
            // 사용자의 정령 목록 조회 (진화 진행 중이 아닌 정령만)
            List<Spirit> allSpirits = spiritService.getUserSpirits(userId);
            List<Spirit> availableSpirits = allSpirits.stream()
                    .filter(spirit -> {
                        // 진화 진행 중이 아니고, 은퇴하지 않은 정령만
                        boolean notEvolving = spirit.getEvolutionInProgress() == null || !spirit.getEvolutionInProgress();
                        boolean notRetired = spirit.getIsRetired() == null || !spirit.getIsRetired();
                        return notEvolving && notRetired;
                    })
                    .collect(Collectors.toList());
            
            model.addAttribute("spirits", availableSpirits);
        } catch (Exception e) {
            model.addAttribute("error", "정령 수련장을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("spirits", List.of());
        }
        
        return "training-grounds";
    }

    /**
     * 정령 훈련 API
     */
    @PostMapping("/api/train")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> trainSpirit(
            @RequestParam Long spiritId,
            @RequestParam String trainingType,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            trainingService.trainSpirit(userId, spiritId, trainingType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "훈련을 완료했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
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

