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

import java.util.HashMap;
import java.util.Map;

/**
 * 사서의 탑 컨트롤러
 * 게임 환경 설정 기능
 */
@Controller
@RequestMapping("/tower-settings")
public class SettingsController {

    @Autowired
    private UserRepository userRepository;

    /**
     * 사서의 탑 페이지
     */
    @GetMapping
    public String towerSettings(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElse(null);
            if (user != null) {
                model.addAttribute("username", user.getUsername());
            }
        }
        
        // 기본 설정 값
        model.addAttribute("soundEnabled", true);
        model.addAttribute("musicEnabled", true);
        model.addAttribute("soundVolume", 70);
        model.addAttribute("musicVolume", 50);
        model.addAttribute("notificationsEnabled", true);
        model.addAttribute("autoSaveEnabled", true);
        
        return "tower-settings";
    }

    /**
     * 설정 저장 API
     */
    @PostMapping("/save-settings")
    @ResponseBody
    public Map<String, Object> saveSettings(
            @RequestParam(required = false, defaultValue = "true") boolean soundEnabled,
            @RequestParam(required = false, defaultValue = "true") boolean musicEnabled,
            @RequestParam(required = false, defaultValue = "70") int soundVolume,
            @RequestParam(required = false, defaultValue = "50") int musicVolume,
            @RequestParam(required = false, defaultValue = "true") boolean notificationsEnabled,
            @RequestParam(required = false, defaultValue = "true") boolean autoSaveEnabled,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // TODO: 실제로는 사용자 설정을 데이터베이스에 저장하는 로직이 필요
            // 현재는 콘솔에만 출력
            System.out.println("=== 설정 저장 ===");
            System.out.println("사운드: " + soundEnabled + " (볼륨: " + soundVolume + "%)");
            System.out.println("음악: " + musicEnabled + " (볼륨: " + musicVolume + "%)");
            System.out.println("알림: " + notificationsEnabled);
            System.out.println("자동 저장: " + autoSaveEnabled);
            if (authentication != null) {
                System.out.println("사용자: " + authentication.getName());
            }
            System.out.println("================");
            
            response.put("success", true);
            response.put("message", "설정이 저장되었습니다.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "설정 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
}

