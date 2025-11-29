package com.soi.tutorial.service;

import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 튜토리얼 서비스
 * 튜토리얼 완료 상태 관리
 */
@Service
@Transactional
public class TutorialService {

    private final UserRepository userRepository;

    @Autowired
    public TutorialService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 사용자의 튜토리얼 완료 여부를 확인합니다.
     */
    public boolean isTutorialCompleted(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        return user.getTutorialCompleted() != null && user.getTutorialCompleted();
    }

    /**
     * 튜토리얼을 완료 처리합니다.
     */
    public void completeTutorial(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 null입니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setTutorialCompleted(true);
        userRepository.save(user);
    }

    /**
     * 튜토리얼을 재시작합니다 (테스트용).
     */
    public void resetTutorial(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID가 null입니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setTutorialCompleted(false);
        userRepository.save(user);
    }
}

