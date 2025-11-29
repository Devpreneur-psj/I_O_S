package com.soi.config;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.entity.SpiritType;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.spirit.repository.SpiritTypeRepository;
import com.soi.user.User;
import com.soi.user.UserRepository;
import com.soi.worldtree.entity.EssencePulseLog;
import com.soi.worldtree.entity.WorldTreeLevel;
import com.soi.worldtree.entity.WorldTreeStatus;
import com.soi.worldtree.repository.EssencePulseLogRepository;
import com.soi.worldtree.repository.WorldTreeLevelRepository;
import com.soi.worldtree.repository.WorldTreeStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 마스터 계정 초기화
 * 테스트 및 시험을 위한 마스터 계정을 자동 생성합니다.
 */
@Configuration
public class MasterAccountInitializer {

    private static final String MASTER_USERNAME = "master";
    private static final String MASTER_PASSWORD = "master123";
    private static final String MASTER_NICKNAME = "마스터";
    private static final String MASTER_EMAIL = "master@soi.test";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WorldTreeStatusRepository worldTreeStatusRepository;

    @Autowired
    private WorldTreeLevelRepository worldTreeLevelRepository;

    @Autowired
    private EssencePulseLogRepository essencePulseLogRepository;

    @Autowired
    private SpiritRepository spiritRepository;

    @Autowired
    private SpiritTypeRepository spiritTypeRepository;

    @Bean
    @Transactional
    public ApplicationRunner initializeMasterAccount() {
        return args -> {
            // 마스터 계정이 이미 존재하는지 확인
            Optional<User> existingMaster = userRepository.findByUsername(MASTER_USERNAME);
            boolean isNewAccount = existingMaster.isEmpty();
            User masterUser;
            
            if (existingMaster.isPresent()) {
                System.out.println("마스터 계정이 이미 존재합니다. 데이터를 업데이트합니다: " + MASTER_USERNAME);
                masterUser = existingMaster.get();
                // 비밀번호 업데이트 (필요시)
                masterUser.setPassword(passwordEncoder.encode(MASTER_PASSWORD));
                masterUser.setNickname(MASTER_NICKNAME);
                masterUser.setEmail(MASTER_EMAIL);
            } else {
                // 마스터 계정 생성
                masterUser = new User();
                masterUser.setUsername(MASTER_USERNAME);
                masterUser.setPassword(passwordEncoder.encode(MASTER_PASSWORD));
                masterUser.setNickname(MASTER_NICKNAME);
                masterUser.setEmail(MASTER_EMAIL);
                System.out.println("마스터 계정이 생성되었습니다: " + MASTER_USERNAME + " / " + MASTER_PASSWORD);
            }
            
            // 마스터 계정에 많은 돈 지급
            masterUser.setMoney(1000000L);
            masterUser = userRepository.save(masterUser);

            // 마스터 계정의 세계수 상태 초기화 (최대 레벨 30)
            Optional<WorldTreeStatus> existingStatus = worldTreeStatusRepository.findByUserId(masterUser.getId());
            WorldTreeStatus worldTreeStatus;
            
            if (existingStatus.isPresent()) {
                worldTreeStatus = existingStatus.get();
            } else {
                worldTreeStatus = new WorldTreeStatus(masterUser.getId());
            }
            
            worldTreeStatus.setCurrentLevel(30);
            
            // 레벨 30의 누적 경험치 설정
            WorldTreeLevel level30 = worldTreeLevelRepository.findByLevel(30)
                    .orElseThrow(() -> new RuntimeException("레벨 30 데이터를 찾을 수 없습니다."));
            worldTreeStatus.setCurrentExp(level30.getCumulativeExp());
            
            // 테스트용 정령의 축복 지급 (많은 양)
            worldTreeStatus.setAvailableEssence(100000L);
            
            // 희귀 정령 수령 플래그 설정 (테스트용)
            worldTreeStatus.setRareSpiritReceived(true);
            
            worldTreeStatus = worldTreeStatusRepository.save(worldTreeStatus);

            // 정령의 축복 로그 기록 (새 계정이거나 로그가 없을 때만)
            if (isNewAccount || existingStatus.isEmpty()) {
                EssencePulseLog log = new EssencePulseLog(masterUser.getId(), 100000, "마스터 계정 초기화");
                essencePulseLogRepository.save(log);
            }

            // 마스터 계정 정령 초기화: 기존 정령 모두 삭제 후 재생성
            resetMasterAccountSpirits(masterUser.getId());
            
            // 테스트용 정령들 재생성
            createTestSpirits(masterUser.getId(), true); // 강제 재생성

            System.out.println("마스터 계정 초기화 완료!");
            System.out.println("로그인 정보:");
            System.out.println("  아이디: " + MASTER_USERNAME);
            System.out.println("  비밀번호: " + MASTER_PASSWORD);
            System.out.println("  세계수 레벨: 30");
            System.out.println("  정령의 축복: 100,000");
        };
    }

    /**
     * 마스터 계정의 기존 정령 모두 삭제
     */
    private void resetMasterAccountSpirits(Long userId) {
        List<Spirit> existingSpirits = spiritRepository.findByUserId(userId);
        if (!existingSpirits.isEmpty()) {
            System.out.println("마스터 계정의 기존 정령 " + existingSpirits.size() + "마리를 삭제합니다.");
            spiritRepository.deleteAll(existingSpirits);
            System.out.println("기존 정령 삭제 완료!");
        }
    }
    
    /**
     * 테스트용 정령들 생성
     * @param userId 마스터 계정 ID
     * @param forceCreate 새 계정인 경우 강제 생성 (기존 계정이면 중복 체크)
     */
    private void createTestSpirits(Long userId, boolean forceCreate) {
        // 기존 정령 목록 조회 (중복 체크용)
        List<Spirit> existingSpirits = forceCreate ? List.of() : spiritRepository.findByUserId(userId);
        
        // 불의 정령 (기본 단계)
        createSpiritIfNotExists(userId, "FIRE", "테스트 불의 정령", 0, false, existingSpirits);
        
        // 물의 정령 (1차 진화)
        createSpiritIfNotExists(userId, "WATER", "테스트 물의 정령", 1, false, existingSpirits);
        
        // 풀의 정령 (2차 진화)
        createSpiritIfNotExists(userId, "WIND", "테스트 풀의 정령", 2, false, existingSpirits);
        
        // 빛의 정령 (기본 단계)
        createSpiritIfNotExists(userId, "LIGHT", "테스트 빛의 정령", 0, false, existingSpirits);
        
        // 어둠의 정령 (고치 상태 - 1차 진화)
        createSpiritIfNotExists(userId, "DARK", "테스트 어둠의 정령", 1, true, existingSpirits);
    }

    /**
     * 정령 생성 헬퍼 메서드 (중복 체크 포함)
     */
    private void createSpiritIfNotExists(Long userId, String typeCode, String name, int evolutionStage, 
                                        boolean isCocoon, List<Spirit> existingSpirits) {
        // 이미 해당 이름의 정령이 존재하는지 확인
        boolean alreadyExists = existingSpirits.stream()
                .anyMatch(spirit -> name.equals(spirit.getName()));
        
        if (alreadyExists) {
            System.out.println("정령이 이미 존재하여 생성을 건너뜁니다: " + name);
            return;
        }
        
        Optional<SpiritType> spiritTypeOpt = spiritTypeRepository.findByTypeCode(typeCode);
        if (spiritTypeOpt.isEmpty()) {
            System.out.println("정령 타입을 찾을 수 없습니다: " + typeCode);
            return;
        }

        SpiritType spiritType = spiritTypeOpt.get();
        Spirit spirit = new Spirit();
        spirit.setUserId(userId);
        spirit.setSpiritType(spiritType.getTypeName());
        spirit.setName(name);
        spirit.setEvolutionStage(evolutionStage);
        // 최대 레벨 30으로 조정
        spirit.setLevel(evolutionStage == 0 ? 1 : evolutionStage == 1 ? 15 : 30);
        spirit.setExperience(evolutionStage == 0 ? 0 : evolutionStage == 1 ? 10000 : 50000);
        spirit.setIntimacy(10);
        spirit.setPersonality("온순");
        
        // 능력치 설정
        spirit.setRangedAttack(100);
        spirit.setMeleeAttack(100);
        spirit.setRangedDefense(100);
        spirit.setMeleeDefense(100);
        spirit.setSpeed(100);
        
        // 상태 설정
        spirit.setHappiness(100);
        spirit.setMood("좋음");
        spirit.setHunger(0);
        spirit.setEnergy(100);
        spirit.setHealthStatus("건강");
        spirit.setAge(0);
        spirit.setMaxLevelReached(evolutionStage == 2);
        
        // 고치 상태 설정
        if (isCocoon) {
            spirit.setEvolutionInProgress(true);
            spirit.setEvolutionStartTime(LocalDateTime.now().minusHours(48)); // 48시간 경과 상태로 설정
            spirit.setEvolutionTargetStage(2);
        } else {
            spirit.setEvolutionInProgress(false);
        }
        
        spiritRepository.save(spirit);
        System.out.println("테스트 정령 생성: " + name);
    }
}

