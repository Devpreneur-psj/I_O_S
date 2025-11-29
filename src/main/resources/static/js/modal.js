/**
 * 공통 모달 시스템
 * alert, confirm, prompt를 커스텀 모달로 대체
 */

// 모달 HTML 생성
function createModalHTML() {
    if (document.getElementById('commonModal')) {
        return; // 이미 존재하면 생성하지 않음
    }
    
    const modalHTML = `
        <div id="commonModal" class="common-modal">
            <div class="common-modal-overlay"></div>
            <div class="common-modal-content" id="commonModalContent">
                <div class="common-modal-header" id="commonModalHeader">
                    <h3 class="common-modal-title" id="modalTitle">알림</h3>
                    <button class="common-modal-close" id="modalCloseBtn">&times;</button>
                </div>
                <div class="common-modal-body">
                    <p class="common-modal-message" id="modalMessage"></p>
                    <div id="modalInputContainer" style="display: none; margin-top: 15px;">
                        <input type="text" id="modalInput" class="common-modal-input" placeholder="입력하세요">
                    </div>
                </div>
                <div class="common-modal-footer" id="modalFooter">
                    <button class="common-modal-btn common-modal-btn-primary" id="modalConfirmBtn">확인</button>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHTML);
    setupModalEvents();
    setupModalDrag();
}

// 모달 이벤트 설정
function setupModalEvents() {
    const modal = document.getElementById('commonModal');
    const overlay = modal.querySelector('.common-modal-overlay');
    const closeBtn = document.getElementById('modalCloseBtn');
    
    // 닫기 버튼
    closeBtn.addEventListener('click', closeModal);
    
    // 오버레이 클릭 시 닫기
    overlay.addEventListener('click', closeModal);
    
    // ESC 키로 닫기
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && modal.classList.contains('show')) {
            closeModal();
        }
    });
}

// 모달 드래그 앤 드롭 설정
function setupModalDrag() {
    const modal = document.getElementById('commonModal');
    const modalContent = document.getElementById('commonModalContent');
    const modalHeader = document.getElementById('commonModalHeader');
    
    if (!modal || !modalContent || !modalHeader) {
        return;
    }
    
    // 전역 드래그 상태 저장
    window.modalDragState = {
        isDragging: false,
        currentX: 0,
        currentY: 0,
        initialX: 0,
        initialY: 0,
        xOffset: 0,
        yOffset: 0
    };
    
    // 헤더를 드래그 핸들로 사용
    modalHeader.style.cursor = 'move';
    modalHeader.addEventListener('mousedown', dragStart);
    document.addEventListener('mousemove', drag);
    document.addEventListener('mouseup', dragEnd);
    
    function dragStart(e) {
        // 닫기 버튼이나 다른 버튼 클릭 시 드래그 방지
        if (e.target.id === 'modalCloseBtn' || 
            e.target.closest('#modalCloseBtn') ||
            e.target.closest('button') ||
            e.target.closest('input')) {
            return;
        }
        
        if (e.target === modalHeader || modalHeader.contains(e.target)) {
            window.modalDragState.initialX = e.clientX - window.modalDragState.xOffset;
            window.modalDragState.initialY = e.clientY - window.modalDragState.yOffset;
            window.modalDragState.isDragging = true;
        }
    }
    
    function drag(e) {
        if (window.modalDragState.isDragging) {
            e.preventDefault();
            window.modalDragState.currentX = e.clientX - window.modalDragState.initialX;
            window.modalDragState.currentY = e.clientY - window.modalDragState.initialY;
            
            window.modalDragState.xOffset = window.modalDragState.currentX;
            window.modalDragState.yOffset = window.modalDragState.currentY;
            
            setTranslate(window.modalDragState.currentX, window.modalDragState.currentY, modalContent);
        }
    }
    
    function dragEnd(e) {
        window.modalDragState.initialX = window.modalDragState.currentX;
        window.modalDragState.initialY = window.modalDragState.currentY;
        window.modalDragState.isDragging = false;
    }
    
    function setTranslate(xPos, yPos, el) {
        el.style.transform = `translate(${xPos}px, ${yPos}px)`;
    }
}

// 모달 열기
function openModal(options) {
    createModalHTML();
    
    const modal = document.getElementById('commonModal');
    const title = document.getElementById('modalTitle');
    const message = document.getElementById('modalMessage');
    const inputContainer = document.getElementById('modalInputContainer');
    const input = document.getElementById('modalInput');
    const footer = document.getElementById('modalFooter');
    const confirmBtn = document.getElementById('modalConfirmBtn');
    
    // 기본값 설정
    const config = {
        title: options.title || '알림',
        message: options.message || '',
        type: options.type || 'info', // info, success, error, warning
        showInput: options.showInput || false,
        inputPlaceholder: options.inputPlaceholder || '입력하세요',
        inputValue: options.inputValue || '',
        showCancel: options.showCancel || false,
        confirmText: options.confirmText || '확인',
        cancelText: options.cancelText || '취소',
        onConfirm: options.onConfirm || null,
        onCancel: options.onCancel || null,
        closeOnOverlay: options.closeOnOverlay !== false
    };
    
    // 모달 내용 설정
    title.textContent = config.title;
    message.textContent = config.message;
    
    // 타입에 따른 스타일 적용
    modal.className = 'common-modal';
    modal.classList.add('show', `modal-${config.type}`);
    
    // 입력 필드 표시/숨김
    if (config.showInput) {
        inputContainer.style.display = 'block';
        input.value = config.inputValue;
        input.placeholder = config.inputPlaceholder;
        input.focus();
    } else {
        inputContainer.style.display = 'none';
    }
    
    // 버튼 설정
    footer.innerHTML = '';
    
    if (config.showCancel) {
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'common-modal-btn common-modal-btn-secondary';
        cancelBtn.textContent = config.cancelText;
        cancelBtn.onclick = function() {
            if (config.onCancel) {
                config.onCancel();
            }
            closeModal();
        };
        footer.appendChild(cancelBtn);
    }
    
    confirmBtn.textContent = config.confirmText;
    confirmBtn.onclick = function() {
        let result = true;
        if (config.showInput) {
            result = input.value;
        }
        if (config.onConfirm) {
            config.onConfirm(result);
        }
        closeModal();
    };
    footer.appendChild(confirmBtn);
    
    // 오버레이 클릭 설정
    const overlay = modal.querySelector('.common-modal-overlay');
    overlay.onclick = config.closeOnOverlay ? closeModal : null;
    
    // 모달 표시
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
    
    // 드래그 위치 초기화
    if (window.modalDragState) {
        window.modalDragState.xOffset = 0;
        window.modalDragState.yOffset = 0;
        const modalContent = document.getElementById('commonModalContent');
        if (modalContent) {
            modalContent.style.transform = 'translate(0px, 0px)';
        }
    }
}

// 모달 닫기
function closeModal() {
    const modal = document.getElementById('commonModal');
    if (modal) {
        modal.classList.remove('show');
        document.body.style.overflow = '';
    }
}

// alert 대체
function showAlert(message, title = '알림', type = 'info') {
    return new Promise((resolve) => {
        openModal({
            title: title,
            message: message,
            type: type,
            showCancel: false,
            onConfirm: () => resolve(true)
        });
    });
}

// confirm 대체
function showConfirm(message, title = '확인', type = 'warning') {
    return new Promise((resolve) => {
        openModal({
            title: title,
            message: message,
            type: type,
            showCancel: true,
            confirmText: '확인',
            cancelText: '취소',
            onConfirm: () => resolve(true),
            onCancel: () => resolve(false)
        });
    });
}

// prompt 대체
function showPrompt(message, defaultValue = '', title = '입력', placeholder = '입력하세요') {
    return new Promise((resolve) => {
        openModal({
            title: title,
            message: message,
            type: 'info',
            showInput: true,
            inputValue: defaultValue,
            inputPlaceholder: placeholder,
            showCancel: true,
            confirmText: '확인',
            cancelText: '취소',
            onConfirm: (value) => resolve(value),
            onCancel: () => resolve(null)
        });
    });
}

// 성공 메시지
function showSuccess(message, title = '성공') {
    return showAlert(message, title, 'success');
}

// 에러 메시지
function showError(message, title = '오류') {
    return showAlert(message, title, 'error');
}

// 경고 메시지
function showWarning(message, title = '경고') {
    return showAlert(message, title, 'warning');
}

// 정보 메시지
function showInfo(message, title = '정보') {
    return showAlert(message, title, 'info');
}

