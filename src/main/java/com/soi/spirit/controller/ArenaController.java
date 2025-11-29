package com.soi.spirit.controller;

import com.soi.explorer.service.CombatService;
import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.spirit.service.SpiritService;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 정령 시합장 컨트롤러
 */
@Controller
@RequestMapping("/arena")
public class ArenaController {

    private final SpiritService spiritService;
    private final UserRepository userRepository;
    private final CombatService combatService;
    private final SpiritRepository spiritRepository;

    @Autowired
    public ArenaController(SpiritService spiritService, 
                          UserRepository userRepository,
                          CombatService combatService,
                          SpiritRepository spiritRepository) {
        this.spiritService = spiritService;
        this.userRepository = userRepository;
        this.combatService = combatService;
        this.spiritRepository = spiritRepository;
    }

    /**
     * 정령 시합장 페이지
     */
    @GetMapping("/spirit-arena")
    public String spiritArena(Model model, Authentication authentication) {
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
                    .collect(java.util.stream.Collectors.toList());
            
            model.addAttribute("spirits", availableSpirits);
        } catch (Exception e) {
            model.addAttribute("error", "정령 시합장을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("spirits", List.of());
        }
        
        return "spirit-arena";
    }

    /**
     * 시합 전투 수행 API
     */
    @PostMapping("/api/battle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> performBattle(
            @RequestParam Long spiritId,
            @RequestParam String difficulty,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            
            // 정령 조회
            Spirit playerSpirit = spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            // 에너지 체크
            if (playerSpirit.getEnergy() < 30) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "에너지가 부족합니다. (최소 30 필요)");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 난이도에 따른 적 생성
            List<Spirit> enemies = generateArenaEnemy(difficulty, playerSpirit.getLevel());
            
            // 전투 수행 (단일 정령을 리스트로 변환)
            List<Spirit> playerSpirits = List.of(playerSpirit);
            CombatService.CombatResult result = combatService.performCombat(playerSpirits, enemies);
            
            // 에너지 소모
            playerSpirit.setEnergy(Math.max(0, playerSpirit.getEnergy() - 30));
            
            // 보상 계산
            long prizeMoney = getPrizeMoney(difficulty);
            int expGain = result.isVictory() ? 100 : 30;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("victory", result.isVictory());
            response.put("rounds", result.getRounds().size());
            response.put("playerRemainingHp", result.getPlayerRemainingHp());
            
            if (result.isVictory()) {
                // 승리 시 보상 지급
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                user.setMoney(user.getMoney() + prizeMoney);
                userRepository.save(user);
                
                // 경험치 획득
                playerSpirit.setExperience(playerSpirit.getExperience() + expGain);
                
                // 행복도 증가
                playerSpirit.setHappiness(Math.min(100, playerSpirit.getHappiness() + 10));
                
                // 친밀도 증가
                playerSpirit.setIntimacy(Math.min(10, playerSpirit.getIntimacy() + 1));
                
                response.put("prizeMoney", prizeMoney);
                response.put("expGain", expGain);
            } else {
                // 패배 시 경험치 약간 획득
                playerSpirit.setExperience(playerSpirit.getExperience() + expGain);
                
                // 행복도 감소
                playerSpirit.setHappiness(Math.max(0, playerSpirit.getHappiness() - 5));
                
                response.put("expGain", expGain);
            }
            
            // 레벨업 체크
            int newLevel = calculateLevel(playerSpirit.getExperience());
            if (newLevel > playerSpirit.getLevel() && newLevel <= 50) {
                playerSpirit.setLevel(newLevel);
                if (newLevel == 50) {
                    playerSpirit.setMaxLevelReached(true);
                }
                response.put("levelUp", true);
                response.put("newLevel", newLevel);
            }
            
            spiritRepository.save(playerSpirit);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 시합장 적 생성
     */
    private List<Spirit> generateArenaEnemy(String difficulty, int playerLevel) {
        Spirit enemy = new Spirit();
        enemy.setName("시합장 상대");
        
        // 난이도에 따른 레벨 조정
        int enemyLevel = playerLevel;
        int statMultiplier = 1;
        
        switch (difficulty) {
            case "EASY":
                enemyLevel = Math.max(1, playerLevel - 5);
                statMultiplier = 1;
                break;
            case "NORMAL":
                enemyLevel = playerLevel;
                statMultiplier = 1;
                break;
            case "HARD":
                enemyLevel = Math.min(50, playerLevel + 5);
                statMultiplier = 2;
                break;
            case "EXPERT":
                enemyLevel = Math.min(50, playerLevel + 10);
                statMultiplier = 3;
                break;
        }
        
        enemy.setLevel(enemyLevel);
        enemy.setSpiritType(getRandomSpiritType());
        
        // 능력치 설정
        int baseStat = enemyLevel * statMultiplier;
        Random random = new Random();
        enemy.setMeleeAttack(baseStat + random.nextInt(20));
        enemy.setRangedAttack(baseStat + random.nextInt(20));
        enemy.setMeleeDefense(baseStat + random.nextInt(20));
        enemy.setRangedDefense(baseStat + random.nextInt(20));
        enemy.setSpeed(baseStat + random.nextInt(20));
        enemy.setEnergy(100);
        
        return List.of(enemy);
    }

    /**
     * 랜덤 정령 타입 반환
     */
    private String getRandomSpiritType() {
        String[] types = {"불의 정령", "물의 정령", "풀의 정령"};
        return types[new Random().nextInt(types.length)];
    }

    /**
     * 상금 계산
     */
    private long getPrizeMoney(String difficulty) {
        switch (difficulty) {
            case "EASY": return 100;
            case "NORMAL": return 300;
            case "HARD": return 500;
            case "EXPERT": return 1000;
            default: return 300;
        }
    }

    /**
     * 경험치로 레벨 계산
     */
    private int calculateLevel(int experience) {
        return Math.min(50, (int) Math.sqrt(experience / 10.0) + 1);
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

