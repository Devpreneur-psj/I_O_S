// 정령 연구소 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // 진화 진행 중인 정령들의 진행 상태 업데이트
    updateEvolutionProgress();
    
    // 30초마다 진행 상태 업데이트
    setInterval(updateEvolutionProgress, 30000);
});

/**
 * 진화 진행 상태 업데이트
 */
function updateEvolutionProgress() {
    const evolvingCards = document.querySelectorAll('.spirit-card.evolving');
    
    evolvingCards.forEach(card => {
        const progressText = card.querySelector('.progress-text');
        if (!progressText || !progressText.id) {
            return;
        }
        
        const spiritId = progressText.id.replace('progress-', '');
        fetchEvolutionStatus(spiritId, progressText, card);
    });
}

/**
 * 진화 상태 조회
 */
async function fetchEvolutionStatus(spiritId, progressElement, cardElement) {
    try {
        const response = await fetch(`/evolution/api/status/${spiritId}`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('진화 상태를 불러올 수 없습니다.');
        }
        
        const status = await response.json();
        
        if (!status.evolutionInProgress) {
            // 진화 완료
            progressElement.textContent = '진화 완료!';
            location.reload(); // 페이지 새로고침
            return;
        }
        
        // 남은 시간 표시
        const hours = status.remainingHours || 0;
        const minutes = status.remainingMinutes || 0;
        
        if (hours > 0) {
            progressElement.textContent = `남은 시간: 약 ${hours}시간 ${minutes}분`;
        } else if (minutes > 0) {
            progressElement.textContent = `남은 시간: 약 ${minutes}분`;
        } else {
            progressElement.textContent = '진화 완료 대기 중...';
        }
        
        // 진행률 계산
        const targetStage = status.evolutionTargetStage;
        const hoursRequired = targetStage == 1 ? 1 : 2;
        const elapsed = hoursRequired - hours - (minutes / 60);
        const progress = Math.min(100, Math.max(0, (elapsed / hoursRequired) * 100));
        
        const progressFill = cardElement.querySelector('.progress-fill');
        if (progressFill) {
            progressFill.style.width = progress + '%';
        }
        
    } catch (error) {
        console.error('Error fetching evolution status:', error);
        progressElement.textContent = '상태 확인 중 오류 발생';
    }
}

/**
 * 진화 시작
 */
async function startEvolution(spiritId) {
    const confirmed = await showConfirm('진화를 시작하시겠습니까? 진화 중에는 정령을 사용할 수 없습니다.', '진화 시작');
    if (!confirmed) {
        return;
    }
    
    try {
        const response = await fetch('/evolution/api/start', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${spiritId}`
        });
        
        const result = await response.json();
        
        if (result.success) {
            showSuccess('진화가 시작되었습니다!');
            location.reload(); // 페이지 새로고침
        } else {
            showError('진화 시작 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error starting evolution:', error);
        showError('진화 시작 중 오류가 발생했습니다: ' + error.message);
    }
}

