/**
 * 모든 모달창에 드래그 기능을 추가하는 공통 유틸리티
 */

// 드래그 상태 저장소
const modalDragStates = new Map();

/**
 * 모달에 드래그 기능 추가
 * @param {HTMLElement} modal - 드래그 가능하게 만들 모달 요소
 * @param {HTMLElement} dragHandle - 드래그 핸들 (헤더 부분, 기본값: .modal-header 또는 .modal-content의 첫 번째 자식)
 */
function makeModalDraggable(modal, dragHandle = null) {
    if (!modal) return;
    
    // 이미 드래그 설정이 되어 있으면 스킵
    if (modalDragStates.has(modal)) {
        return;
    }
    
    // 드래그 핸들 찾기 (더 넓은 범위로 검색)
    if (!dragHandle) {
        // 1. 모달 오버레이 내부의 modal-header 찾기
        dragHandle = modal.querySelector('.modal-header');
        
        // 2. modal-content 내부의 modal-header 찾기
        if (!dragHandle) {
            const modalContent = modal.querySelector('.modal-content');
            if (modalContent) {
                dragHandle = modalContent.querySelector('.modal-header');
            }
        }
        
        // 3. modal-title 찾기
        if (!dragHandle) {
            dragHandle = modal.querySelector('.modal-title')?.closest('.modal-header') ||
                         modal.querySelector('.modal-title');
        }
        
        // 4. modal-content 자체를 드래그 핸들로 사용
        if (!dragHandle) {
            dragHandle = modal.querySelector('.modal-content');
        }
    }
    
    if (!dragHandle) {
        // 경고 메시지 개선 (디버깅 정보 추가)
        console.warn('드래그 핸들을 찾을 수 없습니다. 모달 구조:', {
            modal: modal,
            modalClass: modal.className,
            modalId: modal.id,
            hasModalHeader: !!modal.querySelector('.modal-header'),
            hasModalContent: !!modal.querySelector('.modal-content'),
            hasModalTitle: !!modal.querySelector('.modal-title')
        });
        return;
    }
    
    // 드래그 상태 초기화
    const dragState = {
        isDragging: false,
        currentX: 0,
        currentY: 0,
        initialX: 0,
        initialY: 0,
        xOffset: 0,
        yOffset: 0,
        modal: modal,
        dragHandle: dragHandle
    };
    
    modalDragStates.set(modal, dragState);
    
    // 모달 콘텐츠 찾기
    const modalContent = modal.querySelector('.modal-content') || modal;
    if (!modalContent) return;
    
    // 초기 위치 저장
    const rect = modalContent.getBoundingClientRect();
    dragState.xOffset = 0;
    dragState.yOffset = 0;
    
    // 드래그 핸들 스타일 설정
    dragHandle.style.cursor = 'move';
    dragHandle.style.userSelect = 'none';
    
    // 이벤트 리스너 추가
    dragHandle.addEventListener('mousedown', (e) => {
        // 버튼이나 입력 필드 클릭 시 드래그 방지
        if (e.target.tagName === 'BUTTON' || 
            e.target.tagName === 'INPUT' ||
            e.target.tagName === 'TEXTAREA' ||
            e.target.closest('button') ||
            e.target.closest('input') ||
            e.target.closest('.close') ||
            e.target.closest('.modal-close-btn')) {
            return;
        }
        
        dragStart(e, dragState);
    });
    
    // 모달이 열릴 때마다 위치 초기화
    const observer = new MutationObserver(() => {
        if (modal.style.display !== 'none' && modal.style.display !== '') {
            resetModalPosition(modal, dragState);
        }
    });
    
    observer.observe(modal, {
        attributes: true,
        attributeFilter: ['style']
    });
}

/**
 * 드래그 시작
 */
function dragStart(e, dragState) {
    dragState.initialX = e.clientX - dragState.xOffset;
    dragState.initialY = e.clientY - dragState.yOffset;
    
    if (e.target === dragState.dragHandle || dragState.dragHandle.contains(e.target)) {
        dragState.isDragging = true;
        document.addEventListener('mousemove', dragHandler);
        document.addEventListener('mouseup', dragEndHandler);
        e.preventDefault();
    }
}

/**
 * 드래그 중
 */
function drag(e) {
    modalDragStates.forEach((dragState) => {
        if (dragState.isDragging) {
            e.preventDefault();
            dragState.currentX = e.clientX - dragState.initialX;
            dragState.currentY = e.clientY - dragState.initialY;
            
            dragState.xOffset = dragState.currentX;
            dragState.yOffset = dragState.currentY;
            
            const modalContent = dragState.modal.querySelector('.modal-content') || dragState.modal;
            setTranslate(dragState.currentX, dragState.currentY, modalContent);
        }
    });
}

/**
 * 드래그 종료
 */
function dragEnd(e) {
    modalDragStates.forEach((dragState) => {
        if (dragState.isDragging) {
            dragState.initialX = dragState.currentX;
            dragState.initialY = dragState.currentY;
            dragState.isDragging = false;
            document.removeEventListener('mousemove', dragHandler);
            document.removeEventListener('mouseup', dragEndHandler);
        }
    });
}

// 전역 이벤트 핸들러
const dragHandler = (e) => drag(e);
const dragEndHandler = (e) => dragEnd(e);

/**
 * 위치 설정
 */
function setTranslate(xPos, yPos, el) {
    el.style.transform = `translate(${xPos}px, ${yPos}px)`;
    el.style.position = 'relative';
}

/**
 * 모달 위치 초기화
 */
function resetModalPosition(modal, dragState) {
    const modalContent = modal.querySelector('.modal-content') || modal;
    if (modalContent) {
        modalContent.style.transform = 'translate(0px, 0px)';
        dragState.xOffset = 0;
        dragState.yOffset = 0;
        dragState.currentX = 0;
        dragState.currentY = 0;
    }
}

/**
 * 모든 모달에 자동으로 드래그 기능 추가
 */
function initAllModalsDraggable() {
    // 모든 모달 찾기 (.modal-overlay 포함)
    const modals = document.querySelectorAll('.modal-overlay, .modal, [class*="modal"], [id*="Modal"], [id*="modal"]');
    
    modals.forEach(modal => {
        // 모달이 실제로 표시되는 요소인지 확인
        if (modal.classList.contains('modal-overlay') ||
            modal.classList.contains('modal') || 
            modal.id.includes('Modal') || 
            modal.id.includes('modal')) {
            // modal-overlay인 경우 modal-content를 찾아서 확인
            if (modal.classList.contains('modal-overlay')) {
                const modalContent = modal.querySelector('.modal-content');
                if (modalContent) {
                    makeModalDraggable(modal);
                }
            } else {
                makeModalDraggable(modal);
            }
        }
    });
    
    // 동적으로 생성된 모달을 위한 MutationObserver
    const observer = new MutationObserver(() => {
        const newModals = document.querySelectorAll('.modal-overlay, .modal, [class*="modal"], [id*="Modal"], [id*="modal"]');
        newModals.forEach(modal => {
            if (!modalDragStates.has(modal)) {
                // modal-overlay인 경우 modal-content를 찾아서 확인
                if (modal.classList.contains('modal-overlay')) {
                    const modalContent = modal.querySelector('.modal-content');
                    if (modalContent) {
                        makeModalDraggable(modal);
                    }
                } else {
                    makeModalDraggable(modal);
                }
            }
        });
    });
    
    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
}

// DOM 로드 시 자동 초기화
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAllModalsDraggable);
} else {
    initAllModalsDraggable();
}

