package com.soi.worldtree.service;

import com.soi.worldtree.dto.BlessingGrantRequest;
import com.soi.worldtree.dto.EssencePulseRequest;
import com.soi.worldtree.dto.LevelUpResult;
import com.soi.worldtree.dto.WorldTreeInfo;
import com.soi.worldtree.entity.EssencePulseLog;
import com.soi.worldtree.entity.WorldTreeLevel;
import com.soi.worldtree.entity.WorldTreeStatus;
import com.soi.worldtree.repository.EssencePulseLogRepository;
import com.soi.worldtree.repository.WorldTreeLevelRepository;
import com.soi.worldtree.repository.WorldTreeStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorldTreeService {

    private final WorldTreeStatusRepository statusRepository;
    private final WorldTreeLevelRepository levelRepository;
    private final EssencePulseLogRepository logRepository;

    @Autowired
    public WorldTreeService(WorldTreeStatusRepository statusRepository,
                           WorldTreeLevelRepository levelRepository,
                           EssencePulseLogRepository logRepository) {
        this.statusRepository = statusRepository;
        this.levelRepository = levelRepository;
        this.logRepository = logRepository;
    }

    /**
     * 정령의 축복을 추가합니다.
     * 정령이 죽었을 때 호출되는 메서드 (추후 확장: 정령의 등급 및 친밀도에 따라 달라짐)
     * 
     * @param userId 사용자 ID
     * @param request 정령의 축복 추가 요청 (정령 정보 포함 가능)
     */
    public void addBlessing(Long userId, EssencePulseRequest request) {
        // 정령 등급 및 상태에 따른 포인트 계산 (추후 확장)
        Integer amount = calculateEssenceAmount(request);
        
        // 사용자 상태 가져오기 또는 생성
        WorldTreeStatus status = statusRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 새 사용자 초기화: 튜토리얼을 위해 LV.1 → LV.2 레벨업 가능한 정령의 축복 지급
                    WorldTreeStatus newStatus = initializeNewUser(userId);
                    return statusRepository.save(newStatus);
                });

        // 정령의 축복 잔량 추가 (EXP로 자동 변환하지 않음)
        status.setAvailableEssence(status.getAvailableEssence() + amount);

        // 로그 기록 (정령 정보 포함)
        EssencePulseLog log = new EssencePulseLog(userId, amount, request.getContentSource());
        log.setSpiritId(request.getSpiritId());
        log.setSpiritGrade(request.getSpiritGrade());
        log.setSpiritStatus(request.getSpiritStatus());
        logRepository.save(log);

        statusRepository.save(status);
    }

    /**
     * 보유 중인 정령의 축복을 경험치로 부여합니다.
     * 
     * @param userId 사용자 ID
     * @param request 부여 요청 (amount가 null이면 전체 부여)
     * @return 레벨업 결과
     */
    public LevelUpResult grantBlessingToExp(Long userId, BlessingGrantRequest request) {
        // 사용자 상태 가져오기 또는 생성
        WorldTreeStatus status = statusRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 새 사용자 초기화: 튜토리얼을 위해 LV.1 → LV.2 레벨업 가능한 정령의 축복 지급
                    WorldTreeStatus newStatus = initializeNewUser(userId);
                    return statusRepository.save(newStatus);
                });

        // 부여할 축복량 결정
        long blessingToGrant;
        if (request.getAmount() == null) {
            // 전체 부여
            blessingToGrant = status.getAvailableEssence();
        } else {
            // 지정된 양만 부여 (보유량을 초과하지 않도록)
            blessingToGrant = Math.min(request.getAmount(), status.getAvailableEssence());
        }

        if (blessingToGrant <= 0) {
            // 부여할 축복이 없음
            return getCurrentLevelUpResult(status, false);
        }

        // 레벨에 따른 배율 계산 (레벨당 8% 증가)
        double multiplier = 1.0 + (status.getCurrentLevel() - 1) * 0.08;
        
        // 축복을 EXP로 변환
        int expToAdd = (int) (blessingToGrant * multiplier);
        status.setCurrentExp(status.getCurrentExp() + expToAdd);
        status.setAvailableEssence(status.getAvailableEssence() - blessingToGrant);

        // 레벨업 처리
        int previousLevel = status.getCurrentLevel();
        boolean leveledUp = processLevelUp(status);
        
        // 15레벨 달성 시 희귀 정령 선택 필요 여부 확인
        boolean rareSpiritSelectionRequired = checkLevel15Achievement(status, previousLevel);
        
        statusRepository.save(status);

        return getCurrentLevelUpResult(status, leveledUp, rareSpiritSelectionRequired);
    }

    /**
     * 현재 레벨업 결과를 반환합니다.
     */
    private LevelUpResult getCurrentLevelUpResult(WorldTreeStatus status) {
        return getCurrentLevelUpResult(status, false, false);
    }

    /**
     * 현재 레벨업 결과를 반환합니다.
     */
    private LevelUpResult getCurrentLevelUpResult(WorldTreeStatus status, boolean leveledUp) {
        return getCurrentLevelUpResult(status, leveledUp, false);
    }

    /**
     * 현재 레벨업 결과를 반환합니다.
     */
    private LevelUpResult getCurrentLevelUpResult(WorldTreeStatus status, boolean leveledUp, boolean rareSpiritSelectionRequired) {
        WorldTreeLevel currentLevel = levelRepository.findByLevel(status.getCurrentLevel())
                .orElseThrow(() -> new RuntimeException("Level not found: " + status.getCurrentLevel()));

        // 기존 사용자의 currentExp가 현재 레벨의 cumulativeExp보다 작으면 자동 보정
        if (status.getCurrentExp() < currentLevel.getCumulativeExp()) {
            status.setCurrentExp(currentLevel.getCumulativeExp());
            statusRepository.save(status);
        }

        WorldTreeLevel nextLevel = levelRepository.findByLevel(status.getCurrentLevel() + 1)
                .orElse(currentLevel);

        int requiredExp = nextLevel.getRequiredExp();
        // 현재 레벨에서의 상대 EXP 계산
        int currentExp = status.getCurrentExp() - currentLevel.getCumulativeExp();
        
        // 정령 생성 기능 언락 여부 확인 (레벨 2 이상)
        boolean spiritCreationUnlocked = status.getCurrentLevel() >= 2;

        return new LevelUpResult(
                leveledUp,
                status.getCurrentLevel(),
                currentExp,
                requiredExp,
                currentLevel.getGrowthEffectDescription(),
                spiritCreationUnlocked,
                rareSpiritSelectionRequired
        );
    }

    /**
     * 레벨업이 필요한지 확인합니다.
     */
    public boolean checkLevelUp(WorldTreeStatus status) {
        if (status.getCurrentLevel() >= 30) {
            return false; // 최대 레벨
        }

        WorldTreeLevel nextLevel = levelRepository.findByLevel(status.getCurrentLevel() + 1)
                .orElse(null);

        if (nextLevel == null) {
            return false;
        }

        return status.getCurrentExp() >= nextLevel.getCumulativeExp();
    }

    /**
     * 레벨업을 처리합니다.
     * 여러 레벨업이 가능하면 모두 처리합니다.
     * 
     * @return 레벨업이 발생했는지 여부
     */
    private boolean processLevelUp(WorldTreeStatus status) {
        boolean leveledUp = false;
        int previousLevel = status.getCurrentLevel();
        while (checkLevelUp(status)) {
            status.setCurrentLevel(status.getCurrentLevel() + 1);
            leveledUp = true;
        }
        return leveledUp;
    }

    /**
     * 15레벨 달성 여부를 확인합니다.
     * 
     * @param status 세계수 상태
     * @param previousLevel 이전 레벨
     * @return 15레벨을 방금 달성했는지 여부
     */
    private boolean checkLevel15Achievement(WorldTreeStatus status, int previousLevel) {
        return previousLevel < 15 && status.getCurrentLevel() >= 15 && !status.getRareSpiritReceived();
    }

    /**
     * 정령의 등급 및 상태에 따라 정령의 축복 포인트를 계산합니다.
     * 추후 확장: 정령 등급과 친밀도에 따라 다른 포인트를 부여할 수 있습니다.
     * 
     * @param request 정령의 축복 추가 요청
     * @return 계산된 포인트 양
     */
    private Integer calculateEssenceAmount(EssencePulseRequest request) {
        // 현재는 요청된 amount를 그대로 사용
        // 추후 확장: 정령 등급과 상태에 따라 포인트 계산
        // 예시:
        // - COMMON: 기본값
        // - RARE: 1.5배
        // - EPIC: 2배
        // - LEGENDARY: 3배
        // - 상태에 따른 보너스/페널티 적용 가능
        
        Integer baseAmount = request.getAmount();
        if (baseAmount == null) {
            return 0;
        }
        
        // 추후 확장: 정령 등급에 따른 배율 적용
        // if (request.getSpiritGrade() != null) {
        //     double multiplier = getGradeMultiplier(request.getSpiritGrade());
        //     baseAmount = (int) (baseAmount * multiplier);
        // }
        
        // 추후 확장: 정령 상태에 따른 보정 적용
        // if (request.getSpiritStatus() != null) {
        //     double statusModifier = getStatusModifier(request.getSpiritStatus());
        //     baseAmount = (int) (baseAmount * statusModifier);
        // }
        
        return baseAmount;
    }

    /**
     * 레벨에 해당하는 성장 효과를 가져옵니다.
     */
    public String getGrowthEffectForLevel(Integer level) {
        return levelRepository.findByLevel(level)
                .map(WorldTreeLevel::getGrowthEffectDescription)
                .orElse("알 수 없는 성장 효과");
    }

    /**
     * 사용자의 세계수 정보를 가져옵니다.
     */
    public WorldTreeInfo getWorldTreeInfo(Long userId) {
        WorldTreeStatus status = statusRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 새 사용자 초기화: 튜토리얼을 위해 LV.1 → LV.2 레벨업 가능한 정령의 축복 지급
                    WorldTreeStatus newStatus = initializeNewUser(userId);
                    return statusRepository.save(newStatus);
                });

        WorldTreeLevel currentLevel = levelRepository.findByLevel(status.getCurrentLevel())
                .orElseThrow(() -> new RuntimeException("Level not found: " + status.getCurrentLevel()));

        // 기존 사용자의 currentExp가 현재 레벨의 cumulativeExp보다 작으면 자동 보정
        if (status.getCurrentExp() < currentLevel.getCumulativeExp()) {
            status.setCurrentExp(currentLevel.getCumulativeExp());
            statusRepository.save(status);
        }

        WorldTreeLevel nextLevel = levelRepository.findByLevel(status.getCurrentLevel() + 1)
                .orElse(currentLevel);

        int requiredExp = nextLevel.getRequiredExp();
        // 현재 레벨에서의 상대 EXP 계산
        int currentExp = status.getCurrentExp() - currentLevel.getCumulativeExp();
        
        // 정령 생성 기능 언락 여부 확인 (레벨 2 이상)
        boolean spiritCreationUnlocked = status.getCurrentLevel() >= 2;

        return new WorldTreeInfo(
                status.getCurrentLevel(),
                currentExp,
                requiredExp,
                status.getCurrentExp(),
                currentLevel.getGrowthEffectDescription(),
                status.getAvailableEssence(),
                spiritCreationUnlocked
        );
    }

    /**
     * 새 사용자 초기화
     * 튜토리얼을 위해 LV.1 → LV.2로 레벨업할 수 있는 정령의 축복을 지급합니다.
     * 
     * @param userId 사용자 ID
     * @return 초기화된 WorldTreeStatus
     */
    private WorldTreeStatus initializeNewUser(Long userId) {
        WorldTreeStatus status = new WorldTreeStatus(userId);
        
        // 레벨 1에서 레벨 2로 가기 위해 필요한 EXP 계산
        WorldTreeLevel level1 = levelRepository.findByLevel(1)
                .orElse(null);
        WorldTreeLevel level2 = levelRepository.findByLevel(2)
                .orElse(null);
        
        // 레벨 데이터가 없으면 기본값 사용 (안전장치)
        if (level1 == null || level2 == null) {
            // 기본값: 레벨 2까지 필요한 EXP는 1000, 배율 1.0, 20% 여유 = 1200
            status.setAvailableEssence(1200L);
            // 초기 EXP를 레벨 1의 누적 EXP로 설정 (1000)
            status.setCurrentExp(1000);
            EssencePulseLog log = new EssencePulseLog(userId, 1200, "튜토리얼");
            logRepository.save(log);
            return status;
        }
        
        // 레벨 1에서 레벨 2로 가기 위해 필요한 EXP
        int requiredExpForLevel2 = level2.getCumulativeExp() - level1.getCumulativeExp();
        
        // 레벨 1의 배율 (1.0 + (1-1) * 0.08 = 1.0)
        double level1Multiplier = 1.0;
        
        // 필요한 정령의 축복 계산 (여유있게 20% 추가)
        long tutorialBlessing = (long) Math.ceil(requiredExpForLevel2 / level1Multiplier * 1.2);
        
        // 튜토리얼용 정령의 축복 지급
        status.setAvailableEssence(tutorialBlessing);
        
        // 초기 EXP를 레벨 1의 누적 EXP로 설정 (레벨 1 시작점)
        status.setCurrentExp(level1.getCumulativeExp());
        
        // 튜토리얼용 로그 기록
        EssencePulseLog log = new EssencePulseLog(userId, (int) tutorialBlessing, "튜토리얼");
        logRepository.save(log);
        
        return status;
    }

    /**
     * 정령 생성 기능이 언락되었는지 확인합니다.
     * 레벨 2 이상이면 정령 생성 기능을 사용할 수 있습니다.
     * 
     * @param userId 사용자 ID
     * @return 정령 생성 기능 언락 여부
     */
    public boolean isSpiritCreationUnlocked(Long userId) {
        WorldTreeStatus status = statusRepository.findByUserId(userId)
                .orElseGet(() -> {
                    WorldTreeStatus newStatus = initializeNewUser(userId);
                    return statusRepository.save(newStatus);
                });
        
        return status.getCurrentLevel() >= 2;
    }

    /**
     * 사용자의 세계수 상태를 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @return 세계수 상태
     */
    public WorldTreeStatus getWorldTreeStatus(Long userId) {
        return statusRepository.findByUserId(userId)
                .orElseGet(() -> {
                    WorldTreeStatus newStatus = initializeNewUser(userId);
                    return statusRepository.save(newStatus);
                });
    }
}

