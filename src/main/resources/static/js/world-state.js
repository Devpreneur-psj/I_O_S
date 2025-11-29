// 월드맵 전역 상태 관리

const WorldState = {
    worldTreeLevel: 1,
    spiritCreationUnlocked: false,
    
    // 상태 업데이트
    update(level, unlocked) {
        this.worldTreeLevel = level || 1;
        this.spiritCreationUnlocked = unlocked || false;
        this.saveToLocalStorage();
        this.notifyListeners();
    },
    
    // localStorage 저장
    saveToLocalStorage() {
        try {
            localStorage.setItem('worldState', JSON.stringify({
                worldTreeLevel: this.worldTreeLevel,
                spiritCreationUnlocked: this.spiritCreationUnlocked,
                lastUpdate: Date.now()
            }));
        } catch (e) {
            console.warn('localStorage 저장 실패:', e);
        }
    },
    
    // localStorage 로드
    loadFromLocalStorage() {
        try {
            const saved = localStorage.getItem('worldState');
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
    
    // 서버에서 최신 상태 가져오기
    async fetchFromServer() {
        try {
            const response = await fetch('/world-tree/api/info', {
                credentials: 'include'
            });
            if (response.ok) {
                const info = await response.json();
                this.update(info.currentLevel, info.spiritCreationUnlocked);
                return info;
            }
        } catch (e) {
            console.warn('서버에서 상태 가져오기 실패:', e);
        }
        return null;
    },
    
    // 리스너
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

// 페이지 로드 시 상태 복원
WorldState.loadFromLocalStorage();

