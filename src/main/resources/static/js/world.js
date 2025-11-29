// 월드맵 JavaScript

// 카테고리별 하위 시설 데이터
const categoryFacilities = {
    'village_life': {
        title: '🏡 기본 마을 및 생활',
        facilities: [
            { id: 'spirit_village', name: '정령의 마을', icon: '🏘️', description: '보유한 정령들을 관리하고 확인할 수 있습니다.' },
            { id: 'nature_garden', name: '자연의 정원', icon: '🌿', description: '정령들을 놀게 하고 친밀도를 높일 수 있습니다.' },
            { id: 'forest_well', name: '숲의 우물', icon: '💧', description: '정령들에게 물을 주고 회복시킬 수 있습니다.' }
        ]
    },
    'growth_research': {
        title: '정령대학교',
        facilities: [
            { id: 'spirit_codex', name: '정령 도서관', icon: '📖' },
            { id: 'arcane_lab', name: '정령 연구소', icon: '🔬' },
            { id: 'magic_academy', name: '기술 강의실', icon: '🎓' },
            { id: 'alchemist_hut', name: '연금 공방', icon: '⚗️' }
        ]
    },
    'shop_economy': {
        title: '🛒 상점 & 경제 활동',
        facilities: [
            { id: 'spirit_market', name: '정령 상점', icon: '🛒' },
            { id: 'mana_store', name: '마나 잡화점', icon: '🏪' },
            { id: 'barrier_exchange', name: '결계 포인트 교환소', icon: '💎' },
            { id: 'auction_hall', name: '정령 경매장', icon: '🏛️' }
        ]
    },
    'combat_adventure': {
        title: '⚔️ 전투 & 모험',
        facilities: [
            { id: 'spirit_arena', name: '정령 시합장', icon: '⚔️', description: '다른 유저의 정령과 전투하여 승리 포인트를 획득합니다.' },
            { id: 'training_grounds', name: '정령 수련장', icon: '🎯', description: '정령을 훈련시켜 능력치를 향상시킵니다.' },
            { id: 'explorer_trail', name: '탐험가의 길 (정령 던전)', icon: '🗺️', description: '던전을 탐험하여 경험치와 아이템을 획득합니다.' }
        ]
    },
    'event_minigame': {
        title: '🎪 이벤트 & 미니게임',
        facilities: [
            { id: 'mana_festival', name: '마나 페스티벌', icon: '🎪' },
            { id: 'elemental_circus', name: '정령 서커스', icon: '🎭' },
            { id: 'fortune_deck', name: '운명 카드점', icon: '🃏' }
        ]
    },
    'community_social': {
        title: '💛 커뮤니티 & 소셜',
        facilities: [
            { id: 'spirit_plaza', name: '정령 광장', icon: '💛', description: '다른 유저들과 실시간으로 소통할 수 있는 공간입니다.' },
            { id: 'friend_grove', name: '친구의 숲', icon: '🌳', description: '친구를 추가하고 관리할 수 있습니다.' },
            { id: 'guild_sanctuary', name: '길드 성소', icon: '🏰', description: '길드를 만들고 함께 활동할 수 있습니다.' }
        ]
    },
    'management_system': {
        title: '🏥 관리 & 시스템',
        facilities: [
            { id: 'healing_center', name: '정령 병원', icon: '🏥' },
            { id: 'ancient_archives', name: '고대 기록실', icon: '📚' },
            { id: 'tower_settings', name: '사서의 탑', icon: '⚙️' }
        ]
    },
};

document.addEventListener('DOMContentLoaded', function() {
    // 모든 카테고리 슬롯 선택
    const categorySlots = document.querySelectorAll('.category-slot');
    const mainHub = document.querySelector('.main-hub');
    
    // 일반 카테고리 슬롯에 클릭 이벤트 리스너 추가
    categorySlots.forEach(slot => {
        slot.addEventListener('click', function(e) {
            e.stopPropagation();
            const categoryId = this.getAttribute('data-category-id');
            openCategoryModal(categoryId);
        });
    });
    
    // 세계수의 심장(메인 허브)은 바로 진입
    if (mainHub) {
        // 위치 강제 설정 (화면 중앙)
        mainHub.style.position = 'fixed';
        mainHub.style.left = '50vw';
        mainHub.style.top = '50vh';
        mainHub.style.transform = 'translate(-50%, -50%)';
        mainHub.style.margin = '0';
        mainHub.style.right = 'auto';
        mainHub.style.bottom = 'auto';
        
        mainHub.addEventListener('click', function(e) {
            e.stopPropagation();
            e.preventDefault();
            window.location.href = '/world-tree/heart';
        });
        // 커서 스타일도 변경
        mainHub.style.cursor = 'pointer';
    }
    
    // 정령 생성 해금 상태 확인 및 UI 업데이트
    updateSpiritCreationUI();
    
    // 정령 생성 바로가기 버튼 표시
    const quickAccess = document.querySelector('.spirit-creation-quick-access');
    const unlockBadge = document.querySelector('.unlock-badge');
    
    if (quickAccess) {
        // 정령 생성 바로가기는 숨김 처리 (UI 외 텍스트 문제 방지)
        quickAccess.style.display = 'none';
        quickAccess.style.visibility = 'hidden';
        quickAccess.style.opacity = '0';
    }
    
    if (unlockBadge) {
        // 정령 생성 해금 배지는 숨김 처리 (UI 외 텍스트 문제 방지)
        unlockBadge.style.display = 'none';
        unlockBadge.style.visibility = 'hidden';
        unlockBadge.style.opacity = '0';
    }
});

/**
 * 정령 생성 해금 상태 확인
 */
async function checkSpiritCreationStatus() {
    try {
        // localStorage에서 확인
        const saved = localStorage.getItem('worldState');
        if (saved) {
            const state = JSON.parse(saved);
            if (Date.now() - (state.lastUpdate || 0) < 3600000) {
                if (state.spiritCreationUnlocked) {
                    return true;
                }
            }
        }
        
        // 서버에서 최신 상태 가져오기
        const response = await fetch('/world-tree/api/info', {
            credentials: 'include'
        });
        if (response.ok) {
            const info = await response.json();
            // 상태 저장
            if (typeof WorldState !== 'undefined') {
                WorldState.update(info.currentLevel, info.spiritCreationUnlocked);
            }
            return info.spiritCreationUnlocked || false;
        }
    } catch (e) {
        console.warn('정령 생성 상태 확인 실패:', e);
    }
    return false;
}

/**
 * 정령 생성 UI 업데이트
 */
function updateSpiritCreationUI() {
    checkSpiritCreationStatus().then(unlocked => {
        const mainHub = document.querySelector('.main-hub');
        const quickAccess = document.querySelector('.spirit-creation-quick-access');
        const unlockBadge = document.querySelector('.unlock-badge');
        
        if (unlocked) {
            if (mainHub) {
                mainHub.classList.add('spirit-creation-unlocked');
            }
            if (quickAccess) {
                quickAccess.style.display = 'flex';
                quickAccess.style.opacity = '1';
            }
            // unlockBadge는 숨김 처리 유지
            if (unlockBadge) {
                unlockBadge.style.display = 'none';
                unlockBadge.style.visibility = 'hidden';
            }
        } else {
            if (mainHub) {
                mainHub.classList.remove('spirit-creation-unlocked');
            }
            if (quickAccess) {
                quickAccess.style.display = 'none';
                quickAccess.style.visibility = 'hidden';
            }
            if (unlockBadge) {
                unlockBadge.style.display = 'none';
                unlockBadge.style.visibility = 'hidden';
            }
        }
    });
}

/**
 * 카테고리 모달 열기
 * @param {string} categoryId - 카테고리 ID
 */
function openCategoryModal(categoryId) {
    // 세계수의 심장은 바로 진입
    if (categoryId === 'world_heart') {
        window.location.href = '/world-tree/heart';
        return;
    }
    
    const category = categoryFacilities[categoryId];
    if (!category) {
        console.error('카테고리를 찾을 수 없습니다:', categoryId);
        return;
    }

    const modal = document.getElementById('categoryModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalBody = document.getElementById('modalBody');

    // 모달 제목 설정
    modalTitle.textContent = category.title;

    // 모달 본문 초기화
    modalBody.innerHTML = '';

        // 하위 시설 카드 생성
        category.facilities.forEach(facility => {
            const facilityCard = document.createElement('div');
            facilityCard.className = 'facility-card';
            facilityCard.setAttribute('data-facility-id', facility.id);
            
            facilityCard.innerHTML = `
                <div class="facility-icon">${facility.icon}</div>
                <div class="facility-name">${facility.name}</div>
            `;

            // 시설 카드 클릭 이벤트
            facilityCard.addEventListener('click', function(e) {
                e.stopPropagation();
                e.preventDefault();
                
                const facilityId = this.getAttribute('data-facility-id');
                const facilityName = facility.name;
                
                console.log('시설 클릭:', facilityId, facilityName);
                console.log('facilityId 타입:', typeof facilityId);
                console.log('facilityId 값:', JSON.stringify(facilityId));
                
                // 모달 먼저 닫기
                closeCategoryModal();
                
                // 정령 광장과 친구의 숲은 직접 처리
                if (facilityId === 'spirit_plaza') {
                    console.log('정령 광장으로 이동');
                    window.location.href = '/spirit-square/plaza';
                    return;
                }
                if (facilityId === 'friend_grove') {
                    console.log('친구의 숲으로 이동');
                    window.location.href = '/friend/list';
                    return;
                }
                
                // 시설별 URL 매핑 (모든 시설을 여기에 명시적으로 정의)
                const facilityUrls = {
                    'world_heart_main': '/world-tree/heart',
                    'spirit_village': '/spirit/village',  // 정령의 마을
                    'explorer_trail': '/explorer/trail',  // 탐험가의 길 (정령 던전)
                    'spirit_market': '/item/shop',        // 정령 상점
                    'arcane_lab': '/evolution/lab',       // 정령 연구소
                    'magic_academy': '/magic-academy/academy',  // 기술 강의실
                    'spirit_codex': '/codex/spirit-library',  // 정령 도서관
                    'spirit_arena': '/arena/spirit-arena',  // 정령 시합장
                    'training_grounds': '/training/grounds',  // 정령 수련장
                    'healing_center': '/healing-center',  // 정령 병원
                    'ancient_archives': '/ancient-archives',  // 고대 기록실
                    'tower_settings': '/tower-settings',  // 사서의 탑
                    'spirit_plaza': '/spirit-square/plaza',  // 정령 광장 (중복이지만 안전을 위해)
                    'friend_grove': '/friend/list'  // 친구의 숲 (중복이지만 안전을 위해)
                    // 나머지 시설들은 /facility/{facilityId}로 라우팅
                };
                
                // URL 결정
                let targetUrl;
                if (facilityUrls[facilityId]) {
                    targetUrl = facilityUrls[facilityId];
                    console.log('매핑된 URL로 이동:', targetUrl);
                } else {
                    targetUrl = `/facility/${facilityId}`;
                    console.log('기본 시설 페이지로 이동:', targetUrl);
                }
                
                // 페이지 이동
                setTimeout(() => {
                    console.log('최종 이동 URL:', targetUrl);
                    window.location.href = targetUrl;
                }, 100);
            });

            modalBody.appendChild(facilityCard);
        });

    // 모달 표시
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';
    
    // 모달 드래그 기능 활성화
    if (typeof makeModalDraggable === 'function') {
        const modalContent = modal.querySelector('.modal-content');
        const modalHeader = modal.querySelector('.modal-header');
        if (modalHeader && modalContent) {
            makeModalDraggable(modal, modalHeader);
        } else {
            makeModalDraggable(modal);
        }
    }
}

/**
 * 카테고리 모달 닫기
 */
function closeCategoryModal() {
    const modal = document.getElementById('categoryModal');
    modal.classList.remove('active');
    document.body.style.overflow = '';
}

/**
 * 시설 클릭 핸들러
 * @param {string} facilityId - 시설 ID
 * @param {string} facilityName - 시설 이름
 */
function handleFacilityClick(facilityId, facilityName) {
    console.log(`${facilityName} (${facilityId}) 선택됨`);
    
    // 모달 닫기
    closeCategoryModal();
    
    // 시설별 URL 매핑
    const facilityUrls = {
        'world_heart_main': '/world-tree/heart',
        'spirit_village': '/spirit/village',  // 정령의 마을
        'explorer_trail': '/explorer/trail',  // 탐험가의 길 (정령 던전)
        'spirit_market': '/item/shop',        // 정령 상점
        'arcane_lab': '/evolution/lab',       // 정령 연구소
        'magic_academy': '/magic-academy/academy',  // 기술 강의실
        'spirit_codex': '/codex/spirit-library',  // 정령 도서관
        'spirit_arena': '/arena/spirit-arena',  // 정령 시합장
        'training_grounds': '/training/grounds',  // 정령 수련장
        'healing_center': '/healing-center',  // 정령 병원
        'ancient_archives': '/ancient-archives',  // 고대 기록실
        'tower_settings': '/tower-settings'  // 사서의 탑
        // 나머지 시설들은 /facility/{facilityId}로 라우팅
    };
    
    // URL이 있으면 이동, 없으면 기본 시설 페이지로 이동
    if (facilityUrls[facilityId]) {
        window.location.href = facilityUrls[facilityId];
    } else {
        window.location.href = `/facility/${facilityId}`;
    }
}

// ESC 키로 모달 닫기
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeCategoryModal();
    }
});

// 모달 배경 클릭 시 닫기 (이미 HTML에서 처리됨)
