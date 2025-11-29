/**
 * 정령광장 WebSocket 실시간 통신
 */

let ws = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 2; // 2번만 시도 후 폴링으로 전환
let reconnectTimeout = null;

/**
 * WebSocket 초기화
 */
function initWebSocket() {
    if (!currentSpiritId) {
        console.warn('정령이 선택되지 않아 WebSocket을 초기화할 수 없습니다.');
        return;
    }
    
    // 기존 연결이 있으면 닫기
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.close();
    }
    
    // WebSocket URL 생성 (프로토콜에 따라 ws:// 또는 wss://)
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/spirit-square`;
    
    try {
        ws = new WebSocket(wsUrl);
        
        ws.onopen = function() {
            console.log('WebSocket 연결 성공');
            reconnectAttempts = 0;
            
            // 입장 메시지 전송
            sendWebSocketMessage({
                type: 'ENTER',
                channelNumber: PLAZA_CHANNEL,
                spiritId: currentSpiritId
            });
        };
        
        ws.onmessage = function(event) {
            try {
                const message = JSON.parse(event.data);
                handleWebSocketMessage(message);
            } catch (error) {
                console.error('WebSocket 메시지 파싱 실패:', error);
            }
        };
        
        ws.onerror = function(error) {
            console.error('WebSocket 오류:', error);
        };
        
        ws.onclose = function() {
            console.log('WebSocket 연결 종료');
            ws = null;
            
            // 재연결 시도 (빠르게 포기하고 폴링으로 전환)
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && currentSpiritId) {
                reconnectAttempts++;
                const delay = 1000; // 1초만 대기
                console.log(`${delay}ms 후 재연결 시도 (${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})`);
                
                reconnectTimeout = setTimeout(() => {
                    initWebSocket();
                }, delay);
            } else {
                console.warn('WebSocket 재연결 실패, 폴링 모드로 전환');
                if (typeof startUpdateInterval === 'function') {
                    startUpdateInterval();
                }
            }
        };
        
    } catch (error) {
        console.error('WebSocket 초기화 실패:', error);
        // 폴링 모드로 폴백
        startUpdateInterval();
    }
}

/**
 * WebSocket 메시지 전송
 */
function sendWebSocketMessage(data) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(data));
    } else {
        console.warn('WebSocket이 연결되지 않았습니다.');
    }
}

/**
 * WebSocket 메시지 처리
 */
function handleWebSocketMessage(message) {
    switch (message.type) {
        case 'USER_JOINED':
            // 새 유저 입장
            if (message.user) {
                addUserToSquare(message.user);
            }
            break;
            
        case 'USER_LEFT':
            // 유저 퇴장
            if (message.userId) {
                removeUserFromSquare(message.userId);
            }
            break;
            
        case 'POSITION_UPDATE':
            // 위치 업데이트
            if (message.userId && message.position) {
                updateUserPosition(message.userId, message.position.x, message.position.y);
            }
            break;
            
        case 'MESSAGE':
            // 채팅 메시지
            if (message.chatMessage) {
                addChatMessage(message.chatMessage);
            }
            break;
            
        case 'USER_LIST':
            // 유저 목록 업데이트
            if (message.users) {
                updateUserList(message.users);
            }
            break;
            
        default:
            console.warn('알 수 없는 WebSocket 메시지 타입:', message.type);
    }
}

/**
 * 위치 업데이트를 WebSocket으로 전송
 */
function sendPositionUpdate(x, y) {
    sendWebSocketMessage({
        type: 'POSITION_UPDATE',
        channelNumber: PLAZA_CHANNEL,
        position: { x: x, y: y }
    });
}

/**
 * 채팅 메시지를 WebSocket으로 전송
 */
function sendChatMessageWebSocket(message) {
    sendWebSocketMessage({
        type: 'MESSAGE',
        channelNumber: PLAZA_CHANNEL,
        message: message
    });
}

/**
 * 유저를 광장에 추가
 */
function addUserToSquare(user) {
    const myUserId = getMyUserId();
    if (user.userId === myUserId) return; // 내 정령은 제외
    
    const squareArea = document.getElementById('squareArea');
    if (!squareArea) return;
    
    let spiritElement = otherUsers.get(user.userId.toString());
    
    if (!spiritElement) {
        spiritElement = createSpiritElement({
            spiritType: user.spiritType,
            evolutionStage: user.spiritEvolutionStage,
            name: user.spiritName,
            nickname: user.nickname,
            username: user.username,
            userId: user.userId
        }, false);
        
        spiritElement.style.position = 'absolute';
        spiritElement.dataset.presenceId = user.id;
        
        squareArea.appendChild(spiritElement);
        otherUsers.set(user.userId.toString(), spiritElement);
    }
    
    // 위치 설정
    const targetX = user.positionX || 50;
    const targetY = user.positionY || 50;
    spiritElement.style.transition = 'left 0.3s ease, top 0.3s ease';
    spiritElement.style.left = targetX + '%';
    spiritElement.style.top = targetY + '%';
    
    // 온라인 유저 수 업데이트
    updateOnlineCount();
}

/**
 * 유저를 광장에서 제거
 */
function removeUserFromSquare(userId) {
    const spiritElement = otherUsers.get(userId.toString());
    if (spiritElement) {
        spiritElement.remove();
        otherUsers.delete(userId.toString());
        speechBubbles.delete(userId.toString());
        updateOnlineCount();
    }
}

/**
 * 유저 위치 업데이트
 */
function updateUserPosition(userId, x, y) {
    const myUserId = getMyUserId();
    if (userId === myUserId) return; // 내 정령은 제외
    
    const spiritElement = otherUsers.get(userId.toString());
    if (spiritElement) {
        spiritElement.style.transition = 'left 0.3s ease, top 0.3s ease';
        spiritElement.style.left = x + '%';
        spiritElement.style.top = y + '%';
    }
}

/**
 * 유저 목록 업데이트
 */
function updateUserList(users) {
    const myUserId = getMyUserId();
    const existingUserIds = new Set();
    
    // 현재 유저들의 ID 수집
    users.forEach(user => {
        if (user.id) existingUserIds.add(user.id.toString());
    });
    
    // 제거된 유저 삭제
    otherUsers.forEach((element, userId) => {
        const presenceId = element.dataset.presenceId;
        if (!presenceId || !existingUserIds.has(presenceId.toString())) {
            element.remove();
            otherUsers.delete(userId);
            speechBubbles.delete(userId);
        }
    });
    
    // 유저 추가 및 위치 업데이트
    users.forEach(user => {
        const userId = user.userId ? user.userId.toString() : null;
        if (!userId || userId === myUserId) return;
        
        addUserToSquare(user);
    });
    
    updateOnlineCount();
}

/**
 * 온라인 유저 수 업데이트
 */
function updateOnlineCount() {
    const count = otherUsers.size + 1; // 다른 유저 + 나
    const currentUsersEl = document.getElementById('currentUsers');
    if (currentUsersEl) {
        currentUsersEl.textContent = `온라인: ${count}명`;
    }
}

/**
 * WebSocket 연결 종료
 */
function closeWebSocket() {
    if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
        reconnectTimeout = null;
    }
    
    if (ws) {
        sendWebSocketMessage({
            type: 'EXIT',
            channelNumber: PLAZA_CHANNEL
        });
        
        ws.close();
        ws = null;
    }
}

