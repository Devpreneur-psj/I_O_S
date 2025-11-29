package com.soi.spirit.controller;

import com.soi.spirit.entity.SpiritType;
import com.soi.spirit.entity.Spirit;
import com.soi.spirit.service.SpiritService;
import com.soi.spirit.service.LifecycleService;
import com.soi.spirit.service.SkillService;
import com.soi.spirit.repository.SkillRepository;
import com.soi.user.User;
import com.soi.user.UserRepository;
import com.soi.worldtree.service.WorldTreeService;
import com.soi.game.service.GameTimeService;
import com.soi.game.entity.GameTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/spirit")
public class SpiritController {

    private final WorldTreeService worldTreeService;
    private final UserRepository userRepository;
    private final SpiritService spiritService;
    private final GameTimeService gameTimeService;
    private final LifecycleService lifecycleService;
    private final SkillService skillService;
    private final SkillRepository skillRepository;

    @Autowired
    public SpiritController(WorldTreeService worldTreeService, 
                           UserRepository userRepository,
                           SpiritService spiritService,
                           GameTimeService gameTimeService,
                           LifecycleService lifecycleService,
                           SkillService skillService,
                           SkillRepository skillRepository) {
        this.worldTreeService = worldTreeService;
        this.userRepository = userRepository;
        this.spiritService = spiritService;
        this.gameTimeService = gameTimeService;
        this.lifecycleService = lifecycleService;
        this.skillService = skillService;
        this.skillRepository = skillRepository;
    }

    /**
     * 정령 생성 페이지
     * 레벨 2 이상일 때만 접근 가능
     */
    @GetMapping("/create")
    public String createSpirit(Model model, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            
            // 정령 생성 기능 언락 여부 확인
            boolean unlocked = false;
            try {
                unlocked = worldTreeService.isSpiritCreationUnlocked(userId);
            } catch (Exception e) {
                model.addAttribute("error", "세계수 정보를 확인할 수 없습니다.");
                return "spirit-create";
            }
            
            if (!unlocked) {
                // 레벨 2 미달성 시 세계수의 심장 페이지로 리다이렉트 (상세 메시지 포함)
                return "redirect:/world-tree/core?error=spirit_creation_locked&message=정령 생성을 위해서는 세계수 레벨 2 이상이 필요합니다. 세계수의 심장에서 정령의 축복을 사용하여 레벨업하세요.";
            }
            
            // 사용 가능한 정령 타입 조회
            var worldTreeInfo = worldTreeService.getWorldTreeInfo(userId);
            if (worldTreeInfo == null || worldTreeInfo.getCurrentLevel() == null) {
                model.addAttribute("error", "세계수 정보를 불러올 수 없습니다.");
                model.addAttribute("unlocked", false);
                model.addAttribute("availableTypes", List.<SpiritType>of());
                model.addAttribute("currentCount", 0L);
                model.addAttribute("maxCount", 0);
                model.addAttribute("canCreate", false);
                return "spirit-create";
            }
            
            int userLevel = worldTreeInfo.getCurrentLevel();
            List<SpiritType> availableTypes = List.<SpiritType>of();
            try {
                availableTypes = spiritService.getAvailableSpiritTypes(userId, userLevel);
            } catch (Exception e) {
                // 정령 타입 조회 실패 시 빈 리스트 사용
                availableTypes = List.of();
            }
            
            // 현재 소유 정령 수 및 최대 소유 수
            long currentCount = 0L;
            int maxCount = 0;
            try {
                currentCount = spiritService.getCurrentSpiritCount(userId);
                maxCount = spiritService.getMaxSpiritCount(userId);
            } catch (Exception e) {
                // 카운트 조회 실패 시 기본값 사용
            }
            
            model.addAttribute("unlocked", unlocked);
            List<SpiritType> safeAvailableTypes = availableTypes != null ? availableTypes : List.<SpiritType>of();
            model.addAttribute("availableTypes", safeAvailableTypes);
            model.addAttribute("currentCount", currentCount);
            model.addAttribute("maxCount", maxCount);
            model.addAttribute("canCreate", currentCount < maxCount && !safeAvailableTypes.isEmpty());
            
        } catch (Exception e) {
            // 모든 예외를 잡아서 안전하게 처리
            model.addAttribute("error", "정령 생성 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("unlocked", false);
            model.addAttribute("availableTypes", List.<SpiritType>of());
            model.addAttribute("currentCount", 0L);
            model.addAttribute("maxCount", 0);
            model.addAttribute("canCreate", false);
        }
        
        return "spirit-create";
    }

    /**
     * 정령 생성 처리
     */
    @PostMapping("/create")
    public String createSpiritPost(@RequestParam(required = false) String spiritTypeCode,
                                   @RequestParam(required = false) String name,
                                   Authentication authentication,
                                   Model model) {
        try {
            if (spiritTypeCode == null || spiritTypeCode.trim().isEmpty()) {
                model.addAttribute("error", "정령 타입을 선택해주세요.");
                return createSpirit(model, authentication);
            }
            
            Long userId = getUserId(authentication);
            spiritService.createSpirit(userId, spiritTypeCode, name);
            return "redirect:/spirit/village?created=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return createSpirit(model, authentication);
        } catch (Exception e) {
            model.addAttribute("error", "정령 생성 중 오류가 발생했습니다: " + e.getMessage());
            return createSpirit(model, authentication);
        }
    }

    /**
     * 정령의 마을 페이지
     */
    @GetMapping("/village")
    public String spiritVillage(@RequestParam(required = false) String created,
                                Model model, 
                                Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            
            // 사용자의 정령 목록 조회
            var allSpirits = spiritService.getUserSpirits(userId);
            
            // 학습 중, 진화 중인 정령 필터링 (공부/연구하러 간 정령은 마을에 표시하지 않음)
            var visibleSpirits = allSpirits.stream()
                    .filter(spirit -> {
                        // 진화 중인 정령 제외
                        if (spirit.getEvolutionInProgress() != null && spirit.getEvolutionInProgress()) {
                            System.out.println("정령 ID " + spirit.getId() + " 제외: 진화 중");
                            return false;
                        }
                        
                        // 학습 중인 정령 제외
                        try {
                            var learnedSkills = skillService.getLearnedSkills(spirit.getId());
                            if (learnedSkills != null && !learnedSkills.isEmpty()) {
                                // 학습 중인 기술이 있는지 확인 (isLearning이 true인 경우)
                                boolean isLearning = learnedSkills.stream()
                                        .anyMatch(ss -> {
                                            // getIsLearning()은 null이면 false를 반환하므로, true인 경우만 체크
                                            Boolean learning = ss.getIsLearning();
                                            boolean result = learning != null && learning.booleanValue() == true;
                                            if (result) {
                                                System.out.println("정령 ID " + spirit.getId() + " 학습 중 발견: SpiritSkill ID " + ss.getId());
                                            }
                                            return result;
                                        });
                                if (isLearning) {
                                    System.out.println("정령 ID " + spirit.getId() + " 제외: 학습 중");
                                    return false;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("정령 ID " + spirit.getId() + " 학습 정보 조회 실패: " + e.getMessage());
                            e.printStackTrace();
                            // 학습 정보 조회 실패 시 표시 (에러 방지)
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());
            
            System.out.println("전체 정령 수: " + (allSpirits != null ? allSpirits.size() : 0) + 
                             ", 표시할 정령 수: " + (visibleSpirits != null ? visibleSpirits.size() : 0));
            
            // 활성 이벤트 조회
            var activeEvents = spiritService.getActiveEvents(userId);
            
            // 게임 시간 정보 조회
            GameTime gameTime = gameTimeService.getOrInitializeGameTime(userId);
            gameTime = gameTimeService.advanceGameTime(userId);
            
            // 서버 현재 시간 추가
            java.time.LocalDateTime serverNow = java.time.ZonedDateTime.now(
                java.time.ZoneId.systemDefault()
            ).toLocalDateTime();
            
            model.addAttribute("spirits", visibleSpirits != null ? visibleSpirits : List.of());
            model.addAttribute("activeEvents", activeEvents != null ? activeEvents : List.of());
            model.addAttribute("gameTime", gameTime);
            model.addAttribute("serverTime", serverNow);
            if ("true".equals(created)) {
                model.addAttribute("message", "정령이 생성되었습니다!");
            }
        } catch (Exception e) {
            model.addAttribute("error", "정령의 마을을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("spirits", List.of());
            model.addAttribute("activeEvents", List.of());
        }
        
        return "spirit-village";
    }

    /**
     * 희귀 정령 선택 페이지 (15레벨 달성 보상)
     */
    @GetMapping("/rare-selection")
    public String rareSpiritSelection(Model model, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            
            // 15레벨 달성 여부 확인
            var worldTreeInfo = worldTreeService.getWorldTreeInfo(userId);
            if (worldTreeInfo == null || worldTreeInfo.getCurrentLevel() == null || worldTreeInfo.getCurrentLevel() < 15) {
                model.addAttribute("error", "15레벨 이상이어야 희귀 정령을 받을 수 있습니다.");
                return "redirect:/world-tree/core";
            }
            
            // 이미 희귀 정령을 받았는지 확인
            var status = worldTreeService.getWorldTreeStatus(userId);
            if (status != null && status.getRareSpiritReceived()) {
                model.addAttribute("error", "이미 희귀 정령을 받았습니다.");
                return "redirect:/spirit/village";
            }
            
            // 희귀 정령 타입 조회 (빛의 정령, 어둠의 정령)
            List<SpiritType> rareTypes = spiritService.getRareSpiritTypes();
            
            model.addAttribute("rareTypes", rareTypes != null ? rareTypes : List.<SpiritType>of());
            
        } catch (Exception e) {
            model.addAttribute("error", "희귀 정령 선택 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "rare-spirit-selection";
    }

    /**
     * 희귀 정령 선택 처리 (15레벨 달성 보상)
     */
    @PostMapping("/rare-selection")
    public String selectRareSpirit(@RequestParam(required = false) String spiritTypeCode,
                                   @RequestParam(required = false) String name,
                                   Authentication authentication,
                                   Model model) {
        try {
            if (spiritTypeCode == null || spiritTypeCode.trim().isEmpty()) {
                model.addAttribute("error", "정령 타입을 선택해주세요.");
                return rareSpiritSelection(model, authentication);
            }
            
            Long userId = getUserId(authentication);
            spiritService.createRareSpiritAsReward(userId, spiritTypeCode, name);
            return "redirect:/spirit/village?rareSpiritCreated=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return rareSpiritSelection(model, authentication);
        } catch (Exception e) {
            model.addAttribute("error", "희귀 정령 생성 중 오류가 발생했습니다: " + e.getMessage());
            return rareSpiritSelection(model, authentication);
        }
    }

    /**
     * 정령 정보 조회 API
     */
    @GetMapping("/api/{spiritId}")
    @ResponseBody
    public ResponseEntity<?> getSpiritInfo(@PathVariable Long spiritId, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            var spiritOpt = spiritService.getSpirit(spiritId, userId);
            if (spiritOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "정령을 찾을 수 없습니다."));
            }
            
            var spirit = spiritOpt.get();
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", spirit.getId());
            response.put("name", spirit.getName());
            response.put("spiritType", spirit.getSpiritType());
            response.put("evolutionStage", spirit.getEvolutionStage());
            response.put("level", spirit.getLevel());
            response.put("experience", spirit.getExperience());
            response.put("intimacy", spirit.getIntimacy());
            response.put("personality", spirit.getPersonality());
            response.put("happiness", spirit.getHappiness());
            response.put("hunger", spirit.getHunger());
            response.put("energy", spirit.getEnergy());
            response.put("healthStatus", spirit.getHealthStatus());
            response.put("mood", spirit.getMood());
            response.put("evolutionInProgress", spirit.getEvolutionInProgress());
            response.put("evolutionStartTime", spirit.getEvolutionStartTime());
            response.put("evolutionTargetStage", spirit.getEvolutionTargetStage());
            response.put("cocoonCareGauge", spirit.getCocoonCareGauge());
            response.put("lastCareDate", spirit.getLastCareDate());
            response.put("dailyCareCount", spirit.getDailyCareCount());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", "정령 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 고치 돌봐주기 API
     */
    @PostMapping("/api/care-cocoon/{spiritId}")
    @ResponseBody
    public ResponseEntity<?> careForCocoon(@PathVariable Long spiritId, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            var result = spiritService.careForCocoon(userId, spiritId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", "돌봐주기 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 정령 생애 주기 정보 조회 API
     */
    @GetMapping("/api/lifecycle/{spiritId}")
    @ResponseBody
    public ResponseEntity<?> getLifecycleInfo(@PathVariable Long spiritId, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            var spiritOpt = spiritService.getSpirit(spiritId, userId);
            if (spiritOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "정령을 찾을 수 없습니다."));
            }
            
            var spirit = spiritOpt.get();
            LifecycleService.LifecycleInfo lifecycleInfo = lifecycleService.getLifecycleInfo(spirit);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("age", lifecycleInfo.getAge());
            response.put("isRetired", lifecycleInfo.isRetired());
            response.put("retiredAt", lifecycleInfo.getRetiredAt());
            response.put("remainingDays", lifecycleInfo.getRemainingDays());
            response.put("lifespanExtended", lifecycleInfo.getLifespanExtended());
            response.put("maxLevelReached", spirit.getMaxLevelReached());
            response.put("lifespanCountdown", spirit.getLifespanCountdown());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", "생애 주기 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 정령의 배운 기술 목록 조회 API
     */
    @GetMapping("/api/skills/{spiritId}")
    @ResponseBody
    public ResponseEntity<?> getSpiritSkills(@PathVariable Long spiritId, Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            var spiritSkills = skillService.getLearnedSkills(spiritId);
            // Skill 정보와 함께 반환 (MagicAcademyController와 동일한 형식)
            List<Map<String, Object>> result = spiritSkills.stream().map(ss -> {
                Map<String, Object> map = new HashMap<>();
                map.put("spiritSkillId", ss.getId());
                map.put("skillId", ss.getSkillId());
                map.put("masteryLevel", ss.getMasteryLevel());
                map.put("learnedAt", ss.getLearnedAt());
                map.put("isLearning", ss.getIsLearning() != null ? ss.getIsLearning() : false);
                map.put("learningStartTime", ss.getLearningStartTime());
                map.put("learningCompletionTime", ss.getLearningCompletionTime());
                
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
                
                return map;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * 사용자의 모든 정령 목록 조회 API
     */
    @GetMapping("/api/my-spirits")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMySpirits(Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Spirit> spirits = spiritService.getUserSpirits(userId);
            List<Map<String, Object>> spiritList = spirits.stream()
                    .map(spirit -> {
                        Map<String, Object> spiritData = new HashMap<>();
                        spiritData.put("id", spirit.getId());
                        spiritData.put("userId", spirit.getUserId());
                        spiritData.put("name", spirit.getName());
                        spiritData.put("spiritType", spirit.getSpiritType());
                        spiritData.put("level", spirit.getLevel());
                        spiritData.put("evolutionStage", spirit.getEvolutionStage());
                        return spiritData;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(spiritList);
        } catch (Exception e) {
            System.err.println("Error fetching user spirits: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 친구의 정령 목록 조회 API
     */
    @GetMapping("/api/friend-spirits")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFriendSpirits(
            @RequestParam Long friendId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 친구 관계 확인은 FriendController에서 이미 했으므로 여기서는 생략
            List<Spirit> spirits = spiritService.getUserSpirits(friendId);
            List<Map<String, Object>> spiritData = spirits.stream()
                    .map(spirit -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", spirit.getId());
                        data.put("name", spirit.getName());
                        data.put("spiritType", spirit.getSpiritType());
                        data.put("level", spirit.getLevel());
                        data.put("evolutionStage", spirit.getEvolutionStage());
                        return data;
                    })
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("spirits", spiritData);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
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

