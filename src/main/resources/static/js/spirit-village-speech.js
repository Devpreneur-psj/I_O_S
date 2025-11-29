// 정령 말풍선 시스템

// 말풍선 관리
const speechBubbles = new Map(); // spiritId -> { element, walker, lastMessage, showTimer, hideTimer }
const SPEECH_BUBBLE_SHOW_INTERVAL = 8000; // 8초마다 말풍선 표시
const SPEECH_BUBBLE_HIDE_DELAY = 10000; // 10초 후 자동 숨김

/**
 * 말풍선 시스템 초기화
 */
function initSpeechBubbles() {
    const walkers = document.querySelectorAll('.spirit-walker');
    
    walkers.forEach(walker => {
        const spiritId = walker.getAttribute('data-spirit-id');
        const bubble = walker.querySelector('.spirit-speech-bubble');
        
        if (!bubble || !spiritId) return;
        
        // 말풍선 클릭 시 닫기
        bubble.addEventListener('click', function(e) {
            e.stopPropagation(); // 정령 모달이 열리지 않도록
            hideSpeechBubble(spiritId);
        });
        
        // 말풍선 데이터 저장
        speechBubbles.set(spiritId, {
            element: bubble,
            walker: walker,
            lastMessage: '',
            showTimer: null,
            hideTimer: null
        });
    });
    
    // 주기적으로 말풍선 표시
    startSpeechBubbleCycle();
}

/**
 * 말풍선 주기적 표시 시작
 */
function startSpeechBubbleCycle() {
    setInterval(() => {
        speechBubbles.forEach((bubbleData, spiritId) => {
            // 정령 타입과 진화 단계 확인
            const spiritType = bubbleData.walker.getAttribute('data-spirit-type');
            const evolutionStage = parseInt(bubbleData.walker.getAttribute('data-evolution-stage') || '0');
            
            // 빛의 정령 또는 어둠의 정령이 1차 진화(evolutionStage == 1) 상태면 말풍선 표시 안 함
            const isInactive = (spiritType === '빛의 정령' || spiritType === '어둠의 정령') && evolutionStage === 1;
            
            if (isInactive) {
                return; // 행동 불가 상태면 말풍선 표시 안 함
            }
            
            // 현재 표시 중이 아니면 새로운 메시지 표시
            if (bubbleData.element.style.display === 'none' || !bubbleData.element.classList.contains('show')) {
                const message = generateSpeechMessage(bubbleData.walker);
                if (message) {
                    showSpeechBubble(spiritId, message);
                }
            }
        });
    }, SPEECH_BUBBLE_SHOW_INTERVAL);
}

/**
 * 정령 상태에 따른 말풍선 메시지 생성
 */
function generateSpeechMessage(walker) {
    const personality = walker.getAttribute('data-personality') || '온순';
    const happiness = parseInt(walker.getAttribute('data-happiness') || '50');
    const hunger = parseInt(walker.getAttribute('data-hunger') || '50');
    const energy = parseInt(walker.getAttribute('data-energy') || '100');
    const healthStatus = walker.getAttribute('data-health-status') || '건강';
    const mood = walker.getAttribute('data-mood') || '보통';
    
    // 상태 우선순위: 건강 > 배고픔 > 에너지 > 행복도 > 기분
    let messages = [];
    
    // 건강 상태
    if (healthStatus !== '건강') {
        messages = getHealthMessages(personality, healthStatus);
    }
    // 배고픔 상태 (80 이상이면 배고픔)
    else if (hunger >= 80) {
        messages = getHungerMessages(personality, hunger);
    }
    // 에너지 부족 (30 이하)
    else if (energy <= 30) {
        messages = getEnergyMessages(personality, energy);
    }
    // 행복도 낮음 (30 이하)
    else if (happiness <= 30) {
        messages = getHappinessMessages(personality, happiness);
    }
    // 행복도 높음 (80 이상)
    else if (happiness >= 80) {
        messages = getHappyMessages(personality);
    }
    // 기분 상태
    else if (mood === '나쁨') {
        messages = getBadMoodMessages(personality);
    }
    // 일반 대화
    else {
        messages = getRandomMessages(personality);
    }
    
    // 랜덤하게 하나 선택
    if (messages.length > 0) {
        return messages[Math.floor(Math.random() * messages.length)];
    }
    
    return null;
}

/**
 * 성격별 건강 메시지
 */
function getHealthMessages(personality, healthStatus) {
    const messages = {
        '고집': [
            '아파도 괜찮아... 하지만 좀 아프긴 해.',
            '건강이 안 좋은데, 뭔가 해줄 수 없을까?',
            '아픈 게 싫어... 빨리 나았으면 좋겠어.'
        ],
        '조심': [
            '건강이 안 좋아 보여... 괜찮을까?',
            '조금 아픈 것 같아. 치료가 필요할 수도...',
            '상태가 좋지 않네. 조심해야 할 것 같아.'
        ],
        '장난꾸러기': [
            '아파도 장난은 계속할 거야!',
            '건강이 안 좋지만... 놀고 싶어!',
            '아픈 게 싫어! 빨리 나아서 놀고 싶어!'
        ],
        '온순': [
            '조금 아픈 것 같아요... 괜찮을까요?',
            '건강이 안 좋아 보여요. 치료가 필요할 수도 있어요.',
            '아픈 게 싫어요... 나았으면 좋겠어요.'
        ],
        '용감': [
            '아파도 괜찮아! 하지만 좀 힘들긴 해.',
            '건강이 안 좋지만 견딜 수 있어!',
            '아픈 게 싫어... 빨리 나아서 다시 강해지고 싶어!'
        ]
    };
    
    return messages[personality] || messages['온순'];
}

/**
 * 성격별 배고픔 메시지
 */
function getHungerMessages(personality, hunger) {
    const messages = {
        '고집': [
            '배고파! 빨리 먹을 거 줘!',
            '배고픔이 심해... 음식이 필요해!',
            '배가 고파서 힘들어. 먹을 거 있어?'
        ],
        '조심': [
            '배가 좀 고픈 것 같아요...',
            '음식이 필요할 수도 있어요.',
            '배고픔이 느껴져요. 뭔가 먹고 싶어요.'
        ],
        '장난꾸러기': [
            '배고파! 먹을 거 주면 장난 안 칠게!',
            '배고픔이 심해! 빨리 먹고 싶어!',
            '배가 고파서 장난칠 힘도 없어!'
        ],
        '온순': [
            '배가 좀 고픈 것 같아요...',
            '음식을 먹고 싶어요.',
            '배고픔이 느껴져요. 뭔가 주실 수 있나요?'
        ],
        '용감': [
            '배고파! 하지만 견딜 수 있어!',
            '음식이 필요해! 빨리 주면 좋겠어!',
            '배가 고파서 힘들지만 버틸 수 있어!'
        ]
    };
    
    return messages[personality] || messages['온순'];
}

/**
 * 성격별 에너지 메시지
 */
function getEnergyMessages(personality, energy) {
    const messages = {
        '고집': [
            '너무 피곤해... 좀 쉬고 싶어.',
            '에너지가 부족해. 휴식이 필요해.',
            '힘이 없어... 좀 쉴 수 있을까?'
        ],
        '조심': [
            '조금 피곤한 것 같아요...',
            '에너지가 부족해 보여요. 휴식이 필요할 수도...',
            '힘이 없어요. 좀 쉬고 싶어요.'
        ],
        '장난꾸러기': [
            '피곤해도 장난은 계속할 거야!',
            '에너지가 부족하지만... 놀고 싶어!',
            '힘이 없어서 장난칠 힘도 없어!'
        ],
        '온순': [
            '조금 피곤한 것 같아요...',
            '에너지가 부족해 보여요. 휴식이 필요해요.',
            '힘이 없어요. 좀 쉬고 싶어요.'
        ],
        '용감': [
            '피곤하지만 견딜 수 있어!',
            '에너지가 부족하지만 괜찮아!',
            '힘이 없지만 버틸 수 있어!'
        ]
    };
    
    return messages[personality] || messages['온순'];
}

/**
 * 성격별 행복도 낮음 메시지
 */
function getHappinessMessages(personality, happiness) {
    const messages = {
        '고집': [
            '기분이 안 좋아... 뭔가 해줄 수 없을까?',
            '행복하지 않아. 좀 더 관심을 가져줘.',
            '외로워... 같이 있어줄 수 있을까?'
        ],
        '조심': [
            '기분이 좀 안 좋은 것 같아요...',
            '행복하지 않아 보여요. 뭔가 필요할 수도...',
            '외로움을 느껴요. 같이 있어주실 수 있나요?'
        ],
        '장난꾸러기': [
            '기분이 안 좋아서 장난칠 마음도 없어!',
            '행복하지 않아... 놀아주면 좋겠어!',
            '외로워... 같이 놀고 싶어!'
        ],
        '온순': [
            '기분이 좀 안 좋은 것 같아요...',
            '행복하지 않아 보여요. 관심을 가져주시면 좋겠어요.',
            '외로움을 느껴요. 같이 있어주실 수 있나요?'
        ],
        '용감': [
            '기분이 안 좋지만 견딜 수 있어!',
            '행복하지 않지만 괜찮아!',
            '외로워도 버틸 수 있어!'
        ]
    };
    
    return messages[personality] || messages['온순'];
}

/**
 * 성격별 행복도 높음 메시지
 */
function getHappyMessages(personality) {
    const messages = {
        '고집': [
            '기분이 정말 좋아!',
            '오늘은 행복해!',
            '너무 좋아! 계속 이렇게 있었으면 좋겠어!'
        ],
        '조심': [
            '기분이 좋은 것 같아요...',
            '오늘은 행복해 보여요.',
            '너무 좋아요! 계속 이렇게 있었으면 좋겠어요.'
        ],
        '장난꾸러기': [
            '기분이 정말 좋아! 장난칠 기분이야!',
            '오늘은 행복해! 같이 놀고 싶어!',
            '너무 좋아! 계속 놀고 싶어!'
        ],
        '온순': [
            '기분이 좋은 것 같아요...',
            '오늘은 행복해 보여요.',
            '너무 좋아요! 계속 이렇게 있었으면 좋겠어요.'
        ],
        '용감': [
            '기분이 정말 좋아! 오늘도 강해질 거야!',
            '오늘은 행복해! 더 강해지고 싶어!',
            '너무 좋아! 계속 강해지고 싶어!'
        ]
    };
    
    return messages[personality] || messages['온순'];
}

/**
 * 성격별 기분 나쁨 메시지
 */
function getBadMoodMessages(personality) {
    const messages = {
        '고집': [
            '기분이 안 좋아...',
            '오늘은 별로야.',
            '뭔가 불만이 있어...'
        ],
        '조심': [
            '기분이 좀 안 좋은 것 같아요...',
            '오늘은 별로인 것 같아요.',
            '뭔가 불만이 있는 것 같아요...'
        ],
        '장난꾸러기': [
            '기분이 안 좋아서 장난칠 마음도 없어!',
            '오늘은 별로야... 놀고 싶지 않아!',
            '뭔가 불만이 있어...'
        ],
        '온순': [
            '기분이 좀 안 좋은 것 같아요...',
            '오늘은 별로인 것 같아요.',
            '뭔가 불만이 있는 것 같아요...'
        ],
        '용감': [
            '기분이 안 좋지만 견딜 수 있어!',
            '오늘은 별로지만 괜찮아!',
            '뭔가 불만이 있지만 버틸 수 있어!'
        ]
    };
    
    return messages[personality] || messages['온순'];
}

/**
 * 성격별 일반 대화 메시지
 */
function getRandomMessages(personality) {
    const messages = {
        '고집': [
            '오늘도 힘내자!',
            '뭔가 하고 싶어!',
            '좀 더 강해지고 싶어!'
        ],
        '조심': [
            '오늘도 조심해야겠어요...',
            '뭔가 조심스러워요.',
            '안전하게 지내고 싶어요.'
        ],
        '장난꾸러기': [
            '오늘도 장난칠 거야!',
            '놀고 싶어!',
            '재미있는 일이 있을까?'
        ],
        '온순': [
            '오늘도 평화로워요.',
            '좋은 하루예요.',
            '편안하게 지내고 싶어요.'
        ],
        '용감': [
            '오늘도 강해질 거야!',
            '뭔가 도전하고 싶어!',
            '더 강해지고 싶어!'
        ]
    };
    
    return messages[personality] || messages['온순'];
}

/**
 * 말풍선 표시
 */
function showSpeechBubble(spiritId, message) {
    const bubbleData = speechBubbles.get(spiritId);
    if (!bubbleData) return;
    
    // 기존 타이머 제거
    if (bubbleData.hideTimer) {
        clearTimeout(bubbleData.hideTimer);
    }
    
    // 메시지 설정
    const content = bubbleData.element.querySelector('.speech-bubble-content');
    if (content) {
        content.textContent = message;
        bubbleData.lastMessage = message;
    }
    
    // 표시
    bubbleData.element.style.display = 'block';
    bubbleData.element.classList.add('show');
    
    // 자동 숨김 타이머
    bubbleData.hideTimer = setTimeout(() => {
        hideSpeechBubble(spiritId);
    }, SPEECH_BUBBLE_HIDE_DELAY);
}

/**
 * 말풍선 숨김
 */
function hideSpeechBubble(spiritId) {
    const bubbleData = speechBubbles.get(spiritId);
    if (!bubbleData) return;
    
    // 타이머 제거
    if (bubbleData.hideTimer) {
        clearTimeout(bubbleData.hideTimer);
        bubbleData.hideTimer = null;
    }
    
    // 숨김
    bubbleData.element.classList.remove('show');
    setTimeout(() => {
        bubbleData.element.style.display = 'none';
    }, 300); // 애니메이션 시간
}

