package com.soi.spirit.service;

import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.SpiritRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 멀티스레드 정령 처리 서비스
 * 각 정령의 자율 행동을 동시에 처리하여 성능을 향상시킵니다.
 */
@Service
@Transactional
public class ThreadedSpiritService {

    private final SpiritRepository spiritRepository;
    private final SpiritAIService spiritAIService;
    
    // 스레드 풀 설정
    private final ExecutorService executorService;
    private static final int MAX_THREADS = 10; // 최대 스레드 수 (과도한 멀티스레딩 방지)
    private static final int BATCH_SIZE = 5; // 한 번에 처리할 정령 수

    @Autowired
    public ThreadedSpiritService(SpiritRepository spiritRepository,
                                 SpiritAIService spiritAIService) {
        this.spiritRepository = spiritRepository;
        this.spiritAIService = spiritAIService;
        
        // 스레드 풀 생성 (최대 10개 스레드)
        this.executorService = Executors.newFixedThreadPool(MAX_THREADS);
    }

    /**
     * 멀티스레드로 정령들의 자율 행동을 처리합니다.
     */
    public void processSpiritsInParallel(Long userId) {
        List<Spirit> spirits = spiritRepository.findByUserId(userId);
        
        if (spirits.isEmpty()) {
            return;
        }

        // 정령들을 배치로 나누어 처리
        List<List<Spirit>> batches = splitIntoBatches(spirits, BATCH_SIZE);
        List<Future<Void>> futures = new ArrayList<>();

        // 각 배치를 별도 스레드에서 처리
        for (List<Spirit> batch : batches) {
            Future<Void> future = executorService.submit(() -> {
                processBatch(batch, userId);
                return null;
            });
            futures.add(future);
        }

        // 모든 배치가 완료될 때까지 대기
        for (Future<Void> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS); // 최대 30초 대기
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.err.println("Error processing spirit batch: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 정령 리스트를 배치로 나눕니다.
     */
    private List<List<Spirit>> splitIntoBatches(List<Spirit> spirits, int batchSize) {
        List<List<Spirit>> batches = new ArrayList<>();
        for (int i = 0; i < spirits.size(); i += batchSize) {
            int end = Math.min(i + batchSize, spirits.size());
            batches.add(new ArrayList<>(spirits.subList(i, end)));
        }
        return batches;
    }

    /**
     * 배치 내 정령들을 처리합니다.
     */
    private void processBatch(List<Spirit> batch, Long userId) {
        for (Spirit spirit : batch) {
            try {
                // 은퇴하거나 진화 중인 정령은 건너뛰기
                if (spirit.getIsRetired() != null && spirit.getIsRetired()) {
                    continue;
                }
                if (spirit.getEvolutionInProgress() != null && spirit.getEvolutionInProgress()) {
                    continue;
                }

                // AI 기반 행동 결정 (향후 활용 가능)
                // String action = spiritAIService.decideBestAction(spirit, userId);
                
                // 기본 자율 행동 처리
                processSingleSpirit(spirit);
                
            } catch (Exception e) {
                System.err.println("Error processing spirit " + spirit.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 단일 정령의 자율 행동을 처리합니다.
     * 현재는 스레드 안전하게 처리하기 위한 플레이스홀더입니다.
     */
    private void processSingleSpirit(Spirit spirit) {
        // 정령이 존재하는지 확인
        if (spirit == null || spirit.getId() == null) {
            return;
        }
        // 실제 처리 로직은 AutonomousBehaviorService에서 처리됨
        // 여기서는 병렬 처리를 위한 구조만 제공
    }

    /**
     * 스레드 풀 종료
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

