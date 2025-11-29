package com.soi.worldtree.controller;

import com.soi.user.User;
import com.soi.user.UserRepository;
import com.soi.worldtree.dto.BlessingGrantRequest;
import com.soi.worldtree.dto.EssencePulseRequest;
import com.soi.worldtree.dto.LevelUpResult;
import com.soi.worldtree.dto.WorldTreeInfo;
import com.soi.worldtree.service.WorldTreeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/world-tree")
public class WorldTreeController {

    private final WorldTreeService worldTreeService;
    private final UserRepository userRepository;

    @Autowired
    public WorldTreeController(WorldTreeService worldTreeService, UserRepository userRepository) {
        this.worldTreeService = worldTreeService;
        this.userRepository = userRepository;
    }

    /**
     * 세계수의 심장 진입 - 즉시 코어 페이지로 리다이렉트
     */
    @GetMapping("/heart")
    public String heart() {
        return "redirect:/world-tree/core";
    }

    /**
     * 세계수 코어 페이지
     */
    @GetMapping("/core")
    public String core(@RequestParam(required = false) String error,
                       @RequestParam(required = false) String message,
                       Model model, Authentication authentication) {
        Long userId = getUserId(authentication);
        WorldTreeInfo info = worldTreeService.getWorldTreeInfo(userId);
        model.addAttribute("worldTreeInfo", info);
        
        // 에러/메시지 전달
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (message != null) {
            model.addAttribute("message", message);
        }
        
        return "world-tree";
    }

    /**
     * 정령의 축복 추가 API
     * 정령이 죽었을 때 호출되는 API
     * 추후 확장: 정령의 등급 및 친밀도에 따라 포인트가 달라질 수 있음
     */
    @PostMapping("/api/blessing/add")
    @ResponseBody
    public ResponseEntity<Void> addBlessing(
            @RequestBody EssencePulseRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        worldTreeService.addBlessing(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 정령의 축복을 경험치로 부여하는 API
     * 
     * @param request 부여 요청 (amount가 null이면 전체 부여)
     * @param authentication 인증 정보
     * @return 레벨업 결과
     */
    @PostMapping("/api/blessing/grant")
    @ResponseBody
    public ResponseEntity<LevelUpResult> grantBlessingToExp(
            @RequestBody BlessingGrantRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        LevelUpResult result = worldTreeService.grantBlessingToExp(userId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * 세계수 정보 조회 API
     */
    @GetMapping("/api/info")
    @ResponseBody
    public ResponseEntity<WorldTreeInfo> getInfo(Authentication authentication) {
        Long userId = getUserId(authentication);
        WorldTreeInfo info = worldTreeService.getWorldTreeInfo(userId);
        return ResponseEntity.ok(info);
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

