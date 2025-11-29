package com.soi.game.scheduler;

import com.soi.game.service.GameTimeService;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.spirit.service.AutonomousBehaviorService;
import com.soi.spirit.service.LifecycleService;
import com.soi.spirit.service.RandomEventService;
import com.soi.spirit.service.SkillService;
import com.soi.spirit.service.SpiritService;
import com.soi.spirit.service.ThreadedSpiritService;
import com.soi.spirit.service.SpiritInteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 게임 스케줄러
 * 정기적으로 자율 행동, 이벤트, 생애 주기 등을 처리
 */
@Component
public class GameScheduler {

    private final GameTimeService gameTimeService;
    private final AutonomousBehaviorService autonomousBehaviorService;
    private final RandomEventService randomEventService;
    private final LifecycleService lifecycleService;
    private final SpiritRepository spiritRepository;
    private final SpiritService spiritService;
    private final SkillService skillService;
    private final ThreadedSpiritService threadedSpiritService;
    private final SpiritInteractionService spiritInteractionService;

    @Autowired
    public GameScheduler(GameTimeService gameTimeService,
                        AutonomousBehaviorService autonomousBehaviorService,
                        RandomEventService randomEventService,
                        LifecycleService lifecycleService,
                        SpiritRepository spiritRepository,
                        SpiritService spiritService,
                        SkillService skillService,
                        ThreadedSpiritService threadedSpiritService,
                        SpiritInteractionService spiritInteractionService) {
        this.gameTimeService = gameTimeService;
        this.autonomousBehaviorService = autonomousBehaviorService;
        this.randomEventService = randomEventService;
        this.lifecycleService = lifecycleService;
        this.spiritRepository = spiritRepository;
        this.spiritService = spiritService;
        this.skillService = skillService;
        this.threadedSpiritService = threadedSpiritService;
        this.spiritInteractionService = spiritInteractionService;
    }

    /**
     * 매 시간마다 실행 (게임 시간 진행 및 자율 행동 처리)
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 (실제 시간)
    public void processGameTimeAndBehaviors() {
        // 모든 사용자의 정령들 처리
        List<Long> userIds = spiritRepository.findAll().stream()
                .map(spirit -> spirit.getUserId())
                .distinct()
                .toList();
        
        for (Long userId : userIds) {
            try {
                // 게임 시간 진행
                gameTimeService.advanceGameTime(userId);
                
                // 멀티스레드로 자율 행동 처리 (성능 향상)
                threadedSpiritService.processSpiritsInParallel(userId);
                
                // 정령 간 상호작용 처리
                spiritInteractionService.processInteractions(userId);
                
                // 생애 주기 처리
                lifecycleService.processLifecycle(userId);
            } catch (Exception e) {
                System.err.println("Error processing user " + userId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 매 6시간마다 실행 (랜덤 이벤트 생성)
     */
    @Scheduled(fixedRate = 21600000) // 6시간마다 (실제 시간)
    public void generateRandomEvents() {
        List<Long> userIds = spiritRepository.findAll().stream()
                .map(spirit -> spirit.getUserId())
                .distinct()
                .toList();
        
        for (Long userId : userIds) {
            try {
                randomEventService.generateRandomEvents(userId);
            } catch (Exception e) {
                System.err.println("Error generating events for user " + userId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 매 10분마다 실행 (진화 완료 처리)
     */
    @Scheduled(fixedRate = 600000) // 10분마다 (실제 시간)
    public void processCompletedEvolutions() {
        try {
            spiritService.processCompletedEvolutions();
        } catch (Exception e) {
            System.err.println("Error processing completed evolutions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 매 1분마다 실행 (기술 학습 완료 처리)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 (실제 시간)
    public void processCompletedLearning() {
        try {
            skillService.processCompletedLearning();
        } catch (Exception e) {
            System.err.println("Error processing completed learning: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

