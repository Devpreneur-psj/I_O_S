package com.soi.community.service;

import com.soi.community.entity.ChatMessage;
import com.soi.community.entity.SpiritSquarePresence;
import com.soi.community.repository.ChatMessageRepository;
import com.soi.community.repository.SpiritSquarePresenceRepository;
import com.soi.spirit.entity.Spirit;
import com.soi.spirit.repository.SpiritRepository;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpiritSquareService {
    
    private final SpiritSquarePresenceRepository presenceRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SpiritRepository spiritRepository;
    
    @Autowired
    public SpiritSquareService(SpiritSquarePresenceRepository presenceRepository,
                              ChatMessageRepository chatMessageRepository,
                              UserRepository userRepository,
                              SpiritRepository spiritRepository) {
        this.presenceRepository = presenceRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.spiritRepository = spiritRepository;
    }
    
    /**
     * 광장 입장
     */
    public SpiritSquarePresence enterSquare(Long userId, Integer channelNumber, Long spiritId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        Spirit spirit = spiritRepository.findById(spiritId)
                .orElseThrow(() -> new IllegalArgumentException("정령을 찾을 수 없습니다."));
        
        if (!spirit.getUserId().equals(userId)) {
            throw new IllegalArgumentException("자신의 정령만 선택할 수 있습니다.");
        }
        
        // 기존 입장 정보 확인 및 업데이트 또는 생성
        Optional<SpiritSquarePresence> existingPresenceOpt = 
                presenceRepository.findByUserIdAndChannelNumber(userId, channelNumber);
        
        SpiritSquarePresence presence;
        if (existingPresenceOpt.isPresent()) {
            // 기존 presence가 있으면 업데이트
            presence = existingPresenceOpt.get();
        } else {
            // 없으면 새로 생성
            presence = new SpiritSquarePresence();
            presence.setUserId(userId);
            presence.setChannelNumber(channelNumber);
        }
        
        // 정보 업데이트
        presence.setUsername(user.getUsername());
        presence.setNickname(user.getNickname());
        presence.setSpiritId(spiritId);
        presence.setSpiritName(spirit.getName());
        presence.setSpiritType(spirit.getSpiritType());
        presence.setSpiritEvolutionStage(spirit.getEvolutionStage() != null ? spirit.getEvolutionStage() : 0);
        
        // 기존 위치 유지 (없으면 기본 위치)
        if (presence.getPositionX() == null) {
            presence.setPositionX(50.0);
        }
        if (presence.getPositionY() == null) {
            presence.setPositionY(50.0);
        }
        
        presence.setLastActivity(LocalDateTime.now());
        
        return presenceRepository.save(presence);
    }
    
    /**
     * 광장 퇴장
     */
    public void exitSquare(Long userId, Integer channelNumber) {
        presenceRepository.findByUserIdAndChannelNumber(userId, channelNumber)
                .ifPresent(presenceRepository::delete);
    }
    
    /**
     * 정령 위치 업데이트
     */
    public void updatePosition(Long userId, Integer channelNumber, Double x, Double y) {
        Optional<SpiritSquarePresence> presenceOpt = presenceRepository.findByUserIdAndChannelNumber(userId, channelNumber);
        if (presenceOpt.isPresent()) {
            SpiritSquarePresence presence = presenceOpt.get();
            presence.setPositionX(x);
            presence.setPositionY(y);
            presence.setLastActivity(LocalDateTime.now());
            presenceRepository.save(presence);
        }
    }
    
    /**
     * 채널의 모든 유저 조회
     */
    public List<SpiritSquarePresence> getChannelUsers(Integer channelNumber) {
        return presenceRepository.findByChannelNumber(channelNumber);
    }
    
    /**
     * 채팅 메시지 전송
     */
    public ChatMessage sendMessage(Long userId, Integer channelNumber, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setChannelNumber(channelNumber);
        chatMessage.setUserId(userId);
        chatMessage.setUsername(user.getUsername());
        chatMessage.setNickname(user.getNickname());
        chatMessage.setMessage(message);
        chatMessage.setCreatedAt(LocalDateTime.now());
        
        return chatMessageRepository.save(chatMessage);
    }
    
    /**
     * 채널의 최근 메시지 조회
     */
    public List<ChatMessage> getRecentMessages(Integer channelNumber, int limit) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(30); // 최근 30분
        List<ChatMessage> messages = chatMessageRepository.findRecentMessagesByChannel(channelNumber, since);
        
        // 최신순으로 정렬하고 limit 적용
        return messages.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 비활성 유저 정리 (5분 이상 활동 없음)
     */
    public void cleanupInactiveUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<SpiritSquarePresence> activePresences = presenceRepository.findActivePresences(threshold);
        
        // 비활성 유저 삭제
        List<SpiritSquarePresence> allPresences = presenceRepository.findAll();
        for (SpiritSquarePresence presence : allPresences) {
            if (!activePresences.contains(presence)) {
                presenceRepository.delete(presence);
            }
        }
    }
}

