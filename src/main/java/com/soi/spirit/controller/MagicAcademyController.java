package com.soi.spirit.controller;

import com.soi.spirit.entity.Skill;
import com.soi.spirit.entity.Spirit;
import com.soi.spirit.entity.SpiritSkill;
import com.soi.spirit.repository.SkillRepository;
import com.soi.spirit.service.SkillService;
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
import java.util.stream.Collectors;

/**
 * 마법 학원 컨트롤러
 */
@Controller
@RequestMapping("/magic-academy")
public class MagicAcademyController {

    private final SkillService skillService;
    private final SpiritService spiritService;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;

    @Autowired
    public MagicAcademyController(SkillService skillService, 
                                  SpiritService spiritService,
                                  UserRepository userRepository,
                                  SkillRepository skillRepository) {
        this.skillService = skillService;
        this.spiritService = spiritService;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    /**
     * 기술 강의실 페이지
     */
    @GetMapping("/academy")
    public String academy(Model model, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            
            // 사용자의 정령 목록 조회
            List<Spirit> spirits = spiritService.getUserSpirits(userId);
            model.addAttribute("spirits", spirits != null ? spirits : List.of());
        } catch (Exception e) {
            model.addAttribute("error", "기술 강의실을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("spirits", List.of());
        }
        
        return "magic-academy";
    }

    /**
     * 정령이 배울 수 있는 기술 목록 조회 API
     */
    @GetMapping("/api/learnable-skills/{spiritId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLearnableSkills(@PathVariable Long spiritId, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            // 정령 소유 확인
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            List<Skill> skills = skillService.getLearnableSkills(spiritId);
            // 학습 중인 기술이 있는지 확인
            List<SpiritSkill> learningSkills = skillService.getLearnedSkills(spiritId).stream()
                    .filter(ss -> ss.getIsLearning() != null && ss.getIsLearning())
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("skills", skills);
            response.put("isLearning", !learningSkills.isEmpty());
            
            // 학습 중인 기술이 있어도 목록은 반환 (프론트엔드에서 안내 메시지 표시)
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting learnable skills: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("skills", List.of());
            errorResponse.put("isLearning", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 정령이 배운 기술 목록 조회 API
     */
    @GetMapping("/api/learned-skills/{spiritId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getLearnedSkills(@PathVariable Long spiritId, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            // 정령 소유 확인
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            List<SpiritSkill> spiritSkills = skillService.getLearnedSkills(spiritId);
            // Skill 정보와 함께 반환
            List<Map<String, Object>> result = spiritSkills.stream().map(ss -> {
                Map<String, Object> map = new HashMap<>();
                map.put("spiritSkillId", ss.getId());
                map.put("skillId", ss.getSkillId());
                map.put("masteryLevel", ss.getMasteryLevel());
                // learnedAt을 문자열로 변환 (null이 아닌 경우)
                // LocalDateTime을 ISO-8601 형식 문자열로 변환
                if (ss.getLearnedAt() != null) {
                    map.put("learnedAt", ss.getLearnedAt().toString());
                } else {
                    map.put("learnedAt", null);
                }
                map.put("isLearning", ss.getIsLearning() != null ? ss.getIsLearning() : false);
                if (ss.getLearningStartTime() != null) {
                    map.put("learningStartTime", ss.getLearningStartTime().toString());
                } else {
                    map.put("learningStartTime", null);
                }
                if (ss.getLearningCompletionTime() != null) {
                    map.put("learningCompletionTime", ss.getLearningCompletionTime().toString());
                } else {
                    map.put("learningCompletionTime", null);
                }
                
                // Skill 정보 추가
                skillRepository.findById(ss.getSkillId()).ifPresent(skill -> {
                    map.put("skillName", skill.getSkillName());
                    map.put("skillType", skill.getSkillType());
                    map.put("elementType", skill.getElementType());
                    map.put("basePower", skill.getBasePower());
                    map.put("description", skill.getDescription());
                    map.put("unlockEvolutionStage", skill.getUnlockEvolutionStage());
                    map.put("learnTimeMinutes", skill.getLearnTimeMinutes());
                    map.put("cooldownSeconds", skill.getCooldownSeconds());
                    map.put("effectType", skill.getEffectType());
                    map.put("effectValue", skill.getEffectValue());
                });
                
                // 디버깅 로그
                System.out.println("API 응답 - SpiritSkill ID: " + ss.getId() + 
                    ", isLearning: " + ss.getIsLearning() + 
                    ", learnedAt: " + ss.getLearnedAt());
                
                return map;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 기술 학습 API
     */
    @PostMapping("/api/learn-skill")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> learnSkill(
            @RequestParam Long spiritId,
            @RequestParam Long skillId,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            // 정령 소유 확인
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            skillService.learnSkill(spiritId, skillId);
            
            // 학습 시작 정보 반환
            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new IllegalArgumentException("기술을 찾을 수 없습니다."));
            List<SpiritSkill> spiritSkills = skillService.getLearnedSkills(spiritId);
            SpiritSkill spiritSkill = spiritSkills.stream()
                    .filter(ss -> ss.getSkillId().equals(skillId) && 
                            (ss.getIsLearning() != null && ss.getIsLearning()))
                    .findFirst()
                    .orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "기술 학습을 시작했습니다!");
            if (spiritSkill != null) {
                response.put("learningStartTime", spiritSkill.getLearningStartTime());
                response.put("learningCompletionTime", spiritSkill.getLearningCompletionTime());
                response.put("learnTimeMinutes", skill.getLearnTimeMinutes());
            }
            response.put("learnedSkillCount", skillService.getLearnedSkillCount(spiritId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 기술 잊기 API
     */
    @PostMapping("/api/forget-skill")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> forgetSkill(
            @RequestParam Long spiritId,
            @RequestParam Long skillId,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            // 정령 소유 확인
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            skillService.forgetSkill(spiritId, skillId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "기술을 잊었습니다.");
            response.put("learnedSkillCount", skillService.getLearnedSkillCount(spiritId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 학습 취소 API
     */
    @PostMapping("/api/cancel-learning")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelLearning(
            @RequestParam Long spiritId,
            @RequestParam Long skillId,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            // 정령 소유 확인
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            skillService.cancelLearning(spiritId, skillId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "기술 학습을 취소했습니다.");
            response.put("learnedSkillCount", skillService.getLearnedSkillCount(spiritId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 정령의 배운 기술 개수 조회 API
     */
    @GetMapping("/api/learned-skill-count/{spiritId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLearnedSkillCount(
            @PathVariable Long spiritId,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            // 정령 소유 확인
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            int count = skillService.getLearnedSkillCount(spiritId);
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("maxCount", 4);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 학습 완료 확인 및 처리 API (프론트엔드에서 학습 완료 시간이 지났을 때 호출)
     */
    @PostMapping("/api/check-learning-completion/{spiritId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkLearningCompletion(@PathVariable Long spiritId, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));

            // 학습 완료 처리
            skillService.processCompletedLearning();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
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

