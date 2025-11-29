// 친구 목록 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    console.log('친구 목록 페이지 로드됨');
    
    // 친구 목록 로드
    loadFriends();
    
    // 추천 친구 로드
    loadRecommendedFriends();
});

/**
 * 친구 목록 로드
 */
async function loadFriends() {
    try {
        // 친구 목록은 서버에서 이미 로드되어 있음
        // 여기서는 UI만 업데이트
        const friendsList = document.getElementById('friendsList');
        // 서버에서 전달된 데이터를 사용하여 렌더링
    } catch (error) {
        console.error('친구 목록 로드 실패:', error);
    }
}

/**
 * 추천 친구 로드
 */
async function loadRecommendedFriends() {
    try {
        // 추천 친구 목록은 서버에서 이미 로드되어 있음
        // 여기서는 UI만 업데이트
        const recommendedFriends = document.getElementById('recommendedFriends');
        // 서버에서 전달된 데이터를 사용하여 렌더링
    } catch (error) {
        console.error('추천 친구 로드 실패:', error);
    }
}

/**
 * 유저 검색
 */
async function searchUsers() {
    const searchInput = document.getElementById('searchInput');
    const keyword = searchInput.value.trim();
    
    if (!keyword) {
        alert('검색어를 입력하세요.');
        return;
    }
    
    try {
        const response = await fetch(`/friend/api/search?keyword=${encodeURIComponent(keyword)}`, {
            credentials: 'include'
        });
        
        const data = await response.json();
        if (!data.success) {
            alert(data.message || '검색에 실패했습니다.');
            return;
        }
        
        displaySearchResults(data.users || []);
    } catch (error) {
        console.error('유저 검색 실패:', error);
        alert('검색 중 오류가 발생했습니다.');
    }
}

/**
 * 검색 결과 표시
 */
function displaySearchResults(users) {
    const searchResults = document.getElementById('searchResults');
    searchResults.innerHTML = '';
    
    if (users.length === 0) {
        searchResults.innerHTML = '<div class="no-results">검색 결과가 없습니다.</div>';
        return;
    }
    
    users.forEach(user => {
        const userCard = document.createElement('div');
        userCard.className = 'user-card';
        userCard.innerHTML = `
            <div class="user-info">
                <div class="user-name">${user.nickname || user.username}</div>
                <div class="user-id">@${user.username}</div>
            </div>
            <button class="btn-add-friend" onclick="sendFriendRequest('${user.id}')">친구 추가</button>
        `;
        searchResults.appendChild(userCard);
    });
}

/**
 * 검색 키 입력 처리
 */
function handleSearchKeyPress(event) {
    if (event.key === 'Enter') {
        searchUsers();
    }
}

/**
 * 친구 요청 보내기
 */
async function sendFriendRequest(friendId) {
    // friendId가 유효한지 확인
    if (!friendId || friendId === 'undefined' || friendId === 'null') {
        console.error('유효하지 않은 friendId:', friendId);
        alert('친구를 찾을 수 없습니다.');
        return;
    }
    
    // friendId를 숫자로 변환
    const friendIdNum = parseInt(friendId);
    if (isNaN(friendIdNum) || friendIdNum <= 0) {
        console.error('유효하지 않은 friendId 형식:', friendId);
        alert('친구 ID가 올바르지 않습니다.');
        return;
    }
    
    try {
        const response = await fetch('/friend/api/send-request', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: new URLSearchParams({
                friendId: friendIdNum.toString()
            })
        });
        
        // 응답이 JSON인지 확인
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            const text = await response.text();
            console.error('예상치 못한 응답 형식:', text);
            alert('서버에서 오류가 발생했습니다. 상태 코드: ' + response.status);
            return;
        }
        
        const data = await response.json();
        if (data.success) {
            alert(data.message || '친구 요청을 보냈습니다.');
            location.reload();
        } else {
            alert(data.message || '친구 요청 전송에 실패했습니다.');
        }
    } catch (error) {
        console.error('친구 요청 전송 실패:', error);
        alert('친구 요청 전송 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 선물 모달 열기
 */
async function openGiftModal(friendId) {
    // 선물할 아이템 목록 가져오기
    try {
        const response = await fetch('/item/api/my-items', {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('아이템 목록을 불러올 수 없습니다.');
        }
        
        const data = await response.json();
        if (!data.success || !data.items) {
            alert('아이템 목록을 불러올 수 없습니다.');
            return;
        }
        
        // 먹이 아이템만 필터링
        const foodItems = data.items.filter(item => item.itemType === 'FOOD' && item.quantity > 0);
        
        if (foodItems.length === 0) {
            alert('선물할 수 있는 먹이 아이템이 없습니다.');
            return;
        }
        
        // 선물 모달 표시 (간단한 구현)
        const itemList = foodItems.map(item => 
            `${item.itemName} (${item.quantity}개)`
        ).join('\n');
        
        const itemId = prompt(`선물할 아이템을 선택하세요:\n${itemList}\n\n아이템 ID를 입력하세요:`, '');
        if (!itemId) return;
        
        const quantity = prompt('선물할 수량을 입력하세요:', '1');
        if (!quantity) return;
        
        await sendGift(friendId, parseInt(itemId), parseInt(quantity));
        
    } catch (error) {
        console.error('선물 모달 열기 실패:', error);
        alert('선물 모달을 여는 중 오류가 발생했습니다.');
    }
}

/**
 * 선물 보내기
 */
async function sendGift(friendId, itemId, quantity) {
    try {
        const response = await fetch('/friend/api/send-gift', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: new URLSearchParams({
                friendId: friendId,
                itemId: itemId,
                quantity: quantity
            })
        });
        
        const data = await response.json();
        if (data.success) {
            alert(data.message || '선물을 보냈습니다.');
        } else {
            alert(data.message || '선물 전송에 실패했습니다.');
        }
    } catch (error) {
        console.error('선물 전송 실패:', error);
        alert('선물 전송 중 오류가 발생했습니다.');
    }
}

/**
 * 친구 요청 수락
 */
async function acceptFriendRequest(friendId) {
    // friendId가 유효한지 확인
    if (!friendId || friendId === 'undefined' || friendId === 'null') {
        console.error('유효하지 않은 friendId:', friendId);
        alert('친구를 찾을 수 없습니다.');
        return;
    }
    
    // friendId를 숫자로 변환
    const friendIdNum = parseInt(friendId);
    if (isNaN(friendIdNum) || friendIdNum <= 0) {
        console.error('유효하지 않은 friendId 형식:', friendId);
        alert('친구 ID가 올바르지 않습니다.');
        return;
    }
    
    try {
        // 버튼 비활성화
        const buttons = document.querySelectorAll(`[onclick*="acceptFriendRequest(${friendId})"]`);
        buttons.forEach(btn => {
            btn.disabled = true;
            btn.style.opacity = '0.6';
        });
        
        const response = await fetch('/friend/api/accept-request', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: new URLSearchParams({
                friendId: friendIdNum.toString()
            })
        });
        
        // 응답이 JSON인지 확인
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            const text = await response.text();
            console.error('예상치 못한 응답 형식:', text);
            alert('서버에서 오류가 발생했습니다. 상태 코드: ' + response.status);
            // 버튼 다시 활성화
            buttons.forEach(btn => {
                btn.disabled = false;
                btn.style.opacity = '1';
            });
            return;
        }
        
        const data = await response.json();
        if (data.success) {
            // 친구 요청 목록에서 제거
            removePendingRequest(friendIdNum);
            
            // 친구 목록 새로고침
            setTimeout(() => {
                location.reload();
            }, 500);
        } else {
            alert(data.message || '친구 요청 수락에 실패했습니다.');
            // 버튼 다시 활성화
            buttons.forEach(btn => {
                btn.disabled = false;
                btn.style.opacity = '1';
            });
        }
    } catch (error) {
        console.error('친구 요청 수락 실패:', error);
        alert('친구 요청 수락 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 친구 요청 목록에서 제거
 */
function removePendingRequest(friendId) {
    const pendingRequests = document.getElementById('pendingRequests');
    if (pendingRequests) {
        const requestCards = pendingRequests.querySelectorAll('.user-card');
        requestCards.forEach(card => {
            const buttons = card.querySelectorAll('button');
            buttons.forEach(btn => {
                if (btn.getAttribute('onclick') && btn.getAttribute('onclick').includes(`acceptFriendRequest(${friendId})`)) {
                    card.style.opacity = '0';
                    card.style.transform = 'translateX(-20px)';
                    setTimeout(() => {
                        card.remove();
                    }, 300);
                }
            });
        });
        
        // 친구 요청이 없으면 섹션 숨기기
        if (pendingRequests.children.length === 0) {
            const section = pendingRequests.closest('.pending-requests-section');
            if (section) {
                section.style.opacity = '0';
                setTimeout(() => {
                    section.style.display = 'none';
                }, 300);
            }
        }
    }
}

/**
 * 친구 요청 거절
 */
async function rejectFriendRequest(friendId) {
    // friendId가 유효한지 확인
    if (!friendId || friendId === 'undefined' || friendId === 'null') {
        console.error('유효하지 않은 friendId:', friendId);
        alert('친구를 찾을 수 없습니다.');
        return;
    }
    
    // friendId를 숫자로 변환
    const friendIdNum = parseInt(friendId);
    if (isNaN(friendIdNum) || friendIdNum <= 0) {
        console.error('유효하지 않은 friendId 형식:', friendId);
        alert('친구 ID가 올바르지 않습니다.');
        return;
    }
    
    if (!confirm('친구 요청을 거절하시겠습니까?')) {
        return;
    }
    
    try {
        const response = await fetch('/friend/api/reject-request', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: new URLSearchParams({
                friendId: friendIdNum.toString()
            })
        });
        
        // 응답이 JSON인지 확인
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            const text = await response.text();
            console.error('예상치 못한 응답 형식:', text);
            alert('서버에서 오류가 발생했습니다. 상태 코드: ' + response.status);
            return;
        }
        
        const data = await response.json();
        if (data.success) {
            alert(data.message || '친구 요청을 거절했습니다.');
            location.reload();
        } else {
            alert(data.message || '친구 요청 거절에 실패했습니다.');
        }
    } catch (error) {
        console.error('친구 요청 거절 실패:', error);
        alert('친구 요청 거절 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 친구의 정령 마을 방문
 */
function visitFriendVillage(friendId) {
    window.location.href = `/friend/visit/${friendId}`;
}

