// 튜토리얼 시스템 JavaScript

let currentTutorialStep = 0;
let tutorialSteps = [];
let tutorialOverlay = null;
let highlightedElement = null;
let highlightBox = null;

/**
 * 튜토리얼 초기화
 */
function initTutorial() {
    // 튜토리얼 상태 확인
    checkTutorialStatus().then(completed => {
        if (!completed) {
            setupTutorialSteps();
            showTutorialStep(0);
        }
    });
}

/**
 * 튜토리얼 상태 확인
 */
async function checkTutorialStatus() {
    try {
        const response = await fetch('/tutorial/api/status', {
            credentials: 'include'
        });
        const data = await response.json();
        return data.completed === true;
    } catch (error) {
        console.error('튜토리얼 상태 확인 실패:', error);
        return true; // 에러 시 튜토리얼 건너뛰기
    }
}

/**
 * 튜토리얼 단계 설정
 */
function setupTutorialSteps() {
    tutorialSteps = [
        {
            title: '정령의 섬에 오신 것을 환영합니다!',
            description: '이 게임은 정령을 키우고 세계수를 성장시키는 판타지 게임입니다.',
            icon: '🌟',
            showSkip: true
        },
        {
            title: '월드맵',
            description: '이곳은 월드맵입니다. 다양한 시설들을 클릭하여 이동할 수 있습니다.',
            icon: '🗺️',
            highlight: '.main-hub',
            position: 'bottom',
            showSkip: true
        },
        {
            title: '세계수의 심장',
            description: '가운데에 있는 세계수의 심장을 클릭하세요. 여기서 레벨업을 할 수 있습니다.',
            icon: '💚',
            highlight: '.main-hub',
            position: 'bottom',
            action: () => {
                // 다음 버튼 클릭 시 자동으로 세계수의 심장으로 이동
            },
            showSkip: true
        },
        {
            title: '튜토리얼 완료!',
            description: '이제 세계수의 심장으로 이동하여 레벨업을 시작하세요!',
            icon: '✨',
            showSkip: false,
            isLast: true
        }
    ];
}

/**
 * 튜토리얼 오버레이 생성
 */
function createTutorialOverlay() {
    if (tutorialOverlay) {
        return tutorialOverlay;
    }

    const overlay = document.createElement('div');
    overlay.className = 'tutorial-overlay';
    overlay.id = 'tutorialOverlay';
    
    document.body.appendChild(overlay);
    tutorialOverlay = overlay;
    return overlay;
}

/**
 * 하이라이트 박스 생성
 */
function createHighlightBox() {
    if (highlightBox) {
        removeHighlightBox();
    }

    const box = document.createElement('div');
    box.className = 'tutorial-highlight-box';
    box.id = 'tutorialHighlightBox';
    document.body.appendChild(box);
    highlightBox = box;
    return box;
}

/**
 * 하이라이트 박스 제거
 */
function removeHighlightBox() {
    if (highlightBox) {
        highlightBox.remove();
        highlightBox = null;
    }
    if (highlightedElement) {
        highlightedElement.classList.remove('tutorial-highlighted');
        highlightedElement = null;
    }
}

/**
 * 요소 하이라이트
 */
function highlightElement(selector, position = 'bottom') {
    removeHighlightBox();
    
    const element = document.querySelector(selector);
    if (!element) {
        console.warn('하이라이트할 요소를 찾을 수 없습니다:', selector);
        return null;
    }

    highlightedElement = element;
    element.classList.add('tutorial-highlighted');
    
    const box = createHighlightBox();
    const rect = element.getBoundingClientRect();
    
    box.style.left = (rect.left - 8) + 'px';
    box.style.top = (rect.top - 8) + 'px';
    box.style.width = (rect.width + 16) + 'px';
    box.style.height = (rect.height + 16) + 'px';
    
    // 화살표 위치 설정
    const arrow = document.querySelector('.tutorial-arrow');
    if (arrow) {
        arrow.className = 'tutorial-arrow ' + position;
    }
    
    return element;
}

/**
 * 튜토리얼 단계 표시
 */
function showTutorialStep(stepIndex) {
    if (stepIndex < 0 || stepIndex >= tutorialSteps.length) {
        completeTutorial();
        return;
    }

    currentTutorialStep = stepIndex;
    const step = tutorialSteps[stepIndex];

    const overlay = createTutorialOverlay();
    overlay.innerHTML = '';

    // 하이라이트 요소가 있으면 하이라이트
    if (step.highlight) {
        setTimeout(() => {
            highlightElement(step.highlight, step.position || 'bottom');
        }, 100);
    } else {
        removeHighlightBox();
    }

    // 화살표 (하이라이트가 있을 때만)
    let arrowHtml = '';
    if (step.highlight && step.position) {
        arrowHtml = `<div class="tutorial-arrow ${step.position}">➤</div>`;
    }

    // 튜토리얼 콘텐츠
    const content = document.createElement('div');
    content.className = 'tutorial-content';
    content.innerHTML = `
        ${arrowHtml}
        <div class="tutorial-header">
            <div class="tutorial-step-indicator">
                ${stepIndex + 1} / ${tutorialSteps.length}
            </div>
            <h2 class="tutorial-title">${step.title}</h2>
        </div>
        <div class="tutorial-body">
            <span class="tutorial-step-icon">${step.icon}</span>
            <p class="tutorial-description">${step.description}</p>
            ${step.tip ? `<div class="tutorial-tip">💡 ${step.tip}</div>` : ''}
        </div>
        <div class="tutorial-actions">
            ${step.showSkip && !step.isLast ? `
                <button class="tutorial-btn tutorial-btn-skip" onclick="skipTutorial()">건너뛰기</button>
            ` : ''}
            ${!step.isLast ? `
                <button class="tutorial-btn tutorial-btn-secondary" onclick="prevTutorialStep()">이전</button>
                <button class="tutorial-btn tutorial-btn-primary" onclick="nextTutorialStep()">다음</button>
            ` : `
                <button class="tutorial-btn tutorial-btn-primary" onclick="completeTutorial()">시작하기</button>
            `}
        </div>
    `;

    overlay.appendChild(content);
    overlay.classList.remove('hidden');

    // 액션이 있으면 실행
    if (step.action && typeof step.action === 'function') {
        step.action();
    }
}

/**
 * 다음 튜토리얼 단계
 */
function nextTutorialStep() {
    const step = tutorialSteps[currentTutorialStep];
    
    // 특별 액션이 있으면 실행
    if (step.action && typeof step.action === 'function') {
        step.action();
    }

    // 마지막 단계면 완료
    if (step.isLast) {
        completeTutorial();
        return;
    }

    // 다음 단계로 이동
    if (currentTutorialStep === 2) {
        // 세계수의 심장으로 이동
        completeTutorial();
        window.location.href = '/world-tree/heart?tutorial=true';
        return;
    }

    showTutorialStep(currentTutorialStep + 1);
}

/**
 * 이전 튜토리얼 단계
 */
function prevTutorialStep() {
    if (currentTutorialStep > 0) {
        showTutorialStep(currentTutorialStep - 1);
    }
}

/**
 * 튜토리얼 건너뛰기
 */
function skipTutorial() {
    if (confirm('튜토리얼을 건너뛰시겠습니까?')) {
        completeTutorial();
    }
}

/**
 * 튜토리얼 완료
 */
async function completeTutorial() {
    try {
        const response = await fetch('/tutorial/api/complete', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            removeTutorialOverlay();
        } else {
            console.error('튜토리얼 완료 처리 실패');
        }
    } catch (error) {
        console.error('튜토리얼 완료 중 오류:', error);
    }
    
    removeTutorialOverlay();
}

/**
 * 튜토리얼 오버레이 제거
 */
function removeTutorialOverlay() {
    if (tutorialOverlay) {
        tutorialOverlay.classList.add('hidden');
        setTimeout(() => {
            if (tutorialOverlay) {
                tutorialOverlay.remove();
                tutorialOverlay = null;
            }
        }, 300);
    }
    removeHighlightBox();
}

// 페이지 로드 시 튜토리얼 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 월드맵에서만 튜토리얼 실행
    if (window.location.pathname === '/world' || window.location.pathname === '/world/') {
        initTutorial();
    }
});

