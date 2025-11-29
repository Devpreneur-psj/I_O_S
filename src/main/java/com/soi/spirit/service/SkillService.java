package com.soi.spirit.service;

import com.soi.spirit.entity.Skill;
import com.soi.spirit.entity.Spirit;
import com.soi.spirit.entity.SpiritSkill;
import com.soi.spirit.repository.SkillRepository;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.spirit.repository.SpiritSkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 기술 관련 서비스
 */
@Service
@Transactional
public class SkillService {

    private final SkillRepository skillRepository;
    private final SpiritSkillRepository spiritSkillRepository;
    private final SpiritRepository spiritRepository;

    @Autowired
    public SkillService(SkillRepository skillRepository, 
                       SpiritSkillRepository spiritSkillRepository,
                       SpiritRepository spiritRepository) {
        this.skillRepository = skillRepository;
        this.spiritSkillRepository = spiritSkillRepository;
        this.spiritRepository = spiritRepository;
    }

    /**
     * 정령이 배울 수 있는 기술 목록 조회
     */
    public List<Skill> getLearnableSkills(Long spiritId) {
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        // 정령의 원소 타입 확인
        String elementType = getElementTypeFromSpiritType(spirit.getSpiritType());
        int evolutionStage = spirit.getEvolutionStage() != null ? spirit.getEvolutionStage() : 0;
        int level = spirit.getLevel() != null ? spirit.getLevel() : 1;
        
        // 이미 배운 기술 제외 (학습 완료된 기술만)
        List<Long> learnedSkillIds = spiritSkillRepository.findBySpiritId(spiritId).stream()
                .filter(ss -> {
                    // 학습 완료된 기술만 제외 (학습 중인 기술은 제외하지 않음)
                    return (ss.getIsLearning() == null || !ss.getIsLearning()) && ss.getLearnedAt() != null;
                })
                .map(SpiritSkill::getSkillId)
                .collect(Collectors.toList());
        
        // 현재 학습 중인 기술이 있는지 확인
        boolean isLearningInProgress = spiritSkillRepository.findBySpiritId(spiritId).stream()
                .anyMatch(ss -> ss.getIsLearning() != null && ss.getIsLearning());
        
        // 기술 슬롯이 가득 찼는지 확인 (학습 완료된 기술만 카운트)
        long learnedSkillCount = spiritSkillRepository.findBySpiritId(spiritId).stream()
                .filter(ss -> (ss.getIsLearning() == null || !ss.getIsLearning()) && ss.getLearnedAt() != null)
                .count();
        
        // 슬롯이 가득 찼거나 학습 중이면 빈 리스트 반환하지 않고, 모든 기술을 반환하되
        // 프론트엔드에서 버튼을 비활성화하도록 함
        // (이렇게 하면 사용자가 슬롯 상태를 확인할 수 있음)
        
        return skillRepository.findByElementTypeAndUnlockEvolutionStageLessThanEqual(elementType, evolutionStage).stream()
                .filter(skill -> {
                    // 이미 배운 기술 제외
                    if (learnedSkillIds.contains(skill.getId())) {
                        return false;
                    }
                    // 레벨 조건 확인
                    if (skill.getRequiredLevel() != null && level < skill.getRequiredLevel()) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 정령이 배운 기술 목록 조회
     */
    public List<SpiritSkill> getLearnedSkills(Long spiritId) {
        return spiritSkillRepository.findBySpiritId(spiritId);
    }

    /**
     * 정령에게 기술 학습
     * 원소와 성격에 따라 효율이 달라짐
     */
    public void learnSkill(Long spiritId, Long skillId) {
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("기술을 찾을 수 없습니다."));
        
        // 이미 배운 기술인지 확인
        if (spiritSkillRepository.existsBySpiritIdAndSkillId(spiritId, skillId)) {
            throw new IllegalArgumentException("이미 배운 기술입니다.");
        }
        
        // 기술 슬롯 제한 확인 (최대 4개)
        List<SpiritSkill> learnedSkills = spiritSkillRepository.findBySpiritId(spiritId).stream()
                .filter(ss -> ss.getIsLearning() == null || !ss.getIsLearning())
                .filter(ss -> ss.getLearnedAt() != null) // 학습 완료된 기술만
                .collect(Collectors.toList());
        
        if (learnedSkills.size() >= 4) {
            throw new IllegalArgumentException("정령은 최대 4개의 기술만 배울 수 있습니다. 기존 기술을 잊어야 합니다.");
        }
        
        // 학습 가능 여부 확인
        String elementType = getElementTypeFromSpiritType(spirit.getSpiritType());
        if (!skill.getElementType().equals(elementType)) {
            throw new IllegalArgumentException("정령의 원소와 맞지 않는 기술입니다.");
        }
        
        int evolutionStage = spirit.getEvolutionStage() != null ? spirit.getEvolutionStage() : 0;
        if (skill.getUnlockEvolutionStage() > evolutionStage) {
            throw new IllegalArgumentException("진화 단계가 부족합니다.");
        }
        
        int level = spirit.getLevel() != null ? spirit.getLevel() : 1;
        if (skill.getRequiredLevel() != null && level < skill.getRequiredLevel()) {
            throw new IllegalArgumentException("레벨이 부족합니다.");
        }
        
        // 이미 학습 중인 기술이 있는지 확인
        List<SpiritSkill> learningSkills = spiritSkillRepository.findBySpiritId(spiritId).stream()
                .filter(ss -> ss.getIsLearning() != null && ss.getIsLearning())
                .collect(Collectors.toList());
        if (!learningSkills.isEmpty()) {
            throw new IllegalArgumentException("이미 다른 기술을 학습 중입니다.");
        }
        
        // 기술 학습 시작
        SpiritSkill spiritSkill = new SpiritSkill();
        spiritSkill.setSpiritId(spiritId);
        spiritSkill.setSkillId(skillId);
        spiritSkill.setIsLearning(true);
        
        // 학습 시간 설정
        int learnTimeMinutes = skill.getLearnTimeMinutes() != null ? skill.getLearnTimeMinutes() : 30;
        LocalDateTime now = LocalDateTime.now();
        spiritSkill.setLearningStartTime(now);
        spiritSkill.setLearningCompletionTime(now.plusMinutes(learnTimeMinutes));
        
        // 성격에 따른 초기 숙련도 보정 (학습 완료 후 적용)
        int initialMastery = calculateInitialMastery(spirit.getPersonality(), skill.getSkillType());
        spiritSkill.setMasteryLevel(initialMastery);
        
        spiritSkillRepository.save(spiritSkill);
    }

    /**
     * 기술 잊기
     */
    public void forgetSkill(Long spiritId, Long skillId) {
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        SpiritSkill spiritSkill = spiritSkillRepository.findBySpiritIdAndSkillId(spiritId, skillId)
                .orElseThrow(() -> new IllegalArgumentException("배우지 않은 기술입니다."));
        
        // 학습 중인 기술은 잊을 수 없음
        if (spiritSkill.getIsLearning() != null && spiritSkill.getIsLearning()) {
            throw new IllegalArgumentException("학습 중인 기술은 잊을 수 없습니다. 먼저 학습을 취소하세요.");
        }
        
        // 기술 삭제
        spiritSkillRepository.delete(spiritSkill);
    }

    /**
     * 학습 취소 (학습 시간 초기화)
     */
    public void cancelLearning(Long spiritId, Long skillId) {
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        SpiritSkill spiritSkill = spiritSkillRepository.findBySpiritIdAndSkillId(spiritId, skillId)
                .orElseThrow(() -> new IllegalArgumentException("학습 중인 기술을 찾을 수 없습니다."));
        
        // 학습 중인 기술만 취소 가능
        if (spiritSkill.getIsLearning() == null || !spiritSkill.getIsLearning()) {
            throw new IllegalArgumentException("학습 중인 기술이 아닙니다.");
        }
        
        // 학습 정보 초기화 및 삭제
        spiritSkillRepository.delete(spiritSkill);
    }

    /**
     * 정령이 배운 기술 개수 조회
     */
    public int getLearnedSkillCount(Long spiritId) {
        List<SpiritSkill> learnedSkills = spiritSkillRepository.findBySpiritId(spiritId).stream()
                .filter(ss -> ss.getIsLearning() == null || !ss.getIsLearning())
                .filter(ss -> ss.getLearnedAt() != null)
                .collect(Collectors.toList());
        return learnedSkills.size();
    }

    /**
     * 학습 완료 처리 (스케줄러에서 호출)
     */
    public void processCompletedLearning() {
        try {
            List<SpiritSkill> learningSkills = spiritSkillRepository.findAll().stream()
                    .filter(ss -> ss.getIsLearning() != null && ss.getIsLearning())
                    .filter(ss -> ss.getLearningCompletionTime() != null)
                    .collect(Collectors.toList());
            
            if (learningSkills.isEmpty()) {
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            
            for (SpiritSkill spiritSkill : learningSkills) {
                if (now.isAfter(spiritSkill.getLearningCompletionTime()) || 
                    now.isEqual(spiritSkill.getLearningCompletionTime())) {
                    // 학습 완료
                    spiritSkill.setIsLearning(false);
                    spiritSkill.setLearnedAt(now);
                    spiritSkillRepository.save(spiritSkill);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing completed learning: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 성격에 따른 초기 숙련도 계산
     */
    private int calculateInitialMastery(String personality, String skillType) {
        // 고집: 근거리 공격 숙련도 +1
        if ("고집".equals(personality) && "MELEE_ATTACK".equals(skillType)) {
            return 2;
        }
        // 조심: 원거리 공격 숙련도 +1
        if ("조심".equals(personality) && "RANGED_ATTACK".equals(skillType)) {
            return 2;
        }
        // 기본 숙련도
        return 1;
    }

    /**
     * 정령 타입에서 원소 타입 추출
     */
    private String getElementTypeFromSpiritType(String spiritType) {
        if (spiritType.contains("불")) {
            return "FIRE";
        } else if (spiritType.contains("물")) {
            return "WATER";
        } else if (spiritType.contains("풀")) {
            return "WIND";
        } else if (spiritType.contains("빛")) {
            return "LIGHT";
        } else if (spiritType.contains("어둠")) {
            return "DARK";
        }
        return "FIRE"; // 기본값
    }

    /**
     * 기술의 실제 위력 계산 (성격, 숙련도 고려)
     */
    public int calculateSkillPower(Long spiritId, Long skillId) {
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("기술을 찾을 수 없습니다."));
        
        SpiritSkill spiritSkill = spiritSkillRepository.findBySpiritId(spiritId).stream()
                .filter(ss -> ss.getSkillId().equals(skillId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("정령이 배우지 않은 기술입니다."));
        
        int basePower = skill.getBasePower();
        int masteryLevel = spiritSkill.getMasteryLevel();
        
        // 숙련도에 따른 위력 증가 (1레벨당 10% 증가)
        double masteryMultiplier = 1.0 + (masteryLevel - 1) * 0.1;
        
        // 성격에 따른 보정
        double personalityMultiplier = getPersonalityMultiplier(spirit.getPersonality(), skill.getSkillType());
        
        return (int) (basePower * masteryMultiplier * personalityMultiplier);
    }

    /**
     * 성격에 따른 위력 배율
     */
    private double getPersonalityMultiplier(String personality, String skillType) {
        // 고집: 근거리 공격 +20%
        if ("고집".equals(personality) && "MELEE_ATTACK".equals(skillType)) {
            return 1.2;
        }
        // 조심: 원거리 공격 +20%
        if ("조심".equals(personality) && "RANGED_ATTACK".equals(skillType)) {
            return 1.2;
        }
        // 기본 배율
        return 1.0;
    }
}

