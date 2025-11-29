package com.soi.explorer.service;

import com.soi.explorer.entity.DungeonProgress;
import com.soi.explorer.entity.DungeonStage;
import com.soi.explorer.repository.DungeonProgressRepository;
import com.soi.explorer.repository.DungeonStageRepository;
import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 던전 서비스
 */
@Service
@Transactional
public class DungeonService {

    private final DungeonStageRepository dungeonStageRepository;
    private final DungeonProgressRepository dungeonProgressRepository;
    private final SpiritRepository spiritRepository;
    private final UserRepository userRepository;

    @Autowired
    public DungeonService(DungeonStageRepository dungeonStageRepository,
                         DungeonProgressRepository dungeonProgressRepository,
                         SpiritRepository spiritRepository,
                         UserRepository userRepository) {
        this.dungeonStageRepository = dungeonStageRepository;
        this.dungeonProgressRepository = dungeonProgressRepository;
        this.spiritRepository = spiritRepository;
        this.userRepository = userRepository;
    }

    /**
     * 모든 던전 스테이지 목록 조회
     */
    public List<DungeonStage> getAllStages() {
        return dungeonStageRepository.findAllByOrderByStageNumberAsc();
    }

    /**
     * 사용자의 던전 진행 상태 조회
     */
    public Map<Integer, DungeonProgress> getUserProgress(Long userId) {
        List<DungeonProgress> progressList = dungeonProgressRepository.findByUserIdOrderByStageNumberAsc(userId);
        return progressList.stream()
                .collect(Collectors.toMap(DungeonProgress::getStageNumber, p -> p));
    }

    /**
     * 스테이지가 잠겨있는지 확인
     */
    public boolean isStageLocked(Long userId, Integer stageNumber) {
        if (stageNumber == 1) {
            return false; // 첫 번째 스테이지는 항상 열림
        }
        
        // 이전 스테이지가 클리어되었는지 확인
        Optional<DungeonProgress> previousProgress = dungeonProgressRepository
                .findByUserIdAndStageNumber(userId, stageNumber - 1);
        
        return previousProgress.isEmpty() || !previousProgress.get().getIsCleared();
    }

    /**
     * 던전 전투 시작
     * 적 정령들을 생성하여 반환 (라운드별)
     * @param stageNumber 스테이지 번호
     * @param roundNumber 라운드 번호 (1, 2, 3)
     * @param userId 사용자 ID (희귀 속성 출몰 여부 확인용)
     * @return 적 정령 목록
     */
    public List<Spirit> generateEnemies(Integer stageNumber, Integer roundNumber, Long userId) {
        Optional<DungeonStage> stageOpt = dungeonStageRepository.findByStageNumber(stageNumber);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 스테이지입니다.");
        }
        
        DungeonStage stage = stageOpt.get();
        List<Spirit> enemies = new ArrayList<>();
        Random random = new Random();
        
        // 기본 적 정령 타입 목록
        List<String> spiritTypes = new ArrayList<>();
        spiritTypes.add("불의 정령");
        spiritTypes.add("물의 정령");
        spiritTypes.add("풀의 정령");
        
        // 희귀 속성 출몰 여부 확인 (사용자가 희귀 정령을 보유하고 있는지)
        boolean hasRareSpirits = false;
        if (userId != null) {
            List<Spirit> userSpirits = spiritRepository.findByUserId(userId);
            hasRareSpirits = userSpirits.stream()
                    .anyMatch(s -> "빛의 정령".equals(s.getSpiritType()) || "어둠의 정령".equals(s.getSpiritType()));
        }
        
        // 희귀 정령 보유 시 희귀 속성 추가
        if (hasRareSpirits) {
            spiritTypes.add("빛의 정령");
            spiritTypes.add("어둠의 정령");
        }
        
        // 마지막 라운드(3라운드)는 보스 몬스터 1마리, 그 외는 일반 몬스터 (수 증가)
        int baseEnemyCount = stage.getEnemyCount() != null ? stage.getEnemyCount() : 3;
        int enemyCount = (roundNumber == 3) ? 1 : (baseEnemyCount + 2); // 일반 라운드는 기본 + 2마리
        boolean isBoss = (roundNumber == 3);
        
        for (int i = 0; i < enemyCount; i++) {
            Spirit enemy = new Spirit();
            
            // 적 타입 설정 (스테이지에 지정된 타입이 있으면 사용, 없으면 랜덤)
            String enemyType = stage.getEnemyType();
            if (enemyType == null || enemyType.isEmpty()) {
                enemyType = spiritTypes.get(random.nextInt(spiritTypes.size()));
            }
            enemy.setSpiritType(enemyType);
            
            // 적 레벨 설정 (기본 레벨 + 랜덤 변동, 보스는 더 높음)
            int levelBase = stage.getEnemyLevelBase();
            if (isBoss) {
                levelBase = levelBase + 5; // 보스는 5레벨 높음
                enemy.setName("보스 " + enemyType);
            } else {
                enemy.setName("던전 적 " + (i + 1));
            }
            
            int level = levelBase + random.nextInt(3) - 1;
            level = Math.max(1, Math.min(50, level));
            enemy.setLevel(level);
            
            // 적 능력치 설정 (초보자 친화적으로 낮춤)
            // 기본 능력치 = 레벨 * 1.5 + 랜덤 변동 (기존 레벨 * 2에서 하향)
            int baseStat = (int)(level * 1.5);
            if (isBoss) {
                baseStat = (int)(baseStat * 1.3); // 보스는 능력치 1.3배 (기존 1.5배에서 하향)
            }
            
            // 성격별 능력치 분포 (랜덤 성격 부여)
            String[] personalities = {"고집", "조심", "장난꾸러기", "온순", "용감"};
            String enemyPersonality = personalities[random.nextInt(personalities.length)];
            
            // 기본 능력치 설정
            int meleeAttack = baseStat;
            int rangedAttack = baseStat;
            int meleeDefense = baseStat;
            int rangedDefense = baseStat;
            int speed = baseStat;
            
            // 성격별 보정 적용 (포켓몬스터 스타일: +10% / -10%)
            double boostMultiplier = 1.1;
            double reduceMultiplier = 0.9;
            
            switch (enemyPersonality) {
                case "고집":
                    meleeAttack = (int)(baseStat * boostMultiplier);
                    rangedAttack = (int)(baseStat * reduceMultiplier);
                    break;
                case "조심":
                    rangedAttack = (int)(baseStat * boostMultiplier);
                    meleeAttack = (int)(baseStat * reduceMultiplier);
                    break;
                case "장난꾸러기":
                    speed = (int)(baseStat * boostMultiplier);
                    meleeDefense = (int)(baseStat * reduceMultiplier);
                    rangedDefense = (int)(baseStat * reduceMultiplier);
                    break;
                case "온순":
                    meleeDefense = (int)(baseStat * boostMultiplier);
                    rangedDefense = (int)(baseStat * boostMultiplier);
                    meleeAttack = (int)(baseStat * reduceMultiplier);
                    rangedAttack = (int)(baseStat * reduceMultiplier);
                    break;
                case "용감":
                    meleeAttack = (int)(baseStat * boostMultiplier);
                    rangedAttack = (int)(baseStat * boostMultiplier);
                    meleeDefense = (int)(baseStat * reduceMultiplier);
                    rangedDefense = (int)(baseStat * reduceMultiplier);
                    break;
            }
            
            // 랜덤 변동 추가 (±5)
            enemy.setMeleeAttack(Math.max(1, meleeAttack + random.nextInt(11) - 5));
            enemy.setRangedAttack(Math.max(1, rangedAttack + random.nextInt(11) - 5));
            enemy.setMeleeDefense(Math.max(1, meleeDefense + random.nextInt(11) - 5));
            enemy.setRangedDefense(Math.max(1, rangedDefense + random.nextInt(11) - 5));
            enemy.setSpeed(Math.max(1, speed + random.nextInt(11) - 5));
            
            // HP는 에너지로 대체 (초보자 친화적으로 낮춤)
            // HP = (레벨 * 8) + 40 (기본), 보스는 1.5배 (기존 대비 하향)
            int baseHp = 40 + (level * 8);
            enemy.setEnergy(isBoss ? (int)(baseHp * 1.5) : baseHp);
            
            enemies.add(enemy);
        }
        
        return enemies;
    }

    /**
     * 던전 클리어 처리
     */
    public DungeonResult clearStage(Long userId, Long spiritId, Integer stageNumber, Integer clearTime) {
        Optional<DungeonStage> stageOpt = dungeonStageRepository.findByStageNumber(stageNumber);
        if (stageOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 스테이지입니다.");
        }
        
        DungeonStage stage = stageOpt.get();
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        if (!spirit.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 정령만 사용할 수 있습니다.");
        }
        
        // 진행 상태 조회 또는 생성
        Optional<DungeonProgress> progressOpt = dungeonProgressRepository
                .findByUserIdAndStageNumber(userId, stageNumber);
        
        DungeonProgress progress;
        boolean isFirstClear = false;
        
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
        } else {
            progress = new DungeonProgress();
            progress.setUserId(userId);
            progress.setStageNumber(stageNumber);
            isFirstClear = true;
        }
        
        // 클리어 처리
        if (!progress.getIsCleared()) {
            progress.setIsCleared(true);
            progress.setClearedAt(LocalDateTime.now());
            isFirstClear = true;
        }
        
        // 최단 시간 업데이트
        if (progress.getBestClearTime() == null || clearTime < progress.getBestClearTime()) {
            progress.setBestClearTime(clearTime);
        }
        
        progress.setClearCount(progress.getClearCount() + 1);
        dungeonProgressRepository.save(progress);
        
        // 보상 지급
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 경험치 지급
        int expGain = stage.getExpReward();
        spirit.setExperience(spirit.getExperience() + expGain);
        
        // 레벨업 체크
        int newLevel = calculateLevel(spirit.getExperience());
        if (newLevel > spirit.getLevel() && newLevel <= 50) {
            spirit.setLevel(newLevel);
            if (newLevel == 50) {
                spirit.setMaxLevelReached(true);
            }
        }
        
        spiritRepository.save(spirit);
        
        // 골드 지급
        long newGold = user.getMoney() + stage.getGoldReward();
        user.setMoney(newGold);
        userRepository.save(user);
        
        return new DungeonResult(true, expGain, stage.getGoldReward(), isFirstClear);
    }

    /**
     * 경험치로 레벨 계산
     */
    private int calculateLevel(int experience) {
        // 간단한 레벨 계산 공식: 레벨 = sqrt(경험치 / 10)
        return Math.min(50, (int) Math.sqrt(experience / 10.0) + 1);
    }

    /**
     * 던전 결과 클래스
     */
    public static class DungeonResult {
        private final boolean success;
        private final int expGain;
        private final int goldGain;
        private final boolean isFirstClear;

        public DungeonResult(boolean success, int expGain, int goldGain, boolean isFirstClear) {
            this.success = success;
            this.expGain = expGain;
            this.goldGain = goldGain;
            this.isFirstClear = isFirstClear;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getExpGain() {
            return expGain;
        }

        public int getGoldGain() {
            return goldGain;
        }

        public boolean isFirstClear() {
            return isFirstClear;
        }
    }
}

