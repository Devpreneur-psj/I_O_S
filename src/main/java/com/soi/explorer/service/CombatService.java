package com.soi.explorer.service;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.entity.Skill;
import com.soi.spirit.entity.SpiritSkill;
import com.soi.spirit.service.SkillService;
import com.soi.spirit.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 전투 서비스
 */
@Service
public class CombatService {

    private final SkillService skillService;
    private final SkillRepository skillRepository;

    @Autowired
    public CombatService(SkillService skillService, SkillRepository skillRepository) {
        this.skillService = skillService;
        this.skillRepository = skillRepository;
    }

    /**
     * 전투를 수행하고 결과를 반환합니다. (모든 플레이어 정령 참가)
     */
    public CombatResult performCombat(List<Spirit> playerSpirits, List<Spirit> enemies) {
        if (playerSpirits == null || playerSpirits.isEmpty()) {
            throw new IllegalArgumentException("플레이어 정령 목록이 null이거나 비어있습니다.");
        }
        if (enemies == null || enemies.isEmpty()) {
            throw new IllegalArgumentException("적 정령 목록이 null이거나 비어있습니다.");
        }
        
        List<CombatRound> rounds = new ArrayList<>();
        
        // 모든 플레이어 정령 복제
        List<Spirit> currentPlayers = new ArrayList<>();
        System.out.println("=== 전투 참여 정령 목록 ===");
        for (Spirit player : playerSpirits) {
            if (player != null) {
                System.out.println("정령 ID: " + player.getId() + ", 이름: " + player.getName() + ", 에너지: " + player.getEnergy());
                Spirit clonedPlayer = cloneSpirit(player);
                if (clonedPlayer != null) {
                    System.out.println("  -> 복제 성공, 복제된 ID: " + clonedPlayer.getId());
                    currentPlayers.add(clonedPlayer);
                } else {
                    System.out.println("  -> 복제 실패!");
                }
            }
        }
        System.out.println("전투 참여 정령 수: " + currentPlayers.size());
        
        if (currentPlayers.isEmpty()) {
            throw new IllegalArgumentException("유효한 플레이어 정령이 없습니다.");
        }
        
        // 모든 적 정령 복제
        List<Spirit> currentEnemies = new ArrayList<>();
        for (Spirit enemy : enemies) {
            if (enemy != null) {
                Spirit clonedEnemy = cloneSpirit(enemy);
                if (clonedEnemy != null) {
                    currentEnemies.add(clonedEnemy);
                }
            }
        }
        
        if (currentEnemies.isEmpty()) {
            throw new IllegalArgumentException("유효한 적 정령이 없습니다.");
        }
        
        int roundNumber = 1;
        int maxRounds = 50; // 최대 라운드 수
        
        // 플레이어 정령 중 하나라도 살아있으면 계속
        boolean hasAlivePlayers = currentPlayers.stream()
                .anyMatch(p -> p != null && (p.getEnergy() == null || p.getEnergy() > 0));
        
        while (hasAlivePlayers && !currentEnemies.isEmpty() && roundNumber <= maxRounds) {
            CombatRound round = performRound(currentPlayers, currentEnemies, roundNumber);
            rounds.add(round);
            
            // 죽은 적 제거
            currentEnemies.removeIf(e -> e == null || (e.getEnergy() != null && e.getEnergy() <= 0));
            
            // 죽은 플레이어 정령 제거
            currentPlayers.removeIf(p -> p == null || (p.getEnergy() != null && p.getEnergy() <= 0));
            
            // 플레이어 생존 확인
            hasAlivePlayers = currentPlayers.stream()
                    .anyMatch(p -> p != null && (p.getEnergy() == null || p.getEnergy() > 0));
            
            if (!hasAlivePlayers) {
                break; // 모든 플레이어 정령 패배
            }
            
            roundNumber++;
        }
        
        boolean victory = hasAlivePlayers && currentEnemies.isEmpty();
        // 남은 플레이어 정령들의 평균 HP 계산
        int totalPlayerEnergy = currentPlayers.stream()
                .filter(p -> p != null && p.getEnergy() != null)
                .mapToInt(Spirit::getEnergy)
                .sum();
        int finalPlayerEnergy = currentPlayers.isEmpty() ? 0 : (totalPlayerEnergy / currentPlayers.size());
        
        int totalEnemyEnergy = currentEnemies.stream()
                .filter(e -> e != null && e.getEnergy() != null)
                .mapToInt(Spirit::getEnergy)
                .sum();
        
        // 전투 후 플레이어 정령 상태 반환 (원본 정령과 매칭하기 위해 이름으로 찾기)
        List<Spirit> finalPlayerSpirits = new ArrayList<>(currentPlayers);
        
        return new CombatResult(victory, rounds, finalPlayerEnergy, totalEnemyEnergy, finalPlayerSpirits);
    }

    /**
     * 한 라운드의 전투를 수행합니다. (모든 플레이어 정령 참가)
     */
    private CombatRound performRound(List<Spirit> players, List<Spirit> enemies, int roundNumber) {
        CombatRound round = new CombatRound(roundNumber);
        
        // 스피드에 따라 공격 순서 결정
        List<Combatant> combatants = new ArrayList<>();
        
        // 모든 플레이어 정령 추가
        System.out.println("=== performRound: 플레이어 정령 추가 ===");
        for (Spirit player : players) {
            if (player != null) {
                int energy = player.getEnergy() != null ? player.getEnergy() : 0;
                System.out.println("플레이어 정령 ID: " + player.getId() + ", 이름: " + player.getName() + ", 에너지: " + energy);
                if (energy > 0) {
                    combatants.add(new Combatant(player, true));
                    System.out.println("  -> 전투원으로 추가됨");
                } else {
                    System.out.println("  -> 에너지 0 이하, 스킵");
                }
            }
        }
        
        // 모든 적 정령 추가
        for (Spirit enemy : enemies) {
            if (enemy != null && (enemy.getEnergy() == null || enemy.getEnergy() > 0)) {
                combatants.add(new Combatant(enemy, false));
            }
        }
        
        // 스피드 순으로 정렬 (null 체크 포함)
        combatants.sort((a, b) -> {
            int speedA = (a.spirit.getSpeed() != null ? a.spirit.getSpeed() : 50);
            int speedB = (b.spirit.getSpeed() != null ? b.spirit.getSpeed() : 50);
            return Integer.compare(speedB, speedA);
        });
        
        // 각 전투원이 공격 (Iterator를 사용하여 ConcurrentModificationException 방지)
        List<Combatant> toRemove = new ArrayList<>();
        Iterator<Combatant> iterator = combatants.iterator();
        
        while (iterator.hasNext()) {
            Combatant attacker = iterator.next();
            if (attacker == null || attacker.spirit == null) {
                continue;
            }
            
            int attackerEnergy = attacker.spirit.getEnergy() != null ? attacker.spirit.getEnergy() : 0;
            if (attackerEnergy <= 0) {
                continue;
            }
            
            // 공격 대상 선택
            Combatant target = selectTarget(attacker, combatants);
            if (target == null || target.spirit == null) {
                continue;
            }
            
            // 플레이어 정령의 경우 배운 기술이 있는지 확인
            SpiritSkill usedSkill = null;
            Skill skill = null;
            if (attacker.isPlayer) {
                try {
                    Long spiritId = attacker.spirit.getId();
                    System.out.println("=== 플레이어 정령 공격 시도 ===");
                    System.out.println("정령 이름: " + attacker.spirit.getName());
                    System.out.println("정령 ID: " + spiritId);
                    
                    if (spiritId == null) {
                        System.err.println("경고: 정령 ID가 null입니다! 공격을 스킵합니다.");
                        continue;
                    }
                    
                    List<SpiritSkill> learnedSkills = skillService.getLearnedSkills(spiritId);
                    if (learnedSkills == null) {
                        learnedSkills = new ArrayList<>();
                    }
                    
                    System.out.println("정령 " + attacker.spirit.getName() + " (ID: " + spiritId + ")의 배운 기술 수: " + learnedSkills.size());
                    
                    // 학습 완료된 기술만 필터링 (학습 중인 기술 제외)
                    List<SpiritSkill> availableSkills = learnedSkills.stream()
                            .filter(ss -> {
                                if (ss == null) {
                                    System.out.println("  - null 기술 스킵");
                                    return false;
                                }
                                boolean isLearning = ss.getIsLearning() != null && ss.getIsLearning();
                                boolean hasLearnedAt = ss.getLearnedAt() != null;
                                System.out.println("  - 기술 ID: " + ss.getSkillId() + ", isLearning: " + isLearning + ", learnedAt: " + hasLearnedAt);
                                return !isLearning && hasLearnedAt;
                            })
                            .collect(java.util.stream.Collectors.toList());
                    
                    System.out.println("  사용 가능한 기술 수: " + availableSkills.size());
                    
                    // 사용할 기술 선택 및 쿨타임 체크
                    List<SpiritSkill> readySkills = new ArrayList<>();
                    LocalDateTime now = LocalDateTime.now();
                    
                    // 기술이 있으면 쿨타임 체크
                    if (!availableSkills.isEmpty()) {
                        for (SpiritSkill ss : availableSkills) {
                            if (ss == null || ss.getSkillId() == null) {
                                System.out.println("  - 기술 ID가 null인 기술 스킵");
                                continue;
                            }
                            
                            Optional<Skill> skillOpt = skillRepository.findById(ss.getSkillId());
                            if (skillOpt.isPresent()) {
                                Skill s = skillOpt.get();
                                int cooldownSeconds = s.getCooldownSeconds() != null ? s.getCooldownSeconds() : 0;
                                
                                if (cooldownSeconds == 0) {
                                    // 쿨타임이 없으면 바로 사용 가능
                                    System.out.println("  - 기술 ID " + ss.getSkillId() + " 사용 가능 (쿨타임 없음)");
                                    readySkills.add(ss);
                                } else if (ss.getLastUsedTime() == null) {
                                    // 한 번도 사용하지 않았으면 사용 가능
                                    System.out.println("  - 기술 ID " + ss.getSkillId() + " 사용 가능 (처음 사용)");
                                    readySkills.add(ss);
                                } else {
                                    // 쿨타임 체크
                                    long secondsSinceLastUse = java.time.Duration.between(ss.getLastUsedTime(), now).getSeconds();
                                    if (secondsSinceLastUse >= cooldownSeconds) {
                                        System.out.println("  - 기술 ID " + ss.getSkillId() + " 사용 가능 (쿨타임 완료: " + secondsSinceLastUse + "/" + cooldownSeconds + "초)");
                                        readySkills.add(ss);
                                    } else {
                                        System.out.println("  - 기술 ID " + ss.getSkillId() + " 쿨타임 중 (" + secondsSinceLastUse + "/" + cooldownSeconds + "초)");
                                    }
                                }
                            } else {
                                System.out.println("  - 기술 ID " + ss.getSkillId() + "를 찾을 수 없음");
                            }
                        }
                    }
                    
                    System.out.println("  준비된 기술 수: " + readySkills.size());
                    
                    // 기술이 있고 쿨타임이 완료된 것이 있으면 사용, 없으면 기본 공격
                    if (!readySkills.isEmpty()) {
                        // 랜덤으로 기술 선택
                        usedSkill = readySkills.get(new Random().nextInt(readySkills.size()));
                        skill = skillRepository.findById(usedSkill.getSkillId()).orElse(null);
                        
                        System.out.println("  ✅ 선택된 기술 ID: " + (usedSkill != null ? usedSkill.getSkillId() : "null") + ", 기술 이름: " + (skill != null ? skill.getSkillName() : "null"));
                        System.out.println("=== 플레이어 정령 기술 공격 진행 ===");
                        
                        // 기술 사용 시간 업데이트
                        if (usedSkill != null) {
                            usedSkill.setLastUsedTime(now);
                        }
                    } else {
                        // 기술이 없거나 모든 기술이 쿨타임 중이면 기본 공격 사용
                        System.out.println("정령 " + attacker.spirit.getName() + " (ID: " + spiritId + ")는 기술이 없거나 모든 기술이 쿨타임 중입니다. 기본 공격을 사용합니다.");
                        usedSkill = null;
                        skill = null;
                    }
                } catch (Exception e) {
                    System.err.println("기술 확인 중 오류 발생: " + e.getMessage());
                    e.printStackTrace();
                    // 오류 발생 시 공격 스킵
                    continue;
                }
            }
            
            // 공격 수행 (기술 정보 포함)
            int damage = calculateDamage(attacker.spirit, target.spirit, attacker.isPlayer, skill, usedSkill);
            int targetEnergy = target.spirit.getEnergy() != null ? target.spirit.getEnergy() : 0;
            target.spirit.setEnergy(Math.max(0, targetEnergy - damage));
            
            String attackerName = attacker.spirit.getName() != null ? attacker.spirit.getName() : "이름없음";
            String targetName = target.spirit.getName() != null ? target.spirit.getName() : "이름없음";
            
            // 공격자와 타겟의 능력치 정보 생성
            Map<String, Object> attackerInfo = new HashMap<>();
            attackerInfo.put("name", attackerName);
            attackerInfo.put("spiritType", attacker.spirit.getSpiritType());
            attackerInfo.put("level", attacker.spirit.getLevel() != null ? attacker.spirit.getLevel() : 1);
            attackerInfo.put("meleeAttack", attacker.spirit.getMeleeAttack() != null ? attacker.spirit.getMeleeAttack() : 50);
            attackerInfo.put("rangedAttack", attacker.spirit.getRangedAttack() != null ? attacker.spirit.getRangedAttack() : 50);
            attackerInfo.put("speed", attacker.spirit.getSpeed() != null ? attacker.spirit.getSpeed() : 50);
            
            Map<String, Object> targetInfo = new HashMap<>();
            targetInfo.put("name", targetName);
            targetInfo.put("spiritType", target.spirit.getSpiritType());
            targetInfo.put("level", target.spirit.getLevel() != null ? target.spirit.getLevel() : 1);
            targetInfo.put("meleeDefense", target.spirit.getMeleeDefense() != null ? target.spirit.getMeleeDefense() : 50);
            targetInfo.put("rangedDefense", target.spirit.getRangedDefense() != null ? target.spirit.getRangedDefense() : 50);
            
            // 사용한 기술 정보 추가
            String skillName = null;
            if (skill != null) {
                skillName = skill.getSkillName();
            }
            
            round.addAction(new CombatAction(
                    attackerName,
                    targetName,
                    damage,
                    target.spirit.getEnergy() <= 0,
                    attackerInfo,
                    targetInfo,
                    skillName
            ));
            
            // 타겟이 죽었으면 제거 목록에 추가
            if (target.spirit.getEnergy() <= 0) {
                toRemove.add(target);
            }
            
            // 승부 결정
            boolean playerAlive = combatants.stream()
                    .anyMatch(c -> c != null && c.isPlayer && c.spirit != null && 
                            (c.spirit.getEnergy() == null || c.spirit.getEnergy() > 0));
            boolean enemiesAlive = combatants.stream()
                    .anyMatch(c -> c != null && !c.isPlayer && c.spirit != null && 
                            (c.spirit.getEnergy() == null || c.spirit.getEnergy() > 0));
            
            if (!playerAlive || !enemiesAlive) {
                break;
            }
        }
        
        // 죽은 전투원 제거
        combatants.removeAll(toRemove);
        
        return round;
    }

    /**
     * 공격 대상을 선택합니다. (랜덤 선택)
     */
    private Combatant selectTarget(Combatant attacker, List<Combatant> combatants) {
        if (attacker == null || attacker.isPlayer) {
            // 플레이어는 적 중 하나를 랜덤으로 선택
            List<Combatant> aliveEnemies = combatants.stream()
                    .filter(c -> c != null && !c.isPlayer && c.spirit != null && 
                            (c.spirit.getEnergy() == null || c.spirit.getEnergy() > 0))
                    .collect(java.util.stream.Collectors.toList());
            
            if (aliveEnemies.isEmpty()) {
                return null;
            }
            
            // 랜덤으로 적 선택
            return aliveEnemies.get(new Random().nextInt(aliveEnemies.size()));
        } else {
            // 적은 플레이어 중 하나를 랜덤으로 선택
            List<Combatant> alivePlayers = combatants.stream()
                    .filter(c -> c != null && c.isPlayer && c.spirit != null && 
                            (c.spirit.getEnergy() == null || c.spirit.getEnergy() > 0))
                    .collect(java.util.stream.Collectors.toList());
            
            if (alivePlayers.isEmpty()) {
                return null;
            }
            
            // 랜덤으로 플레이어 선택
            return alivePlayers.get(new Random().nextInt(alivePlayers.size()));
        }
    }

    /**
     * 데미지를 계산합니다. (포켓몬스터 스타일 공식)
     * Damage = ((((2 * Level / 5 + 2) * Power * Attack / Defense) / 50) + 2) * Modifier
     */
    private int calculateDamage(Spirit attacker, Spirit defender, boolean isPlayerAttack, Skill skill, SpiritSkill spiritSkill) {
        if (attacker == null || defender == null) {
            return 1; // 기본 데미지
        }
        
        // 레벨 가져오기
        int attackerLevel = attacker.getLevel() != null ? attacker.getLevel() : 1;
        int defenderLevel = defender.getLevel() != null ? defender.getLevel() : 1;
        
            // 기술 위력 결정
            int power;
            if (skill != null && spiritSkill != null) {
                // 기술 위력 사용 (기술의 basePower + 숙련도 보정) - 초보자 친화적으로 강화
                int basePower = skill.getBasePower() != null ? skill.getBasePower() : 50;
                int masteryLevel = spiritSkill.getMasteryLevel() != null ? spiritSkill.getMasteryLevel() : 1;
                // 숙련도에 따라 위력 증가 (1레벨: 110%, 5레벨: 180%) - 초보자도 효과적
                double masteryMultiplier = 1.1 + ((masteryLevel - 1) * 0.175);
                // 기술 사용 시 추가 보너스: 기술 위력 자체를 1.3배 증가
                power = (int) (basePower * masteryMultiplier * 1.3);
            } else {
                // 기술이 없으면 기본 위력 (능력치 기반) - 기본 공격도 효과적
                int rangedAttack = attacker.getRangedAttack() != null ? attacker.getRangedAttack() : 50;
                int meleeAttack = attacker.getMeleeAttack() != null ? attacker.getMeleeAttack() : 50;
                // 기본 공격도 1.2배 보너스 적용
                power = (int) (Math.max(rangedAttack, meleeAttack) * 1.2);
            }
        
        // 공격력 결정 (성격 보정 포함)
        int rangedAttack = attacker.getRangedAttack() != null ? attacker.getRangedAttack() : 50;
        int meleeAttack = attacker.getMeleeAttack() != null ? attacker.getMeleeAttack() : 50;
        int attack = Math.max(rangedAttack, meleeAttack);
        
        // 방어력 결정 (성격 보정 포함)
        int rangedDefense = defender.getRangedDefense() != null ? defender.getRangedDefense() : 50;
        int meleeDefense = defender.getMeleeDefense() != null ? defender.getMeleeDefense() : 50;
        int defense = Math.max(rangedDefense, meleeDefense);
        
        // 포켓몬스터 스타일 데미지 공식
        // Damage = ((((2 * Level / 5 + 2) * Power * Attack / Defense) / 50) + 2) * Modifier
        double levelFactor = (2.0 * attackerLevel / 5.0 + 2.0);
        double damageBeforeModifier = ((levelFactor * power * attack / Math.max(1, defense)) / 50.0) + 2.0;
        
        // 상성 계산
        String attackerType = attacker.getSpiritType() != null ? attacker.getSpiritType() : "불의 정령";
        String defenderType = defender.getSpiritType() != null ? defender.getSpiritType() : "불의 정령";
        double typeMultiplier = calculateTypeMultiplier(attackerType, defenderType);
        
        // 레벨 차이 보정 (레벨 차이가 클수록 데미지 차이) - 5% per level로 증가
        double levelDiffMultiplier = 1.0 + ((attackerLevel - defenderLevel) * 0.05);
        levelDiffMultiplier = Math.max(0.5, Math.min(1.5, levelDiffMultiplier)); // 0.5배 ~ 1.5배 제한
        
        // 최종 데미지 계산
        double finalDamage = damageBeforeModifier * typeMultiplier * levelDiffMultiplier;
        
        // 랜덤 변동 (85% ~ 100%) - 포켓몬스터 스타일 (더 정확한 수치)
        double randomFactor = 0.85 + (new Random().nextDouble() * 0.15);
        finalDamage = finalDamage * randomFactor;
        
        // 최소 데미지 보장 (레벨에 비례, 최소 3) - 초보자 친화적으로 증가
        int minDamage = Math.max(3, attackerLevel * 2);
        
        // 플레이어 공격 시 추가 보너스 적용 (초보자 친화적)
        if (isPlayerAttack) {
            finalDamage = finalDamage * 1.2; // 플레이어 데미지 20% 증가
        }
        
        return Math.max(minDamage, (int) Math.round(finalDamage));
    }

    /**
     * 상성 배율을 계산합니다. (포켓몬스터 스타일: 효과가 굉장함 2.0x, 효과가 별로 0.5x)
     */
    private double calculateTypeMultiplier(String attackerType, String defenderType) {
        if (attackerType == null || defenderType == null) {
            return 1.0;
        }
        
        // SpiritElement 열거형을 사용한 상성 계산
        com.soi.spirit.enums.SpiritElement attacker = 
            com.soi.spirit.enums.SpiritElement.fromTypeName(attackerType);
        com.soi.spirit.enums.SpiritElement defender = 
            com.soi.spirit.enums.SpiritElement.fromTypeName(defenderType);
        
        return com.soi.spirit.enums.SpiritElement.getTypeEffectiveness(attacker, defender);
    }

    /**
     * 정령을 복제합니다 (전투 중 원본을 보호하기 위해)
     * 중요: ID는 반드시 복사해야 기술 조회가 가능합니다.
     */
    private Spirit cloneSpirit(Spirit original) {
        if (original == null) {
            return null;
        }
        
        Spirit clone = new Spirit();
        // ID 복사 (기술 조회에 필수)
        clone.setId(original.getId());
        clone.setName(original.getName() != null ? original.getName() : "이름없음");
        clone.setSpiritType(original.getSpiritType() != null ? original.getSpiritType() : "불의 정령");
        clone.setLevel(original.getLevel() != null ? original.getLevel() : 1);
        clone.setMeleeAttack(original.getMeleeAttack() != null ? original.getMeleeAttack() : 50);
        clone.setRangedAttack(original.getRangedAttack() != null ? original.getRangedAttack() : 50);
        clone.setMeleeDefense(original.getMeleeDefense() != null ? original.getMeleeDefense() : 50);
        clone.setRangedDefense(original.getRangedDefense() != null ? original.getRangedDefense() : 50);
        clone.setSpeed(original.getSpeed() != null ? original.getSpeed() : 50);
        clone.setEnergy(original.getEnergy() != null ? original.getEnergy() : 100);
        return clone;
    }

    /**
     * 전투원 클래스
     */
    private static class Combatant {
        Spirit spirit;
        boolean isPlayer;

        Combatant(Spirit spirit, boolean isPlayer) {
            this.spirit = spirit;
            this.isPlayer = isPlayer;
        }
    }

    /**
     * 전투 결과 클래스
     */
    public static class CombatResult {
        private final boolean victory;
        private final List<CombatRound> rounds;
        private final int playerRemainingHp;
        private final int totalEnemyRemainingHp;
        private final List<Spirit> finalPlayerSpirits; // 전투 후 플레이어 정령 상태

        public CombatResult(boolean victory, List<CombatRound> rounds, 
                          int playerRemainingHp, int totalEnemyRemainingHp,
                          List<Spirit> finalPlayerSpirits) {
            this.victory = victory;
            this.rounds = rounds;
            this.playerRemainingHp = playerRemainingHp;
            this.totalEnemyRemainingHp = totalEnemyRemainingHp;
            this.finalPlayerSpirits = finalPlayerSpirits != null ? finalPlayerSpirits : new ArrayList<>();
        }

        public boolean isVictory() {
            return victory;
        }

        public List<CombatRound> getRounds() {
            return rounds;
        }

        public int getPlayerRemainingHp() {
            return playerRemainingHp;
        }

        public int getTotalEnemyRemainingHp() {
            return totalEnemyRemainingHp;
        }

        public List<Spirit> getFinalPlayerSpirits() {
            return finalPlayerSpirits;
        }
    }

    /**
     * 전투 라운드 클래스
     */
    public static class CombatRound {
        private final int roundNumber;
        private final List<CombatAction> actions = new ArrayList<>();

        public CombatRound(int roundNumber) {
            this.roundNumber = roundNumber;
        }

        public void addAction(CombatAction action) {
            actions.add(action);
        }

        public int getRoundNumber() {
            return roundNumber;
        }

        public List<CombatAction> getActions() {
            return actions;
        }
    }

    /**
     * 전투 액션 클래스
     */
    public static class CombatAction {
        private final String attacker;
        private final String target;
        private final int damage;
        private final boolean isKill;
        private final Map<String, Object> attackerInfo;
        private final Map<String, Object> targetInfo;
        private final String skillName;

        public CombatAction(String attacker, String target, int damage, boolean isKill,
                          Map<String, Object> attackerInfo, Map<String, Object> targetInfo, String skillName) {
            this.attacker = attacker;
            this.target = target;
            this.damage = damage;
            this.isKill = isKill;
            this.attackerInfo = attackerInfo;
            this.targetInfo = targetInfo;
            this.skillName = skillName;
        }

        public String getAttacker() {
            return attacker;
        }

        public String getTarget() {
            return target;
        }

        public int getDamage() {
            return damage;
        }

        public boolean isKill() {
            return isKill;
        }

        public Map<String, Object> getAttackerInfo() {
            return attackerInfo;
        }

        public Map<String, Object> getTargetInfo() {
            return targetInfo;
        }

        public String getSkillName() {
            return skillName;
        }
    }
}

