// 정령 도서관 JavaScript

// filterSpirits 함수 정의
function filterSpirits(filter) {
    const cards = document.querySelectorAll('.spirit-id-card');
    const filterBtns = document.querySelectorAll('.filter-btn');
    
    // 필터 버튼 활성화 상태 업데이트
    filterBtns.forEach(btn => {
        if (btn.getAttribute('data-filter') === filter) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
    
    cards.forEach(card => {
        const isRare = card.getAttribute('data-rare') === 'true';
        const typeName = card.getAttribute('data-type-name');
        const isOwned = typeof userSpiritTypes !== 'undefined' && userSpiritTypes && userSpiritTypes.includes(typeName);
        
        let show = false;
        
        switch (filter) {
            case 'all':
                show = true;
                break;
            case 'common':
                show = !isRare;
                break;
            case 'rare':
                show = isRare;
                break;
            case 'owned':
                show = isOwned;
                break;
            case 'not-owned':
                show = !isOwned;
                break;
            default:
                show = true;
        }
        
        if (show) {
            card.style.display = 'block';
            setTimeout(() => {
                card.style.opacity = '1';
                card.style.transform = 'scale(1)';
            }, 10);
        } else {
            card.style.opacity = '0';
            card.style.transform = 'scale(0.95)';
            setTimeout(() => {
                card.style.display = 'none';
            }, 300);
        }
    });
}

// DOM 로드 완료 후 초기화
document.addEventListener('DOMContentLoaded', function() {
    console.log('Spirit Codex script loaded');
    // 초기 필터: 전체
    try {
        filterSpirits('all');
    } catch (error) {
        console.error('Error initializing filter:', error);
    }
});
