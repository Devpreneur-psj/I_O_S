package com.soi.spirit.controller;

import com.soi.spirit.service.CompetitionService;
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
 * 대회/경쟁 관련 컨트롤러
 */
@Controller
@RequestMapping("/competition")
public class CompetitionController {

    private final CompetitionService competitionService;
    private final UserRepository userRepository;

    @Autowired
    public CompetitionController(CompetitionService competitionService, UserRepository userRepository) {
        this.competitionService = competitionService;
        this.userRepository = userRepository;
    }

    /**
     * 대회 참가 API
     */
    @PostMapping("/api/participate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> participateInCompetition(
            @RequestParam Long spiritId,
            @RequestParam String competitionType,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            CompetitionService.CompetitionResult result = 
                competitionService.participateInCompetition(userId, spiritId, competitionType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("won", result.isWon());
            response.put("prizeMoney", result.getPrizeMoney());
            response.put("winChance", result.getWinChance());
            response.put("message", result.isWon() ? "대회에서 승리했습니다!" : "대회에서 패배했습니다.");
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

