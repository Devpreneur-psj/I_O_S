package com.soi.controller;

import com.soi.tutorial.service.TutorialService;
import com.soi.user.User;
import com.soi.user.UserRepository;
import com.soi.worldtree.service.WorldTreeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WorldController {

    private final TutorialService tutorialService;
    private final UserRepository userRepository;
    private final WorldTreeService worldTreeService;

    @Autowired
    public WorldController(TutorialService tutorialService, 
                          UserRepository userRepository,
                          WorldTreeService worldTreeService) {
        this.tutorialService = tutorialService;
        this.userRepository = userRepository;
        this.worldTreeService = worldTreeService;
    }

    @GetMapping("/world")
    public String world(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Long userId = getUserId(authentication);
            
            // 튜토리얼 상태
            boolean tutorialCompleted = tutorialService.isTutorialCompleted(userId);
            model.addAttribute("tutorialCompleted", tutorialCompleted);
            model.addAttribute("showTutorial", !tutorialCompleted);
            
            // 세계수 정보 (정령 생성 해금 상태 포함)
            try {
                var worldTreeInfo = worldTreeService.getWorldTreeInfo(userId);
                if (worldTreeInfo != null) {
                    model.addAttribute("worldTreeLevel", worldTreeInfo.getCurrentLevel());
                    model.addAttribute("spiritCreationUnlocked", worldTreeInfo.isSpiritCreationUnlocked());
                }
            } catch (Exception e) {
                // 에러 발생 시 기본값 사용
                model.addAttribute("worldTreeLevel", 1);
                model.addAttribute("spiritCreationUnlocked", false);
            }
        } else {
            model.addAttribute("tutorialCompleted", true);
            model.addAttribute("showTutorial", false);
            model.addAttribute("worldTreeLevel", 1);
            model.addAttribute("spiritCreationUnlocked", false);
        }
        return "world";
    }

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return user.getId();
    }
}

