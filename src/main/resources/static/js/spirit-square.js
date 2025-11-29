// 정령 광장 JavaScript

// 전역 변수
const PLAZA_CHANNEL = 1; // 단일 광장 (채널 번호 고정)
let currentSpiritId = null;
let chatMode = true; // true: 채팅 모드, false: 이동 모드
let myPosition = { x: 50, y: 50 };
let otherUsers = new Map();
let updateInterval = null;
let autoMoveInterval = null; // 자동 이동 애니메이션
let lastManualMoveTime = Date.now(); // 마지막 수동 이동 시간
const AUTO_MOVE_DELAY = 3000; // 3초 후 자동 이동 시작
let autoMoveTarget = null; // 자동 이동 목표 위치

document.addEventListener('DOMContentLoaded', function() {
    console.log('정령 광장 페이지 로드됨');
    
    // 정령 선택 모달 열기
    openSpiritSelectModal();
    
    // 광장 클릭 핸들러 설정
    setupSquareClickHandler();
    
    // 키보드 이동 핸들러 설정
    setupKeyboardControls();
    
    // 전역 엔터키 핸들러 설정 (모드 전환용)
    setupGlobalEnterKeyHandler();
    
    // 모달 바깥 클릭 시 닫기
    setupModalCloseHandlers();
    
    // 버튼 이벤트 핸들러 직접 설정
    setupButtonHandlers();
});

/**
 * 버튼 핸들러 설정
 */
function setupButtonHandlers() {
    // 나가기 버튼
    const exitBtn = document.querySelector('.btn-exit');
    if (exitBtn) {
        exitBtn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            console.log('나가기 버튼 클릭됨');
            exitSquare();
        });
    }
}

/**
 * 모달 닫기 핸들러 설정
 */
function setupModalCloseHandlers() {
    // 정령 선택 모달 바깥 클릭 시 닫기 (정령 선택 전에는 닫을 수 없음)
    const spiritModal = document.getElementById('spiritSelectModal');
    if (spiritModal) {
        spiritModal.addEventListener('click', function(e) {
            if (e.target === spiritModal && !currentSpiritId) {
                // 정령 선택 전에는 월드로 돌아가기
                if (confirm('정령을 선택하지 않고 나가시겠습니까?')) {
                    window.location.href = '/world';
                }
            }
        });
    }
}

/**
 * 대표 정령 선택 모달 열기
 */
async function openSpiritSelectModal() {
    try {
        const response = await fetch('/spirit/api/my-spirits', {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('정령 목록을 불러올 수 없습니다.');
        }
        
        const spirits = await response.json();
        if (!spirits || spirits.length === 0) {
            alert('사용할 수 있는 정령이 없습니다.');
            return;
        }
        
        const spiritList = document.getElementById('spiritList');
        spiritList.innerHTML = '';
        
        spirits.forEach(spirit => {
            const spiritCard = document.createElement('div');
            spiritCard.className = 'spirit-card';
            spiritCard.style.cursor = 'pointer';
            spiritCard.onclick = function() {
                enterPlaza(spirit.id);
            };
            
            const step = (spirit.evolutionStage || 0) + 1;
            const typeCode = getTypeCode(spirit.spiritType);
            const imagePath = `/images/spirits/step${step}_${typeCode}.png`;
            
            spiritCard.innerHTML = `
                <img src="${imagePath}" alt="${spirit.name || '이름없음'}" class="spirit-image">
                <div class="spirit-info">
                    <div class="spirit-name">${spirit.name || '이름없음'}</div>
                    <div class="spirit-type">${spirit.spiritType || '정령'}</div>
                    <div class="spirit-level">Lv.${spirit.level || 1}</div>
                </div>
            `;
            
            spiritList.appendChild(spiritCard);
        });
        
        const modal = document.getElementById('spiritSelectModal');
        if (modal) {
            modal.style.display = 'block';
            // 모달 드래그 활성화
            if (typeof makeModalDraggable === 'function') {
                makeModalDraggable(modal);
            }
        }
    } catch (error) {
        console.error('정령 목록 조회 실패:', error);
        alert('정령 목록을 불러오는 중 오류가 발생했습니다.');
        window.location.href = '/world';
    }
}

/**
 * 대표 정령 선택 모달 닫기
 */
function closeSpiritSelectModal() {
    const modal = document.getElementById('spiritSelectModal');
    modal.style.display = 'none';
}

/**
 * 정령 타입 코드 가져오기
 */
function getTypeCode(spiritType) {
    if (!spiritType) return 'fire';
    if (spiritType.includes('물') || spiritType.includes('WATER')) return 'water';
    if (spiritType.includes('풀') || spiritType.includes('WIND') || spiritType.includes('LEAF')) return 'leaf';
    if (spiritType.includes('빛') || spiritType.includes('LIGHT')) return 'light';
    if (spiritType.includes('어둠') || spiritType.includes('DARK')) return 'dark';
    return 'fire';
}

/**
 * 광장 입장
 */
async function enterPlaza(spiritId) {
    try {
        closeSpiritSelectModal();
        
        const response = await fetch('/spirit-square/api/enter-channel', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: new URLSearchParams({
                channelNumber: PLAZA_CHANNEL,
                spiritId: spiritId
            })
        });
        
        const data = await response.json();
        if (!data.success) {
            alert(data.message || '광장 입장에 실패했습니다.');
            openSpiritSelectModal(); // 실패 시 모달 다시 열기
            return;
        }
        
        currentSpiritId = spiritId;
        
        // 정령 선택 모달 닫기
        const modal = document.getElementById('spiritSelectModal');
        if (modal) {
            modal.style.display = 'none';
        }
        
        // 광장 초기화
        await initializeSquare();
        
        // 실시간 WebSocket 연결 시도 (실패하면 폴링으로 폴백)
        try {
            if (typeof initWebSocket === 'function') {
                initWebSocket();
            } else {
                startUpdateInterval();
            }
        } catch (error) {
            console.warn('WebSocket 연결 실패, 폴링 모드로 전환:', error);
            startUpdateInterval();
        }
        
        // 자동 이동 애니메이션 시작
        startAutoMoveAnimation();
        
        // 자동 말풍선 표시 시작
        startAutoSpeechBubbles();
        
        // 이동 모드로 시작 (커서 설정)
        const squareArea = document.getElementById('squareArea');
        if (squareArea) {
            squareArea.style.cursor = 'crosshair';
        }
        
        console.log('광장 입장 완료, 정령 ID:', spiritId);
        
    } catch (error) {
        console.error('광장 입장 실패:', error);
        alert('광장 입장 중 오류가 발생했습니다.');
        openSpiritSelectModal(); // 실패 시 모달 다시 열기
    }
}

/**
 * 광장 초기화
 */
async function initializeSquare() {
    const squareArea = document.getElementById('squareArea');
    if (!squareArea) {
        console.error('squareArea 요소를 찾을 수 없습니다.');
        return;
    }
    
    squareArea.innerHTML = '';
    
    // 내 정령 표시
    await renderMySpirit();
    
    // 다른 유저들 로드
    await loadChannelUsers();
    
    // 채팅 메시지 로드
    await loadChatMessages();
    
    // 채팅 입력 활성화
    const chatInput = document.getElementById('chatInput');
    if (chatInput) {
        chatInput.disabled = false;
        chatInput.placeholder = '메시지를 입력하세요 (Enter: 전송, 빈 메시지 Enter: 이동 모드)';
        // 채팅 입력창 이벤트 설정
        setupChatInputEvents();
        chatInput.focus();
    }
    
    console.log('광장 초기화 완료');
}

/**
 * 내 정령 렌더링
 */
async function renderMySpirit() {
    try {
        const response = await fetch('/spirit/api/my-spirits', {
            credentials: 'include'
        });
        
        if (!response.ok) return;
        
        const spirits = await response.json();
        const mySpirit = spirits.find(s => s.id === currentSpiritId);
        if (!mySpirit) return;
        
        const squareArea = document.getElementById('squareArea');
        const mySpiritElement = createSpiritElement(mySpirit, true);
        mySpiritElement.id = 'mySpirit';
        mySpiritElement.style.position = 'absolute';
        mySpiritElement.style.left = myPosition.x + '%';
        mySpiritElement.style.top = myPosition.y + '%';
        squareArea.appendChild(mySpiritElement);
        
        // 초기 위치 저장
        previousPosition.x = myPosition.x;
        previousPosition.y = myPosition.y;
        
    } catch (error) {
        console.error('내 정령 렌더링 실패:', error);
    }
}

// 말풍선 관리
const speechBubbles = new Map(); // userId -> { element, timeout }
const SPEECH_BUBBLE_DURATION = 5000; // 5초 후 말풍선 사라짐

/**
 * 정령 요소 생성
 */
function createSpiritElement(spirit, isMine) {
    const container = document.createElement('div');
    container.className = 'spirit-walker' + (isMine ? ' my-spirit' : '');
    container.dataset.userId = spirit.userId || '';
    
    const step = (spirit.evolutionStage || 0) + 1;
    const typeCode = getTypeCode(spirit.spiritType);
    const imagePath = `/images/spirits/step${step}_${typeCode}.png`;
    
    container.innerHTML = `
        <div class="speech-bubble" style="display: none;">
            <div class="speech-bubble-content"></div>
        </div>
        <img src="${imagePath}" alt="${spirit.name}" class="spirit-image">
        <div class="spirit-nickname">${spirit.nickname || spirit.username || '이름없음'}</div>
    `;
    
    return container;
}

/**
 * 광장 유저들 로드
 */
async function loadChannelUsers() {
    try {
        const response = await fetch(`/spirit-square/api/channel-users?channelNumber=${PLAZA_CHANNEL}`);
        const data = await response.json();
        
        if (!data.success || !data.users) return;
        
        // 온라인 유저 수 업데이트
        const userCount = data.users ? data.users.length : 0;
        const currentUsersEl = document.getElementById('currentUsers');
        if (currentUsersEl) {
            currentUsersEl.textContent = `온라인: ${userCount}명`;
        }
        
        const squareArea = document.getElementById('squareArea');
        if (!squareArea) return;
        
        const myUserId = await getMyUserId();
        const existingUserIds = new Set();
        
        // 현재 서버에 있는 유저들의 presenceId 수집
        data.users.forEach(user => {
            if (user.id) existingUserIds.add(user.id.toString());
        });
        
        // 기존에 있던 유저 중 제거된 유저 삭제
        otherUsers.forEach((element, userId) => {
            const presenceId = element.dataset.presenceId;
            if (!presenceId || !existingUserIds.has(presenceId.toString())) {
                element.remove();
                otherUsers.delete(userId);
                speechBubbles.delete(userId);
            }
        });
        
        // 새로운 유저 추가 및 위치 업데이트
        data.users.forEach(user => {
            const userId = user.userId ? user.userId.toString() : null;
            if (!userId || userId === myUserId) return; // 내 정령은 제외
            
            let spiritElement = otherUsers.get(userId);
            
            if (!spiritElement || !squareArea.contains(spiritElement)) {
                // 새 유저 추가 또는 DOM에서 제거된 경우 재추가
                if (spiritElement && !squareArea.contains(spiritElement)) {
                    otherUsers.delete(userId);
                }
                
                spiritElement = createSpiritElement({
                    spiritType: user.spiritType,
                    evolutionStage: user.spiritEvolutionStage,
                    name: user.spiritName,
                    nickname: user.nickname,
                    username: user.username,
                    userId: userId
                }, false);
                
                spiritElement.style.position = 'absolute';
                spiritElement.dataset.presenceId = user.id;
                
                squareArea.appendChild(spiritElement);
                otherUsers.set(userId, spiritElement);
            }
            
            // 위치 업데이트 (부드러운 이동)
            const targetX = user.positionX || 50;
            const targetY = user.positionY || 50;
            
            // 이전 위치 가져오기 (방향 계산용)
            const currentX = parseFloat(spiritElement.style.left) || targetX;
            const dx = targetX - currentX;
            
            // 이동 방향에 따라 이미지 반전 (다른 유저의 정령)
            const imageElement = spiritElement.querySelector('.spirit-image');
            if (imageElement && Math.abs(dx) > 0.5) {
                if (dx > 0) {
                    imageElement.style.setProperty('--flip-direction', '1'); // 오른쪽
                } else {
                    imageElement.style.setProperty('--flip-direction', '-1'); // 왼쪽
                }
            }
            
            // 부드러운 이동 애니메이션
            if (spiritElement.style.left !== targetX + '%' || spiritElement.style.top !== targetY + '%') {
                spiritElement.style.transition = 'left 0.5s ease, top 0.5s ease';
                spiritElement.style.left = targetX + '%';
                spiritElement.style.top = targetY + '%';
            }
        });
        
    } catch (error) {
        console.error('광장 유저 로드 실패:', error);
    }
}

// 첫 로드 여부 추적
let isFirstMessageLoad = true;

/**
 * 채팅 메시지 로드
 */
async function loadChatMessages() {
    try {
        const response = await fetch(`/spirit-square/api/messages?channelNumber=${PLAZA_CHANNEL}`);
        const data = await response.json();
        
        if (!data.success || !data.messages) return;
        
        const chatMessages = document.getElementById('chatMessages');
        if (!chatMessages) return;
        
        // 첫 로드 시 모든 메시지 표시 (말풍선 없이)
        if (isFirstMessageLoad) {
            chatMessages.innerHTML = '';
            const allMessages = data.messages.reverse();
            allMessages.forEach(msg => {
                addChatMessage(msg, false); // 첫 로드 시 말풍선 표시 안 함
            });
            
            if (allMessages.length > 0) {
                const lastMsgTime = Math.max(...allMessages.map(msg => new Date(msg.createdAt).getTime()));
                lastLoadedMessageTime = lastMsgTime;
            }
            isFirstMessageLoad = false;
        } else {
            // 새 메시지만 필터링 (마지막 로드 시간 이후의 메시지)
            const newMessages = data.messages.filter(msg => {
                const msgTime = new Date(msg.createdAt).getTime();
                return msgTime > lastLoadedMessageTime;
            });
            
            // 새로운 메시지가 있으면 추가 (말풍선 표시)
            if (newMessages.length > 0) {
                newMessages.forEach(msg => {
                    addChatMessage(msg, true); // 새 메시지는 말풍선 표시
                });
                
                // 마지막 메시지 시간 업데이트
                const lastMsgTime = Math.max(...newMessages.map(msg => new Date(msg.createdAt).getTime()));
                lastLoadedMessageTime = lastMsgTime;
            }
        }
        
    } catch (error) {
        console.error('채팅 메시지 로드 실패:', error);
    }
}

/**
 * 채팅 메시지 추가
 * @param {Object} message - 메시지 객체
 * @param {boolean} showBubble - 말풍선 표시 여부 (기본: true)
 */
function addChatMessage(message, showBubble = true) {
    // 채팅 메시지 시간 업데이트
    lastChatMessageTime = Date.now();
    
    // 채팅창에 메시지 추가
    const chatMessages = document.getElementById('chatMessages');
    const messageElement = document.createElement('div');
    messageElement.className = 'chat-message';
    
    const time = new Date(message.createdAt).toLocaleTimeString('ko-KR', { 
        hour: '2-digit', 
        minute: '2-digit' 
    });
    
    messageElement.innerHTML = `
        <span class="message-time">${time}</span>
        <span class="message-nickname">${message.nickname || message.username}:</span>
        <span class="message-text">${escapeHtml(message.message)}</span>
    `;
    
    chatMessages.appendChild(messageElement);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    // 말풍선으로도 표시 (옵션)
    if (showBubble) {
        showSpeechBubble(message.userId, message.message, message.nickname || message.username);
    }
}

/**
 * HTML 이스케이프
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 말풍선 표시
 */
async function showSpeechBubble(userId, message, nickname) {
    const squareArea = document.getElementById('squareArea');
    if (!squareArea) return;
    
    // 해당 유저의 정령 요소 찾기
    let spiritElement = null;
    const myUserId = await getMyUserId();
    const userIdStr = userId ? userId.toString() : null;
    const myUserIdStr = myUserId ? myUserId.toString() : null;
    
    if (userIdStr === myUserIdStr) {
        // 내 정령
        spiritElement = document.getElementById('mySpirit');
    } else {
        // 다른 유저의 정령
        spiritElement = otherUsers.get(userIdStr);
        if (!spiritElement) {
            // 아직 로드되지 않았으면 다시 시도
            setTimeout(() => {
                showSpeechBubble(userId, message, nickname);
            }, 500);
            return;
        }
    }
    
    if (!spiritElement) return;
    
    // 기존 말풍선 제거
    const existingBubble = spiritElement.querySelector('.speech-bubble');
    if (existingBubble) {
        // 기존 타이머 제거
        const bubbleData = speechBubbles.get(userIdStr);
        if (bubbleData && bubbleData.timeout) {
            clearTimeout(bubbleData.timeout);
        }
        
        // 애니메이션과 함께 제거
        existingBubble.classList.remove('show');
        existingBubble.classList.add('hide');
        setTimeout(() => {
            existingBubble.remove();
        }, 300);
    }
    
    // 새로운 말풍선 생성
    const bubble = document.createElement('div');
    bubble.className = 'speech-bubble';
    
    // 글자 수에 따라 말풍선 크기 조정 (기본 비율 1:3)
    const messageLength = message.length;
    // 최소 너비 80px, 글자당 약 8px 추가 (기본 비율 고려)
    const estimatedWidth = Math.max(80, Math.min(300, messageLength * 8 + 40));
    // 높이는 너비의 1/3
    const estimatedHeight = estimatedWidth / 3;
    
    bubble.innerHTML = `<div class="speech-bubble-content">${escapeHtml(message)}</div>`;
    
    // 말풍선 크기 설정
    bubble.style.width = estimatedWidth + 'px';
    bubble.style.minHeight = estimatedHeight + 'px';
    
    spiritElement.insertBefore(bubble, spiritElement.firstChild);
    
    // 말풍선 표시
    setTimeout(() => {
        bubble.classList.add('show');
    }, 10);
    
    // 자동 숨김
    const timeout = setTimeout(() => {
        bubble.classList.remove('show');
        bubble.classList.add('hide');
        setTimeout(() => {
            if (bubble.parentElement) {
                bubble.remove();
            }
            speechBubbles.delete(userIdStr);
        }, 300);
    }, SPEECH_BUBBLE_DURATION);
    
    // 말풍선 데이터 저장
    speechBubbles.set(userIdStr, {
        element: bubble,
        timeout: timeout
    });
}

/**
 * 메시지 전송
 */
async function sendMessage() {
    // 정령 선택 전에는 채팅 불가
    if (!currentSpiritId) {
        alert('먼저 정령을 선택해주세요.');
        return;
    }
    
    if (!chatMode) {
        toggleChatMode();
        return;
    }
    
    const chatInput = document.getElementById('chatInput');
    if (!chatInput) {
        console.error('chatInput 요소를 찾을 수 없습니다.');
        return;
    }
    
    const message = chatInput.value.trim();
    
    if (!message) return;
    
    try {
        const response = await fetch('/spirit-square/api/send-message', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: new URLSearchParams({
                channelNumber: PLAZA_CHANNEL,
                message: message
            })
        });
        
        const data = await response.json();
        if (data.success) {
            chatInput.value = '';
            // 서버 응답으로 받은 메시지는 이미 말풍선이 표시되므로 채팅창에만 추가
            // (서버에서 broadcast되거나 loadChatMessages에서 다시 로드될 때 말풍선 표시)
            if (data.message) {
                addChatMessage(data.message, false); // 말풍선 표시 안 함 (중복 방지)
            }
            // WebSocket은 별도로 처리됨
        } else {
            alert(data.message || '메시지 전송에 실패했습니다.');
        }
    } catch (error) {
        console.error('메시지 전송 실패:', error);
        alert('메시지 전송 중 오류가 발생했습니다.');
    }
}

/**
 * 채팅 키 입력 처리
 */
function handleChatKeyPress(event) {
    if (event.key === 'Enter' || event.keyCode === 13) {
        event.preventDefault();
        event.stopPropagation();
        
        const chatInput = document.getElementById('chatInput');
        if (!chatInput) return;
        
        if (chatMode) {
            const message = chatInput.value.trim();
            if (message) {
                // 메시지가 있으면 전송
                sendMessage();
            } else {
                // 메시지가 없으면 이동 모드로 전환
                toggleChatMode();
            }
        }
        // 이동 모드에서는 채팅 입력창이 포커스되지 않으므로 처리 불필요
    }
}

// 전역 엔터키 핸들러 함수 (외부에서 참조 가능하도록)
let globalEnterKeyHandler = null;

/**
 * 전역 엔터키 이벤트 핸들러 (모드 전환용)
 */
function setupGlobalEnterKeyHandler() {
    // 기존 핸들러 제거
    if (globalEnterKeyHandler) {
        document.removeEventListener('keydown', globalEnterKeyHandler);
    }
    
    // 새로운 핸들러 생성
    globalEnterKeyHandler = function(event) {
        // 채팅 입력창이 포커스되어 있으면 처리하지 않음 (입력창 이벤트가 처리)
        const chatInput = document.getElementById('chatInput');
        if (chatInput && document.activeElement === chatInput) {
            return;
        }
        
        // 모달이 열려있으면 처리하지 않음
        const modal = document.getElementById('spiritSelectModal');
        if (modal && modal.style.display !== 'none') {
            return;
        }
        
        // 정령이 선택되지 않았으면 처리하지 않음
        if (!currentSpiritId) {
            return;
        }
        
        // 엔터키 처리
        if (event.key === 'Enter' || event.keyCode === 13) {
            event.preventDefault();
            event.stopPropagation();
            
            console.log('전역 엔터키 처리, chatMode:', chatMode);
            
            if (!chatMode) {
                // 이동 모드에서 엔터키: 채팅 모드로 전환
                console.log('이동 모드 -> 채팅 모드 전환');
                toggleChatMode();
            } else {
                // 채팅 모드에서는 입력창 이벤트가 처리하므로 여기서는 처리 안 함
            }
        }
    };
    
    // 전역 엔터키 핸들러 추가 (캡처 단계에서 처리)
    document.addEventListener('keydown', globalEnterKeyHandler, true);
}

/**
 * 채팅 입력창 키 이벤트 설정
 */
function setupChatInputEvents() {
    const chatInput = document.getElementById('chatInput');
    if (!chatInput) return;
    
    // 새로운 핸들러 함수 생성 (기존 것과 충돌 방지)
    const keyHandler = function(e) {
        if (e.key === 'Enter' || e.keyCode === 13) {
            e.preventDefault();
            e.stopPropagation();
            
            if (chatMode) {
                const message = chatInput.value.trim();
                if (message) {
                    sendMessage();
                } else {
                    // 빈 메시지에서 엔터: 이동 모드로 전환
                    toggleChatMode();
                }
            }
        }
    };
    
    // 기존 이벤트 리스너 제거
    if (chatInput._keyHandler) {
        chatInput.removeEventListener('keydown', chatInput._keyHandler);
    }
    
    // 새로운 핸들러 저장 및 추가
    chatInput._keyHandler = keyHandler;
    chatInput.addEventListener('keydown', keyHandler);
}

/**
 * 채팅 모드 토글
 */
function toggleChatMode() {
    chatMode = !chatMode;
    const chatInputContainer = document.getElementById('chatInputContainer');
    const toggleBtn = document.querySelector('.btn-toggle-chat');
    const squareArea = document.getElementById('squareArea');
    
    if (chatMode) {
        chatInputContainer.style.display = 'block';
        toggleBtn.textContent = '이동 모드';
        if (squareArea) {
            squareArea.style.cursor = 'default';
            squareArea.classList.remove('move-mode');
        }
        const chatInput = document.getElementById('chatInput');
        if (chatInput) {
            setupChatInputEvents(); // 이벤트 재설정
            chatInput.focus();
        }
    } else {
        chatInputContainer.style.display = 'none';
        toggleBtn.textContent = '채팅 모드';
        if (squareArea && currentSpiritId) {
            squareArea.style.cursor = 'crosshair';
            squareArea.classList.add('move-mode');
        }
    }
}

// 이전 위치 저장 (방향 계산용)
let previousPosition = { x: 50, y: 50 };

/**
 * 위치 업데이트
 */
async function updatePosition(x, y, isManual = true) {
    if (!currentSpiritId) return;
    
    const mySpirit = document.getElementById('mySpirit');
    if (!mySpirit) return;
    
    // 이동 방향 계산
    const dx = x - previousPosition.x;
    const dy = y - previousPosition.y;
    
    // 이미지 요소 가져오기
    const imageElement = mySpirit.querySelector('.spirit-image');
    if (imageElement) {
        // 이동 방향에 따라 이미지 반전
        // 오른쪽 이동: 정방향 (--flip-direction: 1)
        // 왼쪽 이동: 반전 (--flip-direction: -1)
        if (Math.abs(dx) > 0.5) { // 작은 이동은 무시
            if (dx > 0) {
                // 오른쪽으로 이동
                imageElement.style.setProperty('--flip-direction', '1');
            } else {
                // 왼쪽으로 이동
                imageElement.style.setProperty('--flip-direction', '-1');
            }
        }
    }
    
    // 수동 이동인 경우 자동 이동 중지 및 재시작
    if (isManual) {
        lastManualMoveTime = Date.now();
        autoMoveTarget = null; // 수동 이동 시 자동 이동 목표 취소
    }
    
    // 즉시 화면 업데이트 (부드러운 이동)
    myPosition.x = x;
    myPosition.y = y;
    previousPosition.x = x;
    previousPosition.y = y;
    
    if (mySpirit) {
        mySpirit.style.transition = 'left 0.3s ease, top 0.3s ease';
        mySpirit.style.left = x + '%';
        mySpirit.style.top = y + '%';
    }
    
    // WebSocket이 연결되어 있으면 WebSocket으로, 아니면 HTTP로 전송
    if (typeof sendPositionUpdate === 'function' && ws && ws.readyState === WebSocket.OPEN) {
        sendPositionUpdate(x, y);
    } else {
        // HTTP fallback (throttle)
        if (!updatePosition.lastUpdate || Date.now() - updatePosition.lastUpdate > 200) {
            updatePosition.lastUpdate = Date.now();
            
            try {
                await fetch('/spirit-square/api/update-position', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    credentials: 'include',
                    body: new URLSearchParams({
                        channelNumber: PLAZA_CHANNEL,
                        x: x,
                        y: y
                    })
                });
            } catch (error) {
                console.error('위치 업데이트 실패:', error);
            }
        }
    }
}

// 마지막으로 로드된 메시지 시간 (첫 로드 여부 확인용)
let lastLoadedMessageTime = 0;

/**
 * 주기적 업데이트 시작
 */
function startUpdateInterval() {
    if (updateInterval) {
        clearInterval(updateInterval);
    }
    
    updateInterval = setInterval(async () => {
        await loadChannelUsers();
        await loadChatMessages();
    }, 1000); // 1초마다 업데이트 (더 부드러운 실시간 느낌)
}

/**
 * 자동 이동 애니메이션 시작 (제자리에서 둥둥 떠다니기)
 */
function startAutoMoveAnimation() {
    if (autoMoveInterval) {
        clearInterval(autoMoveInterval);
    }
    
    autoMoveInterval = setInterval(() => {
        if (!currentSpiritId || chatMode) return;
        
        const now = Date.now();
        const timeSinceLastMove = now - lastManualMoveTime;
        
        // 3초 이상 가만히 있으면 자동 이동 시작
        if (timeSinceLastMove >= AUTO_MOVE_DELAY) {
            // 새로운 목표 위치 설정 (현재 위치 주변으로 둥둥 떠다니기)
            if (!autoMoveTarget || Math.abs(autoMoveTarget.x - myPosition.x) < 1 && 
                Math.abs(autoMoveTarget.y - myPosition.y) < 1) {
                // 현재 위치를 중심으로 작은 범위 내에서 랜덤 위치 선택
                const range = 10; // 현재 위치 기준 ±10% 범위
                const angle = Math.random() * Math.PI * 2;
                const distance = Math.random() * range;
                
                autoMoveTarget = {
                    x: Math.max(5, Math.min(95, myPosition.x + Math.cos(angle) * distance)),
                    y: Math.max(10, Math.min(90, myPosition.y + Math.sin(angle) * distance))
                };
            }
            
            // 목표 위치로 부드럽게 이동
            const dx = autoMoveTarget.x - myPosition.x;
            const dy = autoMoveTarget.y - myPosition.y;
            const distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 0.5) {
                // 천천히 이동 (속도: 0.5% per frame)
                const speed = 0.5;
                const moveX = myPosition.x + (dx / distance) * speed;
                const moveY = myPosition.y + (dy / distance) * speed;
                
                updatePosition(moveX, moveY, false); // 자동 이동
            }
        }
    }, 50); // 50ms마다 업데이트
}

// 자동 말풍선 표시 관리
let autoSpeechBubbleInterval = null;
let lastChatMessageTime = 0; // 마지막 채팅 메시지 시간

/**
 * 자동 말풍선 표시 시작 (채팅이 없을 때도 말풍선 표시)
 */
function startAutoSpeechBubbles() {
    // 기존 인터벌이 있으면 정리
    if (autoSpeechBubbleInterval) {
        clearInterval(autoSpeechBubbleInterval);
    }
    
    // 주기적으로 자동 말풍선 표시
    autoSpeechBubbleInterval = setInterval(() => {
        if (!currentSpiritId) return;
        
        // 최근 10초 이내에 채팅 메시지가 있으면 자동 말풍선 표시 안 함
        const timeSinceLastChat = Date.now() - lastChatMessageTime;
        if (timeSinceLastChat < 10000) {
            return;
        }
        
        // 랜덤 메시지 생성 (정령마을 스타일)
        const messages = [
            '안녕하세요!',
            '오늘 날씨 좋네요',
            '여기 정말 예쁘다',
            '다들 즐거우신가요?',
            '와! 사람들이 많네요',
            '무엇을 할까?',
            '재밌어요!',
            '좋아요~'
        ];
        
        // 30% 확률로 자동 말풍선 표시
        if (Math.random() < 0.3) {
            getMyUserId().then(myUserId => {
                if (myUserId) {
                    const randomMessage = messages[Math.floor(Math.random() * messages.length)];
                    showSpeechBubble(myUserId, randomMessage, '나');
                }
            });
        }
    }, 8000); // 8초마다 체크
}

/**
 * 광장 나가기
 */
async function exitSquare() {
    if (!confirm('정령 광장을 나가시겠습니까?')) {
        return;
    }
    
    // WebSocket 연결 종료
    if (typeof closeWebSocket === 'function') {
        closeWebSocket();
    }
    
    // 주기적 업데이트 중지
    if (updateInterval) {
        clearInterval(updateInterval);
        updateInterval = null;
    }
    
    // 자동 이동 애니메이션 중지
    if (autoMoveInterval) {
        clearInterval(autoMoveInterval);
        autoMoveInterval = null;
    }
    
    // 자동 말풍선 표시 중지
    if (autoSpeechBubbleInterval) {
        clearInterval(autoSpeechBubbleInterval);
        autoSpeechBubbleInterval = null;
    }
    
    // 광장에 입장한 상태라면 서버에 알림
    if (currentSpiritId) {
        try {
            await fetch('/spirit-square/api/exit-channel', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                credentials: 'include',
                body: new URLSearchParams({
                    channelNumber: PLAZA_CHANNEL
                })
            });
        } catch (error) {
            console.error('광장 퇴장 처리 실패:', error);
        }
        
        if (updateInterval) {
            clearInterval(updateInterval);
        }
    }
    
    // 월드로 이동
    window.location.href = '/world';
}

/**
 * 광장 클릭 핸들러 설정
 */
function setupSquareClickHandler() {
    const squareArea = document.getElementById('squareArea');
    if (!squareArea) return;
    
    squareArea.addEventListener('click', function(e) {
        // 정령 클릭은 제외
        if (e.target.closest('.spirit-walker')) {
            return;
        }
        
        if (!chatMode && currentSpiritId) {
            const rect = squareArea.getBoundingClientRect();
            const x = ((e.clientX - rect.left) / rect.width) * 100;
            const y = ((e.clientY - rect.top) / rect.height) * 100;
            
            // 위치 제한 (0-100%)
            const clampedX = Math.max(5, Math.min(95, x));
            const clampedY = Math.max(10, Math.min(90, y));
            
            updatePosition(clampedX, clampedY, true);
        }
    });
}

/**
 * 키보드 이동 컨트롤 설정
 */
let pressedKeys = new Set();
let moveSpeed = 2; // 이동 속도 (퍼센트)
let moveInterval = null;

function setupKeyboardControls() {
    // 키 다운 이벤트
    document.addEventListener('keydown', function(e) {
        // 엔터키는 전역 핸들러에서 처리
        if (e.key === 'Enter' || e.keyCode === 13) {
            return;
        }
        
        // 채팅 입력 중이면 이동 키 무시
        const chatInput = document.getElementById('chatInput');
        if (chatInput && document.activeElement === chatInput) {
            return;
        }
        
        // 모달이 열려있으면 이동 키 무시
        const modal = document.getElementById('spiritSelectModal');
        if (modal && modal.style.display !== 'none') {
            return;
        }
        
        // 이동 모드가 아니면 이동 키 무시
        if (chatMode || !currentSpiritId) {
            return;
        }
        
        const key = e.key.toLowerCase();
        
        // WASD 또는 화살표 키
        if (key === 'w' || key === 'arrowup' || 
            key === 's' || key === 'arrowdown' || 
            key === 'a' || key === 'arrowleft' || 
            key === 'd' || key === 'arrowright') {
            
            e.preventDefault();
            
            if (!pressedKeys.has(key)) {
                pressedKeys.add(key);
                
                // 연속 이동 시작
                if (!moveInterval) {
                    moveInterval = setInterval(() => {
                        moveWithKeyboard();
                    }, 50); // 50ms마다 이동 (부드러운 이동)
                }
                
                // 즉시 한 번 이동
                moveWithKeyboard();
            }
        }
    });
    
    // 키 업 이벤트
    document.addEventListener('keyup', function(e) {
        const key = e.key.toLowerCase();
        
        if (key === 'w' || key === 'arrowup' || 
            key === 's' || key === 'arrowdown' || 
            key === 'a' || key === 'arrowleft' || 
            key === 'd' || key === 'arrowright') {
            
            pressedKeys.delete(key);
            
            // 누르고 있는 키가 없으면 이동 중지
            if (pressedKeys.size === 0 && moveInterval) {
                clearInterval(moveInterval);
                moveInterval = null;
            }
        }
    });
    
    // 페이지 떠날 때 정리
    window.addEventListener('beforeunload', function() {
        if (moveInterval) {
            clearInterval(moveInterval);
            moveInterval = null;
        }
    });
}

/**
 * 키보드로 이동
 */
function moveWithKeyboard() {
    if (!currentSpiritId || chatMode || pressedKeys.size === 0) {
        return;
    }
    
    let deltaX = 0;
    let deltaY = 0;
    
    // 눌린 키에 따라 이동 방향 결정
    pressedKeys.forEach(key => {
        switch(key) {
            case 'w':
            case 'arrowup':
                deltaY -= moveSpeed;
                break;
            case 's':
            case 'arrowdown':
                deltaY += moveSpeed;
                break;
            case 'a':
            case 'arrowleft':
                deltaX -= moveSpeed;
                break;
            case 'd':
            case 'arrowright':
                deltaX += moveSpeed;
                break;
        }
    });
    
    // 새로운 위치 계산
    let newX = myPosition.x + deltaX;
    let newY = myPosition.y + deltaY;
    
    // 위치 제한 (5-95%, 10-90%)
    newX = Math.max(5, Math.min(95, newX));
    newY = Math.max(10, Math.min(90, newY));
    
    // 이동 방향에 따라 이미지 방향 변경 (키보드 이동 시에도)
    const mySpirit = document.getElementById('mySpirit');
    if (mySpirit && deltaX !== 0) {
        const imageElement = mySpirit.querySelector('.spirit-image');
        if (imageElement) {
            if (deltaX > 0) {
                imageElement.style.setProperty('--flip-direction', '1'); // 오른쪽
            } else {
                imageElement.style.setProperty('--flip-direction', '-1'); // 왼쪽
            }
        }
    }
    
    // 위치 업데이트
    updatePosition(newX, newY, true);
}

/**
 * 내 유저 ID 가져오기
 */
let cachedUserId = null;
async function getMyUserId() {
    if (cachedUserId) return cachedUserId;
    
    try {
        const response = await fetch('/spirit/api/my-spirits', {
            credentials: 'include'
        });
        if (response.ok) {
            const spirits = await response.json();
            if (spirits && spirits.length > 0 && spirits[0].userId) {
                cachedUserId = spirits[0].userId.toString();
                return cachedUserId;
            }
        }
    } catch (error) {
        console.error('유저 ID 조회 실패:', error);
    }
    return null;
}

