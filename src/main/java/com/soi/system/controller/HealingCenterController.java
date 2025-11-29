package com.soi.system.controller;

import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 정령 병원 컨트롤러
 * 게임 오류 문의 및 피드백 기능
 */
@Controller
@RequestMapping("/healing-center")
public class HealingCenterController {

    @Autowired
    private UserRepository userRepository;

    /**
     * 정령 병원 페이지
     */
    @GetMapping
    public String healingCenter(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElse(null);
            if (user != null) {
                model.addAttribute("username", user.getUsername());
            }
        }
        return "healing-center";
    }

    /**
     * 피드백 제출 API
     */
    @PostMapping("/submit-feedback")
    @ResponseBody
    public Map<String, Object> submitFeedback(
            @RequestParam String category,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String email,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // TODO: 실제로는 데이터베이스에 저장하는 로직이 필요
            // 현재는 콘솔에만 출력
            System.out.println("=== 피드백 제출 ===");
            System.out.println("카테고리: " + category);
            System.out.println("제목: " + title);
            System.out.println("내용: " + content);
            System.out.println("이메일: " + (email != null ? email : "없음"));
            System.out.println("제출 시간: " + LocalDateTime.now());
            if (authentication != null) {
                System.out.println("사용자: " + authentication.getName());
            }
            System.out.println("==================");
            
            response.put("success", true);
            response.put("message", "피드백이 성공적으로 제출되었습니다. 감사합니다!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "피드백 제출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
}

