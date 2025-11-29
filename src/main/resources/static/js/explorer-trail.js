// 탐험가의 길 (정령 던전) JavaScript

// 전역 변수
let selectedStageNumber = null;
let selectedSpiritId = null;
let battlePaused = false;
let battleAnimationPromise = null;
let currentBattleResult = null;
let currentStageNumber = null;
let currentSpiritId = null;

document.addEventListener('DOMContentLoaded', function() {
    console.log('탐험가의 길 페이지 로드됨');
});

/**
 * 스테이지 선택 (정령 선택 없이 바로 전투 시작)
 */
async function selectStage(stageNumber) {
    console.log('selectStage 호출:', stageNumber);
    
    // 숫자로 변환
    const stageNum = parseInt(stageNumber, 10);
    if (isNaN(stageNum)) {
        console.error('잘못된 스테이지 번호:', stageNumber);
        showError('잘못된 스테이지 번호입니다.');
        return;
    }
    
    const stageCard = document.querySelector(`[data-stage-number="${stageNum}"]`);
    if (!stageCard) {
        console.error('스테이지 카드를 찾을 수 없습니다:', stageNum);
        return;
    }
    
    // 잠긴 스테이지 체크
    if (stageCard.classList.contains('locked')) {
        showWarning('이전 스테이지를 먼저 클리어해야 합니다.');
        return;
    }
    
    // 정령 선택 없이 바로 전투 시작 (첫 번째 정령 ID 사용, 실제로는 모든 정령이 참가)
    try {
        // 사용자의 첫 번째 정령 ID 가져오기 (실제로는 모든 정령이 참가하므로 임의의 ID 사용)
        const response = await fetch('/spirit/api/my-spirits', {
            credentials: 'include'
        });
        
        if (response.ok) {
            const spirits = await response.json();
            if (spirits && spirits.length > 0) {
                const firstSpiritId = spirits[0].id;
                await startBattle(firstSpiritId, stageNum);
            } else {
                showError('참가할 수 있는 정령이 없습니다.');
            }
        } else {
            showError('정령 정보를 불러올 수 없습니다.');
        }
    } catch (error) {
        console.error('정령 정보 조회 실패:', error);
        showError('정령 정보를 불러오는 중 오류가 발생했습니다.');
    }
}

// 전역 스코프에 명시적으로 등록
window.selectStage = selectStage;

/**
 * 전투 시작
 */
async function startBattle(spiritId, stageNumber) {
    try {
        // 파라미터 유효성 검사
        if (!spiritId || spiritId === null || spiritId === 'null' || spiritId === 'undefined') {
            showError('정령을 선택해주세요.');
            return;
        }
        
        if (!stageNumber || stageNumber === null || stageNumber === 'null' || stageNumber === 'undefined') {
            showError('스테이지를 선택해주세요.');
            return;
        }
        
        // 숫자로 변환
        const spiritIdNum = parseInt(spiritId, 10);
        const stageNumberNum = parseInt(stageNumber, 10);
        
        if (isNaN(spiritIdNum) || isNaN(stageNumberNum)) {
            showError('잘못된 파라미터입니다.');
            return;
        }
        
        // 전역 변수에 저장 (다시하기용)
        currentSpiritId = spiritIdNum;
        currentStageNumber = stageNumberNum;
        battlePaused = false;
        
        // 로딩 표시
        showLoading('전투 준비 중...');
        
        // 전투 수행
        const response = await fetch('/explorer/api/perform-battle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${spiritIdNum}&stageNumber=${stageNumberNum}`
        });
        
        hideLoading();
        
        if (!response.ok) {
            // 에러 응답도 JSON으로 파싱 시도
            let errorMessage = '전투 요청 실패';
            try {
                const errorResult = await response.json();
                if (errorResult.message) {
                    errorMessage = errorResult.message;
                }
            } catch (e) {
                // JSON 파싱 실패 시 기본 메시지 사용
            }
            throw new Error(errorMessage);
        }
        
        const result = await response.json();
        currentBattleResult = result; // 다시하기용으로 저장
        
        if (result.success) {
            // 전투 애니메이션 표시
            battleAnimationPromise = showBattleAnimation(result);
            await battleAnimationPromise;
        } else {
            showError('전투 실패: ' + (result.message || '알 수 없는 오류'));
        }
    } catch (error) {
        hideLoading();
        console.error('전투 오류:', error);
        showError('전투 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 전투 결과 표시
 */
function showBattleResult(result) {
    const modal = document.getElementById('battleResultModal');
    const title = document.getElementById('battleResultTitle');
    const content = document.getElementById('battleResultContent');
    
    if (!modal || !title || !content) return;
    
    if (result.victory) {
        title.textContent = '🎉 승리!';
        let resultText = `
            <div class="battle-result-victory">
                <p>던전을 클리어했습니다!</p>
                <p>라운드 수: ${result.rounds}</p>
                <p>남은 HP: ${result.playerRemainingHp}</p>
                <p class="reward">경험치: +${result.expGain}</p>
                <p class="reward">골드: +${result.goldGain}</p>
                ${result.isFirstClear ? '<p class="first-clear">✨ 첫 클리어 보너스!</p>' : ''}
            </div>
        `;
        content.innerHTML = resultText;
    } else {
        title.textContent = '💀 패배';
        content.innerHTML = `
            <div class="battle-result-defeat">
                <p>전투에서 패배했습니다.</p>
                <p>정령을 더 강화하고 다시 도전하세요.</p>
            </div>
        `;
    }
    
    modal.style.display = 'block';
    
    // 승리 시 페이지 새로고침 (진행 상태 업데이트)
    if (result.victory) {
        setTimeout(() => {
            location.reload();
        }, 3000);
    }
}

/**
 * 전투 결과 모달 닫기
 */
function closeBattleResultModal() {
    const modal = document.getElementById('battleResultModal');
    if (modal) {
        modal.style.display = 'none';
    }
    location.reload();
}

// 전역 스코프에 명시적으로 등록
window.closeBattleResultModal = closeBattleResultModal;

/**
 * 전투 애니메이션 표시 (던전 라운드 3개 시스템)
 */
async function showBattleAnimation(result) {
    const battleModal = document.getElementById('battleAnimationModal');
    if (!battleModal) return;
    
    // 전투 화면 표시
    battleModal.style.display = 'block';
    
    // 컨트롤 버튼 이벤트 리스너 등록
    setupBattleControls();
    
    // 던전 라운드별 처리
    const dungeonRounds = result.dungeonRounds || [];
    const playersInfo = result.playersInfo || [];
    
    // 디버깅: 전투 데이터 확인
    console.log('전투 데이터:', {
        dungeonRounds: dungeonRounds.length,
        playersInfo: playersInfo.length,
        roundsData: dungeonRounds.map(r => ({
            round: r.roundNumber,
            roundsData: r.roundsData?.length || 0,
            actions: r.roundsData?.reduce((sum, rd) => sum + (rd.actions?.length || 0), 0) || 0
        }))
    });
    
    if (dungeonRounds.length === 0) {
        console.warn('던전 라운드가 없습니다. 결과를 표시합니다.');
        battleModal.style.display = 'none';
        showBattleResult(result);
        return;
    }
    
    // 모든 플레이어 정령 표시 (왼쪽)
    await setupPlayerSpirits(playersInfo);
    
    // 기술 사용 로그 초기화
    const skillLogContent = document.getElementById('skillLogContent');
    if (skillLogContent) {
        skillLogContent.innerHTML = '';
    }
    
    // 던전 라운드별 전투 진행
    for (let dungeonRoundIndex = 0; dungeonRoundIndex < dungeonRounds.length; dungeonRoundIndex++) {
        // 일시정지 체크
        await waitIfPaused();
        
        const dungeonRound = dungeonRounds[dungeonRoundIndex];
        
        // 라운드 시작 표시
        await waitIfPaused();
        await showRoundTransition(dungeonRound.roundNumber, dungeonRoundIndex > 0);
        
        // 적 생성 및 다가오는 애니메이션
        const enemiesInfo = dungeonRound.enemiesInfo || [];
        await spawnEnemies(enemiesInfo);
        
        // 전역 변수에 저장 (액션 애니메이션에서 사용)
        window.currentEnemiesInfo = enemiesInfo;
        
        // 라운드별 전투 애니메이션
        if (dungeonRound.roundsData && dungeonRound.roundsData.length > 0) {
            let hasAnyActions = false;
            for (let i = 0; i < dungeonRound.roundsData.length; i++) {
                const round = dungeonRound.roundsData[i];
                
                // 액션이 있는 경우에만 애니메이션 실행
                if (round.actions && round.actions.length > 0) {
                    hasAnyActions = true;
                    console.log(`라운드 ${dungeonRound.roundNumber}, 턴 ${i + 1}: ${round.actions.length}개 액션 실행`);
                    await animateRound(round, dungeonRound, i);
                } else {
                    console.warn(`라운드 ${dungeonRound.roundNumber}, 턴 ${i + 1}: 액션이 없습니다.`);
                }
                
                // 전투 종료 확인
                const playerHp = dungeonRound.playerRemainingHp || 0;
                const aliveEnemies = enemiesInfo.filter(e => (e.currentHp || 0) > 0).length || 0;
                
                if (playerHp <= 0 || aliveEnemies === 0) {
                    break;
                }
            }
            
            // 액션이 하나도 없으면 경고 메시지 표시
            if (!hasAnyActions) {
                console.error('라운드에 액션이 하나도 없습니다. 기술이 없거나 모든 기술이 쿨타임 중일 수 있습니다.');
                addSkillLog('⚠️ 공격할 수 있는 기술이 없습니다.');
                await sleep(2000);
            }
        } else {
            // roundsData가 없거나 비어있으면 경고
            console.error('roundsData가 없거나 비어있습니다.');
            addSkillLog('⚠️ 전투 데이터가 없습니다.');
            await sleep(2000);
        }
        
        // 라운드 종료 처리
        if (!dungeonRound.victory) {
            break; // 패배 시 다음 라운드 진행 안 함
        }
        
        // 라운드 간 대기
        if (dungeonRoundIndex < dungeonRounds.length - 1) {
            await sleep(1000);
        }
    }
    
    // 전투 결과 표시
    setTimeout(() => {
        battleModal.style.display = 'none';
        showBattleResult(result);
    }, 2000);
}

/**
 * 플레이어 정령들 설정 (모든 정령 표시, 세로로 3마리씩 배치)
 */
async function setupPlayerSpirits(playersInfo) {
    const playerSide = document.querySelector('.player-side');
    if (!playerSide) return;
    
    // 전역 변수에 저장 (액션 애니메이션에서 사용)
    window.currentPlayersInfo = playersInfo;
    
    // 기존 플레이어 정령 제거
    playerSide.innerHTML = '';
    
    // 최대 6마리까지 지원 (세로로 3마리씩 2줄)
    const maxSpirits = 6;
    const displayedSpirits = playersInfo.slice(0, maxSpirits);
    
    // 첫 번째 줄: 앞에서 3마리 (세로 배치)
    const firstRow = document.createElement('div');
    firstRow.className = 'player-row';
    for (let i = 0; i < Math.min(3, displayedSpirits.length); i++) {
        const playerEl = createPlayerElement(displayedSpirits[i], i);
        firstRow.appendChild(playerEl);
    }
    playerSide.appendChild(firstRow);
    
    // 두 번째 줄: 뒤에서 3마리 (4번째부터 6번째까지, 세로 배치)
    if (displayedSpirits.length > 3) {
        const secondRow = document.createElement('div');
        secondRow.className = 'player-row';
        for (let i = 3; i < displayedSpirits.length; i++) {
            const playerEl = createPlayerElement(displayedSpirits[i], i);
            secondRow.appendChild(playerEl);
        }
        playerSide.appendChild(secondRow);
    }
}

/**
 * 플레이어 정령 요소 생성
 */
function createPlayerElement(player, index) {
    const playerDiv = document.createElement('div');
    playerDiv.className = 'battle-character player-character';
    playerDiv.id = `player-${index}`;
    
    const imagePath = getSpiritImagePath(player.spiritType, player.evolutionStage || 0);
    const elementClass = getElementClass(player.spiritType);
    
    playerDiv.innerHTML = `
        <div class="character-sprite ${elementClass}" id="playerSprite-${index}">
            <img class="character-image" id="playerImage-${index}" src="${imagePath}" alt="${player.name || '플레이어'}">
        </div>
        <div class="character-info">
            <div class="character-name" id="playerName-${index}">${player.name || '플레이어'}</div>
            <div class="hp-bar-container">
                <div class="hp-bar" id="playerHpBar-${index}">
                    <div class="hp-fill" id="playerHpFill-${index}"></div>
                </div>
                <div class="hp-text" id="playerHpText-${index}">${player.maxHp || 100}/${player.maxHp || 100}</div>
            </div>
        </div>
    `;
    
    // 초기 HP 설정
    const hpBar = playerDiv.querySelector(`#playerHpBar-${index}`);
    const hpFill = playerDiv.querySelector(`#playerHpFill-${index}`);
    const hpText = playerDiv.querySelector(`#playerHpText-${index}`);
    updateHpBar(hpBar, hpFill, hpText, player.currentHp || player.maxHp || 100, player.maxHp || 100);
    
    return playerDiv;
}

/**
 * 라운드 전환 애니메이션
 */
async function showRoundTransition(roundNumber, isTransition) {
    const dungeonBackground = document.querySelector('.dungeon-background');
    if (!dungeonBackground) return;
    
    // 라운드 전환 메시지 표시
    const logContent = document.getElementById('logContent');
    if (logContent) {
        addLogEntry(`=== 던전 라운드 ${roundNumber} 시작 ===`, 'normal');
    }
    
    if (isTransition) {
        // 라운드 전환 오버레이 표시
        const transitionDiv = document.createElement('div');
        transitionDiv.className = 'round-transition';
        transitionDiv.textContent = `라운드 ${roundNumber}`;
        dungeonBackground.appendChild(transitionDiv);
        
        await sleep(1500);
        transitionDiv.remove();
    } else {
        await sleep(500);
    }
}

/**
 * 적 생성 및 다가오는 애니메이션 (플레이어와 같은 대형: 세로로 3마리씩 2줄)
 */
async function spawnEnemies(enemiesInfo) {
    const enemySide = document.getElementById('enemySide');
    if (!enemySide) return;
    
    // 기존 적 제거
    enemySide.innerHTML = '';
    
    // 최대 6마리까지 지원 (세로로 3마리씩 2줄)
    const maxEnemies = 6;
    const displayedEnemies = enemiesInfo.slice(0, maxEnemies);
    
    // 첫 번째 줄: 앞에서 3마리 (세로 배치)
    const firstRow = document.createElement('div');
    firstRow.className = 'enemy-row';
    for (let i = 0; i < Math.min(3, displayedEnemies.length); i++) {
        const enemy = displayedEnemies[i];
        const enemyEl = createEnemyElement(enemy, i);
        
        // 초기 위치를 화면 밖 오른쪽으로 설정
        enemyEl.style.transform = 'translateX(200px)';
        enemyEl.style.opacity = '0';
        
        firstRow.appendChild(enemyEl);
        
        // 다가오는 애니메이션
        await sleep(200);
        enemyEl.style.transition = 'all 0.5s ease-out';
        enemyEl.style.transform = 'translateX(0)';
        enemyEl.style.opacity = '1';
    }
    enemySide.appendChild(firstRow);
    
    // 두 번째 줄: 뒤에서 3마리 (4번째부터 6번째까지, 세로 배치)
    if (displayedEnemies.length > 3) {
        const secondRow = document.createElement('div');
        secondRow.className = 'enemy-row';
        for (let i = 3; i < displayedEnemies.length; i++) {
            const enemy = displayedEnemies[i];
            const enemyEl = createEnemyElement(enemy, i);
            
            // 초기 위치를 화면 밖 오른쪽으로 설정
            enemyEl.style.transform = 'translateX(200px)';
            enemyEl.style.opacity = '0';
            
            secondRow.appendChild(enemyEl);
            
            // 다가오는 애니메이션
            await sleep(200);
            enemyEl.style.transition = 'all 0.5s ease-out';
            enemyEl.style.transform = 'translateX(0)';
            enemyEl.style.opacity = '1';
        }
        enemySide.appendChild(secondRow);
    }
    
    await sleep(500);
}

/**
 * 적 요소 생성 (속성별로 다른 이미지, 가로 배치, 보스 크기 조정)
 */
function createEnemyElement(enemy, index) {
    const enemyDiv = document.createElement('div');
    const isBoss = enemy.isBoss || false;
    enemyDiv.className = 'battle-character enemy-character' + (isBoss ? ' boss' : '');
    enemyDiv.id = `enemy-${index}`;
    
    // 속성별 이미지 경로 (기본 진화 단계로 설정)
    const imagePath = getSpiritImagePath(enemy.spiritType, 0);
    const elementClass = getElementClass(enemy.spiritType);
    
    enemyDiv.innerHTML = `
        <div class="character-sprite ${elementClass}" id="enemySprite-${index}">
            <img class="character-image" id="enemyImage-${index}" src="${imagePath}" alt="${enemy.name || '적'}">
        </div>
        <div class="character-info">
            <div class="character-name" id="enemyName-${index}">${enemy.name || '적'}</div>
            <div class="hp-bar-container">
                <div class="hp-bar" id="enemyHpBar-${index}">
                    <div class="hp-fill" id="enemyHpFill-${index}"></div>
                </div>
                <div class="hp-text" id="enemyHpText-${index}">${enemy.maxHp || 100}/${enemy.maxHp || 100}</div>
            </div>
        </div>
    `;
    
    // 초기 HP 설정
    const hpBar = enemyDiv.querySelector(`#enemyHpBar-${index}`);
    const hpFill = enemyDiv.querySelector(`#enemyHpFill-${index}`);
    const hpText = enemyDiv.querySelector(`#enemyHpText-${index}`);
    updateHpBar(hpBar, hpFill, hpText, enemy.currentHp || enemy.maxHp || 100, enemy.maxHp || 100);
    
    return enemyDiv;
}

/**
 * 속성별 이모지 반환
 */
function getElementEmoji(spiritType) {
    if (!spiritType) return '👹';
    if (spiritType.includes('불') || spiritType.includes('FIRE')) return '🔥';
    if (spiritType.includes('물') || spiritType.includes('WATER')) return '💧';
    if (spiritType.includes('풀') || spiritType.includes('WIND')) return '🌿';
    if (spiritType.includes('빛') || spiritType.includes('LIGHT')) return '✨';
    if (spiritType.includes('어둠') || spiritType.includes('DARK')) return '🌑';
    return '👹';
}

/**
 * 속성별 CSS 클래스 반환
 */
function getElementClass(spiritType) {
    if (!spiritType) return '';
    if (spiritType.includes('불') || spiritType.includes('FIRE')) return 'fire-element';
    if (spiritType.includes('물') || spiritType.includes('WATER')) return 'water-element';
    if (spiritType.includes('풀') || spiritType.includes('WIND')) return 'wind-element';
    if (spiritType.includes('빛') || spiritType.includes('LIGHT')) return 'light-element';
    if (spiritType.includes('어둠') || spiritType.includes('DARK')) return 'dark-element';
    return '';
}

/**
 * HP 바 업데이트
 */
function updateHpBar(hpBar, hpFill, hpText, currentHp, maxHp) {
    if (!hpBar || !hpFill || !hpText) return;
    
    const percentage = Math.max(0, Math.min(100, (currentHp / maxHp) * 100));
    hpFill.style.width = percentage + '%';
    hpText.textContent = `${Math.max(0, Math.floor(currentHp))}/${maxHp}`;
    
    // HP에 따른 색상 변경
    hpFill.classList.remove('low', 'medium');
    if (percentage <= 30) {
        hpFill.classList.add('low');
    } else if (percentage <= 60) {
        hpFill.classList.add('medium');
    }
}

/**
 * 라운드 애니메이션 (자연스러운 타이밍)
 */
async function animateRound(round, dungeonRound, roundIndex) {
    if (!round.actions || round.actions.length === 0) return;
    
    // 일시정지 체크
    await waitIfPaused();
    
    // 모든 액션을 동시에 시작하되, 자연스러운 타이밍으로 조정
    const actionPromises = round.actions.map((action, index) => {
        return new Promise(async (resolve) => {
            // 일시정지 체크
            await waitIfPaused();
            
            setTimeout(async () => {
                await waitIfPaused();
                await animateAction(action, dungeonRound);
                resolve();
            }, index * 300); // 각 액션을 300ms 간격으로 시작
        });
    });
    
    await Promise.all(actionPromises);
    await waitIfPaused();
    await sleep(500); // 라운드 종료 후 대기
}

/**
 * 액션 애니메이션 (모든 정령 지원)
 */
async function animateAction(action, dungeonRound) {
    const playersInfo = window.currentPlayersInfo || [];
    const enemiesInfo = dungeonRound.enemiesInfo || [];
    
    // 공격자 찾기
    const attackerName = action.attacker;
    const isPlayerAttack = playersInfo.some(p => p.name === attackerName);
    
    let attackerEl = null;
    let attackerInfo = null;
    
    if (isPlayerAttack) {
        const playerIndex = playersInfo.findIndex(p => p.name === attackerName);
        if (playerIndex !== -1) {
            attackerEl = document.getElementById(`player-${playerIndex}`);
            attackerInfo = playersInfo[playerIndex];
        }
    } else {
        attackerEl = findEnemyElement(attackerName);
        attackerInfo = enemiesInfo.find(e => e.name === attackerName);
    }
    
    // 타겟 찾기
    const targetName = action.target;
    const isPlayerTarget = playersInfo.some(p => p.name === targetName);
    
    let targetEl = null;
    let targetInfo = null;
    
    if (isPlayerTarget) {
        const playerIndex = playersInfo.findIndex(p => p.name === targetName);
        if (playerIndex !== -1) {
            targetEl = document.getElementById(`player-${playerIndex}`);
            targetInfo = playersInfo[playerIndex];
        }
    } else {
        targetEl = findEnemyElement(targetName);
        targetInfo = enemiesInfo.find(e => e.name === targetName);
    }
    
    if (!attackerEl || !targetEl || !attackerInfo) return;
    
    const attackerSprite = attackerEl.querySelector('.character-sprite');
    const targetSprite = targetEl.querySelector('.character-sprite');
    
    // 공격 애니메이션 (더 자연스럽게)
    if (attackerSprite) {
        attackerSprite.classList.add('attacking');
        // 공격 방향에 따라 이동
        if (isPlayerAttack) {
            attackerSprite.style.transform = 'translateX(30px) scale(1.1)';
        } else {
            attackerSprite.style.transform = 'translateX(-30px) scale(1.1)';
        }
        setTimeout(() => {
            attackerSprite.classList.remove('attacking');
            attackerSprite.style.transform = '';
        }, 600);
    }
    
    // 기술별 이펙트 생성 (속성과 위력 기반, 능력치 비례)
    const damage = action.damage || 0;
    
    // 공격자의 능력치 정보 (액션 데이터에서 가져오거나 기본값 사용)
    const attackerStats = action.attackerInfo || attackerInfo || {};
    const attackerAttack = Math.max(
        attackerStats.meleeAttack || attackerInfo?.meleeAttack || 50,
        attackerStats.rangedAttack || attackerInfo?.rangedAttack || 50
    );
    const attackerLevel = attackerStats.level || attackerInfo?.level || 1;
    
    // 데미지에 비례한 위력 레벨 계산 (능력치 기반)
    const powerLevel = getPowerLevelByStats(damage, attackerAttack, attackerLevel);
    const elementType = attackerStats.spiritType || attackerInfo?.spiritType || '';
    const skillType = (attackerStats.meleeAttack || 0) > (attackerStats.rangedAttack || 0) ? 'melee' : 'ranged';
    
    createSkillEffectByElement(attackerSprite, targetSprite, powerLevel, skillType, damage, elementType);
    
    // 피해 애니메이션
    if (targetSprite) {
        targetSprite.classList.add('taking-damage');
        setTimeout(() => targetSprite.classList.remove('taking-damage'), 500);
    }
    
    // 데미지 텍스트 표시
    showDamageText(targetSprite, damage, action.isKill);
    
    // HP 업데이트
    if (isPlayerAttack) {
        // 적 HP 감소
        if (targetInfo) {
            targetInfo.currentHp = Math.max(0, (targetInfo.currentHp || targetInfo.maxHp) - damage);
            const enemyIndex = enemiesInfo.findIndex(e => e.name === targetName);
            if (enemyIndex !== -1) {
                const hpBar = document.getElementById(`enemyHpBar-${enemyIndex}`);
                const hpFill = document.getElementById(`enemyHpFill-${enemyIndex}`);
                const hpText = document.getElementById(`enemyHpText-${enemyIndex}`);
                if (hpBar && hpFill && hpText) {
                    updateHpBar(hpBar, hpFill, hpText, targetInfo.currentHp, targetInfo.maxHp);
                }
            }
        }
    } else {
        // 플레이어 HP 감소
        if (targetInfo) {
            targetInfo.currentHp = Math.max(0, (targetInfo.currentHp || targetInfo.maxHp) - damage);
            const playerIndex = playersInfo.findIndex(p => p.name === targetName);
            if (playerIndex !== -1) {
                const hpBar = document.getElementById(`playerHpBar-${playerIndex}`);
                const hpFill = document.getElementById(`playerHpFill-${playerIndex}`);
                const hpText = document.getElementById(`playerHpText-${playerIndex}`);
                if (hpBar && hpFill && hpText) {
                    updateHpBar(hpBar, hpFill, hpText, targetInfo.currentHp, targetInfo.maxHp);
                }
            }
        }
    }
    
    // 기술 사용 로그 추가 (항상 표시)
    const skillName = action.skillName || null;
    console.log('기술 로그 추가:', { skillName, attackerName, targetName, damage, isPlayerAttack });
    
    if (skillName && skillName !== 'null' && skillName.trim() !== '') {
        // 기술 이름이 있으면 기술 사용 로그
        if (isPlayerAttack) {
            addSkillLog(`✨ ${attackerName}이(가) "${skillName}"을(를) 사용했습니다! → ${targetName}에게 ${damage}의 피해!`);
        } else {
            addSkillLog(`⚔️ ${attackerName}이(가) "${skillName}"을(를) 사용했습니다! → ${targetName}에게 ${damage}의 피해!`);
        }
    } else {
        // 기술 이름이 없으면 기본 공격 로그
        if (isPlayerAttack) {
            addSkillLog(`⚔️ ${attackerName}이(가) ${targetName}에게 ${damage}의 피해를 입혔습니다!`);
        } else {
            addSkillLog(`💥 ${attackerName}이(가) ${targetName}에게 ${damage}의 피해를 입혔습니다!`);
        }
    }
    
    // 기존 로그 추가
    const actionType = isPlayerAttack ? 'attack' : 'defense';
    addLogEntry(`${attackerName}이(가) ${targetName}에게 ${damage}의 피해를 입혔습니다!`, actionType);
    
    if (action.isKill) {
        addLogEntry(`${targetName}이(가) 쓰러졌습니다!`, 'defeat');
        // 적 제거 애니메이션
        if (!isPlayerTarget && targetEl) {
            targetEl.style.opacity = '0';
            targetEl.style.transform = 'scale(0)';
            targetEl.style.transition = 'all 0.5s ease';
            setTimeout(() => targetEl.remove(), 500);
        }
    }
}

/**
 * 위력 레벨 결정 (데미지 기반)
 */
function getPowerLevel(damage) {
    if (damage >= 80) return 'very-strong';
    if (damage >= 50) return 'strong';
    if (damage >= 30) return 'normal';
    return 'weak';
}

/**
 * 위력 레벨 결정 (능력치 비례)
 */
function getPowerLevelByStats(damage, attackPower, level) {
    // 능력치와 레벨을 고려한 위력 계산
    const basePower = (attackPower + level * 2) / 10;
    const normalizedDamage = damage / basePower;
    
    if (normalizedDamage >= 8 || damage >= 100) return 'very-strong';
    if (normalizedDamage >= 5 || damage >= 60) return 'strong';
    if (normalizedDamage >= 3 || damage >= 30) return 'normal';
    return 'weak';
}

/**
 * 기술별 이펙트 생성 (속성별 고유 이펙트)
 */
function createSkillEffectByElement(attackerSprite, targetSprite, powerLevel, skillType, damage, elementType) {
    if (!attackerSprite || !targetSprite) return;
    
    const effectsContainer = document.getElementById('skillEffectsContainer');
    if (!effectsContainer) return;
    
    const attackerRect = attackerSprite.getBoundingClientRect();
    const targetRect = targetSprite.getBoundingClientRect();
    const containerRect = effectsContainer.getBoundingClientRect();
    
    // 속성별 이펙트 생성
    const elementClass = getElementClass(elementType || '');
    const effect = document.createElement('div');
    effect.className = `skill-effect ${powerLevel} ${skillType} ${elementClass}`;
    
    // 속성별 고유 이펙트
    let effectContent = getSkillEffectByElement(elementType, powerLevel, skillType);
    effect.innerHTML = effectContent;
    
    // 시작 위치 (공격자)
    const startX = attackerRect.left - containerRect.left + attackerRect.width / 2;
    const startY = attackerRect.top - containerRect.top + attackerRect.height / 2;
    
    // 목표 위치 (타겟)
    const endX = targetRect.left - containerRect.left + targetRect.width / 2;
    const endY = targetRect.top - containerRect.top + targetRect.height / 2;
    
    effect.style.left = startX + 'px';
    effect.style.top = startY + 'px';
    
    effectsContainer.appendChild(effect);
    
    // 애니메이션 실행
    requestAnimationFrame(() => {
        effect.style.transition = 'all 0.8s ease-out';
        effect.style.left = endX + 'px';
        effect.style.top = endY + 'px';
        effect.style.transform = 'scale(1.5)';
    });
    
    // 애니메이션 종료 후 제거
    setTimeout(() => {
        effect.style.opacity = '0';
        effect.style.transform = 'scale(0.5)';
        setTimeout(() => effect.remove(), 300);
    }, 1500);
}

/**
 * 속성별 기술 이펙트 반환
 */
function getSkillEffectByElement(elementType, powerLevel, skillType) {
    if (!elementType) return '💥';
    
    // 위력에 따른 배수
    const powerMultiplier = powerLevel === 'very-strong' ? 3 : 
                           powerLevel === 'strong' ? 2 : 
                           powerLevel === 'normal' ? 1 : 0.5;
    
    // 속성별 이펙트
    if (elementType.includes('불') || elementType.includes('FIRE')) {
        const fireEmoji = '🔥';
        return fireEmoji.repeat(Math.max(1, Math.floor(powerMultiplier)));
    } else if (elementType.includes('물') || elementType.includes('WATER')) {
        const waterEmoji = '💧';
        return waterEmoji.repeat(Math.max(1, Math.floor(powerMultiplier)));
    } else if (elementType.includes('풀') || elementType.includes('WIND') || elementType.includes('LEAF')) {
        const windEmoji = '🌿';
        return windEmoji.repeat(Math.max(1, Math.floor(powerMultiplier)));
    } else if (elementType.includes('빛') || elementType.includes('LIGHT')) {
        const lightEmoji = '✨';
        return lightEmoji.repeat(Math.max(1, Math.floor(powerMultiplier * 1.5)));
    } else if (elementType.includes('어둠') || elementType.includes('DARK')) {
        const darkEmoji = '🌑';
        return darkEmoji.repeat(Math.max(1, Math.floor(powerMultiplier * 1.5)));
    }
    
    return '💥';
}

/**
 * 데미지 텍스트 표시
 */
function showDamageText(targetSprite, damage, isKill) {
    if (!targetSprite) return;
    
    const damageContainer = document.getElementById('damageTextContainer');
    if (!damageContainer) return;
    
    const rect = targetSprite.getBoundingClientRect();
    const containerRect = damageContainer.getBoundingClientRect();
    
    const damageText = document.createElement('div');
    damageText.className = 'damage-text' + (damage >= 50 ? ' critical' : '');
    damageText.textContent = '-' + damage;
    
    // 상대 좌표로 설정
    const x = rect.left - containerRect.left + rect.width / 2;
    const y = rect.top - containerRect.top + rect.height / 2;
    
    damageText.style.left = x + 'px';
    damageText.style.top = y + 'px';
    
    damageContainer.appendChild(damageText);
    
    setTimeout(() => {
        damageText.remove();
    }, 1500);
}

/**
 * 이모지에서 속성 타입 추출
 */
function getElementTypeFromEmoji(emoji) {
    if (emoji.includes('🔥')) return '불의 정령';
    if (emoji.includes('💧')) return '물의 정령';
    if (emoji.includes('🌿')) return '풀의 정령';
    if (emoji.includes('✨')) return '빛의 정령';
    if (emoji.includes('🌑')) return '어둠의 정령';
    return '';
}

/**
 * 정령 이미지 경로 생성
 */
function getSpiritImagePath(spiritType, evolutionStage) {
    if (!spiritType) return '/images/spirits/step1_fire.png';
    
    // 진화 단계에 따른 step 번호 (0 -> step1, 1 -> step2, 2 -> step3)
    const step = evolutionStage !== undefined ? evolutionStage + 1 : 1;
    
    // 정령 타입에 따른 코드
    let typeCode = 'fire';
    if (spiritType.includes('물') || spiritType.includes('WATER')) {
        typeCode = 'water';
    } else if (spiritType.includes('풀') || spiritType.includes('WIND') || spiritType.includes('LEAF')) {
        typeCode = 'leaf';
    } else if (spiritType.includes('빛') || spiritType.includes('LIGHT')) {
        typeCode = 'light';
    } else if (spiritType.includes('어둠') || spiritType.includes('DARK')) {
        typeCode = 'dark';
    }
    
    return `/images/spirits/step${step}_${typeCode}.png`;
}

/**
 * 적 요소 찾기
 */
function findEnemyElement(enemyName) {
    const enemies = document.querySelectorAll('.enemy-character');
    for (const enemy of enemies) {
        const nameEl = enemy.querySelector('.character-name');
        if (nameEl && nameEl.textContent === enemyName) {
            return enemy;
        }
    }
    return null;
}

/**
 * 적 인덱스 찾기
 */
function findEnemyIndex(enemyName) {
    const enemies = document.querySelectorAll('.enemy-character');
    for (let i = 0; i < enemies.length; i++) {
        const nameEl = enemies[i].querySelector('.character-name');
        if (nameEl && nameEl.textContent === enemyName) {
            return i;
        }
    }
    return -1;
}

/**
 * 기술 사용 로그 추가
 */
function addSkillLog(message) {
    const logContent = document.getElementById('skillLogContent');
    if (!logContent) return;
    
    const logEntry = document.createElement('div');
    logEntry.className = 'skill-log-entry';
    logEntry.textContent = message;
    
    logContent.appendChild(logEntry);
    
    // 스크롤을 맨 아래로
    logContent.scrollTop = logContent.scrollHeight;
    
    // 최대 20개까지만 유지 (오래된 로그 제거)
    while (logContent.children.length > 20) {
        logContent.removeChild(logContent.firstChild);
    }
}

/**
 * 로그 항목 추가
 */
function addLogEntry(message, type = 'normal') {
    const logContent = document.getElementById('logContent');
    if (!logContent) return;
    
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    entry.textContent = message;
    
    logContent.appendChild(entry);
    logContent.scrollTop = logContent.scrollHeight;
}

/**
 * Sleep 함수
 */
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * 로딩 표시
 */
function showLoading(message) {
    // 간단한 로딩 표시
    const loadingDiv = document.createElement('div');
    loadingDiv.id = 'loadingOverlay';
    loadingDiv.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
        color: white;
        font-size: 24px;
    `;
    loadingDiv.textContent = message || '로딩 중...';
    document.body.appendChild(loadingDiv);
}

/**
 * 로딩 숨기기
 */
function hideLoading() {
    const loadingDiv = document.getElementById('loadingOverlay');
    if (loadingDiv) {
        loadingDiv.remove();
    }
}

/**
 * 일시정지 대기 함수
 */
function waitIfPaused() {
    return new Promise((resolve) => {
        const checkPause = () => {
            if (!battlePaused) {
                resolve();
            } else {
                setTimeout(checkPause, 100);
            }
        };
        checkPause();
    });
}

/**
 * 전투 일시정지
 */
function pauseBattle() {
    battlePaused = true;
    const pauseBtn = document.getElementById('pauseBtn');
    const resumeBtn = document.getElementById('resumeBtn');
    if (pauseBtn) pauseBtn.style.display = 'none';
    if (resumeBtn) resumeBtn.style.display = 'inline-block';
    addSkillLog('⏸️ 전투가 일시정지되었습니다.');
}

/**
 * 전투 재개
 */
function resumeBattle() {
    battlePaused = false;
    const pauseBtn = document.getElementById('pauseBtn');
    const resumeBtn = document.getElementById('resumeBtn');
    if (pauseBtn) pauseBtn.style.display = 'inline-block';
    if (resumeBtn) resumeBtn.style.display = 'none';
    addSkillLog('▶️ 전투가 재개되었습니다.');
}

/**
 * 전투 다시하기
 */
async function restartBattle() {
    if (!confirm('전투를 다시 시작하시겠습니까?')) {
        return;
    }
    
    // 현재 전투 종료
    battlePaused = false;
    const battleModal = document.getElementById('battleAnimationModal');
    if (battleModal) {
        battleModal.style.display = 'none';
    }
    
    // 잠시 대기 후 다시 시작
    await sleep(500);
    
    if (currentSpiritId && currentStageNumber) {
        await startBattle(currentSpiritId, currentStageNumber);
    } else {
        showError('다시하기 정보가 없습니다.');
    }
}

/**
 * 전투 종료
 */
function exitBattle() {
    if (!confirm('전투를 종료하시겠습니까? 진행 상황은 저장되지 않습니다.')) {
        return;
    }
    
    battlePaused = false;
    const battleModal = document.getElementById('battleAnimationModal');
    if (battleModal) {
        battleModal.style.display = 'none';
    }
    
    // 전역 변수 초기화
    currentBattleResult = null;
    currentStageNumber = null;
    currentSpiritId = null;
    battleAnimationPromise = null;
}

/**
 * 전투 컨트롤 버튼 이벤트 리스너 설정
 */
function setupBattleControls() {
    const pauseBtn = document.getElementById('pauseBtn');
    const resumeBtn = document.getElementById('resumeBtn');
    const restartBtn = document.getElementById('restartBtn');
    const exitBtn = document.getElementById('exitBtn');
    
    if (pauseBtn) {
        pauseBtn.onclick = pauseBattle;
    }
    if (resumeBtn) {
        resumeBtn.onclick = resumeBattle;
    }
    if (restartBtn) {
        restartBtn.onclick = restartBattle;
    }
    if (exitBtn) {
        exitBtn.onclick = exitBattle;
    }
}

// 전역 스코프에 함수 등록 (안전을 위해)
window.pauseBattle = pauseBattle;
window.resumeBattle = resumeBattle;
window.restartBattle = restartBattle;
window.exitBattle = exitBattle;

// 모달 외부 클릭 시 닫기
window.onclick = function(event) {
    const spiritModal = document.getElementById('spiritSelectModal');
    const battleModal = document.getElementById('battleResultModal');
    
    if (event.target == spiritModal) {
        closeSpiritSelectModal();
    }
    if (event.target == battleModal) {
        closeBattleResultModal();
    }
}
