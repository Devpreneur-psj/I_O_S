package com.soi.community.service;

import com.soi.community.entity.Friend;
import com.soi.community.entity.FriendGift;
import com.soi.community.repository.FriendRepository;
import com.soi.community.repository.FriendGiftRepository;
import com.soi.spirit.entity.Item;
import com.soi.spirit.entity.UserItem;
import com.soi.spirit.repository.ItemRepository;
import com.soi.spirit.repository.UserItemRepository;
import com.soi.user.User;
import com.soi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FriendService {
    
    private final FriendRepository friendRepository;
    private final FriendGiftRepository friendGiftRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    
    @Autowired
    public FriendService(FriendRepository friendRepository,
                        FriendGiftRepository friendGiftRepository,
                        UserRepository userRepository,
                        ItemRepository itemRepository,
                        UserItemRepository userItemRepository) {
        this.friendRepository = friendRepository;
        this.friendGiftRepository = friendGiftRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.userItemRepository = userItemRepository;
    }
    
    /**
     * 친구 요청 보내기
     */
    public Friend sendFriendRequest(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new IllegalArgumentException("사용자 ID 또는 친구 ID가 유효하지 않습니다.");
        }
        
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }
        
        // 이미 친구 요청을 보냈는지 확인 (양방향 확인)
        boolean alreadyExists = friendRepository.existsByUserIdAndFriendId(userId, friendId);
        if (alreadyExists) {
            // 상태 확인
            Optional<Friend> existingFriend = friendRepository.findByUserIdAndFriendId(userId, friendId);
            if (existingFriend.isPresent()) {
                String status = existingFriend.get().getStatus();
                if ("PENDING".equals(status)) {
                    throw new IllegalArgumentException("이미 친구 요청을 보냈습니다. 승인을 기다리고 있습니다.");
                } else if ("ACCEPTED".equals(status)) {
                    throw new IllegalArgumentException("이미 친구입니다.");
                }
            }
            throw new IllegalArgumentException("이미 친구 요청을 보냈거나 친구입니다.");
        }
        
        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus("PENDING");
        
        return friendRepository.save(friend);
    }
    
    /**
     * 친구 요청 수락
     */
    public void acceptFriendRequest(Long userId, Long friendId) {
        // 받은 친구 요청 찾기 (friendId가 보낸 사람, userId가 받은 사람)
        Optional<Friend> incomingRequestOpt = friendRepository.findByUserIdAndFriendId(friendId, userId);
        if (incomingRequestOpt.isPresent()) {
            Friend incomingRequest = incomingRequestOpt.get();
            incomingRequest.setStatus("ACCEPTED");
            friendRepository.save(incomingRequest);
        } else {
            throw new IllegalArgumentException("친구 요청을 찾을 수 없습니다.");
        }
        
        // 양방향 친구 관계 생성 (userId -> friendId)
        Optional<Friend> reverseFriendOpt = friendRepository.findByUserIdAndFriendId(userId, friendId);
        if (reverseFriendOpt.isEmpty()) {
            Friend reverseFriend = new Friend();
            reverseFriend.setUserId(userId);
            reverseFriend.setFriendId(friendId);
            reverseFriend.setStatus("ACCEPTED");
            friendRepository.save(reverseFriend);
        } else {
            // 이미 존재하면 상태만 ACCEPTED로 업데이트
            Friend reverseFriend = reverseFriendOpt.get();
            reverseFriend.setStatus("ACCEPTED");
            friendRepository.save(reverseFriend);
        }
    }
    
    /**
     * 친구 요청 거절
     */
    public void rejectFriendRequest(Long userId, Long friendId) {
        Optional<Friend> friendOpt = friendRepository.findByUserIdAndFriendId(friendId, userId);
        if (friendOpt.isPresent()) {
            friendRepository.delete(friendOpt.get());
        } else {
            throw new IllegalArgumentException("친구 요청을 찾을 수 없습니다.");
        }
    }
    
    /**
     * 친구 목록 조회
     */
    public List<User> getFriends(Long userId) {
        List<Friend> friends = friendRepository.findByUserIdAndStatus(userId, "ACCEPTED");
        List<Long> friendIds = friends.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toList());
        
        return userRepository.findAllById(friendIds);
    }
    
    /**
     * 받은 친구 요청 목록 조회 (PENDING 상태)
     */
    public List<User> getPendingFriendRequests(Long userId) {
        List<Friend> pendingRequests = friendRepository.findByFriendIdAndStatus(userId, "PENDING");
        List<Long> requestUserIds = pendingRequests.stream()
                .map(Friend::getUserId)
                .collect(Collectors.toList());
        
        return userRepository.findAllById(requestUserIds);
    }
    
    /**
     * 추천 친구 목록 (친구가 아닌 유저들)
     */
    public List<User> getRecommendedFriends(Long userId, int limit) {
        List<Friend> friends = friendRepository.findByUserIdAndStatus(userId, "ACCEPTED");
        List<Long> friendIds = friends.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toList());
        friendIds.add(userId); // 자기 자신 제외
        
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> !friendIds.contains(user.getId()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 아이디/닉네임으로 유저 검색
     */
    public List<User> searchUsers(String keyword) {
        return userRepository.findAll().stream()
                .filter(user -> user.getUsername().contains(keyword) || 
                               (user.getNickname() != null && user.getNickname().contains(keyword)))
                .limit(20)
                .collect(Collectors.toList());
    }
    
    /**
     * 친구에게 아이템 선물하기 (하루 한 번)
     */
    public void sendGift(Long senderId, Long receiverId, Long itemId, Integer quantity) {
        // 오늘 이미 선물했는지 확인
        if (friendGiftRepository.existsBySenderIdAndReceiverIdAndGiftDate(senderId, receiverId, LocalDate.now())) {
            throw new IllegalArgumentException("하루에 한 번만 선물할 수 있습니다.");
        }
        
        // 친구 관계 확인
        Optional<Friend> friendOpt = friendRepository.findByUserIdAndFriendId(senderId, receiverId);
        if (friendOpt.isEmpty() || !"ACCEPTED".equals(friendOpt.get().getStatus())) {
            throw new IllegalArgumentException("친구 관계가 아닙니다.");
        }
        
        // 보낸 사람의 아이템 확인
        Optional<UserItem> userItemOpt = userItemRepository.findByUserIdAndItemId(senderId, itemId);
        if (userItemOpt.isEmpty() || userItemOpt.get().getQuantity() < quantity) {
            throw new IllegalArgumentException("아이템이 부족합니다.");
        }
        
        // 아이템 정보 가져오기
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));
        
        // 먹이 아이템인지 확인
        if (!item.getItemType().equals("FOOD")) {
            throw new IllegalArgumentException("먹이 아이템만 선물할 수 있습니다.");
        }
        
        // 보낸 사람의 아이템 차감
        UserItem senderItem = userItemOpt.get();
        senderItem.setQuantity(senderItem.getQuantity() - quantity);
        userItemRepository.save(senderItem);
        
        // 받는 사람의 아이템 추가
        Optional<UserItem> receiverItemOpt = userItemRepository.findByUserIdAndItemId(receiverId, itemId);
        if (receiverItemOpt.isPresent()) {
            UserItem receiverItem = receiverItemOpt.get();
            receiverItem.setQuantity(receiverItem.getQuantity() + quantity);
            userItemRepository.save(receiverItem);
        } else {
            UserItem receiverItem = new UserItem();
            receiverItem.setUserId(receiverId);
            receiverItem.setItemId(itemId);
            receiverItem.setQuantity(quantity);
            userItemRepository.save(receiverItem);
        }
        
        // 선물 기록 저장
        FriendGift gift = new FriendGift();
        gift.setSenderId(senderId);
        gift.setReceiverId(receiverId);
        gift.setItemId(itemId);
        gift.setItemName(item.getItemName());
        gift.setQuantity(quantity);
        gift.setGiftDate(LocalDate.now());
        friendGiftRepository.save(gift);
    }
}

