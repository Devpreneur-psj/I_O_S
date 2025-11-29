// 정령의 마을 JavaScript

// 정령 이동 애니메이션 관리
const spiritWalkers = [];
let animationFrameId = null;

// 말풍선 시스템
// speechBubbles, SPEECH_BUBBLE_SHOW_INTERVAL, SPEECH_BUBBLE_HIDE_DELAY는 spirit-village-speech.js에서 정의됨

// 드래그 앤 드롭 시스템
let draggedSpirit = null;
let dragOffset = { x: 0, y: 0 };
let isDragging = false;

/**
 * 학습 중이거나 진화 중인 정령 숨기기 (병렬 처리)
 */
async function hideBusySpirits() {
    if (!spiritsData || spiritsData.length === 0) {
        console.log('spiritsData가 없습니다.');
        return;
    }
    
    console.log('정령 필터링 시작:', spiritsData.length + '마리');
    
    // 먼저 진화 중인 정령 숨기기 (동기 처리)
    spiritsData.forEach(spirit => {
        const walkerElement = document.querySelector(`.spirit-walker[data-spirit-id="${spirit.id}"]`);
        if (!walkerElement) {
            return;
        }
        
        // 진화 중인 정령 숨기기
        if (spirit.evolutionInProgress === true) {
            console.log('정령 ID ' + spirit.id + ' 숨김: 진화 중');
            walkerElement.style.display = 'none';
        } else {
            // 일단 표시 (학습 중인지 확인 후 숨길 수 있음)
            walkerElement.style.display = '';
        }
    });
    
    // 학습 중인 정령 확인 (병렬 API 호출)
    const spiritsToCheck = spiritsData.filter(spirit => {
        // 이미 진화 중으로 숨겨진 정령은 제외
        return !(spirit.evolutionInProgress === true);
    });
    
    if (spiritsToCheck.length === 0) {
        console.log('확인할 정령이 없습니다.');
        return;
    }
    
    const learningChecks = spiritsToCheck.map(async (spirit) => {
        const walkerElement = document.querySelector(`.spirit-walker[data-spirit-id="${spirit.id}"]`);
        if (!walkerElement) {
            return { spiritId: spirit.id, isLearning: false };
        }
        
        try {
            const response = await fetch(`/spirit/api/skills/${spirit.id}`, {
                credentials: 'include'
            });
            if (response.ok) {
                const skills = await response.json();
                if (skills && skills.length > 0) {
                    const isLearning = skills.some(ss => ss.isLearning === true || ss.isLearning === 'true');
                    return { spiritId: spirit.id, isLearning: isLearning, element: walkerElement };
                }
            }
        } catch (error) {
            console.error('정령 ID ' + spirit.id + ' 학습 정보 조회 실패:', error);
        }
        
        return { spiritId: spirit.id, isLearning: false, element: walkerElement };
    });
    
    // 모든 API 호출 완료 대기 (병렬 처리)
    const results = await Promise.all(learningChecks);
    
    // 결과에 따라 정령 숨기기/표시
    results.forEach(result => {
        if (result.element) {
            if (result.isLearning) {
                console.log('정령 ID ' + result.spiritId + ' 숨김: 학습 중');
                result.element.style.display = 'none';
            } else {
                // 학습 완료된 정령 표시
                result.element.style.display = '';
            }
        }
    });
    
    console.log('정령 필터링 완료');
}

/**
 * 배경 이미지에 어울리는 이동 가능 영역 정의
 * 배경 이미지의 특정 영역(길, 풀밭 등)에서만 정령들이 이동하도록 제한
 */
function getWalkingZones(areaWidth, areaHeight) {
    // 화면 비율에 맞춰 이동 가능 영역 정의 (퍼센트 기준)
    const zones = [
        // 왼쪽 하단 - 나무와 모닥불 근처 풀밭
        {
            minX: 0.05, maxX: 0.35,
            minY: 0.55, maxY: 0.90,
            weight: 1.0
        },
        // 중앙 - 길 주변
        {
            minX: 0.30, maxX: 0.70,
            minY: 0.50, maxY: 0.85,
            weight: 1.5 // 길 주변이 더 자연스러움
        },
        // 오른쪽 - 연못 근처
        {
            minX: 0.60, maxX: 0.85,
            minY: 0.50, maxY: 0.80,
            weight: 1.0
        },
        // 중앙 상단 - 언덕 근처
        {
            minX: 0.40, maxX: 0.70,
            minY: 0.30, maxY: 0.60,
            weight: 0.8
        },
        // 왼쪽 상단 - 나무 근처
        {
            minX: 0.10, maxX: 0.40,
            minY: 0.25, maxY: 0.55,
            weight: 0.7
        }
    ];
    
    return zones.map(zone => ({
        minX: zone.minX * areaWidth,
        maxX: zone.maxX * areaWidth,
        minY: zone.minY * areaHeight,
        maxY: zone.maxY * areaHeight,
        weight: zone.weight
    }));
}

/**
 * 랜덤하게 이동 가능한 영역 선택
 */
function getRandomZone(zones) {
    // 가중치에 따라 영역 선택
    const totalWeight = zones.reduce((sum, zone) => sum + zone.weight, 0);
    let random = Math.random() * totalWeight;
    
    for (const zone of zones) {
        random -= zone.weight;
        if (random <= 0) {
            return zone;
        }
    }
    return zones[0]; // 기본값
}

/**
 * 영역 내 랜덤 위치 생성
 */
function getRandomPositionInZone(zone, size) {
    return {
        x: zone.minX + Math.random() * (zone.maxX - zone.minX - size),
        y: zone.minY + Math.random() * (zone.maxY - zone.minY - size)
    };
}

/**
 * 정령들이 배경 위를 돌아다니도록 초기화
 */
async function initSpiritWalkers() {
    // 기존 애니메이션 중지
    if (animationFrameId !== null) {
        cancelAnimationFrame(animationFrameId);
        animationFrameId = null;
    }
    
    // 기존 정령 데이터 초기화
    spiritWalkers.length = 0;
    
    // 학습 중이거나 진화 중인 정령 숨기기 (먼저 완료)
    try {
        await hideBusySpirits();
    } catch (error) {
        console.error('정령 필터링 중 오류:', error);
    }
    
    // 숨겨지지 않은 정령만 선택
    const walkers = document.querySelectorAll('.spirit-walker:not([style*="display: none"])');
    const walkingArea = document.querySelector('.spirit-walking-area');
    
    if (!walkingArea || walkers.length === 0) {
        console.log('정령 또는 이동 영역을 찾을 수 없습니다.');
        return;
    }
    
    const areaWidth = walkingArea.offsetWidth;
    const areaHeight = walkingArea.offsetHeight;
    
    console.log(`정령 초기화: ${walkers.length}마리, 영역 크기: ${areaWidth}x${areaHeight}`);
    
    // 이동 가능 영역 정의
    const walkingZones = getWalkingZones(areaWidth, areaHeight);
    
    walkers.forEach((walker, index) => {
        // 정령 타입과 진화 단계 확인
        const spiritType = walker.getAttribute('data-spirit-type');
        const evolutionStage = parseInt(walker.getAttribute('data-evolution-stage') || '0');
        
        // 빛의 정령 또는 어둠의 정령이 1차 진화(evolutionStage == 1) 상태면 행동 불가
        const isInactive = (spiritType === '빛의 정령' || spiritType === '어둠의 정령') && evolutionStage === 1;
        
        // 초기 영역 선택
        const initialZone = getRandomZone(walkingZones);
        const initialPos = getRandomPositionInZone(initialZone, 100);
        
        // 목표 영역 선택 (같은 영역 또는 인접 영역)
        const targetZone = Math.random() < 0.7 ? initialZone : getRandomZone(walkingZones);
        const targetPos = getRandomPositionInZone(targetZone, 100);
        
        const spiritId = parseInt(walker.getAttribute('data-spirit-id'));
        const personality = walker.getAttribute('data-personality') || '온순';
        
        const spiritData = {
            element: walker,
            spiritId: spiritId,
            personality: personality,
            x: initialPos.x,
            y: initialPos.y,
            targetX: targetPos.x,
            targetY: targetPos.y,
            speed: 0.2 + Math.random() * 0.3, // 이동 속도 (0.2 ~ 0.5) - 더 천천히
            currentZone: initialZone,
            targetZone: targetZone,
            walkingZones: walkingZones,
            changeDirectionTimer: 0,
            changeDirectionInterval: 3000 + Math.random() * 4000, // 3~7초마다 방향 변경
            size: 100,
            isInactive: isInactive, // 행동 불가 상태 플래그
            // 상호작용 관련
            interactionState: 'none', // none, talking, playing, fighting, falling
            interactionTimer: 0,
            interactionTarget: null,
            fallTimer: 0,
            isFalling: false,
            // 드래그 관련
            isDragged: false
        };
        
        // 초기 위치 설정 (position: absolute 명시적 설정)
        walker.style.position = 'absolute';
        walker.style.left = spiritData.x + 'px';
        walker.style.top = spiritData.y + 'px';
        walker.style.margin = '0';
        walker.style.padding = '0';
        
        // 고치 상태면 시각적 표시 (클릭은 가능하도록 유지)
        if (isInactive) {
            walker.classList.add('spirit-cocoon');
            walker.style.opacity = '0.8';
            // 클릭은 가능하도록 pointerEvents 유지 (상태창을 볼 수 있어야 함)
        }
        
        // 드래그 앤 드롭 이벤트 추가
        setupDragAndDrop(spiritData);
        
        spiritWalkers.push(spiritData);
    });
    
    // 애니메이션 시작
    if (spiritWalkers.length > 0) {
        console.log(`${spiritWalkers.length}마리 정령 애니메이션 시작`);
        // 기존 애니메이션 중지 후 새로 시작
        if (animationFrameId !== null) {
            cancelAnimationFrame(animationFrameId);
            animationFrameId = null;
        }
        // 애니메이션 즉시 시작
        console.log('animateSpirits() 호출');
        animateSpirits();
    } else {
        console.warn('정령이 없어 애니메이션을 시작할 수 없습니다.');
    }
    
    // 전역 마우스 이벤트 리스너 추가 (드래그 중 마우스가 정령 밖으로 나갔을 때 처리)
    setupGlobalDragListeners();
}

/**
 * 드래그 앤 드롭 이벤트 설정
 */
function setupDragAndDrop(spiritData) {
    const element = spiritData.element;
    
    // 마우스 다운 이벤트
    element.addEventListener('mousedown', (e) => {
        // 고치 상태(비활성)는 드래그 불가
        if (spiritData.isInactive) {
            return;
        }
        
        // 상호작용 중이면 드래그 불가
        if (spiritData.interactionState !== 'none' || spiritData.isFalling) {
            return;
        }
        
        e.preventDefault();
        e.stopPropagation();
        
        // 드래그 시작
        isDragging = true;
        draggedSpirit = spiritData;
        spiritData.isDragged = true;
        
        // 드래그 오프셋 계산 (마우스 위치와 정령 위치의 차이)
        const rect = element.getBoundingClientRect();
        const walkingArea = document.querySelector('.spirit-walking-area');
        const areaRect = walkingArea.getBoundingClientRect();
        
        dragOffset.x = e.clientX - (rect.left + rect.width / 2) - areaRect.left;
        dragOffset.y = e.clientY - (rect.top + rect.height / 2) - areaRect.top;
        
        // 드래그 중 시각적 효과
        element.style.cursor = 'grabbing';
        element.style.zIndex = '1000';
        element.style.opacity = '0.8';
        element.style.transform = 'scale(1.1)';
        
        // 상호작용 종료 (드래그 중에는 상호작용 불가)
        if (spiritData.interactionState !== 'none') {
            endInteraction(spiritData);
        }
    });
}

/**
 * 전역 마우스 이벤트 리스너 설정
 */
function setupGlobalDragListeners() {
    const walkingArea = document.querySelector('.spirit-walking-area');
    if (!walkingArea) {
        return;
    }
    
    // 마우스 이동 이벤트
    walkingArea.addEventListener('mousemove', (e) => {
        if (!isDragging || !draggedSpirit) {
            return;
        }
        
        e.preventDefault();
        
        // 마우스 위치를 walking area 기준으로 변환
        const areaRect = walkingArea.getBoundingClientRect();
        const mouseX = e.clientX - areaRect.left - dragOffset.x;
        const mouseY = e.clientY - areaRect.top - dragOffset.y;
        
        // 영역 경계 체크
        const clampedX = Math.max(0, Math.min(areaRect.width - draggedSpirit.size, mouseX));
        const clampedY = Math.max(0, Math.min(areaRect.height - draggedSpirit.size, mouseY));
        
        // 정령 위치 업데이트
        draggedSpirit.x = clampedX;
        draggedSpirit.y = clampedY;
        draggedSpirit.element.style.left = draggedSpirit.x + 'px';
        draggedSpirit.element.style.top = draggedSpirit.y + 'px';
    });
    
    // 마우스 업 이벤트
    document.addEventListener('mouseup', (e) => {
        if (!isDragging || !draggedSpirit) {
            return;
        }
        
        e.preventDefault();
        
        // 드래그 종료
        const spirit = draggedSpirit;
        spirit.isDragged = false;
        spirit.element.style.cursor = 'pointer';
        spirit.element.style.zIndex = '100';
        spirit.element.style.opacity = '';
        spirit.element.style.transform = '';
        
        // 새로운 목표 위치 설정 (현재 위치에서 시작)
        const walkingArea = document.querySelector('.spirit-walking-area');
        if (walkingArea) {
            const areaWidth = walkingArea.offsetWidth;
            const areaHeight = walkingArea.offsetHeight;
            const walkingZones = getWalkingZones(areaWidth, areaHeight);
            
            // 현재 위치가 속한 영역 찾기
            let currentZone = walkingZones[0];
            for (const zone of walkingZones) {
                if (spirit.x >= zone.minX && spirit.x <= zone.maxX &&
                    spirit.y >= zone.minY && spirit.y <= zone.maxY) {
                    currentZone = zone;
                    break;
                }
            }
            
            spirit.currentZone = currentZone;
            const targetPos = getRandomPositionInZone(currentZone, spirit.size);
            spirit.targetX = targetPos.x;
            spirit.targetY = targetPos.y;
            
            // 방향 변경 타이머 리셋
            spirit.changeDirectionTimer = 0;
            spirit.changeDirectionInterval = 3000 + Math.random() * 4000;
        }
        
        // 드래그 상태 초기화
        draggedSpirit = null;
        isDragging = false;
        dragOffset = { x: 0, y: 0 };
    });
}

/**
 * 정령들 애니메이션 루프
 */
function animateSpirits() {
    const walkingArea = document.querySelector('.spirit-walking-area');
    if (!walkingArea) {
        console.warn('walkingArea를 찾을 수 없습니다.');
        if (animationFrameId !== null) {
            cancelAnimationFrame(animationFrameId);
            animationFrameId = null;
        }
        return;
    }
    
    if (spiritWalkers.length === 0) {
        // 정령이 없으면 애니메이션 중지
        console.warn('spiritWalkers가 비어있어 애니메이션을 중지합니다.');
        if (animationFrameId !== null) {
            cancelAnimationFrame(animationFrameId);
            animationFrameId = null;
        }
        return;
    }
    
    // 디버깅: 활성 정령 수 확인
    const activeSpirits = spiritWalkers.filter(s => !s.isInactive && !s.isDragged && !s.isFalling && s.interactionState === 'none');
    if (activeSpirits.length === 0 && spiritWalkers.length > 0) {
        console.warn('활성 정령이 없습니다. 모든 정령이 비활성 상태입니다.');
    }
    
    const areaWidth = walkingArea.offsetWidth;
    const areaHeight = walkingArea.offsetHeight;
    
    // 상호작용 처리
    handleSpiritInteractions();
    
    spiritWalkers.forEach(spirit => {
        // 행동 불가 상태면 애니메이션 스킵
        if (spirit.isInactive) {
            return;
        }
        
        // 드래그 중이면 애니메이션 스킵
        if (spirit.isDragged) {
            return;
        }
        
        // 넘어지는 상태 처리
        if (spirit.isFalling) {
            spirit.fallTimer += 16;
            if (spirit.fallTimer >= 2000) { // 2초 후 일어남
                spirit.isFalling = false;
                spirit.fallTimer = 0;
                spirit.element.classList.remove('spirit-falling');
                // 텔레포트는 makeSpiritFall에서 setTimeout으로 처리됨
            }
            return; // 넘어지는 동안은 이동하지 않음
        }
        
        // 상호작용 중이면 이동하지 않음
        if (spirit.interactionState !== 'none') {
            spirit.interactionTimer += 16;
            // 상호작용 시간 초과 시 종료
            if (spirit.interactionTimer >= 5000) { // 5초
                endInteraction(spirit);
            }
            return;
        }
        
        // 방향 변경 타이머 업데이트
        spirit.changeDirectionTimer += 16; // 약 60fps 가정
        
        if (spirit.changeDirectionTimer >= spirit.changeDirectionInterval) {
            // 새로운 목표 영역 선택 (70% 확률로 같은 영역, 30% 확률로 다른 영역)
            if (Math.random() < 0.7) {
                // 같은 영역 내에서 새로운 위치
                spirit.targetZone = spirit.currentZone;
            } else {
                // 다른 영역 선택
                spirit.targetZone = getRandomZone(spirit.walkingZones);
                spirit.currentZone = spirit.targetZone;
            }
            
            const targetPos = getRandomPositionInZone(spirit.targetZone, spirit.size);
            spirit.targetX = targetPos.x;
            spirit.targetY = targetPos.y;
            
            spirit.changeDirectionTimer = 0;
            spirit.changeDirectionInterval = 3000 + Math.random() * 4000;
        }
        
        // 목표 위치로 이동
        const dx = spirit.targetX - spirit.x;
        const dy = spirit.targetY - spirit.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 3) {
            // 이동 전 충돌 체크
            const nextX = spirit.x + (dx / distance) * spirit.speed;
            const nextY = spirit.y + (dy / distance) * spirit.speed;
            
            // 다른 정령과의 충돌 체크
            let collisionDetected = false;
            for (const otherSpirit of spiritWalkers) {
                if (otherSpirit === spirit || otherSpirit.isInactive || 
                    otherSpirit.isFalling || otherSpirit.interactionState !== 'none') {
                    continue;
                }
                
                const otherDx = nextX - otherSpirit.x;
                const otherDy = nextY - otherSpirit.y;
                const otherDistance = Math.sqrt(otherDx * otherDx + otherDy * otherDy);
                const collisionDistance = 90; // 충돌 거리 (정령 크기 고려)
                
                if (otherDistance < collisionDistance) {
                    // 충돌 감지 - 상호작용 처리
                    if (spirit.interactionState === 'none' && otherSpirit.interactionState === 'none') {
                        handleCollision(spirit, otherSpirit);
                    }
                    collisionDetected = true;
                    break;
                }
            }
            
            // 충돌이 없으면 이동
            if (!collisionDetected) {
                spirit.x = nextX;
                spirit.y = nextY;
            } else {
                // 충돌이 감지되면 반대 방향으로 이동하거나 새로운 목표 설정
                const targetPos = getRandomPositionInZone(spirit.currentZone, spirit.size);
                spirit.targetX = targetPos.x;
                spirit.targetY = targetPos.y;
            }
            
            // 현재 영역 경계 체크 (영역을 벗어나지 않도록)
            const currentZone = spirit.currentZone;
            spirit.x = Math.max(currentZone.minX, Math.min(currentZone.maxX - spirit.size, spirit.x));
            spirit.y = Math.max(currentZone.minY, Math.min(currentZone.maxY - spirit.size, spirit.y));
            
            // 요소 위치 업데이트
            spirit.element.style.left = spirit.x + 'px';
            spirit.element.style.top = spirit.y + 'px';
            
            // 이동 방향에 따라 이미지만 반전 (이름은 원래 방향 유지)
            const imageElement = spirit.element.querySelector('.spirit-walker-image');
            const nameElement = spirit.element.querySelector('.spirit-walker-name');
            
            if (imageElement) {
                // CSS 변수를 사용하여 float 애니메이션과 함께 작동
                if (dx > 0) {
                    imageElement.style.setProperty('--flip-direction', '1');
                } else if (dx < 0) {
                    imageElement.style.setProperty('--flip-direction', '-1');
                }
            }
            
            // 이름은 항상 정방향 유지 (매 프레임마다 강제로 재설정)
            if (nameElement) {
                // translateX(-50%)로 중앙 정렬하고 scaleX(1)로 정방향 유지
                nameElement.style.setProperty('transform', 'translateX(-50%) scaleX(1)', 'important');
                nameElement.style.setProperty('-webkit-transform', 'translateX(-50%) scaleX(1)', 'important');
                // 추가 보장을 위해 직접 스타일도 설정
                nameElement.style.transform = 'translateX(-50%) scaleX(1)';
                nameElement.style.webkitTransform = 'translateX(-50%) scaleX(1)';
            }
        } else {
            // 목표에 도달했으면 새로운 목표 설정 (같은 영역 내)
            const targetPos = getRandomPositionInZone(spirit.currentZone, spirit.size);
            spirit.targetX = targetPos.x;
            spirit.targetY = targetPos.y;
        }
    });
    
    animationFrameId = requestAnimationFrame(animateSpirits);
}

/**
 * 정령들 간 상호작용 처리 (이미 이동 로직에서 처리되므로 여기서는 제거)
 * 충돌 감지는 이동 전에 처리됩니다.
 */
function handleSpiritInteractions() {
    // 충돌 감지는 이동 로직에서 처리되므로 여기서는 추가 처리 불필요
    // 이 함수는 향후 확장을 위해 유지
}

/**
 * 충돌 처리
 */
function handleCollision(spirit1, spirit2) {
    // 이미 상호작용 중이면 무시
    if (spirit1.interactionState !== 'none' || spirit2.interactionState !== 'none') {
        return;
    }
    
    // 30% 확률로 부딪혀서 넘어짐
    if (Math.random() < 0.3) {
        makeSpiritFall(spirit1);
        makeSpiritFall(spirit2);
        return;
    }
    
    // 성격 호환성 체크
    const compatibility = checkPersonalityCompatibility(spirit1.personality, spirit2.personality);
    
    if (compatibility === 'compatible') {
        // 비슷한 성격끼리 놀기
        startPlaying(spirit1, spirit2);
    } else if (compatibility === 'incompatible') {
        // 안 맞는 정령들끼리 싸우기
        startFighting(spirit1, spirit2);
    } else {
        // 일반 대화
        startTalking(spirit1, spirit2);
    }
}

/**
 * 성격 호환성 체크
 */
function checkPersonalityCompatibility(personality1, personality2) {
    // 같은 성격끼리는 호환
    if (personality1 === personality2) {
        return 'compatible';
    }
    
    // 호환되는 성격 조합
    const compatiblePairs = [
        ['고집', '용감'],
        ['조심', '온순'],
        ['장난꾸러기', '온순'],
        ['온순', '조심'],
        ['온순', '장난꾸러기']
    ];
    
    for (const pair of compatiblePairs) {
        if ((pair[0] === personality1 && pair[1] === personality2) ||
            (pair[0] === personality2 && pair[1] === personality1)) {
            return 'compatible';
        }
    }
    
    // 비호환 조합
    const incompatiblePairs = [
        ['고집', '조심'],
        ['고집', '온순'],
        ['용감', '조심'],
        ['용감', '온순']
    ];
    
    for (const pair of incompatiblePairs) {
        if ((pair[0] === personality1 && pair[1] === personality2) ||
            (pair[0] === personality2 && pair[1] === personality1)) {
            return 'incompatible';
        }
    }
    
    return 'neutral';
}

/**
 * 정령이 넘어지게 함
 */
function makeSpiritFall(spirit) {
    spirit.isFalling = true;
    spirit.fallTimer = 0;
    spirit.element.classList.add('spirit-falling');
    
    // 말풍선 표시
    showInteractionBubble(spirit.element, '아야!', 2000);
    
    // 넘어지는 애니메이션 종료 후 텔레포트
    setTimeout(() => {
        teleportSpiritToRandomLocation(spirit);
    }, 2000);
}

/**
 * 정령들이 놀기 시작
 */
function startPlaying(spirit1, spirit2) {
    spirit1.interactionState = 'playing';
    spirit1.interactionTarget = spirit2;
    spirit1.interactionTimer = 0;
    spirit2.interactionState = 'playing';
    spirit2.interactionTarget = spirit1;
    spirit2.interactionTimer = 0;
    
    spirit1.element.classList.add('spirit-playing');
    spirit2.element.classList.add('spirit-playing');
    
    // 말풍선 표시
    const playMessages = ['재밌다!', '함께 놀자!', '즐거워!', '신나!'];
    showInteractionBubble(spirit1.element, playMessages[Math.floor(Math.random() * playMessages.length)], 3000);
    showInteractionBubble(spirit2.element, playMessages[Math.floor(Math.random() * playMessages.length)], 3000);
}

/**
 * 정령들이 싸우기 시작
 */
function startFighting(spirit1, spirit2) {
    spirit1.interactionState = 'fighting';
    spirit1.interactionTarget = spirit2;
    spirit1.interactionTimer = 0;
    spirit2.interactionState = 'fighting';
    spirit2.interactionTarget = spirit1;
    spirit2.interactionTimer = 0;
    
    spirit1.element.classList.add('spirit-fighting');
    spirit2.element.classList.add('spirit-fighting');
    
    // 말풍선 표시
    const fightMessages = ['으르렁!', '싫어!', '가까이 오지 마!', '화나!'];
    showInteractionBubble(spirit1.element, fightMessages[Math.floor(Math.random() * fightMessages.length)], 3000);
    showInteractionBubble(spirit2.element, fightMessages[Math.floor(Math.random() * fightMessages.length)], 3000);
}

/**
 * 정령들이 대화 시작
 */
function startTalking(spirit1, spirit2) {
    spirit1.interactionState = 'talking';
    spirit1.interactionTarget = spirit2;
    spirit1.interactionTimer = 0;
    spirit2.interactionState = 'talking';
    spirit2.interactionTarget = spirit1;
    spirit2.interactionTimer = 0;
    
    spirit1.element.classList.add('spirit-talking');
    spirit2.element.classList.add('spirit-talking');
    
    // 말풍선 표시
    const talkMessages = ['안녕!', '좋은 하루야!', '반가워!', '어떻게 지내?'];
    showInteractionBubble(spirit1.element, talkMessages[Math.floor(Math.random() * talkMessages.length)], 3000);
    showInteractionBubble(spirit2.element, talkMessages[Math.floor(Math.random() * talkMessages.length)], 3000);
}

/**
 * 상호작용 종료
 */
function endInteraction(spirit) {
    spirit.interactionState = 'none';
    spirit.interactionTimer = 0;
    const targetSpirit = spirit.interactionTarget;
    spirit.interactionTarget = null;
    
    spirit.element.classList.remove('spirit-playing', 'spirit-fighting', 'spirit-talking');
    
    // 상호작용 종료 후 무작위 위치로 텔레포트
    teleportSpiritToRandomLocation(spirit);
    
    // 상대방도 텔레포트 (상호작용 중이었다면)
    if (targetSpirit && targetSpirit.interactionState !== 'none') {
        endInteraction(targetSpirit);
    }
}

/**
 * 정령을 무작위 위치로 텔레포트
 */
function teleportSpiritToRandomLocation(spirit) {
    if (spirit.isInactive) {
        return;
    }
    
    const walkingArea = document.querySelector('.spirit-walking-area');
    if (!walkingArea) {
        return;
    }
    
    const areaWidth = walkingArea.offsetWidth;
    const areaHeight = walkingArea.offsetHeight;
    const walkingZones = getWalkingZones(areaWidth, areaHeight);
    
    // 무작위 영역 선택
    const randomZone = getRandomZone(walkingZones);
    const newPos = getRandomPositionInZone(randomZone, spirit.size);
    
    // 위치 텔레포트
    spirit.x = newPos.x;
    spirit.y = newPos.y;
    spirit.currentZone = randomZone;
    
    // 요소 위치 업데이트
    spirit.element.style.left = spirit.x + 'px';
    spirit.element.style.top = spirit.y + 'px';
    
    // 새로운 목표 위치 설정
    const targetPos = getRandomPositionInZone(randomZone, spirit.size);
    spirit.targetX = targetPos.x;
    spirit.targetY = targetPos.y;
    
    // 방향 변경 타이머 리셋
    spirit.changeDirectionTimer = 0;
    spirit.changeDirectionInterval = 3000 + Math.random() * 4000;
}

/**
 * 상호작용 말풍선 표시
 */
function showInteractionBubble(element, message, duration) {
    // 기존 말풍선 제거
    const existingBubble = element.querySelector('.interaction-bubble');
    if (existingBubble) {
        existingBubble.remove();
    }
    
    // 새 말풍선 생성
    const bubble = document.createElement('div');
    bubble.className = 'interaction-bubble';
    bubble.textContent = message;
    element.appendChild(bubble);
    
    // 일정 시간 후 제거
    setTimeout(() => {
        if (bubble.parentNode) {
            bubble.remove();
        }
    }, duration);
}

/**
 * 정령 모달 열기
 */
async function openSpiritModal(spiritId) {
    // spiritsData에서 해당 정령 찾기
    let spirit = spiritsData.find(s => s.id === spiritId);
    
    if (!spirit) {
        console.error('정령을 찾을 수 없습니다:', spiritId);
        return;
    }
    
    // 최신 정령 정보 가져오기 (돌봐주기 게이지 등 최신 정보 포함)
    try {
        const response = await fetch(`/spirit/api/${spiritId}`, {
            credentials: 'include'
        });
        if (response.ok) {
            const latestSpirit = await response.json();
            // 최신 정보로 업데이트
            spirit = { ...spirit, ...latestSpirit };
        }
    } catch (error) {
        console.error('최신 정령 정보를 가져오는 중 오류:', error);
        // 오류가 발생해도 기존 데이터로 계속 진행
    }
    
    const modal = document.getElementById('spiritModal');
    const modalBody = document.getElementById('spiritModalBody');
    
    // 고치 상태 확인 (희귀 정령의 1차 진화 단계)
    const isCocoon = (spirit.spiritType === '빛의 정령' || spirit.spiritType === '어둠의 정령') && 
                     spirit.evolutionStage === 1;
    
    // 고치 상태일 때 특별 UI
    let cocoonSection = '';
    if (isCocoon) {
        // evolutionStartTime 파싱 (ISO 8601 형식 또는 다른 형식)
        let evolutionStartTime;
        if (spirit.evolutionStartTime) {
            // 문자열인 경우 파싱
            if (typeof spirit.evolutionStartTime === 'string') {
                evolutionStartTime = new Date(spirit.evolutionStartTime);
            } else {
                evolutionStartTime = new Date(spirit.evolutionStartTime);
            }
        } else {
            // evolutionStartTime이 없으면 현재 시간으로 설정 (처음 고치 상태가 된 경우)
            evolutionStartTime = new Date();
        }
        
        const now = new Date();
        const elapsedMs = now - evolutionStartTime;
        const elapsedHours = Math.max(0, Math.floor(elapsedMs / (1000 * 60 * 60)));
        const elapsedMinutes = Math.floor((elapsedMs % (1000 * 60 * 60)) / (1000 * 60));
        
        // 돌봐주기 게이지 정보
        const careGauge = spirit.cocoonCareGauge || 0;
        const maxGauge = 1000; // MAX_COCOON_CARE_GAUGE
        const gaugeProgress = Math.min(100, (careGauge / maxGauge) * 100);
        
        // 오늘 돌봐주기 횟수 (API에서 받아오거나 기본값 0)
        const dailyCareCount = spirit.dailyCareCount || 0;
        const maxDailyCareCount = 5;
        const remainingCareCount = maxDailyCareCount - dailyCareCount;
        const canCare = remainingCareCount > 0;
        
        cocoonSection = `
            <div class="cocoon-status" style="background: linear-gradient(135deg, rgba(255, 215, 0, 0.1), rgba(255, 165, 0, 0.1)); padding: 20px; border-radius: 15px; margin: 20px 0; border: 2px solid #FFD700;">
                <h4 style="color: #FFD700; margin-bottom: 15px;">🦋 고치 상태 🦋</h4>
                <p style="color: #4A5568; margin-bottom: 15px;">정령이 고치 안에서 진화하고 있습니다. 돌봐주면 게이지가 증가하고, 게이지가 최대치에 도달하면 정령 연구소에서 진화를 시작할 수 있습니다.</p>
                
                <!-- 돌봐주기 게이지 -->
                <div class="care-gauge" style="margin-bottom: 15px;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                        <span style="font-weight: 600; color: #4A5568;">돌봐주기 게이지</span>
                        <span style="font-weight: 600; color: #4A5568;">${careGauge}/${maxGauge}</span>
                    </div>
                    <div style="background: #E2E8F0; height: 20px; border-radius: 10px; overflow: hidden;">
                        <div style="background: linear-gradient(90deg, #FFD700, #FFA500); height: 100%; width: ${gaugeProgress}%; transition: width 0.3s ease;"></div>
                    </div>
                    ${careGauge >= maxGauge ? '<p style="color: #FFD700; font-size: 14px; font-weight: 600; margin-top: 5px;">✅ 게이지가 최대치에 도달했습니다! 정령 연구소에서 진화를 시작할 수 있습니다.</p>' : ''}
                </div>
                
                <!-- 오늘 돌봐주기 횟수 -->
                <div class="daily-care-info" style="margin-bottom: 15px; padding: 10px; background: rgba(255, 255, 255, 0.3); border-radius: 8px;">
                    <p style="color: #4A5568; font-size: 14px; margin: 5px 0;">
                        오늘 돌봐주기: <strong>${dailyCareCount}/${maxDailyCareCount}</strong>회
                        ${remainingCareCount > 0 ? `<span style="color: #48BB78;">(남은 횟수: ${remainingCareCount}회)</span>` : '<span style="color: #F56565;">(오늘 더 이상 돌볼 수 없습니다)</span>'}
                    </p>
                    <p style="color: #718096; font-size: 12px; margin: 5px 0;">
                        한 번 돌봐주면 게이지가 <strong>10</strong>씩 증가합니다.
                    </p>
                </div>
                
                <button class="action-btn care-btn" onclick="careForCocoon(${spirit.id})" 
                        style="width: 100%; background: ${canCare ? 'linear-gradient(135deg, #FFD700, #FFA500)' : '#A0AEC0'}; color: white; border: none; padding: 12px; border-radius: 10px; font-weight: 600; cursor: ${canCare ? 'pointer' : 'not-allowed'}; margin-top: 10px;"
                        ${!canCare ? 'disabled' : ''}>
                    💝 돌봐주기 ${!canCare ? '(오늘 횟수 초과)' : ''}
                </button>
            </div>
        `;
    }
    
    // 생애 주기 정보 및 기술 목록 로드
    loadLifecycleInfo(spiritId, (lifecycleInfo) => {
        loadSpiritSkills(spiritId, (skills) => {
            renderSpiritModal(spirit, isCocoon, cocoonSection, lifecycleInfo, skills);
        });
    });
}

/**
 * 정령의 배운 기술 목록 로드
 */
async function loadSpiritSkills(spiritId, callback) {
    try {
        const response = await fetch(`/spirit/api/skills/${spiritId}`, {
            credentials: 'include'
        });
        
        if (response.ok) {
            const skills = await response.json();
            callback(skills);
        } else {
            callback([]);
        }
    } catch (error) {
        console.error('기술 목록 로드 실패:', error);
        callback([]);
    }
}

/**
 * 생애 주기 정보 로드
 */
async function loadLifecycleInfo(spiritId, callback) {
    try {
        const response = await fetch(`/spirit/api/lifecycle/${spiritId}`, {
            credentials: 'include'
        });
        
        if (response.ok) {
            const lifecycleInfo = await response.json();
            callback(lifecycleInfo);
        } else {
            callback(null);
        }
    } catch (error) {
        console.error('생애 주기 정보 로드 실패:', error);
        callback(null);
    }
}

/**
 * 정령 모달 렌더링
 */
function renderSpiritModal(spirit, isCocoon, cocoonSection, lifecycleInfo, skills) {
    const modal = document.getElementById('spiritModal');
    const modalBody = document.getElementById('spiritModalBody');
    
    // 생애 주기 정보 섹션 생성
    let lifecycleSection = '';
    if (lifecycleInfo) {
        const isRetired = lifecycleInfo.isRetired;
        const remainingDays = lifecycleInfo.remainingDays;
        const maxLevelReached = lifecycleInfo.maxLevelReached;
        
        if (isRetired) {
            lifecycleSection = `
                <div class="lifecycle-status" style="background: linear-gradient(135deg, rgba(168, 230, 207, 0.2), rgba(135, 206, 235, 0.2)); padding: 20px; border-radius: 15px; margin: 20px 0; border: 2px solid #A8E6CF;">
                    <h4 style="color: #A8E6CF; margin-bottom: 15px;">✨ 은퇴한 정령 ✨</h4>
                    <p style="color: rgba(255, 255, 255, 0.8); margin-bottom: 10px;">이 정령은 이미 은퇴했습니다.</p>
                    <p style="color: rgba(255, 255, 255, 0.7); font-size: 14px;">은퇴 시: ${lifecycleInfo.retiredAt ? new Date(lifecycleInfo.retiredAt).toLocaleDateString('ko-KR') : '알 수 없음'}</p>
                    <p style="color: #FFD700; font-size: 16px; font-weight: 600; margin-top: 10px;">정령의 축복: ${spirit.intimacy * 10}개</p>
                </div>
            `;
        } else if (maxLevelReached && remainingDays >= 0) {
            const warningClass = remainingDays <= 3 ? 'warning' : '';
            lifecycleSection = `
                <div class="lifecycle-status ${warningClass}" style="background: linear-gradient(135deg, rgba(255, 165, 0, 0.2), rgba(255, 99, 71, 0.2)); padding: 20px; border-radius: 15px; margin: 20px 0; border: 2px solid ${remainingDays <= 3 ? '#FF6347' : '#FFA500'};">
                    <h4 style="color: ${remainingDays <= 3 ? '#FF6347' : '#FFA500'}; margin-bottom: 15px;">⏰ 수명 카운트다운</h4>
                    <p style="color: rgba(255, 255, 255, 0.8); margin-bottom: 10px;">정령이 50레벨에 도달했습니다. 수명이 다하면 은퇴합니다.</p>
                    <p style="color: ${remainingDays <= 3 ? '#FF6347' : '#FFA500'}; font-size: 18px; font-weight: 700; margin: 10px 0;">
                        남은 시간: ${remainingDays}일
                    </p>
                    ${lifecycleInfo.lifespanExtended > 0 ? `<p style="color: rgba(255, 255, 255, 0.7); font-size: 14px;">연장된 수명: +${lifecycleInfo.lifespanExtended}일</p>` : ''}
                    ${remainingDays <= 3 ? '<p style="color: #FF6347; font-size: 14px; font-weight: 600; margin-top: 10px;">⚠️ 수명이 얼마 남지 않았습니다! 생명의 열매를 사용하여 수명을 연장하세요.</p>' : ''}
                </div>
            `;
        } else if (lifecycleInfo.age > 0) {
            lifecycleSection = `
                <div class="lifecycle-status" style="background: rgba(0, 0, 0, 0.2); padding: 15px; border-radius: 10px; margin: 15px 0;">
                    <p style="color: rgba(255, 255, 255, 0.7); font-size: 14px;">나이: ${lifecycleInfo.age}일</p>
                </div>
            `;
        }
    }
    
    // 배운 기술 목록 생성 (오른쪽 상단)
    let skillsSection = '';
    if (skills && skills.length > 0) {
        const learnedSkills = skills.filter(ss => !ss.isLearning && ss.learnedAt);
        const learningSkills = skills.filter(ss => ss.isLearning);
        
        if (learnedSkills.length > 0 || learningSkills.length > 0) {
            skillsSection = `
                <div class="spirit-skills-preview" style="position: absolute; top: 10px; right: 10px; background: rgba(0, 0, 0, 0.6); padding: 10px; border-radius: 8px; max-width: 200px; font-size: 12px;">
                    <div style="color: #A8E6CF; font-weight: 600; margin-bottom: 5px; font-size: 11px;">배운 기술 (${learnedSkills.length}/4)</div>
                    ${learnedSkills.map(ss => `
                        <div style="color: rgba(255, 255, 255, 0.9); margin: 3px 0; font-size: 11px;">• ${escapeHtml(ss.skillName || '알 수 없음')}</div>
                    `).join('')}
                    ${learningSkills.map(ss => `
                        <div style="color: #FFD700; margin: 3px 0; font-size: 11px;">• ${escapeHtml(ss.skillName || '알 수 없음')} <span style="color: rgba(255, 255, 255, 0.6);">(학습 중)</span></div>
                    `).join('')}
                </div>
            `;
        }
    }
    
    // 모달 내용 생성
    modalBody.innerHTML = `
        <div class="spirit-header" id="spiritModalHeader" style="position: relative; cursor: move;">
            <h3>${escapeHtml(spirit.name)}</h3>
            <span class="spirit-type-badge">${escapeHtml(spirit.spiritType)}</span>
            ${isCocoon ? '<span class="cocoon-badge" style="background: #FFD700; color: white; padding: 5px 10px; border-radius: 15px; font-size: 12px; margin-left: 10px;">고치</span>' : ''}
            ${lifecycleInfo && lifecycleInfo.isRetired ? '<span class="retired-badge" style="background: #A8E6CF; color: white; padding: 5px 10px; border-radius: 15px; font-size: 12px; margin-left: 10px;">은퇴</span>' : ''}
            ${skillsSection}
        </div>
        
        ${cocoonSection}
        ${lifecycleSection}
        
        <div class="spirit-stats">
            <div class="stat-row">
                <span class="stat-label">레벨:</span>
                <span class="stat-value">${spirit.level || 1}</span>
            </div>
            <div class="stat-row">
                <span class="stat-label">진화 단계:</span>
                <span class="stat-value">${spirit.evolutionStage === 0 ? '기본' : spirit.evolutionStage === 1 ? '1차 진화' : '2차 진화'}</span>
            </div>
            <div class="stat-row">
                <span class="stat-label">친밀도:</span>
                <span class="stat-value">${spirit.intimacy || 1}</span><span>/10</span>
            </div>
            <div class="stat-row">
                <span class="stat-label">성격:</span>
                <span class="stat-value">${escapeHtml(spirit.personality || '고집')}</span>
            </div>
        </div>

        <!-- 상태 표시 -->
        <div class="spirit-status">
            <div class="status-item">
                <span class="status-label">건강:</span>
                <span class="status-value">${escapeHtml(spirit.healthStatus || '건강')}</span>
            </div>
            <div class="status-item">
                <span class="status-label">행복도:</span>
                <div class="status-bar">
                    <div class="status-fill happiness" style="width: ${spirit.happiness || 50}%"></div>
                    <span class="status-text">${spirit.happiness || 50}</span>
                </div>
            </div>
            <div class="status-item">
                <span class="status-label">배고픔:</span>
                <div class="status-bar">
                    <div class="status-fill hunger" style="width: ${100 - (spirit.hunger || 50)}%"></div>
                    <span class="status-text">${100 - (spirit.hunger || 50)}</span>
                </div>
            </div>
            <div class="status-item">
                <span class="status-label">에너지:</span>
                <div class="status-bar">
                    <div class="status-fill energy" style="width: ${spirit.energy || 100}%"></div>
                    <span class="status-text">${spirit.energy || 100}</span>
                </div>
            </div>
            <div class="status-item">
                <span class="status-label">기분:</span>
                <span class="status-value">${escapeHtml(spirit.mood || '보통')}</span>
            </div>
        </div>

        ${!isCocoon ? `
        <!-- 능력치 표시 (육각형 스테이터스) -->
        <div class="spirit-abilities">
            <div class="hexagon-stats-container">
                <svg class="hexagon-stats-chart" viewBox="0 0 220 220" xmlns="http://www.w3.org/2000/svg">
                    <!-- 배경 육각형 그리드 -->
                    <g class="hexagon-grid">
                        <!-- 최대값 육각형 (100%) -->
                        <polygon class="hexagon-bg" points="110,20 190,70 190,150 110,200 30,150 30,70" />
                        <!-- 80% 육각형 -->
                        <polygon class="hexagon-bg-80" points="110,36 174,76 174,144 110,184 46,144 46,76" />
                        <!-- 60% 육각형 -->
                        <polygon class="hexagon-bg-60" points="110,52 158,82 158,138 110,168 62,138 62,82" />
                        <!-- 40% 육각형 -->
                        <polygon class="hexagon-bg-40" points="110,68 142,88 142,132 110,152 78,132 78,88" />
                        <!-- 20% 육각형 -->
                        <polygon class="hexagon-bg-20" points="110,84 126,94 126,126 110,136 94,126 94,94" />
                    </g>
                    
                    <!-- 능력치 데이터 (정규화: 0-100) -->
                    <g class="hexagon-stats">
                        ${generateHexagonStats(spirit)}
                    </g>
                    
                    <!-- 능력치 라벨 및 수치 (이미지 스타일) -->
                    <g class="hexagon-labels">
                        <!-- HP (상단 중앙) -->
                        <circle cx="110" cy="20" r="3" fill="white" class="stat-dot"/>
                        <text x="110" y="12" class="stat-label">HP</text>
                        <text x="110" y="30" class="stat-value-text">${spirit.energy || 100}/${spirit.energy || 100}</text>
                        
                        <!-- 특수공격 (좌측 상단) -->
                        <circle cx="30" cy="70" r="3" fill="white" class="stat-dot"/>
                        <text x="25" y="60" class="stat-label">특수공격</text>
                        <text x="25" y="80" class="stat-value-text">${spirit.rangedAttack || 50}</text>
                        
                        <!-- 공격 (우측 상단) -->
                        <circle cx="190" cy="70" r="3" fill="white" class="stat-dot"/>
                        <text x="195" y="60" class="stat-label">공격</text>
                        <text x="195" y="80" class="stat-value-text">${spirit.meleeAttack || 50}</text>
                        
                        <!-- 방어 (우측 하단) -->
                        <circle cx="190" cy="150" r="3" fill="white" class="stat-dot"/>
                        <text x="195" y="140" class="stat-label">방어</text>
                        <text x="195" y="160" class="stat-value-text">${spirit.meleeDefense || 50}</text>
                        
                        <!-- 스피드 (하단 중앙) -->
                        <circle cx="110" cy="200" r="3" fill="white" class="stat-dot"/>
                        <text x="110" y="192" class="stat-label">스피드</text>
                        <text x="110" y="210" class="stat-value-text">${spirit.speed || 50}</text>
                        
                        <!-- 특수방어 (좌측 하단) -->
                        <circle cx="30" cy="150" r="3" fill="white" class="stat-dot"/>
                        <text x="25" y="140" class="stat-label">특수방어</text>
                        <text x="25" y="160" class="stat-value-text">${spirit.rangedDefense || 50}</text>
                    </g>
                </svg>
            </div>
        </div>

        <!-- 액션 버튼 -->
        <div class="spirit-actions">
            <a href="/training/grounds?spiritId=${spirit.id}" class="action-btn training-btn" ${spirit.energy < 20 ? 'style="opacity: 0.5; pointer-events: none;"' : ''}>
                🎯 훈련하기
            </a>
            <button class="action-btn item-btn" onclick="openItemUseModal(${spirit.id})">
                📦 아이템 사용
            </button>
            <a href="/arena/spirit-arena?spiritId=${spirit.id}" class="action-btn competition-btn" ${spirit.energy < 30 ? 'style="opacity: 0.5; pointer-events: none;"' : ''}>
                ⚔️ 대회 참가
            </a>
        </div>
        ` : ''}
    `;
    
    // 모달 표시
    modal.style.setProperty('display', 'flex', 'important');
    modal.style.setProperty('visibility', 'visible', 'important');
    modal.style.setProperty('opacity', '1', 'important');
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
    
    // 모달이 열릴 때마다 닫기 버튼 이벤트 다시 등록
    const closeButton = modal.querySelector('.spirit-modal-close');
    if (closeButton) {
        // 기존 이벤트 리스너 제거 후 새로 추가
        const newCloseButton = closeButton.cloneNode(true);
        closeButton.parentNode.replaceChild(newCloseButton, closeButton);
        
        newCloseButton.addEventListener('click', function(e) {
            console.log('닫기 버튼 클릭 (모달 열릴 때 등록)');
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
            closeSpiritModal();
            return false;
        });
        
        newCloseButton.onclick = function(e) {
            console.log('닫기 버튼 onclick (모달 열릴 때 등록)');
            e.preventDefault();
            e.stopPropagation();
            closeSpiritModal();
            return false;
        };
    }
}

/**
 * 육각형 스테이터스 차트 생성
 */
function generateHexagonStats(spirit) {
    // 실제 능력치 값 가져오기
    const actualRangedAttack = spirit.rangedAttack || 50;
    const actualMeleeAttack = spirit.meleeAttack || 50;
    const actualMeleeDefense = spirit.meleeDefense || 50;
    const actualRangedDefense = spirit.rangedDefense || 50;
    const actualSpeed = spirit.speed || 50;
    const actualEnergy = spirit.energy || 100;
    
    // 능력치 최대값 설정 (실제 게임에서 능력치는 0-100 범위)
    const maxStatValue = 100; // 능력치 최대값
    const maxEnergy = 100; // 에너지 최대값
    
    // 능력치 정규화 (0-100 범위로 변환)
    const rangedAttack = Math.min(100, Math.max(0, actualRangedAttack));
    const meleeAttack = Math.min(100, Math.max(0, actualMeleeAttack));
    const meleeDefense = Math.min(100, Math.max(0, actualMeleeDefense));
    const rangedDefense = Math.min(100, Math.max(0, actualRangedDefense));
    const speed = Math.min(100, Math.max(0, actualSpeed));
    const hp = Math.min(100, Math.max(0, actualEnergy));
    
    // 육각형 모양을 유지하기 위해 모든 능력치의 평균값 계산
    const avgStat = (rangedAttack + meleeAttack + meleeDefense + rangedDefense + speed + hp) / 6;
    
    // 평균값을 기준으로 육각형 그리기 (육각형 모양 유지)
    const baseValue = avgStat;
    
    // 육각형 중심점 (viewBox 220x220 기준으로 조정)
    const centerX = 110;
    const centerY = 110;
    const radius = 70;
    
    // 각 능력치의 좌표 계산 (이미지 스타일)
    // HP: 위 (0도)
    // 특수공격: 왼쪽 위 (-60도)
    // 공격: 오른쪽 위 (60도)
    // 방어: 오른쪽 아래 (120도)
    // 특수방어: 왼쪽 아래 (240도)
    // 스피드: 아래 (180도)
    
    function getPoint(angle, value) {
        const rad = (angle - 90) * Math.PI / 180; // -90도로 시작 (위쪽)
        const distance = radius * (value / 100);
        const x = centerX + distance * Math.cos(rad);
        const y = centerY + distance * Math.sin(rad);
        return { x, y };
    }
    
    // 육각형 모양을 유지하기 위해 평균값으로 모든 점 생성
    const points = [
        getPoint(0, baseValue),         // HP (위)
        getPoint(-60, baseValue),       // 특수공격 (왼쪽 위)
        getPoint(60, baseValue),        // 공격 (오른쪽 위)
        getPoint(120, baseValue),       // 방어 (오른쪽 아래)
        getPoint(240, baseValue),       // 특수방어 (왼쪽 아래)
        getPoint(180, baseValue)        // 스피드 (아래)
    ];
    
    return `
        <polygon class="hexagon-stats-fill" 
                 points="${points.map(p => `${p.x},${p.y}`).join(' ')}" 
                 fill="rgba(135, 206, 235, 0.4)" 
                 stroke="rgba(135, 206, 235, 0.8)" 
                 stroke-width="2"/>
        <polygon class="hexagon-stats-border" 
                 points="${points.map(p => `${p.x},${p.y}`).join(' ')}" 
                 fill="none" 
                 stroke="rgba(135, 206, 235, 0.9)" 
                 stroke-width="1.5"/>
    `;
}

/**
 * 정령 모달 닫기
 */
function closeSpiritModal() {
    console.log('closeSpiritModal 호출됨');
    const modal = document.getElementById('spiritModal');
    if (modal) {
        // 클래스 제거
        modal.classList.remove('show');
        // 인라인 스타일로 확실하게 숨김 (!important 효과)
        modal.style.setProperty('display', 'none', 'important');
        modal.style.setProperty('visibility', 'hidden', 'important');
        modal.style.setProperty('opacity', '0', 'important');
        document.body.style.overflow = '';
        console.log('모달 닫힘');
    } else {
        console.error('모달 요소를 찾을 수 없습니다');
    }
}

// 전역 함수로 등록 (HTML에서 직접 호출 가능하도록)
window.closeSpiritModal = closeSpiritModal;

/**
 * 정령 모달 드래그 앤 드롭 설정
 */
function setupSpiritModalDrag(modal) {
    const modalContent = modal.querySelector('.spirit-modal-content');
    const modalHeader = modal.querySelector('#spiritModalHeader');
    
    if (!modalContent || !modalHeader) {
        return;
    }
    
    let isDragging = false;
    let currentX;
    let currentY;
    let initialX;
    let initialY;
    let xOffset = 0;
    let yOffset = 0;
    
    // 헤더를 드래그 핸들로 사용
    modalHeader.style.cursor = 'move';
    modalHeader.addEventListener('mousedown', dragStart);
    document.addEventListener('mousemove', drag);
    document.addEventListener('mouseup', dragEnd);
    
    function dragStart(e) {
        // 닫기 버튼이나 다른 버튼 클릭 시 드래그 방지
        if (e.target.closest('.spirit-modal-close') || 
            e.target.closest('button') || 
            e.target.closest('a')) {
            return;
        }
        
        initialX = e.clientX - xOffset;
        initialY = e.clientY - yOffset;
        
        if (e.target === modalHeader || modalHeader.contains(e.target)) {
            isDragging = true;
        }
    }
    
    function drag(e) {
        if (isDragging) {
            e.preventDefault();
            currentX = e.clientX - initialX;
            currentY = e.clientY - initialY;
            
            xOffset = currentX;
            yOffset = currentY;
            
            setTranslate(currentX, currentY, modalContent);
        }
    }
    
    function dragEnd(e) {
        initialX = currentX;
        initialY = currentY;
        isDragging = false;
    }
    
    function setTranslate(xPos, yPos, el) {
        el.style.transform = `translate(${xPos}px, ${yPos}px)`;
    }
    
    // 모달이 열릴 때마다 위치 초기화
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
                if (modal.classList.contains('show')) {
                    // 모달이 열릴 때 위치 초기화
                    xOffset = 0;
                    yOffset = 0;
                    modalContent.style.transform = 'translate(0px, 0px)';
                }
            }
        });
    });
    
    observer.observe(modal, { attributes: true, attributeFilter: ['class'] });
}

/**
 * 고치 돌봐주기
 */
async function careForCocoon(spiritId) {
    try {
        const response = await fetch(`/spirit/api/care-cocoon/${spiritId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
        });

        if (!response.ok) {
            // 에러 응답 파싱
            let errorMessage = '돌봐주기 실패';
            try {
                const errorData = await response.json();
                if (errorData.error) {
                    errorMessage = errorData.error;
                }
            } catch (e) {
                errorMessage = '돌봐주기 실패: ' + response.status;
            }
            showError(errorMessage);
            // 모달 새로고침 (에러가 발생해도 최신 정보 표시)
            openSpiritModal(spiritId);
            return;
        }

        const result = await response.json();
        
        // 성공 메시지 표시
        if (result.message) {
            showSuccess(result.message);
        } else {
            showSuccess('정령을 돌봐주었습니다! 게이지가 증가했습니다.');
        }
        
        // 모달 새로고침 (업데이트된 정보로)
        openSpiritModal(spiritId);
        
    } catch (error) {
        console.error('Error caring for cocoon:', error);
        showError('돌봐주기 중 오류가 발생했습니다: ' + error.message);
        // 모달 새로고침 (에러가 발생해도 최신 정보 표시)
        openSpiritModal(spiritId);
    }
}

/**
 * HTML 이스케이프 함수
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 현재 시간 업데이트 함수
function updateCurrentTime() {
    const currentTimeDisplay = document.getElementById('currentTimeDisplay');
    if (currentTimeDisplay) {
        const now = new Date();
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        currentTimeDisplay.textContent = `${hours}:${minutes}:${seconds}`;
    }
}

// 모달 외부 클릭 시 닫기
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOMContentLoaded - 모달 이벤트 리스너 설정');
    
    // 현재 시간 표시 초기화 및 업데이트
    updateCurrentTime();
    // 1초마다 현재 시간 업데이트
    setInterval(updateCurrentTime, 1000);
    
    // 모달이 처음에 보이지 않도록 확실하게 숨김
    const modal = document.getElementById('spiritModal');
    if (modal) {
        modal.style.setProperty('display', 'none', 'important');
        modal.style.setProperty('visibility', 'hidden', 'important');
        modal.style.setProperty('opacity', '0', 'important');
        modal.classList.remove('show');
    }
    
    // 약간의 지연을 두고 모달 요소 찾기 (동적으로 생성될 수 있으므로)
    setTimeout(function() {
        const modal = document.getElementById('spiritModal');
        const modalContent = modal ? modal.querySelector('.spirit-modal-content') : null;
        const closeButton = modal ? modal.querySelector('.spirit-modal-close') : null;
        
        console.log('모달 요소 찾기:', { modal: !!modal, modalContent: !!modalContent, closeButton: !!closeButton });
        
        // 닫기 버튼 클릭 이벤트 (여러 방법으로 시도)
        if (closeButton) {
            // 기존 이벤트 리스너 제거 후 새로 추가
            const newCloseButton = closeButton.cloneNode(true);
            closeButton.parentNode.replaceChild(newCloseButton, closeButton);
            
            newCloseButton.addEventListener('click', function(e) {
                console.log('닫기 버튼 클릭됨');
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();
                closeSpiritModal();
                return false;
            });
            
            // onclick 속성도 추가 (이중 보험)
            newCloseButton.onclick = function(e) {
                console.log('닫기 버튼 onclick 호출됨');
                e.preventDefault();
                e.stopPropagation();
                closeSpiritModal();
                return false;
            };
            
            // 마우스 다운 이벤트도 추가
            newCloseButton.addEventListener('mousedown', function(e) {
                console.log('닫기 버튼 mousedown');
                e.preventDefault();
                e.stopPropagation();
            });
        }
        
        // 모달 외부 클릭 시 닫기 (모달 콘텐츠 외부)
        if (modal && modalContent) {
            modal.addEventListener('click', function(e) {
                // 모달 콘텐츠나 닫기 버튼을 클릭한 경우가 아니면 닫기
                if (e.target === modal) {
                    console.log('모달 외부 클릭 - 닫기');
                    closeSpiritModal();
                }
            });
            
            // 모달 콘텐츠 클릭 시 이벤트 전파 중지 (모달이 닫히지 않도록)
            modalContent.addEventListener('click', function(e) {
                // 닫기 버튼이 아닌 경우에만 전파 중지
                if (!e.target.classList.contains('spirit-modal-close')) {
                    e.stopPropagation();
                }
            });
        }
    }, 100);
    
    // ESC 키로 모달 닫기
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeSpiritModal();
        }
    });
    
    // 정령 이동 애니메이션 초기화
    setTimeout(async () => {
        console.log('정령 애니메이션 초기화 시작');
        const walkers = document.querySelectorAll('.spirit-walker');
        const walkingArea = document.querySelector('.spirit-walking-area');
        console.log('정령 요소 개수:', walkers.length, '이동 영역:', !!walkingArea);
        
        if (walkers.length > 0 && walkingArea) {
            await initSpiritWalkers();
            if (typeof initSpeechBubbles === 'function') {
                initSpeechBubbles(); // 말풍선 시스템 초기화
            }
        } else {
            console.error('정령 요소나 이동 영역을 찾을 수 없습니다. 재시도합니다...');
            // 재시도
            setTimeout(async () => {
                console.log('재시도: 정령 애니메이션 초기화');
                await initSpiritWalkers();
                if (typeof initSpeechBubbles === 'function') {
                    initSpeechBubbles();
                }
            }, 1000);
        }
    }, 500); // 페이지 로드 후 약간의 지연을 두고 시작
    
/**
 * 학습 중이거나 진화 중인 정령 숨기기 (병렬 처리)
 */
async function hideBusySpirits() {
    if (!spiritsData || spiritsData.length === 0) {
        console.log('spiritsData가 없습니다.');
        return;
    }
    
    console.log('정령 필터링 시작:', spiritsData.length + '마리');
    
    // 먼저 진화 중인 정령 숨기기 (동기 처리)
    spiritsData.forEach(spirit => {
        const walkerElement = document.querySelector(`.spirit-walker[data-spirit-id="${spirit.id}"]`);
        if (!walkerElement) {
            return;
        }
        
        // 진화 중인 정령 숨기기
        if (spirit.evolutionInProgress === true) {
            console.log('정령 ID ' + spirit.id + ' 숨김: 진화 중');
            walkerElement.style.display = 'none';
        } else {
            // 일단 표시 (학습 중인지 확인 후 숨길 수 있음)
            walkerElement.style.display = '';
        }
    });
    
    // 학습 중인 정령 확인 (병렬 API 호출)
    const spiritsToCheck = spiritsData.filter(spirit => {
        // 이미 진화 중으로 숨겨진 정령은 제외
        return !(spirit.evolutionInProgress === true);
    });
    
    if (spiritsToCheck.length === 0) {
        console.log('확인할 정령이 없습니다.');
        return;
    }
    
    const learningChecks = spiritsToCheck.map(async (spirit) => {
            const walkerElement = document.querySelector(`.spirit-walker[data-spirit-id="${spirit.id}"]`);
            if (!walkerElement) {
                return { spiritId: spirit.id, isLearning: false };
            }
            
            try {
                const response = await fetch(`/spirit/api/skills/${spirit.id}`, {
                    credentials: 'include'
                });
                if (response.ok) {
                    const skills = await response.json();
                    if (skills && skills.length > 0) {
                        const isLearning = skills.some(ss => ss.isLearning === true || ss.isLearning === 'true');
                        return { spiritId: spirit.id, isLearning: isLearning, element: walkerElement };
                    }
                }
            } catch (error) {
                console.error('정령 ID ' + spirit.id + ' 학습 정보 조회 실패:', error);
            }
            
            return { spiritId: spirit.id, isLearning: false, element: walkerElement };
        });
    
    // 모든 API 호출 완료 대기 (병렬 처리)
    const results = await Promise.all(learningChecks);
    
    // 결과에 따라 정령 숨기기/표시
    results.forEach(result => {
        if (result.element) {
            if (result.isLearning) {
                console.log('정령 ID ' + result.spiritId + ' 숨김: 학습 중');
                result.element.style.display = 'none';
            } else {
                // 학습 완료된 정령 표시
                result.element.style.display = '';
            }
        }
    });
    
    console.log('정령 필터링 완료');
}

    // 창 크기 변경 시 정령 위치 재조정
    window.addEventListener('resize', function() {
        const walkingArea = document.querySelector('.spirit-walking-area');
        if (walkingArea && spiritWalkers.length > 0) {
            const areaWidth = walkingArea.offsetWidth;
            const areaHeight = walkingArea.offsetHeight;
            
            // 이동 가능 영역 재계산
            const walkingZones = getWalkingZones(areaWidth, areaHeight);
            
            spiritWalkers.forEach(spirit => {
                // 영역 정보 업데이트
                spirit.walkingZones = walkingZones;
                
                // 현재 위치가 유효한 영역에 있는지 확인
                let inValidZone = false;
                for (const zone of walkingZones) {
                    if (spirit.x >= zone.minX && spirit.x <= zone.maxX - spirit.size &&
                        spirit.y >= zone.minY && spirit.y <= zone.maxY - spirit.size) {
                        spirit.currentZone = zone;
                        inValidZone = true;
                        break;
                    }
                }
                
                // 유효한 영역에 없으면 가장 가까운 영역으로 이동
                if (!inValidZone) {
                    spirit.currentZone = getRandomZone(walkingZones);
                    const newPos = getRandomPositionInZone(spirit.currentZone, spirit.size);
                    spirit.x = newPos.x;
                    spirit.y = newPos.y;
                    spirit.element.style.left = spirit.x + 'px';
                    spirit.element.style.top = spirit.y + 'px';
                }
                
                // 목표 위치도 영역 내로 제한
                const targetPos = getRandomPositionInZone(spirit.currentZone, spirit.size);
                spirit.targetX = targetPos.x;
                spirit.targetY = targetPos.y;
            });
        }
    });
});

/**
 * 훈련 모달 열기
 */
async function openTrainingModal(spiritId) {
    const spirit = spiritsData.find(s => s.id === spiritId);
    if (!spirit) {
        return;
    }
    
    if (spirit.energy < 20) {
        showWarning('에너지가 부족합니다. (최소 20 필요)');
        return;
    }
    
    const trainingType = await showPrompt('훈련 타입을 선택하세요:\n1. ATTACK (공격)\n2. DEFENSE (방어)\n3. SPEED (스피드)\n4. BALANCED (균형)', 'BALANCED', '훈련 타입 선택');
    
    if (!trainingType) {
        return;
    }
    
    // 입력값이 숫자로 들어온 경우 변환
    const typeMap = {
        '1': 'ATTACK',
        '2': 'DEFENSE',
        '3': 'SPEED',
        '4': 'BALANCED'
    };
    const finalTrainingType = typeMap[trainingType] || trainingType.toUpperCase();
    
    if (!['ATTACK', 'DEFENSE', 'SPEED', 'BALANCED'].includes(finalTrainingType)) {
        showError('잘못된 훈련 타입입니다.');
        return;
    }
    
    trainSpirit(spiritId, finalTrainingType);
}

/**
 * 정령 훈련
 */
async function trainSpirit(spiritId, trainingType) {
    try {
        const response = await fetch('/training/api/train', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${spiritId}&trainingType=${trainingType}`
        });
        
        const result = await response.json();
        
        if (result.success) {
            showSuccess('훈련이 완료되었습니다!');
            location.reload(); // 페이지 새로고침
        } else {
            showError('훈련 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error training spirit:', error);
        showError('훈련 중 오류가 발생했습니다: ' + error.message);
    }
}

// 전역 변수: 인벤토리 관련
let currentSpiritIdForInventory = null;
let inventoryModal = null;

/**
 * 아이템 사용 모달 열기 (인벤토리 UI 사용)
 */
async function openItemUseModal(spiritId) {
    console.log('openItemUseModal called with spiritId:', spiritId);
    currentSpiritIdForInventory = spiritId;
    
    // 인벤토리 모달이 없으면 생성
    if (!inventoryModal || !document.getElementById('spiritInventoryModal')) {
        console.log('Creating inventory modal...');
        createInventoryModal();
    }
    
    // 모달 요소 다시 가져오기
    inventoryModal = document.getElementById('spiritInventoryModal');
    if (!inventoryModal) {
        console.error('Failed to create inventory modal');
        showError('인벤토리 모달을 생성할 수 없습니다.');
        return;
    }
    
    // 아이템 목록 로드
    await loadInventoryForSpirit(spiritId);
    
    // 모달 표시
    console.log('Showing inventory modal');
    inventoryModal.style.display = 'flex';
    inventoryModal.style.visibility = 'visible';
    inventoryModal.style.opacity = '1';
    inventoryModal.classList.add('active');
    document.body.style.overflow = 'hidden';
}

/**
 * 인벤토리 모달 생성
 */
function createInventoryModal() {
    // 기존 모달이 있으면 제거
    const existing = document.getElementById('spiritInventoryModal');
    if (existing) {
        existing.remove();
    }
    
    // 모달 HTML 생성
    const modalHTML = `
        <div id="spiritInventoryModal" class="inventory-modal-overlay">
            <div class="inventory-modal-content">
                <div class="inventory-modal-header">
                    <h2>📦 아이템 인벤토리</h2>
                    <button class="inventory-modal-close" onclick="closeInventoryModal()">✕</button>
                </div>
                <div class="inventory-modal-body">
                    <!-- 인벤토리 탭 메뉴 -->
                    <div class="inventory-tabs">
                        <button class="inventory-tab-btn active" data-inventory-tab="all">전체</button>
                        <button class="inventory-tab-btn" data-inventory-tab="FOOD">소비</button>
                        <button class="inventory-tab-btn" data-inventory-tab="TOY">기타</button>
                    </div>
                    
                    <!-- 인벤토리 그리드 -->
                    <div class="inventory-window">
                        <div class="inventory-grid" id="spiritInventoryGrid">
                            <!-- 아이템들이 여기에 동적으로 추가됨 -->
                        </div>
                    </div>
                    
                    <!-- 아이템 툴팁 -->
                    <div id="spiritItemTooltip" class="item-tooltip" style="display: none;">
                        <div class="tooltip-header">
                            <span class="tooltip-item-name" id="spiritTooltipItemName"></span>
                        </div>
                        <div class="tooltip-status" id="spiritTooltipStatus"></div>
                        <div class="tooltip-description" id="spiritTooltipDescription"></div>
                        <div class="tooltip-effect" id="spiritTooltipEffect"></div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHTML);
    inventoryModal = document.getElementById('spiritInventoryModal');
    
    if (!inventoryModal) {
        console.error('Failed to create inventory modal element');
        return;
    }
    
    console.log('Inventory modal created successfully');
    
    // 인벤토리 탭 이벤트 설정
    setupSpiritInventoryTabs();
    
    // 모달 외부 클릭 시 닫기
    inventoryModal.addEventListener('click', function(e) {
        if (e.target === inventoryModal) {
            closeInventoryModal();
        }
    });
    
    // 닫기 버튼 이벤트 (onclick 대신 addEventListener 사용)
    const closeBtn = inventoryModal.querySelector('.inventory-modal-close');
    if (closeBtn) {
        closeBtn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            closeInventoryModal();
        });
    }
}

/**
 * 정령용 인벤토리 탭 설정
 */
function setupSpiritInventoryTabs() {
    const inventoryTabBtns = document.querySelectorAll('#spiritInventoryModal .inventory-tab-btn');
    let currentFilter = 'all';
    
    inventoryTabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            inventoryTabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.getAttribute('data-inventory-tab');
            renderSpiritInventoryItems(currentFilter);
        });
    });
}

/**
 * 정령용 인벤토리 아이템 로드
 */
async function loadInventoryForSpirit(spiritId) {
    try {
        const response = await fetch('/item/api/my-items', {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('아이템 목록을 불러올 수 없습니다.');
        }
        
        const myItems = await response.json();
        
        // 아이템 정보 가져오기
        const itemsResponse = await fetch('/item/api/available-items', {
            credentials: 'include'
        });
        
        if (!itemsResponse.ok) {
            throw new Error('아이템 정보를 불러올 수 없습니다.');
        }
        
        const availableItems = await itemsResponse.json();
        
        // 전역 변수에 저장 (renderSpiritInventoryItems에서 사용)
        window.spiritInventoryData = {
            myItems: myItems,
            availableItems: availableItems
        };
        
        renderSpiritInventoryItems('all');
    } catch (error) {
        console.error('Error loading inventory:', error);
        showError('아이템 목록을 불러오는 중 오류가 발생했습니다.');
    }
}

/**
 * 정령용 인벤토리 아이템 렌더링
 */
function renderSpiritInventoryItems(filter = 'all') {
    const grid = document.getElementById('spiritInventoryGrid');
    if (!grid || !window.spiritInventoryData) return;
    
    grid.innerHTML = '';
    
    const { myItems, availableItems } = window.spiritInventoryData;
    
    // 필터링
    let filteredItems = [];
    if (filter === 'all') {
        filteredItems = myItems;
    } else if (filter === 'FOOD') {
        filteredItems = myItems.filter(userItem => {
            const item = availableItems.find(i => i.id === userItem.itemId);
            return item && (item.itemType === 'FOOD' || item.itemType === 'MEDICINE' || item.itemType === 'VITAMIN' || item.itemType === 'ENERGY');
        });
    } else if (filter === 'TOY') {
        filteredItems = myItems.filter(userItem => {
            const item = availableItems.find(i => i.id === userItem.itemId);
            return item && (item.itemType === 'TOY' || item.itemType === 'STAT_BOOST' || item.itemType === 'LIFESPAN_EXTENSION');
        });
    }
    
    if (filteredItems.length === 0) {
        for (let i = 0; i < 40; i++) {
            const slot = createEmptyInventorySlotForSpirit();
            grid.appendChild(slot);
        }
        return;
    }
    
    // 아이템 슬롯 생성
    const maxSlots = 40;
    const itemMap = new Map();
    
    filteredItems.forEach(userItem => {
        const item = availableItems.find(i => i.id === userItem.itemId);
        if (item) {
            itemMap.set(userItem.itemId, {
                item: item,
                quantity: userItem.quantity,
                userItemId: userItem.id
            });
        }
    });
    
    let slotIndex = 0;
    itemMap.forEach((itemData, itemId) => {
        if (slotIndex < maxSlots) {
            const slot = createSpiritInventorySlot(itemData.item, itemData.quantity, itemData.userItemId);
            grid.appendChild(slot);
            slotIndex++;
        }
    });
    
    while (slotIndex < maxSlots) {
        const slot = createEmptyInventorySlotForSpirit();
        grid.appendChild(slot);
        slotIndex++;
    }
}

/**
 * 정령용 인벤토리 슬롯 생성
 */
function createSpiritInventorySlot(item, quantity, userItemId) {
    const slot = document.createElement('div');
    slot.className = 'inventory-slot item-slot';
    slot.setAttribute('data-item-id', item.id);
    slot.setAttribute('data-user-item-id', userItemId);
    
    const icon = getItemIconForSpirit(item.itemType);
    
    slot.innerHTML = `
        <div class="slot-icon">${icon}</div>
        ${quantity > 1 ? `<div class="slot-quantity">${quantity}</div>` : ''}
    `;
    
    // 더블클릭 이벤트 추가
    slot.addEventListener('dblclick', function() {
        if (currentSpiritIdForInventory) {
            useItemOnSpirit(currentSpiritIdForInventory, item.id);
            closeInventoryModal();
        }
    });
    
    // 마우스 호버 시 툴팁 표시
    slot.addEventListener('mouseenter', function(e) {
        showSpiritItemTooltip(item, e);
    });
    
    slot.addEventListener('mouseleave', function() {
        hideSpiritItemTooltip();
    });
    
    slot.addEventListener('mousemove', function(e) {
        updateSpiritTooltipPosition(e);
    });
    
    return slot;
}

/**
 * 빈 인벤토리 슬롯 생성 (정령용)
 */
function createEmptyInventorySlotForSpirit() {
    const slot = document.createElement('div');
    slot.className = 'inventory-slot empty-slot';
    return slot;
}

/**
 * 아이템 아이콘 반환 (정령용)
 */
function getItemIconForSpirit(itemType) {
    const icons = {
        'FOOD': '🍖',
        'MEDICINE': '💊',
        'TOY': '🎲',
        'VITAMIN': '💊',
        'STAT_BOOST': '⚡',
        'ENERGY': '⚡',
        'LIFESPAN_EXTENSION': '🍎'
    };
    return icons[itemType] || '📦';
}

/**
 * 정령용 아이템 툴팁 표시
 */
function showSpiritItemTooltip(item, event) {
    const tooltip = document.getElementById('spiritItemTooltip');
    if (!tooltip) return;
    
    const tooltipName = document.getElementById('spiritTooltipItemName');
    const tooltipStatus = document.getElementById('spiritTooltipStatus');
    const tooltipDescription = document.getElementById('spiritTooltipDescription');
    const tooltipEffect = document.getElementById('spiritTooltipEffect');
    
    if (tooltipName) tooltipName.textContent = item.itemName || '알 수 없음';
    if (tooltipStatus) tooltipStatus.textContent = '교환 불가';
    if (tooltipDescription) tooltipDescription.textContent = item.description || '설명 없음';
    
    if (tooltipEffect) {
        const effectText = getEffectTextForSpirit(item);
        if (effectText) {
            tooltipEffect.textContent = effectText;
            tooltipEffect.style.display = 'block';
        } else {
            tooltipEffect.style.display = 'none';
        }
    }
    
    tooltip.style.display = 'block';
    updateSpiritTooltipPosition(event);
}

/**
 * 정령용 아이템 툴팁 숨기기
 */
function hideSpiritItemTooltip() {
    const tooltip = document.getElementById('spiritItemTooltip');
    if (tooltip) {
        tooltip.style.display = 'none';
    }
}

/**
 * 정령용 툴팁 위치 업데이트
 */
function updateSpiritTooltipPosition(event) {
    const tooltip = document.getElementById('spiritItemTooltip');
    if (!tooltip || tooltip.style.display === 'none') return;
    
    const x = event.clientX + 10;
    const y = event.clientY + 10;
    
    tooltip.style.left = x + 'px';
    tooltip.style.top = y + 'px';
}

/**
 * 효과 텍스트 반환 (정령용)
 */
function getEffectTextForSpirit(item) {
    if (!item.effectType || !item.effectValue) {
        return '';
    }
    
    const effects = {
        'HEALTH': '건강 회복',
        'HAPPINESS': `행복도 +${item.effectValue}`,
        'ENERGY': `에너지 +${item.effectValue}`,
        'STAT_BOOST': `${getStatNameForSpirit(item.targetStat)} +${item.effectValue}`,
        'HUNGER': `배고픔 -${item.effectValue}`,
        'LIFESPAN': `수명 +${item.effectValue}일`
    };
    
    return effects[item.effectType] || '';
}

/**
 * 능력치 이름 반환 (정령용)
 */
function getStatNameForSpirit(targetStat) {
    const stats = {
        'RANGED_ATTACK': '원거리 공격',
        'MELEE_ATTACK': '근거리 공격',
        'SPEED': '스피드',
        'RANGED_DEFENSE': '원거리 방어',
        'MELEE_DEFENSE': '근거리 방어'
    };
    return stats[targetStat] || targetStat;
}

/**
 * 인벤토리 모달 닫기
 */
function closeInventoryModal() {
    console.log('closeInventoryModal called');
    const modal = document.getElementById('spiritInventoryModal');
    if (modal) {
        modal.style.display = 'none';
        modal.style.visibility = 'hidden';
        modal.style.opacity = '0';
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }
    if (inventoryModal) {
        inventoryModal.style.display = 'none';
        inventoryModal.style.visibility = 'hidden';
        inventoryModal.style.opacity = '0';
        inventoryModal.classList.remove('active');
    }
    currentSpiritIdForInventory = null;
}

/**
 * 정령에게 아이템 사용
 */
async function useItemOnSpirit(spiritId, itemId) {
    try {
        const response = await fetch('/item/api/use', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${spiritId}&itemId=${itemId}`
        });
        
        const result = await response.json();
        
        if (result.success) {
            showSuccess('아이템을 사용했습니다!');
            location.reload(); // 페이지 새로고침
        } else {
            showError('아이템 사용 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error using item:', error);
        showError('아이템 사용 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 대회 참가 모달 열기
 */
async function openCompetitionModal(spiritId) {
    const spirit = spiritsData.find(s => s.id === spiritId);
    if (!spirit) {
        return;
    }
    
    if (spirit.energy < 30) {
        showWarning('에너지가 부족합니다. (최소 30 필요)');
        return;
    }
    
    const competitionType = await showPrompt('대회 난이도를 선택하세요:\n1. EASY (쉬움) - 상금: 100골드\n2. NORMAL (보통) - 상금: 300골드\n3. HARD (어려움) - 상금: 500골드\n4. EXPERT (전문가) - 상금: 1000골드', 'NORMAL', '대회 난이도 선택');
    
    if (!competitionType) {
        return;
    }
    
    // 입력값이 숫자로 들어온 경우 변환
    const compTypeMap = {
        '1': 'EASY',
        '2': 'NORMAL',
        '3': 'HARD',
        '4': 'EXPERT'
    };
    const finalCompetitionType = compTypeMap[competitionType] || competitionType.toUpperCase();
    
    if (!['EASY', 'NORMAL', 'HARD', 'EXPERT'].includes(finalCompetitionType)) {
        showError('잘못된 대회 난이도입니다.');
        return;
    }
    
    participateInCompetition(spiritId, finalCompetitionType);
}

/**
 * 대회 참가
 */
async function participateInCompetition(spiritId, competitionType) {
    try {
        const response = await fetch('/competition/api/participate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${spiritId}&competitionType=${competitionType}`
        });
        
        const result = await response.json();
        
        if (result.success) {
            if (result.won) {
                showSuccess(`🎉 대회에서 승리했습니다!\n상금: ${result.prizeMoney}골드\n승률: ${result.winChance}%`);
            } else {
                showInfo(`대회에서 패배했습니다.\n승률: ${result.winChance}%`);
            }
            location.reload(); // 페이지 새로고침
        } else {
            showError('대회 참가 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error participating in competition:', error);
        showError('대회 참가 중 오류가 발생했습니다: ' + error.message);
    }
}

