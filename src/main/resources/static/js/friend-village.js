// 친구의 정령 마을 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    console.log('친구의 정령 마을 페이지 로드됨');
    
    const friendId = window.friendId;
    if (friendId) {
        loadFriendSpirits(friendId);
    }
});

/**
 * 친구의 정령 목록 로드
 */
async function loadFriendSpirits(friendId) {
    try {
        const response = await fetch(`/spirit/api/friend-spirits?friendId=${friendId}`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('정령 목록을 불러올 수 없습니다.');
        }
        
        const data = await response.json();
        if (!data.success || !data.spirits) {
            alert('정령 목록을 불러올 수 없습니다.');
            return;
        }
        
        displayFriendSpirits(data.spirits);
    } catch (error) {
        console.error('친구의 정령 목록 로드 실패:', error);
        alert('정령 목록을 불러오는 중 오류가 발생했습니다.');
    }
}

/**
 * 친구의 정령 표시
 */
function displayFriendSpirits(spirits) {
    const container = document.getElementById('spiritsContainer');
    container.innerHTML = '';
    
    if (spirits.length === 0) {
        container.innerHTML = '<div class="no-spirits">정령이 없습니다.</div>';
        return;
    }
    
    spirits.forEach(spirit => {
        const spiritCard = document.createElement('div');
        spiritCard.className = 'spirit-card';
        
        const step = (spirit.evolutionStage || 0) + 1;
        const typeCode = getTypeCode(spirit.spiritType);
        const imagePath = `/images/spirits/step${step}_${typeCode}.png`;
        
        spiritCard.innerHTML = `
            <img src="${imagePath}" alt="${spirit.name}" class="spirit-image">
            <div class="spirit-info">
                <div class="spirit-name">${spirit.name || '이름없음'}</div>
                <div class="spirit-type">${spirit.spiritType}</div>
                <div class="spirit-level">Lv.${spirit.level || 1}</div>
            </div>
        `;
        
        container.appendChild(spiritCard);
    });
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

