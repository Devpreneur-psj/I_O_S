package com.soi.explorer.controller;

import com.soi.explorer.entity.DungeonProgress;
import com.soi.explorer.entity.DungeonStage;
import com.soi.explorer.service.CombatService;
import com.soi.explorer.service.DungeonService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/explorer")
public class ExplorerController {

    private final UserRepository userRepository;
    private final DungeonService dungeonService;
    private final CombatService combatService;
    private final SpiritService spiritService;

    @Autowired
    public ExplorerController(UserRepository userRepository,
                             DungeonService dungeonService,
                             CombatService combatService,
                             SpiritService spiritService) {
        this.userRepository = userRepository;
        this.dungeonService = dungeonService;
        this.combatService = combatService;
        this.spiritService = spiritService;
    }

    /**
     * 탐험가의 길 (정령 던전) 메인 페이지
     */
    @GetMapping("/trail")
    public String trail(Model model, Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                model.addAttribute("error", "로그인이 필요합니다.");
                model.addAttribute("stages", List.of());
                model.addAttribute("progressMap", new HashMap<>());
                model.addAttribute("spirits", List.of());
                return "explorer-trail";
            }
            
            Long userId = getUserId(authentication);
            if (userId == null) {
                model.addAttribute("error", "사용자 정보를 찾을 수 없습니다.");
                model.addAttribute("stages", List.of());
                model.addAttribute("progressMap", new HashMap<>());
                model.addAttribute("spirits", List.of());
                return "explorer-trail";
            }
            
            // 스테이지 목록 조회
            List<DungeonStage> stages = List.of();
            try {
                stages = dungeonService.getAllStages();
                if (stages == null) {
                    stages = List.of();
                }
            } catch (Exception e) {
                System.err.println("Error fetching stages: " + e.getMessage());
                e.printStackTrace();
                stages = List.of();
            }
            
            // 사용자의 진행 상태 조회
            Map<Integer, DungeonProgress> progressMap = new HashMap<>();
            try {
                progressMap = dungeonService.getUserProgress(userId);
                if (progressMap == null) {
                    progressMap = new HashMap<>();
                }
            } catch (Exception e) {
                System.err.println("Error fetching user progress: " + e.getMessage());
                e.printStackTrace();
                progressMap = new HashMap<>();
            }
            
            // 사용 가능한 정령 목록
            List<Spirit> spirits = List.of();
            try {
                spirits = spiritService.getUserSpirits(userId);
                if (spirits == null) {
                    spirits = List.of();
                } else {
                    spirits = spirits.stream()
                            .filter(s -> s != null && 
                                    (s.getEvolutionInProgress() == null || !s.getEvolutionInProgress())
                                    && (s.getIsRetired() == null || !s.getIsRetired()))
                            .collect(java.util.stream.Collectors.toList());
                }
            } catch (Exception e) {
                System.err.println("Error fetching user spirits: " + e.getMessage());
                e.printStackTrace();
                spirits = List.of();
            }
            
            model.addAttribute("stages", stages);
            model.addAttribute("progressMap", progressMap);
            model.addAttribute("spirits", spirits);
            
        } catch (Exception e) {
            System.err.println("Unexpected error in trail: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "탐험가의 길을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("stages", List.of());
            model.addAttribute("progressMap", new HashMap<>());
            model.addAttribute("spirits", List.of());
        }
        
        return "explorer-trail";
    }

    /**
     * 던전 전투 시작 API
     */
    @PostMapping("/api/start-battle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startBattle(
            @RequestParam Long spiritId,
            @RequestParam Integer stageNumber,
            Authentication authentication) {
        
        try {
            Long userId = getUserId(authentication);
            
            // 정령 소유 확인
            spiritService.getSpirit(spiritId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
            
            // 스테이지 잠금 확인
            if (dungeonService.isStageLocked(userId, stageNumber)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "이전 스테이지를 먼저 클리어해야 합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 적 생성 (1라운드 기준)
            List<Spirit> enemies = dungeonService.generateEnemies(stageNumber, 1, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("enemies", enemies.stream().map(e -> {
                Map<String, Object> enemyData = new HashMap<>();
                enemyData.put("name", e.getName());
                enemyData.put("spiritType", e.getSpiritType());
                enemyData.put("level", e.getLevel());
                enemyData.put("meleeAttack", e.getMeleeAttack());
                enemyData.put("rangedAttack", e.getRangedAttack());
                enemyData.put("meleeDefense", e.getMeleeDefense());
                enemyData.put("rangedDefense", e.getRangedDefense());
                enemyData.put("speed", e.getSpeed());
                enemyData.put("energy", e.getEnergy());
                return enemyData;
            }).collect(java.util.stream.Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 던전 전투 수행 API
     */
    @PostMapping("/api/perform-battle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> performBattle(
            @RequestParam Long spiritId,
            @RequestParam Integer stageNumber,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "사용자 정보를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 모든 정령 조회 (보유 중인 모든 정령이 던전에 참가)
            List<Spirit> playerSpirits = null;
            Map<String, Integer> initialHpMap = new HashMap<>(); // 초기 HP 저장용
            try {
                playerSpirits = spiritService.getUserSpirits(userId);
                // 진화 중이거나 학습 중이거나 은퇴한 정령 제외
                playerSpirits = playerSpirits.stream()
                        .filter(s -> (s.getEvolutionInProgress() == null || !s.getEvolutionInProgress())
                                && (s.getIsRetired() == null || !s.getIsRetired()))
                        .collect(Collectors.toList());
                
                if (playerSpirits == null || playerSpirits.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "참가할 수 있는 정령이 없습니다.");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 초기 HP 저장 (전투 전 상태)
                for (Spirit spirit : playerSpirits) {
                    if (spirit.getName() != null) {
                        initialHpMap.put(spirit.getName(), spirit.getEnergy() != null ? spirit.getEnergy() : 100);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching spirits: " + e.getMessage());
                e.printStackTrace();
                response.put("success", false);
                response.put("message", "정령을 불러올 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 스테이지 잠금 확인
            try {
                if (dungeonService.isStageLocked(userId, stageNumber)) {
                    response.put("success", false);
                    response.put("message", "이전 스테이지를 먼저 클리어해야 합니다.");
                    return ResponseEntity.badRequest().body(response);
                }
            } catch (Exception e) {
                System.err.println("Error checking stage lock: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 던전 라운드 3개 처리
            List<Map<String, Object>> dungeonRounds = new ArrayList<>();
            boolean overallVictory = true;
            long totalStartTime = System.currentTimeMillis();
            
            System.out.println("=== 전투 시작 ===");
            System.out.println("플레이어 정령 수: " + (playerSpirits != null ? playerSpirits.size() : 0));
            System.out.println("스테이지 번호: " + stageNumber);
            
            for (int roundNum = 1; roundNum <= 3; roundNum++) {
                System.out.println("--- 라운드 " + roundNum + " 시작 ---");
                Map<String, Object> roundResult = new HashMap<>();
                roundResult.put("roundNumber", roundNum);
                
                // 각 라운드별 적 생성
                List<Spirit> enemies = null;
                try {
                    System.out.println("적 생성 시도: 스테이지 " + stageNumber + ", 라운드 " + roundNum);
                    enemies = dungeonService.generateEnemies(stageNumber, roundNum, userId);
                    System.out.println("생성된 적 수: " + (enemies != null ? enemies.size() : 0));
                    if (enemies == null || enemies.isEmpty()) {
                        System.err.println("적 생성 실패: 적이 생성되지 않았습니다.");
                        roundResult.put("success", false);
                        roundResult.put("message", "적을 생성할 수 없습니다.");
                        dungeonRounds.add(roundResult);
                        overallVictory = false;
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("Error generating enemies for round " + roundNum + ": " + e.getMessage());
                    e.printStackTrace();
                    roundResult.put("success", false);
                    roundResult.put("message", "적을 생성할 수 없습니다: " + e.getMessage());
                    dungeonRounds.add(roundResult);
                    overallVictory = false;
                    break;
                }
                
                // 각 라운드별 전투 수행 (모든 정령 참가)
                CombatService.CombatResult result = null;
                try {
                    System.out.println("전투 수행 시작: 플레이어 " + playerSpirits.size() + "명 vs 적 " + enemies.size() + "명");
                    result = combatService.performCombat(playerSpirits, enemies);
                    System.out.println("전투 완료: 승리=" + result.isVictory() + ", 라운드 수=" + (result.getRounds() != null ? result.getRounds().size() : 0));
                    
                    roundResult.put("success", true);
                    roundResult.put("victory", result.isVictory());
                    roundResult.put("rounds", result.getRounds() != null ? result.getRounds().size() : 0);
                    roundResult.put("playerRemainingHp", result.getPlayerRemainingHp());
                    
                    // 전투 상세 정보 (애니메이션용)
                    List<Map<String, Object>> roundsData = new ArrayList<>();
                    if (result.getRounds() != null) {
                        System.out.println("라운드 데이터 생성 시작: " + result.getRounds().size() + "개 라운드");
                        for (CombatService.CombatRound round : result.getRounds()) {
                            Map<String, Object> roundData = new HashMap<>();
                            roundData.put("roundNumber", round.getRoundNumber());
                            List<Map<String, Object>> actionsData = new ArrayList<>();
                            if (round.getActions() != null) {
                                System.out.println("  라운드 " + round.getRoundNumber() + ": " + round.getActions().size() + "개 액션");
                                for (CombatService.CombatAction action : round.getActions()) {
                                    Map<String, Object> actionData = new HashMap<>();
                                    actionData.put("attacker", action.getAttacker());
                                    actionData.put("target", action.getTarget());
                                    actionData.put("damage", action.getDamage());
                                    actionData.put("isKill", action.isKill());
                                    actionData.put("skillName", action.getSkillName()); // 기술 이름 추가
                                    // 능력치 정보 추가 (애니메이션에서 사용)
                                    if (action.getAttackerInfo() != null) {
                                        actionData.put("attackerInfo", action.getAttackerInfo());
                                    }
                                    if (action.getTargetInfo() != null) {
                                        actionData.put("targetInfo", action.getTargetInfo());
                                    }
                                    actionsData.add(actionData);
                                }
                            } else {
                                System.out.println("  라운드 " + round.getRoundNumber() + ": 액션이 없습니다.");
                            }
                            roundData.put("actions", actionsData);
                            roundsData.add(roundData);
                        }
                    } else {
                        System.err.println("경고: result.getRounds()가 null입니다.");
                    }
                    roundResult.put("roundsData", roundsData);
                    System.out.println("생성된 roundsData: " + roundsData.size() + "개");
                    
                    // 적 정보 (능력치 포함)
                    List<Map<String, Object>> enemiesInfo = new ArrayList<>();
                    for (Spirit enemy : enemies) {
                        Map<String, Object> enemyInfo = new HashMap<>();
                        enemyInfo.put("name", enemy.getName());
                        enemyInfo.put("spiritType", enemy.getSpiritType());
                        enemyInfo.put("level", enemy.getLevel() != null ? enemy.getLevel() : 1);
                        enemyInfo.put("meleeAttack", enemy.getMeleeAttack() != null ? enemy.getMeleeAttack() : 50);
                        enemyInfo.put("rangedAttack", enemy.getRangedAttack() != null ? enemy.getRangedAttack() : 50);
                        enemyInfo.put("meleeDefense", enemy.getMeleeDefense() != null ? enemy.getMeleeDefense() : 50);
                        enemyInfo.put("rangedDefense", enemy.getRangedDefense() != null ? enemy.getRangedDefense() : 50);
                        enemyInfo.put("speed", enemy.getSpeed() != null ? enemy.getSpeed() : 50);
                        enemyInfo.put("maxHp", enemy.getEnergy() != null ? enemy.getEnergy() : 100);
                        enemyInfo.put("currentHp", enemy.getEnergy() != null ? enemy.getEnergy() : 100);
                        enemyInfo.put("isBoss", roundNum == 3); // 3라운드는 보스
                        enemiesInfo.add(enemyInfo);
                    }
                    roundResult.put("enemiesInfo", enemiesInfo);
                    
                    // roundResult를 dungeonRounds에 추가 (승리/패배 관계없이)
                    dungeonRounds.add(roundResult);
                    System.out.println("라운드 " + roundNum + " 완료, dungeonRounds 크기: " + dungeonRounds.size());
                    
                    if (!result.isVictory()) {
                        overallVictory = false;
                        System.out.println("패배로 인해 다음 라운드 진행 중단");
                        break; // 패배 시 다음 라운드 진행 안 함
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error performing combat for round " + roundNum + ": " + e.getMessage());
                    e.printStackTrace();
                    roundResult.put("success", false);
                    roundResult.put("message", "전투 수행 중 오류가 발생했습니다: " + e.getMessage());
                    dungeonRounds.add(roundResult);
                    overallVictory = false;
                    break;
                }
            }
            
            System.out.println("=== 전투 종료 ===");
            System.out.println("최종 dungeonRounds 크기: " + dungeonRounds.size());
            System.out.println("최종 승리 여부: " + overallVictory);
            
            long totalEndTime = System.currentTimeMillis();
            int clearTime = (int) ((totalEndTime - totalStartTime) / 1000);
            
            // 플레이어 정령 정보 (모든 정령) - 전투 후 업데이트된 HP 사용, 능력치 포함
            List<Map<String, Object>> playersInfo = new ArrayList<>();
            for (Spirit spirit : playerSpirits) {
                Map<String, Object> playerInfo = new HashMap<>();
                playerInfo.put("id", spirit.getId());
                playerInfo.put("name", spirit.getName());
                playerInfo.put("spiritType", spirit.getSpiritType());
                playerInfo.put("evolutionStage", spirit.getEvolutionStage() != null ? spirit.getEvolutionStage() : 0);
                playerInfo.put("level", spirit.getLevel() != null ? spirit.getLevel() : 1);
                playerInfo.put("meleeAttack", spirit.getMeleeAttack() != null ? spirit.getMeleeAttack() : 50);
                playerInfo.put("rangedAttack", spirit.getRangedAttack() != null ? spirit.getRangedAttack() : 50);
                playerInfo.put("meleeDefense", spirit.getMeleeDefense() != null ? spirit.getMeleeDefense() : 50);
                playerInfo.put("rangedDefense", spirit.getRangedDefense() != null ? spirit.getRangedDefense() : 50);
                playerInfo.put("speed", spirit.getSpeed() != null ? spirit.getSpeed() : 50);
                
                // 초기 HP를 maxHp로 사용
                int maxHp = initialHpMap.getOrDefault(spirit.getName(), 100);
                playerInfo.put("maxHp", maxHp);
                
                // 전투 후 현재 HP (각 라운드에서 업데이트됨)
                int currentHp = spirit.getEnergy() != null ? spirit.getEnergy() : maxHp;
                playerInfo.put("currentHp", Math.max(0, currentHp));
                
                playersInfo.add(playerInfo);
            }
            
            response.put("success", true);
            response.put("victory", overallVictory);
            response.put("dungeonRounds", dungeonRounds);
            response.put("playersInfo", playersInfo);
            
            System.out.println("응답 데이터:");
            System.out.println("  success: " + response.get("success"));
            System.out.println("  victory: " + response.get("victory"));
            System.out.println("  dungeonRounds 크기: " + (dungeonRounds != null ? dungeonRounds.size() : 0));
            System.out.println("  playersInfo 크기: " + (playersInfo != null ? playersInfo.size() : 0));
            
            // 승리 시 던전 클리어 처리
            if (overallVictory) {
                try {
                    DungeonService.DungeonResult dungeonResult = dungeonService.clearStage(
                            userId, spiritId, stageNumber, clearTime);
                    response.put("expGain", dungeonResult.getExpGain());
                    response.put("goldGain", dungeonResult.getGoldGain());
                    response.put("isFirstClear", dungeonResult.isFirstClear());
                } catch (Exception e) {
                    System.err.println("Error clearing stage: " + e.getMessage());
                    e.printStackTrace();
                    response.put("clearError", "클리어 처리 중 오류가 발생했습니다: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Unexpected error in performBattle: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "전투 중 예상치 못한 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 인증된 사용자 ID 가져오기
     */
    private Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            String username = authentication.getName();
            if (username == null || username.isEmpty()) {
                return null;
            }
            
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                System.err.println("User not found: " + username);
                return null;
            }
            
            return user.getId();
        } catch (Exception e) {
            System.err.println("Error getting user ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

