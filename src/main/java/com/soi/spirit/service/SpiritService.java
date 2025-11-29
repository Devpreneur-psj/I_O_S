package com.soi.spirit.service;

import com.soi.spirit.entity.*;
import com.soi.spirit.repository.*;
import com.soi.worldtree.repository.WorldTreeStatusRepository;
import com.soi.worldtree.entity.WorldTreeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 정령 관련 서비스
 */
@Service
@Transactional
public class SpiritService {

    private final SpiritRepository spiritRepository;
    private final SpiritTypeRepository spiritTypeRepository;
    private final WorldTreeStatusRepository worldTreeStatusRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final SpiritActionRepository spiritActionRepository;
    private final GameEventRepository gameEventRepository;

    @Autowired
    public SpiritService(SpiritRepository spiritRepository,
                        SpiritTypeRepository spiritTypeRepository,
                        WorldTreeStatusRepository worldTreeStatusRepository,
                        ItemRepository itemRepository,
                        UserItemRepository userItemRepository,
                        SpiritActionRepository spiritActionRepository,
                        GameEventRepository gameEventRepository) {
        this.spiritRepository = spiritRepository;
        this.spiritTypeRepository = spiritTypeRepository;
        this.worldTreeStatusRepository = worldTreeStatusRepository;
        this.itemRepository = itemRepository;
        this.userItemRepository = userItemRepository;
        this.spiritActionRepository = spiritActionRepository;
        this.gameEventRepository = gameEventRepository;
    }

    /**
     * 사용자가 소유할 수 있는 최대 정령 수를 반환합니다.
     * Lv.2: 1마리, Lv.4: 2마리, Lv.8: 3마리, Lv.16: 5마리
     */
    public int getMaxSpiritCount(Long userId) {
        try {
            WorldTreeStatus status = worldTreeStatusRepository.findByUserId(userId)
                    .orElse(null);
            
            if (status == null || status.getCurrentLevel() == null) {
                return 0;
            }

            int level = status.getCurrentLevel();
            if (level >= 16) {
                return 5;
            } else if (level >= 8) {
                return 3;
            } else if (level >= 4) {
                return 2;
            } else if (level >= 2) {
                return 1;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 사용자가 현재 소유한 정령 수를 반환합니다.
     * 희귀 정령은 보유 제한에 포함되지 않습니다.
     */
    public long getCurrentSpiritCount(Long userId) {
        try {
            // 희귀 정령을 제외한 정령 수 계산
            List<Spirit> allSpirits = spiritRepository.findByUserId(userId);
            if (allSpirits == null) {
                return 0L;
            }
            long count = allSpirits.stream()
                    .filter(spirit -> {
                        String type = spirit.getSpiritType();
                        return !type.equals("빛의 정령") && !type.equals("어둠의 정령");
                    })
                    .count();
            return count;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 정령 생성 가능 여부를 확인합니다.
     */
    public boolean canCreateSpirit(Long userId, String spiritTypeCode) {
        // 정령 타입 확인
        Optional<SpiritType> spiritTypeOpt = spiritTypeRepository.findByTypeCode(spiritTypeCode);
        if (spiritTypeOpt.isEmpty()) {
            return false;
        }

        SpiritType spiritType = spiritTypeOpt.get();
        
        // 희귀 정령 해금 레벨 확인
        if (spiritType.getIsRare()) {
            WorldTreeStatus status = worldTreeStatusRepository.findByUserId(userId)
                    .orElse(null);
            if (status == null || status.getCurrentLevel() < spiritType.getUnlockLevel()) {
                return false;
            }
            
            // 희귀 정령은 15레벨 달성 시 한 번만 생성 가능
            if (!status.getRareSpiritReceived()) {
                return false; // 아직 희귀 정령을 받지 않았으면 일반 경로로 생성 불가
            }
            
            // 이미 희귀 정령을 받았는지 확인 (같은 타입의 희귀 정령이 이미 있는지)
            List<Spirit> userSpirits = spiritRepository.findByUserId(userId);
            boolean hasRareSpirit = userSpirits.stream()
                    .anyMatch(s -> s.getSpiritType().equals(spiritType.getTypeName()));
            if (hasRareSpirit) {
                return false; // 이미 희귀 정령을 소유하고 있음
            }
            
            // 희귀 정령은 보유 제한에 포함되지 않으므로 여기서 반환
            return true;
        }
        
        // 일반 정령은 최대 소유 수 확인
        int maxCount = getMaxSpiritCount(userId);
        long currentCount = getCurrentSpiritCount(userId);
        
        if (currentCount >= maxCount) {
            return false;
        }

        return true;
    }

    /**
     * 15레벨 달성 보상으로 희귀 정령을 생성합니다.
     * 이 메서드는 희귀 정령 선택 API에서만 호출됩니다.
     */
    public Spirit createRareSpiritAsReward(Long userId, String spiritTypeCode, String name) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다.");
        }
        
        if (spiritTypeCode == null || spiritTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("정령 타입을 선택해주세요.");
        }

        // 정령 타입 확인
        SpiritType spiritType = spiritTypeRepository.findByTypeCode(spiritTypeCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 정령 타입입니다."));

        // 희귀 정령인지 확인
        if (!spiritType.getIsRare()) {
            throw new IllegalArgumentException("희귀 정령만 선택할 수 있습니다.");
        }

        // 15레벨 달성 여부 확인
        WorldTreeStatus status = worldTreeStatusRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("세계수 정보를 찾을 수 없습니다."));
        
        if (status.getCurrentLevel() < 15) {
            throw new IllegalArgumentException("15레벨 이상이어야 희귀 정령을 받을 수 있습니다.");
        }

        if (status.getRareSpiritReceived()) {
            throw new IllegalArgumentException("이미 희귀 정령을 받았습니다.");
        }

        // 이미 같은 타입의 희귀 정령이 있는지 확인
        List<Spirit> userSpirits = spiritRepository.findByUserId(userId);
        boolean hasRareSpirit = userSpirits.stream()
                .anyMatch(s -> s.getSpiritType().equals(spiritType.getTypeName()));
        if (hasRareSpirit) {
            throw new IllegalArgumentException("이미 이 희귀 정령을 소유하고 있습니다.");
        }

        // 정령 생성
        Spirit spirit = new Spirit();
        spirit.setUserId(userId);
        spirit.setSpiritType(spiritType.getTypeName());
        spirit.setName(name != null && !name.trim().isEmpty() ? name : spiritType.getBaseStageName());
        spirit.setEvolutionStage(0);
        spirit.setLevel(1);
        spirit.setExperience(0);
        spirit.setIntimacy(1);
        
        // 성격 랜덤 설정
        String[] personalities = {"고집", "조심", "장난꾸러기", "온순", "용감"};
        spirit.setPersonality(personalities[(int)(Math.random() * personalities.length)]);
        
        // 기본 능력치 설정
        applyPersonalityStats(spirit, spirit.getPersonality());
        
        // 세계수 레벨에 따른 능력치 보너스 적용
        applyWorldTreeLevelBonus(spirit, userId);
        
        // 기본 상태 설정
        spirit.setHappiness(50);
        spirit.setMood("보통");
        spirit.setHunger(50);
        spirit.setEnergy(100);
        spirit.setHealthStatus("건강");
        spirit.setAge(0);

        // 희귀 정령 수령 플래그 설정
        status.setRareSpiritReceived(true);
        worldTreeStatusRepository.save(status);

        // 희귀 정령도 다른 정령들과 같이 1단계(기본 단계)부터 시작
        return spiritRepository.save(spirit);
    }

    /**
     * 정령을 생성합니다.
     */
    public Spirit createSpirit(Long userId, String spiritTypeCode, String name) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다.");
        }
        
        if (spiritTypeCode == null || spiritTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("정령 타입을 선택해주세요.");
        }
        
        if (!canCreateSpirit(userId, spiritTypeCode)) {
            throw new IllegalArgumentException("정령을 생성할 수 없습니다. (소유 제한 또는 해금 레벨 미달)");
        }

        SpiritType spiritType = spiritTypeRepository.findByTypeCode(spiritTypeCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 정령 타입입니다."));

        if (spiritType.getTypeName() == null || spiritType.getBaseStageName() == null) {
            throw new IllegalArgumentException("정령 타입 정보가 올바르지 않습니다.");
        }

        Spirit spirit = new Spirit();
        spirit.setUserId(userId);
        spirit.setSpiritType(spiritType.getTypeName());
        spirit.setName(name != null && !name.trim().isEmpty() ? name : spiritType.getBaseStageName());
        spirit.setEvolutionStage(0);
        spirit.setLevel(1);
        spirit.setExperience(0);
        spirit.setIntimacy(1);
        
        // 성격 랜덤 설정 (추후 확장 가능)
        String[] personalities = {"고집", "조심", "장난꾸러기", "온순", "용감"};
        spirit.setPersonality(personalities[(int)(Math.random() * personalities.length)]);
        
        // 기본 능력치 설정 (성격에 따라 추후 조정)
        // 성격에 따른 능력치 초기화
        applyPersonalityStats(spirit, spirit.getPersonality());
        
        // 세계수 레벨에 따른 능력치 보너스 적용
        applyWorldTreeLevelBonus(spirit, userId);
        
        // 기본 상태 설정
        spirit.setHappiness(50);
        spirit.setMood("보통");
        spirit.setHunger(50);
        spirit.setEnergy(100);
        spirit.setHealthStatus("건강");
        spirit.setAge(0);

        return spiritRepository.save(spirit);
    }
    
    /**
     * 세계수 레벨에 따른 능력치 보너스를 적용합니다.
     * 레벨별 보너스:
     * - Lv.3-6: 모든 능력치 +5
     * - Lv.7-12: 모든 능력치 +10
     * - Lv.13-30: 모든 능력치 +15
     * 
     * @param spirit 정령
     * @param userId 사용자 ID
     */
    private void applyWorldTreeLevelBonus(Spirit spirit, Long userId) {
        try {
            WorldTreeStatus status = worldTreeStatusRepository.findByUserId(userId).orElse(null);
            if (status == null || status.getCurrentLevel() == null) {
                return; // 보너스 없음
            }
            
            int worldTreeLevel = status.getCurrentLevel();
            int bonus = 0;
            
            // 레벨별 보너스 계산 (누적 방식)
            if (worldTreeLevel >= 3) {
                // Lv.3-6: +5
                if (worldTreeLevel <= 6) {
                    bonus = 5;
                }
                // Lv.7-12: +10
                else if (worldTreeLevel <= 12) {
                    bonus = 10;
                }
                // Lv.13-30: +15
                else {
                    bonus = 15;
                }
            }
            
            // 모든 능력치에 보너스 적용
            if (bonus > 0) {
                spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + bonus));
                spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + bonus));
                spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + bonus));
                spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + bonus));
                spirit.setSpeed(Math.min(100, spirit.getSpeed() + bonus));
            }
        } catch (Exception e) {
            // 보너스 적용 실패 시 무시 (기본 능력치 유지)
            System.err.println("세계수 레벨 보너스 적용 실패: " + e.getMessage());
        }
    }

    /**
     * 성격에 따른 능력치를 적용합니다. (포켓몬스터 스타일: 특정 능력치 +10%, 다른 능력치 -10%)
     * 고집 = 근거리공격 +10%, 원거리공격 -10%
     * 조심 = 원거리공격 +10%, 근거리공격 -10%
     * 장난꾸러기 = 스피드 +10%, 방어력 -10%
     * 온순 = 방어력 +10%, 공격력 -10%
     * 용감 = 공격력 +10%, 방어력 -10%
     */
    private void applyPersonalityStats(Spirit spirit, String personality) {
        int baseStat = 50;
        
        // 기본 능력치 설정
        spirit.setRangedAttack(baseStat);
        spirit.setMeleeAttack(baseStat);
        spirit.setRangedDefense(baseStat);
        spirit.setMeleeDefense(baseStat);
        spirit.setSpeed(baseStat);
        
        // 성격별 보정 (포켓몬스터 스타일: +10% / -10%)
        double boostMultiplier = 1.1; // +10%
        double reduceMultiplier = 0.9; // -10%
        
        switch (personality) {
            case "고집":
                // 근거리공격 +10%, 원거리공격 -10%
                spirit.setMeleeAttack((int)(baseStat * boostMultiplier));
                spirit.setRangedAttack((int)(baseStat * reduceMultiplier));
                break;
            case "조심":
                // 원거리공격 +10%, 근거리공격 -10%
                spirit.setRangedAttack((int)(baseStat * boostMultiplier));
                spirit.setMeleeAttack((int)(baseStat * reduceMultiplier));
                break;
            case "장난꾸러기":
                // 스피드 +10%, 방어력 -10%
                spirit.setSpeed((int)(baseStat * boostMultiplier));
                spirit.setRangedDefense((int)(baseStat * reduceMultiplier));
                spirit.setMeleeDefense((int)(baseStat * reduceMultiplier));
                break;
            case "온순":
                // 방어력 +10%, 공격력 -10%
                spirit.setRangedDefense((int)(baseStat * boostMultiplier));
                spirit.setMeleeDefense((int)(baseStat * boostMultiplier));
                spirit.setRangedAttack((int)(baseStat * reduceMultiplier));
                spirit.setMeleeAttack((int)(baseStat * reduceMultiplier));
                break;
            case "용감":
                // 공격력 +10%, 방어력 -10%
                spirit.setRangedAttack((int)(baseStat * boostMultiplier));
                spirit.setMeleeAttack((int)(baseStat * boostMultiplier));
                spirit.setRangedDefense((int)(baseStat * reduceMultiplier));
                spirit.setMeleeDefense((int)(baseStat * reduceMultiplier));
                break;
            default:
                // 기본값 (변경 없음)
                break;
        }
    }

    /**
     * 정령에게 경험치를 추가하고 레벨업을 처리합니다.
     * 포켓몬스터 스타일: 레벨업 시 성격에 따라 능력치가 성장합니다.
     * 
     * @param spiritId 정령 ID
     * @param userId 사용자 ID
     * @param exp 경험치
     * @return 레벨업 여부
     */
    public boolean addExperience(Long spiritId, Long userId, int exp) {
        Spirit spirit = getSpirit(spiritId, userId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        if (spirit.getLevel() >= 30) { // 최대 레벨 30으로 변경
            // 최대 레벨 도달
            if (!spirit.getMaxLevelReached()) {
                spirit.setMaxLevelReached(true);
                spirit.setLifespanCountdown(java.time.LocalDateTime.now());
            }
            return false;
        }
        
        // 경험치 추가
        spirit.setExperience(spirit.getExperience() + exp);
        
        // 레벨업 체크 및 처리
        boolean leveledUp = processSpiritLevelUp(spirit);
        
        spiritRepository.save(spirit);
        return leveledUp;
    }

    /**
     * 정령의 레벨업을 처리합니다.
     * 포켓몬스터 스타일: 레벨업 시 성격에 따라 능력치가 성장합니다.
     * 
     * @param spirit 정령
     * @return 레벨업 여부
     */
    private boolean processSpiritLevelUp(Spirit spirit) {
        boolean leveledUp = false;
        
        // Medium Slow 타입 경험치 테이블 (레벨당 필요 경험치)
        while (spirit.getLevel() < 30 && getRequiredExpForLevel(spirit.getLevel() + 1) <= spirit.getExperience()) {
            spirit.setLevel(spirit.getLevel() + 1);
            leveledUp = true;
            
            // 레벨업 시 성격에 따라 능력치 성장
            applyPersonalityGrowth(spirit);
            
            // 30레벨 달성 시 수명 카운트다운 시작
            if (spirit.getLevel() >= 30) {
                spirit.setMaxLevelReached(true);
                spirit.setLifespanCountdown(java.time.LocalDateTime.now());
            }
        }
        
        return leveledUp;
    }

    /**
     * 레벨에 필요한 경험치를 반환합니다.
     * Medium Slow 타입: 초반 완만, 후반 급격한 성장
     */
    private int getRequiredExpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        // Medium Slow 타입: level³ * 0.8 + level * 50
        double baseExp = Math.pow(level, 3) * 0.8;
        double linearExp = level * 50;
        return (int) Math.round(baseExp + linearExp);
    }

    /**
     * 레벨업 시 성격에 따라 능력치를 성장시킵니다.
     * 포켓몬스터 스타일: 성격에 따라 특정 능력치가 더 많이 성장합니다.
     * 
     * 성장치:
     * - 고집: 근거리공격 +3, 원거리공격 +1, 방어력 +2, 스피드 +2
     * - 조심: 원거리공격 +3, 근거리공격 +1, 방어력 +2, 스피드 +2
     * - 장난꾸러기: 스피드 +3, 공격력 +2, 방어력 +1, 원거리공격 +2
     * - 온순: 방어력 +3, 공격력 +1, 스피드 +2, 원거리공격 +2
     * - 용감: 근거리공격 +2, 원거리공격 +2, 스피드 +2, 방어력 +1
     */
    private void applyPersonalityGrowth(Spirit spirit) {
        String personality = spirit.getPersonality();
        
        switch (personality) {
            case "고집":
                // 근거리공격 중심 성장
                spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 3));
                spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 1));
                spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 2));
                spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 2));
                spirit.setSpeed(Math.min(100, spirit.getSpeed() + 2));
                break;
            case "조심":
                // 원거리공격 중심 성장
                spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 3));
                spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 1));
                spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 2));
                spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 2));
                spirit.setSpeed(Math.min(100, spirit.getSpeed() + 2));
                break;
            case "장난꾸러기":
                // 스피드 중심 성장
                spirit.setSpeed(Math.min(100, spirit.getSpeed() + 3));
                spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 2));
                spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 2));
                spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 1));
                spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 1));
                break;
            case "온순":
                // 방어력 중심 성장
                spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 3));
                spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 3));
                spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 1));
                spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 2));
                spirit.setSpeed(Math.min(100, spirit.getSpeed() + 2));
                break;
            case "용감":
                // 공격력 균형 성장
                spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 2));
                spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 2));
                spirit.setSpeed(Math.min(100, spirit.getSpeed() + 2));
                spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 1));
                spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 1));
                break;
            default:
                // 기본 성장 (균형)
                spirit.setMeleeAttack(Math.min(100, spirit.getMeleeAttack() + 2));
                spirit.setRangedAttack(Math.min(100, spirit.getRangedAttack() + 2));
                spirit.setRangedDefense(Math.min(100, spirit.getRangedDefense() + 2));
                spirit.setMeleeDefense(Math.min(100, spirit.getMeleeDefense() + 2));
                spirit.setSpeed(Math.min(100, spirit.getSpeed() + 2));
                break;
        }
    }

    /**
     * 사용자의 모든 정령을 조회합니다.
     */
    public List<Spirit> getUserSpirits(Long userId) {
        try {
            if (userId == null) {
                return List.of();
            }
            List<Spirit> spirits = spiritRepository.findByUserId(userId);
            if (spirits == null) {
                return List.of();
            }
            
            // 기존 정령들의 null 필드를 기본값으로 채워주기
            for (Spirit spirit : spirits) {
                initializeSpiritDefaults(spirit);
            }
            
            return spirits;
        } catch (Exception e) {
            System.err.println("Error fetching user spirits: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * 정령의 null 필드를 기본값으로 초기화합니다.
     */
    private void initializeSpiritDefaults(Spirit spirit) {
        // getter가 기본값을 반환하므로, 실제 필드가 null인지 확인하기 위해 리플렉션 사용
        try {
            java.lang.reflect.Field healthStatusField = Spirit.class.getDeclaredField("healthStatus");
            healthStatusField.setAccessible(true);
            if (healthStatusField.get(spirit) == null) {
                spirit.setHealthStatus("건강");
            }
            
            java.lang.reflect.Field happinessField = Spirit.class.getDeclaredField("happiness");
            happinessField.setAccessible(true);
            if (happinessField.get(spirit) == null) {
                spirit.setHappiness(50);
            }
            
            java.lang.reflect.Field moodField = Spirit.class.getDeclaredField("mood");
            moodField.setAccessible(true);
            if (moodField.get(spirit) == null) {
                spirit.setMood("보통");
            }
            
            java.lang.reflect.Field hungerField = Spirit.class.getDeclaredField("hunger");
            hungerField.setAccessible(true);
            if (hungerField.get(spirit) == null) {
                spirit.setHunger(50);
            }
            
            java.lang.reflect.Field energyField = Spirit.class.getDeclaredField("energy");
            energyField.setAccessible(true);
            if (energyField.get(spirit) == null) {
                spirit.setEnergy(100);
            }
            
            java.lang.reflect.Field ageField = Spirit.class.getDeclaredField("age");
            ageField.setAccessible(true);
            if (ageField.get(spirit) == null) {
                spirit.setAge(0);
            }
            
            java.lang.reflect.Field isRetiredField = Spirit.class.getDeclaredField("isRetired");
            isRetiredField.setAccessible(true);
            if (isRetiredField.get(spirit) == null) {
                spirit.setIsRetired(false);
            }
        } catch (Exception e) {
            // 리플렉션 실패 시 안전하게 기본값 설정
            System.err.println("Warning: Could not initialize spirit defaults using reflection: " + e.getMessage());
        }
    }

    /**
     * 정령 정보를 조회합니다.
     */
    public Optional<Spirit> getSpirit(Long spiritId, Long userId) {
        Optional<Spirit> spirit = spiritRepository.findById(spiritId);
        if (spirit.isPresent() && !spirit.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        return spirit;
    }

    /**
     * 사용자가 생성 가능한 정령 타입 목록을 반환합니다.
     */
    public List<SpiritType> getAvailableSpiritTypes(Long userId, Integer userLevel) {
        try {
            // 기본 정령은 레벨 2 이상, 희귀 정령은 레벨 15 이상
            if (userLevel == null || userLevel < 2) {
                return List.of();
            }
            
            // Repository에서 조회
            List<SpiritType> types = spiritTypeRepository.findByUnlockLevelLessThanEqual(userLevel);
            
            // 데이터가 없으면 빈 리스트 반환
            if (types == null || types.isEmpty()) {
                return List.of();
            }
            
            return types;
        } catch (Exception e) {
            // 데이터베이스 오류 시 빈 리스트 반환
            System.err.println("Error getting available spirit types: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * 정령에게 아이템을 사용합니다.
     * @deprecated ItemService.useItemOnSpirit()을 사용하세요.
     */
    @Deprecated
    public void useItemOnSpirit(Long userId, Long spiritId, Long itemId) {
        throw new UnsupportedOperationException("ItemService.useItemOnSpirit()을 사용하세요.");
    }

    /**
     * 정령을 훈련시킵니다.
     * @deprecated TrainingService.trainSpirit()을 사용하세요.
     */
    @Deprecated
    public void trainSpirit(Long userId, Long spiritId, String trainingType) {
        throw new UnsupportedOperationException("TrainingService.trainSpirit()을 사용하세요.");
    }

    /**
     * 정령의 최근 행동 로그를 조회합니다.
     */
    public List<SpiritAction> getSpiritActions(Long spiritId, Long userId) {
        try {
            // 정령이 사용자의 것인지 확인
            Optional<Spirit> spirit = getSpirit(spiritId, userId);
            if (spirit.isEmpty()) {
                return List.of();
            }
            
            return spiritActionRepository.findBySpiritIdOrderByCreatedAtDesc(spiritId);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 사용자의 활성 이벤트를 조회합니다.
     */
    public List<GameEvent> getActiveEvents(Long userId) {
        try {
            return gameEventRepository.findActiveEventsByUserId(userId);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 희귀 정령 타입 목록을 반환합니다.
     */
    /**
     * 희귀 정령 타입 목록을 반환합니다.
     */
    public List<SpiritType> getRareSpiritTypes() {
        try {
            List<SpiritType> types = spiritTypeRepository.findByIsRareTrue();
            return types != null ? types : List.of();
        } catch (Exception e) {
            System.err.println("Error getting rare spirit types: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    // 희귀 정령 고치 돌봐주기 게이지 최대값 (밸런스 조정 시 이 값만 변경하면 됨)
    private static final int MAX_COCOON_CARE_GAUGE = 1000; // TODO: 밸런스 조정 시 수정
    private static final int MAX_DAILY_CARE_COUNT = 5; // 하루 최대 돌봐주기 횟수
    private static final int CARE_GAUGE_PER_ACTION = 10; // 돌봐주기 한 번당 게이지 증가량

    /**
     * 고치 상태의 정령을 돌봐줍니다.
     * 하루에 최대 5번까지 가능하며, 한 번에 게이지 10씩 증가합니다.
     * 게이지가 최대치에 도달하면 정령 연구소에서 2차 진화를 시작할 수 있습니다.
     * 
     * @param userId 사용자 ID
     * @param spiritId 정령 ID
     * @return 돌봐주기 결과 (게이지 정보 포함)
     */
    public java.util.Map<String, Object> careForCocoon(Long userId, Long spiritId) {
        // 정령 조회 및 소유 확인
        Spirit spirit = getSpirit(spiritId, userId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));

        // 고치 상태 확인 (희귀 정령의 1차 진화 단계)
        boolean isRare = spirit.getSpiritType().equals("빛의 정령") || spirit.getSpiritType().equals("어둠의 정령");
        if (!isRare || spirit.getEvolutionStage() != 1) {
            throw new IllegalArgumentException("고치 상태의 희귀 정령만 돌볼 수 있습니다.");
        }

        // 이미 진화 진행 중이면 돌볼 수 없음
        if (spirit.getEvolutionInProgress() != null && spirit.getEvolutionInProgress()) {
            throw new IllegalArgumentException("이미 진화가 진행 중인 정령은 돌볼 수 없습니다.");
        }

        // 오늘 날짜 확인
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate lastCareDate = spirit.getLastCareDate();
        int dailyCareCount = spirit.getDailyCareCount() != null ? spirit.getDailyCareCount() : 0;

        // 날짜가 바뀌었으면 돌봐주기 횟수 초기화
        if (lastCareDate == null || !lastCareDate.equals(today)) {
            dailyCareCount = 0;
            spirit.setLastCareDate(today);
        }

        // 하루 최대 횟수 확인
        if (dailyCareCount >= MAX_DAILY_CARE_COUNT) {
            throw new IllegalArgumentException("하루에 최대 " + MAX_DAILY_CARE_COUNT + "번까지만 돌볼 수 있습니다. (오늘: " + dailyCareCount + "/" + MAX_DAILY_CARE_COUNT + ")");
        }

        // 돌봐주기 게이지 증가 (한 번에 10씩 증가, 최대값까지)
        int currentGauge = spirit.getCocoonCareGauge() != null ? spirit.getCocoonCareGauge() : 0;
        int newGauge = Math.min(MAX_COCOON_CARE_GAUGE, currentGauge + CARE_GAUGE_PER_ACTION);
        spirit.setCocoonCareGauge(newGauge);
        
        // 오늘 돌봐주기 횟수 증가
        spirit.setDailyCareCount(dailyCareCount + 1);
        spirit.setLastCareDate(today);
        
        // 친밀도 증가
        spirit.setIntimacy(Math.min(10, spirit.getIntimacy() + 1));
        
        // 행복도 증가
        spirit.setHappiness(Math.min(100, spirit.getHappiness() + 10));
        
        spiritRepository.save(spirit);
        
        boolean isMaxGauge = newGauge >= MAX_COCOON_CARE_GAUGE;
        int remainingCareCount = MAX_DAILY_CARE_COUNT - (dailyCareCount + 1);
        
        return java.util.Map.of(
            "careGauge", newGauge,
            "maxGauge", MAX_COCOON_CARE_GAUGE,
            "isMaxGauge", isMaxGauge,
            "dailyCareCount", dailyCareCount + 1,
            "maxDailyCareCount", MAX_DAILY_CARE_COUNT,
            "remainingCareCount", remainingCareCount,
            "intimacy", spirit.getIntimacy(),
            "happiness", spirit.getHappiness(),
            "message", isMaxGauge ? "돌봐주기 게이지가 최대치에 도달했습니다! 정령 연구소에서 진화를 시작할 수 있습니다." : "돌봐주기 게이지가 " + CARE_GAUGE_PER_ACTION + " 증가했습니다. (오늘: " + (dailyCareCount + 1) + "/" + MAX_DAILY_CARE_COUNT + ")"
        );
    }

    /**
     * 진화 가능한 정령 목록 조회
     * 1차 진화: 레벨 15 이상
     * 2차 진화: 레벨 30 이상 (일반 정령) 또는 돌봐주기 게이지 최대치 (희귀 정령 고치 상태)
     */
    public List<Spirit> getEvolvableSpirits(Long userId) {
        try {
            List<Spirit> allSpirits = spiritRepository.findByUserId(userId);
            if (allSpirits == null) {
                return List.of();
            }
            
            return allSpirits.stream()
                    .filter(spirit -> {
                        // 진화 진행 중이 아니어야 함
                        if (spirit.getEvolutionInProgress() != null && spirit.getEvolutionInProgress()) {
                            return false;
                        }
                        
                        int currentStage = spirit.getEvolutionStage() != null ? spirit.getEvolutionStage() : 0;
                        int level = spirit.getLevel() != null ? spirit.getLevel() : 1;
                        boolean isRare = spirit.getSpiritType().equals("빛의 정령") || spirit.getSpiritType().equals("어둠의 정령");
                        
                        // 1차 진화 가능: 레벨 15 이상, 현재 0단계
                        if (currentStage == 0 && level >= 15) {
                            return true;
                        }
                        
                        // 2차 진화 가능: 레벨 30 이상, 현재 1단계
                        if (currentStage == 1 && level >= 30) {
                            // 희귀 정령의 경우 돌봐주기 게이지가 최대치에 도달해야 함
                            if (isRare) {
                                int careGauge = spirit.getCocoonCareGauge() != null ? spirit.getCocoonCareGauge() : 0;
                                return careGauge >= MAX_COCOON_CARE_GAUGE;
                            }
                            // 일반 정령은 레벨만 확인
                            return true;
                        }
                        
                        return false;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 진화 진행 중인 정령 목록 조회
     */
    public List<Spirit> getEvolvingSpirits(Long userId) {
        try {
            List<Spirit> allSpirits = spiritRepository.findByUserId(userId);
            if (allSpirits == null) {
                return List.of();
            }
            
            return allSpirits.stream()
                    .filter(spirit -> {
                        return spirit.getEvolutionInProgress() != null && spirit.getEvolutionInProgress();
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 정령 진화 시작
     * 1차 진화: 레벨 15, 1시간 소요
     * 2차 진화: 레벨 30, 24시간 소요
     */
    public void startEvolution(Long userId, Long spiritId) {
        Spirit spirit = getSpirit(spiritId, userId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        // 이미 진화 진행 중인지 확인
        if (spirit.getEvolutionInProgress() != null && spirit.getEvolutionInProgress()) {
            throw new IllegalArgumentException("이미 진화가 진행 중입니다.");
        }
        
        int currentStage = spirit.getEvolutionStage() != null ? spirit.getEvolutionStage() : 0;
        int level = spirit.getLevel() != null ? spirit.getLevel() : 1;
        
        // 진화 가능 여부 확인
        if (currentStage == 0 && level < 15) {
            throw new IllegalArgumentException("1차 진화를 위해서는 레벨 15가 필요합니다.");
        }
        
        if (currentStage == 1 && level < 30) {
            throw new IllegalArgumentException("2차 진화를 위해서는 레벨 30이 필요합니다.");
        }
        
        // 희귀 정령의 고치 상태는 별도 처리
        boolean isRare = spirit.getSpiritType().equals("빛의 정령") || spirit.getSpiritType().equals("어둠의 정령");
        
        if (isRare && currentStage == 0) {
            // 희귀 정령의 1차 진화는 자동으로 고치 상태가 됨
            spirit.setEvolutionStage(1);
            spirit.setEvolutionInProgress(true);
            spirit.setEvolutionStartTime(java.time.LocalDateTime.now());
            spirit.setEvolutionTargetStage(2); // 고치에서 2단계로
            spiritRepository.save(spirit);
            return;
        }
        
        // 희귀 정령의 2차 진화 (고치 상태에서 3단계로)
        if (isRare && currentStage == 1) {
            // 돌봐주기 게이지가 최대치에 도달했는지 확인
            int careGauge = spirit.getCocoonCareGauge() != null ? spirit.getCocoonCareGauge() : 0;
            if (careGauge < MAX_COCOON_CARE_GAUGE) {
                throw new IllegalArgumentException("돌봐주기 게이지가 최대치에 도달해야 2차 진화를 시작할 수 있습니다. (현재: " + careGauge + "/" + MAX_COCOON_CARE_GAUGE + ")");
            }
            
            // 2차 진화 시작 (24시간 소요)
            spirit.setEvolutionInProgress(true);
            spirit.setEvolutionStartTime(java.time.LocalDateTime.now());
            spirit.setEvolutionTargetStage(2); // 고치(1단계)에서 3단계(2)로
            spirit.setCocoonCareGauge(0); // 게이지 초기화
            spiritRepository.save(spirit);
            return;
        }
        
        // 일반 진화 처리
        int targetStage = currentStage + 1;
        int hoursRequired = targetStage == 1 ? 1 : 24; // 1차 진화: 1시간, 2차 진화: 24시간
        
        spirit.setEvolutionInProgress(true);
        spirit.setEvolutionStartTime(java.time.LocalDateTime.now());
        spirit.setEvolutionTargetStage(targetStage);
        
        spiritRepository.save(spirit);
    }

    /**
     * 진화 완료 처리 (스케줄러에서 호출)
     */
    public void processCompletedEvolutions() {
        try {
            List<Spirit> evolvingSpirits = spiritRepository.findByEvolutionInProgressTrue();
            if (evolvingSpirits == null || evolvingSpirits.isEmpty()) {
                return;
            }
            
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            
            for (Spirit spirit : evolvingSpirits) {
                if (spirit.getEvolutionStartTime() == null || spirit.getEvolutionTargetStage() == null) {
                    continue;
                }
                
                int hoursRequired = spirit.getEvolutionTargetStage() == 1 ? 1 : 24; // 1차 진화: 1시간, 2차 진화: 24시간
                java.time.LocalDateTime endTime = spirit.getEvolutionStartTime().plusHours(hoursRequired);
                
                if (now.isAfter(endTime) || now.isEqual(endTime)) {
                    // 진화 완료
                    spirit.setEvolutionStage(spirit.getEvolutionTargetStage());
                    spirit.setEvolutionInProgress(false);
                    spirit.setEvolutionStartTime(null);
                    spirit.setEvolutionTargetStage(null);
                    spiritRepository.save(spirit);
                }
            }
        } catch (Exception e) {
            // 로그만 남기고 계속 진행
            System.err.println("Error processing completed evolutions: " + e.getMessage());
        }
    }
}

