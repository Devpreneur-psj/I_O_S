// 상점 JavaScript

let availableItems = [];
let myItems = [];
let currentFilter = 'all';
let selectedItem = null;

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    loadUserMoney();
    loadAvailableItems();
    loadMyItems();
    setupTabs();
    setupFilters();
});

/**
 * 사용자 금액 로드
 */
async function loadUserMoney() {
    try {
        const response = await fetch('/item/api/user-money', {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('사용자 금액을 불러올 수 없습니다.');
        }
        
        const result = await response.json();
        const money = result.money || 0;
        document.getElementById('userMoney').textContent = money;
        // 메소 표시도 업데이트
        const mesoDisplay = document.getElementById('mesoDisplay');
        if (mesoDisplay) {
            mesoDisplay.textContent = money.toLocaleString();
        }
    } catch (error) {
        console.error('Error loading user money:', error);
        document.getElementById('userMoney').textContent = '0';
    }
}

/**
 * 판매 가능한 아이템 목록 로드
 */
async function loadAvailableItems() {
    try {
        const response = await fetch('/item/api/available-items', {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('아이템 목록을 불러올 수 없습니다.');
        }
        
        availableItems = await response.json();
        renderShopItems();
    } catch (error) {
        console.error('Error loading available items:', error);
        showError('아이템 목록을 불러오는 중 오류가 발생했습니다.');
    }
}

/**
 * 보유 아이템 목록 로드
 */
async function loadMyItems() {
    try {
        const response = await fetch('/item/api/my-items', {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('보유 아이템 목록을 불러올 수 없습니다.');
        }
        
        myItems = await response.json();
        renderInventoryItems();
    } catch (error) {
        console.error('Error loading my items:', error);
    }
}

/**
 * 상점 아이템 렌더링
 */
function renderShopItems() {
    const grid = document.getElementById('shopItemsGrid');
    grid.innerHTML = '';
    
    const filteredItems = currentFilter === 'all' 
        ? availableItems 
        : availableItems.filter(item => item.itemType === currentFilter);
    
    filteredItems.forEach(item => {
        const card = createItemCard(item, 'shop');
        grid.appendChild(card);
    });
}

let currentInventoryFilter = 'all';

/**
 * 인벤토리 아이템 렌더링 (메이플스토리 스타일)
 */
function renderInventoryItems() {
    const grid = document.getElementById('inventoryItemsGrid');
    grid.innerHTML = '';
    
    // 필터링된 아이템 목록
    let filteredItems = [];
    if (currentInventoryFilter === 'all') {
        filteredItems = myItems;
    } else if (currentInventoryFilter === 'FOOD') {
        // 소비 아이템: FOOD, MEDICINE, VITAMIN, ENERGY
        filteredItems = myItems.filter(userItem => {
            const item = availableItems.find(i => i.id === userItem.itemId);
            return item && (item.itemType === 'FOOD' || item.itemType === 'MEDICINE' || item.itemType === 'VITAMIN' || item.itemType === 'ENERGY');
        });
    } else if (currentInventoryFilter === 'TOY') {
        // 기타 아이템: TOY, STAT_BOOST, LIFESPAN_EXTENSION
        filteredItems = myItems.filter(userItem => {
            const item = availableItems.find(i => i.id === userItem.itemId);
            return item && (item.itemType === 'TOY' || item.itemType === 'STAT_BOOST' || item.itemType === 'LIFESPAN_EXTENSION');
        });
    } else {
        filteredItems = myItems.filter(userItem => {
            const item = availableItems.find(i => i.id === userItem.itemId);
            return item && item.itemType === currentInventoryFilter;
        });
    }
    
    if (filteredItems.length === 0) {
        // 빈 슬롯들 표시
        for (let i = 0; i < 40; i++) {
            const slot = createEmptyInventorySlot();
            grid.appendChild(slot);
        }
        return;
    }
    
    // 아이템 슬롯 생성 (최대 40개 슬롯)
    const maxSlots = 40;
    const itemMap = new Map();
    
    // 아이템 정보와 수량을 결합하여 맵에 저장
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
    
    // 아이템 슬롯 생성
    let slotIndex = 0;
    itemMap.forEach((itemData, itemId) => {
        if (slotIndex < maxSlots) {
            const slot = createInventorySlot(itemData.item, itemData.quantity, itemData.userItemId);
            grid.appendChild(slot);
            slotIndex++;
        }
    });
    
    // 나머지 빈 슬롯 채우기
    while (slotIndex < maxSlots) {
        const slot = createEmptyInventorySlot();
        grid.appendChild(slot);
        slotIndex++;
    }
}

/**
 * 빈 인벤토리 슬롯 생성
 */
function createEmptyInventorySlot() {
    const slot = document.createElement('div');
    slot.className = 'inventory-slot empty-slot';
    return slot;
}

/**
 * 아이템이 있는 인벤토리 슬롯 생성
 */
function createInventorySlot(item, quantity, userItemId) {
    const slot = document.createElement('div');
    slot.className = 'inventory-slot item-slot';
    slot.setAttribute('data-item-id', item.id);
    slot.setAttribute('data-user-item-id', userItemId);
    
    const icon = getItemIcon(item.itemType);
    
    slot.innerHTML = `
        <div class="slot-icon">${icon}</div>
        ${quantity > 1 ? `<div class="slot-quantity">${quantity}</div>` : ''}
    `;
    
    // 더블클릭 이벤트 추가
    slot.addEventListener('dblclick', function() {
        useItemFromInventory(item.id);
    });
    
    // 마우스 호버 시 툴팁 표시
    slot.addEventListener('mouseenter', function(e) {
        showItemTooltip(item, e);
    });
    
    slot.addEventListener('mouseleave', function() {
        hideItemTooltip();
    });
    
    slot.addEventListener('mousemove', function(e) {
        updateTooltipPosition(e);
    });
    
    return slot;
}

/**
 * 아이템 카드 생성
 */
function createItemCard(item, type, quantity = 0) {
    const card = document.createElement('div');
    card.className = `item-card ${type}`;
    
    const icon = getItemIcon(item.itemType);
    const effectText = getEffectText(item);
    
    card.innerHTML = `
        <div class="item-icon">${icon}</div>
        <div class="item-name">${item.itemName}</div>
        <div class="item-description">${item.description || '설명 없음'}</div>
        ${effectText ? `<div class="item-effect">${effectText}</div>` : ''}
        <div class="item-price">
            ${type === 'shop' 
                ? `<span class="price-amount">${item.price} 골드</span>
                   <button class="buy-btn" onclick="openPurchaseModal(${item.id})">구매</button>`
                : `<span class="item-quantity">보유: ${quantity}개</span>
                   <button class="use-btn" onclick="useItem(${item.id})">사용</button>`
            }
        </div>
    `;
    
    return card;
}

/**
 * 아이템 아이콘 반환
 */
function getItemIcon(itemType) {
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
 * 효과 텍스트 반환
 */
function getEffectText(item) {
    if (!item.effectType || !item.effectValue) {
        return '';
    }
    
    const effects = {
        'HEALTH': '건강 회복',
        'HAPPINESS': `행복도 +${item.effectValue}`,
        'ENERGY': `에너지 +${item.effectValue}`,
        'STAT_BOOST': `${getStatName(item.targetStat)} +${item.effectValue}`,
        'HUNGER': `배고픔 -${item.effectValue}`,
        'LIFESPAN': `수명 +${item.effectValue}일`
    };
    
    return effects[item.effectType] || '';
}

/**
 * 능력치 이름 반환
 */
function getStatName(targetStat) {
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
 * 탭 설정
 */
function setupTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const tabId = this.getAttribute('data-tab');
            
            // 모든 탭 비활성화
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            
            // 선택한 탭 활성화
            this.classList.add('active');
            document.getElementById(`${tabId}Tab`).classList.add('active');
            
            // 인벤토리 탭이 활성화되면 인벤토리 아이템 다시 렌더링
            if (tabId === 'inventory') {
                renderInventoryItems();
            }
        });
    });
    
    // 인벤토리 내부 탭 설정
    setupInventoryTabs();
}

/**
 * 인벤토리 내부 탭 설정
 */
function setupInventoryTabs() {
    const inventoryTabBtns = document.querySelectorAll('.inventory-tab-btn');
    
    inventoryTabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            // 모든 인벤토리 탭 비활성화
            inventoryTabBtns.forEach(b => b.classList.remove('active'));
            
            // 선택한 탭 활성화
            this.classList.add('active');
            currentInventoryFilter = this.getAttribute('data-inventory-tab');
            renderInventoryItems();
        });
    });
}

/**
 * 필터 설정
 */
function setupFilters() {
    const filterBtns = document.querySelectorAll('.filter-btn');
    
    filterBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            filterBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.getAttribute('data-filter');
            renderShopItems();
        });
    });
}

/**
 * 구매 모달 열기
 */
function openPurchaseModal(itemId) {
    const item = availableItems.find(i => i.id === itemId);
    if (!item) {
        return;
    }
    
    selectedItem = item;
    document.getElementById('purchaseItemName').textContent = item.itemName;
    document.getElementById('purchaseItemDescription').textContent = item.description || '설명 없음';
    document.getElementById('purchaseQuantity').value = 1;
    updatePurchaseTotal();
    
    // 수량 변경 시 총 가격 업데이트
    document.getElementById('purchaseQuantity').addEventListener('input', updatePurchaseTotal);
    
    document.getElementById('purchaseModal').classList.add('active');
}

/**
 * 구매 모달 닫기
 */
function closePurchaseModal() {
    document.getElementById('purchaseModal').classList.remove('active');
    selectedItem = null;
}

/**
 * 구매 총 가격 업데이트
 */
function updatePurchaseTotal() {
    if (!selectedItem) {
        return;
    }
    
    const quantity = parseInt(document.getElementById('purchaseQuantity').value) || 1;
    const totalPrice = selectedItem.price * quantity;
    document.getElementById('purchaseTotalPrice').textContent = totalPrice;
}

/**
 * 구매 확인
 */
async function confirmPurchase() {
    if (!selectedItem) {
        return;
    }
    
    const quantity = parseInt(document.getElementById('purchaseQuantity').value) || 1;
    
    try {
        const response = await fetch('/item/api/purchase', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `itemId=${selectedItem.id}&quantity=${quantity}`
        });
        
        const result = await response.json();
        
        if (result.success) {
            showSuccess('구매가 완료되었습니다!');
            closePurchaseModal();
            loadUserMoney();
            loadMyItems();
        } else {
            showError('구매 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error purchasing item:', error);
        showError('구매 중 오류가 발생했습니다.');
    }
}

/**
 * 인벤토리에서 아이템 사용 (더블클릭)
 */
async function useItemFromInventory(itemId) {
    // 정령 선택 모달을 띄워야 하지만, 일단 알림으로 처리
    const spiritId = await showPrompt('아이템을 사용할 정령 ID를 입력하세요:', '', '아이템 사용');
    if (!spiritId) {
        return;
    }
    
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
            loadMyItems();
        } else {
            showError('아이템 사용 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error using item:', error);
        showError('아이템 사용 중 오류가 발생했습니다.');
    }
}

/**
 * 아이템 사용 (기존 함수 - 호환성 유지)
 */
async function useItem(itemId) {
    await useItemFromInventory(itemId);
}

/**
 * 아이템 툴팁 표시
 */
function showItemTooltip(item, event) {
    const tooltip = document.getElementById('itemTooltip');
    if (!tooltip) return;
    
    const tooltipName = document.getElementById('tooltipItemName');
    const tooltipStatus = document.getElementById('tooltipStatus');
    const tooltipDescription = document.getElementById('tooltipDescription');
    const tooltipEffect = document.getElementById('tooltipEffect');
    
    if (tooltipName) tooltipName.textContent = item.itemName || '알 수 없음';
    
    // 상태 (교환 불가 등)
    if (tooltipStatus) {
        tooltipStatus.textContent = '교환 불가';
    }
    
    // 설명
    if (tooltipDescription) {
        tooltipDescription.textContent = item.description || '설명 없음';
    }
    
    // 효과
    if (tooltipEffect) {
        const effectText = getEffectText(item);
        if (effectText) {
            tooltipEffect.textContent = effectText;
            tooltipEffect.style.display = 'block';
        } else {
            tooltipEffect.style.display = 'none';
        }
    }
    
    tooltip.style.display = 'block';
    updateTooltipPosition(event);
}

/**
 * 아이템 툴팁 숨기기
 */
function hideItemTooltip() {
    const tooltip = document.getElementById('itemTooltip');
    if (tooltip) {
        tooltip.style.display = 'none';
    }
}

/**
 * 툴팁 위치 업데이트
 */
function updateTooltipPosition(event) {
    const tooltip = document.getElementById('itemTooltip');
    if (!tooltip || tooltip.style.display === 'none') return;
    
    const x = event.clientX + 10;
    const y = event.clientY + 10;
    
    tooltip.style.left = x + 'px';
    tooltip.style.top = y + 'px';
}

/**
 * 인벤토리 창 닫기 (호환성)
 */
function closeInventoryWindow() {
    // 인벤토리 탭을 닫는 대신 상점 탭으로 전환
    const shopTabBtn = document.querySelector('.tab-btn[data-tab="shop"]');
    if (shopTabBtn) {
        shopTabBtn.click();
    }
}

