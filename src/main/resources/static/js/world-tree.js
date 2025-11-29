// 세계수의 심장 JavaScript

/**
 * 정령의 축복 추가 (테스트용)
 */
async function addBlessing(amount, contentSource) {
    try {
        const response = await fetch('/world-tree/api/blessing/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                amount: amount,
                contentSource: contentSource
            })
        });

        if (!response.ok) {
            throw new Error('정령의 축복 추가 실패');
        }

        // UI 업데이트
        updateWorldTreeInfo();
        
    } catch (error) {
        console.error('Error adding blessing:', error);
        showError('정령의 축복 추가 중 오류가 발생했습니다.');
    }
}

/**
 * 정령의 축복을 경험치로 부여 (10개)
 */
async function grantBlessing(amount) {
    try {
        const response = await fetch('/world-tree/api/blessing/grant', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include', // 쿠키 포함
            body: JSON.stringify({
                amount: amount
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Response error:', response.status, errorText);
            throw new Error('정령의 축복 부여 실패: ' + response.status);
        }

        const result = await response.json();
        
        // 레벨업 체크
        if (result.leveledUp) {
            showLevelUpAnimation(result.newLevel, result.growthEffect);
            
            // 전역 상태 업데이트
            if (result.newLevel != null) {
                GameState.updateWorldTreeLevel(result.newLevel);
            }
            
            // 15레벨 달성 시 희귀 정령 선택 화면으로 이동
            if (result.rareSpiritSelectionRequired) {
                setTimeout(() => {
                    window.location.href = '/spirit/rare-selection';
                }, 2000); // 레벨업 애니메이션 후 이동
                return;
            }
        }

        // 정령 생성 기능 언락 여부 업데이트
        if (result.spiritCreationUnlocked !== undefined) {
            // 실시간 UI 업데이트 (애니메이션 포함)
            updateSpiritCreationButton(result.spiritCreationUnlocked);
        }

        // UI 업데이트
        updateWorldTreeInfo();
        
    } catch (error) {
        console.error('Error granting blessing:', error);
        showError('정령의 축복 부여 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 정령의 축복을 경험치로 일괄 부여 (전체)
 */
async function grantAllBlessing() {
    try {
        const response = await fetch('/world-tree/api/blessing/grant', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include', // 쿠키 포함
            body: JSON.stringify({
                amount: null // null이면 전체 부여
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Response error:', response.status, errorText);
            throw new Error('정령의 축복 일괄 부여 실패: ' + response.status);
        }

        const result = await response.json();
        
        // 레벨업 체크
        if (result.leveledUp) {
            showLevelUpAnimation(result.newLevel, result.growthEffect);
            
            // 전역 상태 업데이트
            if (result.newLevel != null) {
                GameState.updateWorldTreeLevel(result.newLevel);
            }
            
            // 15레벨 달성 시 희귀 정령 선택 화면으로 이동
            if (result.rareSpiritSelectionRequired) {
                setTimeout(() => {
                    window.location.href = '/spirit/rare-selection';
                }, 2000); // 레벨업 애니메이션 후 이동
                return;
            }
        }

        // 정령 생성 기능 언락 여부 업데이트
        if (result.spiritCreationUnlocked !== undefined) {
            // 실시간 UI 업데이트 (애니메이션 포함)
            updateSpiritCreationButton(result.spiritCreationUnlocked);
        }

        // UI 업데이트
        updateWorldTreeInfo();
        
    } catch (error) {
        console.error('Error granting all blessing:', error);
        showError('정령의 축복 일괄 부여 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 세계수 정보 업데이트
 */
async function updateWorldTreeInfo() {
    try {
        const response = await fetch('/world-tree/api/info', {
            credentials: 'include' // 쿠키 포함
        });
        if (!response.ok) {
            throw new Error('정보 조회 실패');
        }

        const info = await response.json();
        
        // 크리스탈 안의 레벨 업데이트
        const levelNumberInside = document.querySelector('.level-number-inside');
        if (levelNumberInside) {
            levelNumberInside.textContent = info.currentLevel;
        }

        // EXP 바 업데이트
        const expFill = document.querySelector('.exp-fill');
        const expText = document.querySelector('.exp-text');
        if (expFill) {
            expFill.style.width = info.expPercentage + '%';
            expFill.setAttribute('data-exp', info.currentExp);
            expFill.setAttribute('data-required', info.requiredExp);
        }
        if (expText) {
            expText.innerHTML = `<span>${info.currentExp}</span> <span> / </span> <span>${info.requiredExp}</span>`;
        }

        // 성장 효과 업데이트
        const effectText = document.querySelector('.effect-text');
        if (effectText) {
            effectText.textContent = info.growthEffect;
        }

        // 좌측 상단: 보유 중인 정령의 축복 잔량 업데이트
        const essenceCounterAmount = document.querySelector('.essence-counter-amount');
        if (essenceCounterAmount && info.availableEssence !== undefined) {
            essenceCounterAmount.textContent = info.availableEssence.toLocaleString();
        }

        // 정령 생성 기능 언락 여부 업데이트 (초기 로드 시)
        if (info.spiritCreationUnlocked !== undefined) {
            // 전역 상태 업데이트
            GameState.updateWorldTreeLevel(info.currentLevel);
            updateSpiritCreationButton(info.spiritCreationUnlocked);
        }

        // 레벨별 기능 리스트 업데이트
        updateFeatureList(info.currentLevel);

    } catch (error) {
        console.error('Error updating world tree info:', error);
    }
}

/**
 * 정령 생성 버튼 표시/숨김 업데이트
 */
function updateSpiritCreationButton(unlocked) {
    const spiritCreationSection = document.querySelector('.spirit-creation-section');
    if (!spiritCreationSection) {
        console.warn('정령 생성 섹션을 찾을 수 없습니다.');
        return;
    }

    if (unlocked) {
        // 애니메이션과 함께 표시
        spiritCreationSection.style.display = 'block';
        spiritCreationSection.classList.add('spirit-creation-unlocked');
        
        // 펄스 애니메이션 효과
        setTimeout(() => {
            spiritCreationSection.classList.add('animate-unlock');
        }, 100);
    } else {
        spiritCreationSection.style.display = 'none';
        spiritCreationSection.classList.remove('spirit-creation-unlocked', 'animate-unlock');
    }
}

/**
 * 레벨업 애니메이션 표시
 */
function showLevelUpAnimation(level, growthEffect) {
    const overlay = document.getElementById('levelUpOverlay');
    const levelElement = document.getElementById('levelUpLevel');
    const effectElement = document.getElementById('levelUpEffect');
    const unlockSection = document.getElementById('levelUpUnlock');
    const unlockText = document.getElementById('unlockText');

    if (overlay && levelElement && effectElement) {
        levelElement.textContent = level;
        effectElement.textContent = growthEffect;
        
        // 레벨별 해금 기능 안내
        if (unlockSection && unlockText) {
            if (level === 2) {
                unlockText.textContent = '✨ 정령 생성 기능이 해금되었습니다!';
                unlockSection.style.display = 'block';
            } else if (level === 4) {
                unlockText.textContent = '✨ 최대 보유 가능 정령이 2마리로 증가했습니다!';
                unlockSection.style.display = 'block';
            } else if (level === 8) {
                unlockText.textContent = '✨ 최대 보유 가능 정령이 3마리로 증가했습니다!';
                unlockSection.style.display = 'block';
            } else if (level === 15) {
                unlockText.textContent = '✨ 희귀 정령 생성 기능이 해금되었습니다!';
                unlockSection.style.display = 'block';
            } else if (level === 16) {
                unlockText.textContent = '✨ 최대 보유 가능 정령이 5마리로 증가했습니다!';
                unlockSection.style.display = 'block';
            } else if (level === 3 || level === 7 || level === 13) {
                // 능력치 보너스 레벨
                const bonus = level >= 13 ? 15 : (level >= 7 ? 10 : 5);
                unlockText.textContent = `✨ 모든 정령의 능력치가 +${bonus} 증가했습니다!`;
                unlockSection.style.display = 'block';
            } else {
                unlockSection.style.display = 'none';
            }
        }
        
        overlay.classList.add('show');

        // 언락 안내가 있으면 더 길게 표시
        const hasUnlock = unlockSection && unlockSection.style.display === 'block';
        const displayTime = hasUnlock ? 4500 : 3000;
        setTimeout(() => {
            overlay.classList.remove('show');
            if (unlockSection) {
                unlockSection.style.display = 'none';
            }
        }, displayTime);
    }
}

/**
 * 레벨별 기능 리스트 생성
 */
function initializeFeatureList() {
    const featureList = document.getElementById('featureList');
    if (!featureList) return;

    // 레벨별 기능 정의
    const features = {
        2: '정령 생성 가능',
        3: '모든 능력치 +5',
        4: '최대 보유 가능 정령 2마리',
        5: '모든 능력치 +5',
        6: '모든 능력치 +5',
        7: '모든 능력치 +10',
        8: '최대 보유 가능 정령 3마리',
        9: '모든 능력치 +10',
        10: '모든 능력치 +10',
        11: '모든 능력치 +10',
        12: '모든 능력치 +10',
        13: '모든 능력치 +15',
        14: '모든 능력치 +15',
        15: '희귀 정령 생성 가능',
        16: '최대 보유 가능 정령 5마리',
        17: '모든 능력치 +15',
        18: '모든 능력치 +15',
        19: '모든 능력치 +15',
        20: '모든 능력치 +15',
        21: '모든 능력치 +15',
        22: '모든 능력치 +15',
        23: '모든 능력치 +15',
        24: '모든 능력치 +15',
        25: '모든 능력치 +15',
        26: '모든 능력치 +15',
        27: '모든 능력치 +15',
        28: '모든 능력치 +15',
        29: '모든 능력치 +15',
        30: '모든 능력치 +15'
    };

    // 레벨 2부터 30까지 리스트 생성
    for (let level = 2; level <= 30; level++) {
        const featureItem = document.createElement('div');
        featureItem.className = 'feature-item';
        featureItem.id = `feature-${level}`;
        
        const levelSpan = document.createElement('span');
        levelSpan.className = 'feature-level';
        levelSpan.textContent = `Lv.${level}`;
        
        const nameSpan = document.createElement('span');
        nameSpan.className = 'feature-name';
        nameSpan.textContent = features[level] || '???';
        
        featureItem.appendChild(levelSpan);
        featureItem.appendChild(nameSpan);
        featureList.appendChild(featureItem);
    }
}

/**
 * 레벨별 기능 리스트 업데이트
 */
function updateFeatureList(currentLevel) {
    for (let level = 2; level <= 30; level++) {
        const featureItem = document.getElementById(`feature-${level}`);
        if (!featureItem) continue;

        // 기존 클래스 제거
        featureItem.classList.remove('unlocked', 'current', 'locked');

        if (level < currentLevel) {
            // 이미 달성한 레벨
            featureItem.classList.add('unlocked');
        } else if (level === currentLevel) {
            // 현재 레벨
            featureItem.classList.add('current');
        } else {
            // 아직 달성하지 않은 레벨
            featureItem.classList.add('locked');
        }
    }
}

/**
 * 오버레이 클릭 시 닫기 및 페이지 로드 시 정보 업데이트
 */
// 전역 상태 관리
const GameState = {
    worldTreeLevel: 1,
    spiritCreationUnlocked: false,
    
    updateWorldTreeLevel(level) {
        this.worldTreeLevel = level;
        this.spiritCreationUnlocked = level >= 2;
        this.saveToLocalStorage();
        this.notifyListeners();
    },
    
    saveToLocalStorage() {
        try {
            localStorage.setItem('gameState', JSON.stringify({
                worldTreeLevel: this.worldTreeLevel,
                spiritCreationUnlocked: this.spiritCreationUnlocked,
                lastUpdate: Date.now()
            }));
        } catch (e) {
            console.warn('localStorage 저장 실패:', e);
        }
    },
    
    loadFromLocalStorage() {
        try {
            const saved = localStorage.getItem('gameState');
            if (saved) {
                const state = JSON.parse(saved);
                // 1시간 이내의 데이터만 사용
                if (Date.now() - (state.lastUpdate || 0) < 3600000) {
                    this.worldTreeLevel = state.worldTreeLevel || 1;
                    this.spiritCreationUnlocked = state.spiritCreationUnlocked || false;
                }
            }
        } catch (e) {
            console.warn('localStorage 로드 실패:', e);
        }
    },
    
    listeners: [],
    
    subscribe(callback) {
        this.listeners.push(callback);
    },
    
    notifyListeners() {
        this.listeners.forEach(callback => {
            try {
                callback({
                    worldTreeLevel: this.worldTreeLevel,
                    spiritCreationUnlocked: this.spiritCreationUnlocked
            });
            } catch (e) {
                console.error('상태 리스너 오류:', e);
            }
        });
    }
};

// 페이지 로드 시 저장된 상태 복원
GameState.loadFromLocalStorage();

document.addEventListener('DOMContentLoaded', function() {
    // 레벨별 기능 리스트 초기화
    initializeFeatureList();
    
    // 페이지 로드 시 세계수 정보 업데이트
    updateWorldTreeInfo();
    
    // 저장된 상태가 있으면 즉시 반영
    if (GameState.spiritCreationUnlocked) {
        updateSpiritCreationButton(true);
    }
    
    const overlay = document.getElementById('levelUpOverlay');
    if (overlay) {
        overlay.addEventListener('click', function() {
            this.classList.remove('show');
            const unlockSection = document.getElementById('levelUpUnlock');
            if (unlockSection) {
                unlockSection.style.display = 'none';
            }
        });
    }

    // ESC 키로 닫기
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            const overlay = document.getElementById('levelUpOverlay');
            if (overlay) {
                overlay.classList.remove('show');
                const unlockSection = document.getElementById('levelUpUnlock');
                if (unlockSection) {
                    unlockSection.style.display = 'none';
                }
            }
        }
    });
});