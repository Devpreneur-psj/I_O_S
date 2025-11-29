// 고대 기록실 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // 패치 노트 카드에 애니메이션 효과 추가
    const patchCards = document.querySelectorAll('.patch-note-card');
    
    patchCards.forEach((card, index) => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        
        setTimeout(() => {
            card.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, index * 100);
    });
});

