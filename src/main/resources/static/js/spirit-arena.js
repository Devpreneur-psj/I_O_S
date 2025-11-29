// 정령 시합장 JavaScript

let selectedSpiritId = null;
let selectedDifficulty = null;

document.addEventListener('DOMContentLoaded', function() {
    const spiritSelect = document.getElementById('spiritSelect');
    
    // URL 파라미터에서 spiritId 확인
    const urlParams = new URLSearchParams(window.location.search);
    const spiritIdParam = urlParams.get('spiritId');
    
    if (spiritIdParam) {
        // 정령 자동 선택
        spiritSelect.value = spiritIdParam;
        selectedSpiritId = spiritIdParam;
        document.getElementById('battleSection').style.display = 'block';
    }
    
    spiritSelect.addEventListener('change', function() {
        selectedSpiritId = this.value;
        if (selectedSpiritId) {
            document.getElementById('battleSection').style.display = 'block';
            updateSpiritPreview(selectedSpiritId);
        } else {
            document.getElementById('battleSection').style.display = 'none';
            document.getElementById('battleResultSection').style.display = 'none';
            document.getElementById('selectedSpiritPreview').style.display = 'none';
        }
    });
    
    // 초기 로드 시 정령 미리보기 업데이트
    if (spiritIdParam) {
        updateSpiritPreview(spiritIdParam);
    }
});

/**
 * 정령 미리보기 업데이트
 */
function updateSpiritPreview(spiritId) {
    const selectedOption = document.querySelector(`#spiritSelect option[value="${spiritId}"]`);
    if (!selectedOption) return;
    
    const spiritType = selectedOption.getAttribute('data-spirit-type');
    const evolutionStage = selectedOption.getAttribute('data-evolution-stage') || '0';
    const spiritName = selectedOption.textContent.split(' (')[0];
    
    // 이미지 경로 생성
    const imagePath = getSpiritImagePath(spiritType, parseInt(evolutionStage));
    
    // 미리보기 표시
    const preview = document.getElementById('selectedSpiritPreview');
    const previewImage = document.getElementById('selectedSpiritImage');
    const previewName = document.getElementById('selectedSpiritName');
    
    if (preview && previewImage && previewName) {
        previewImage.src = imagePath;
        previewName.textContent = spiritName;
        preview.style.display = 'block';
    }
}

/**
 * 정령 타입과 진화 단계에 따른 이미지 경로 반환
 */
function getSpiritImagePath(spiritType, evolutionStage) {
    const stage = evolutionStage || 0;
    const typeMap = {
        '불의 정령': 'fire',
        '물의 정령': 'water',
        '풀의 정령': 'leaf',
        '빛의 정령': 'light',
        '어둠의 정령': 'dark'
    };
    
    const typeCode = typeMap[spiritType] || 'fire';
    const step = stage === 0 ? 'step1' : (stage === 1 ? 'step2' : 'step3');
    
    return `/images/spirits/${step}_${typeCode}.png`;
}

/**
 * 난이도 선택
 */
function selectDifficulty(difficulty, element) {
    selectedDifficulty = difficulty;
    
    // 모든 난이도 카드에서 선택 상태 제거
    document.querySelectorAll('.difficulty-card').forEach(card => {
        card.classList.remove('selected');
    });
    
    // 선택한 난이도 카드에 선택 상태 추가
    if (element) {
        element.classList.add('selected');
    } else {
        const card = document.querySelector(`[data-difficulty="${difficulty}"]`);
        if (card) {
            card.classList.add('selected');
        }
    }
    
    // 시합 시작 버튼 활성화
    const battleBtn = document.getElementById('battleBtn');
    const difficultyNames = {
        'EASY': '쉬움',
        'NORMAL': '보통',
        'HARD': '어려움',
        'EXPERT': '전문가'
    };
    battleBtn.disabled = false;
    battleBtn.textContent = `${difficultyNames[difficulty]} 난이도로 시합 시작`;
}

/**
 * 시합 시작
 */
async function startBattle() {
    if (!selectedSpiritId) {
        showWarning('정령을 선택해주세요.');
        return;
    }
    
    if (!selectedDifficulty) {
        showWarning('난이도를 선택해주세요.');
        return;
    }
    
    try {
        // 로딩 표시
        const battleBtn = document.getElementById('battleBtn');
        battleBtn.disabled = true;
        battleBtn.textContent = '전투 중...';
        
        const response = await fetch('/arena/api/battle', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${selectedSpiritId}&difficulty=${selectedDifficulty}`
        });
        
        const result = await response.json();
        
        battleBtn.disabled = false;
        battleBtn.textContent = '난이도를 선택하세요';
        
        if (result.success) {
            displayBattleResult(result);
        } else {
            showError('시합 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error starting battle:', error);
        showError('시합 중 오류가 발생했습니다: ' + error.message);
        const battleBtn = document.getElementById('battleBtn');
        battleBtn.disabled = false;
        battleBtn.textContent = '난이도를 선택하세요';
    }
}

/**
 * 시합 결과 표시
 */
function displayBattleResult(result) {
    const resultSection = document.getElementById('battleResultSection');
    const resultDiv = document.getElementById('battleResult');
    
    resultSection.style.display = 'block';
    resultSection.scrollIntoView({ behavior: 'smooth' });
    
    if (result.victory) {
        let html = `
            <h3 style="color: #FFD700; margin-bottom: 20px;">🎉 승리!</h3>
            <p style="font-size: 18px; margin-bottom: 10px;">라운드 수: ${result.rounds}</p>
            <p style="font-size: 18px; margin-bottom: 10px;">남은 HP: ${result.playerRemainingHp}</p>
            <p style="font-size: 18px; margin-bottom: 10px; color: #FFD700;">상금: +${result.prizeMoney}골드</p>
            <p style="font-size: 18px; margin-bottom: 10px; color: #A8E6CF;">경험치: +${result.expGain}</p>
        `;
        
        if (result.levelUp) {
            html += `<p style="color: #FFA500; font-size: 20px; font-weight: 700; margin-top: 15px;">✨ 레벨업! Lv.${result.newLevel}</p>`;
        }
        
        html += `<p style="color: rgba(255,255,255,0.7); margin-top: 20px;">정령이 경험치와 상금을 획득했습니다!</p>`;
        
        resultDiv.innerHTML = html;
        
        // 3초 후 페이지 새로고침
        setTimeout(() => {
            location.reload();
        }, 3000);
    } else {
        resultDiv.innerHTML = `
            <h3 style="color: #FF6347; margin-bottom: 20px;">💀 패배</h3>
            <p style="font-size: 18px; margin-bottom: 10px;">라운드 수: ${result.rounds}</p>
            <p style="font-size: 18px; margin-bottom: 10px; color: #A8E6CF;">경험치: +${result.expGain}</p>
            <p style="color: rgba(255,255,255,0.7); margin-top: 20px;">다음 시합을 위해 정령을 더 강화하세요!</p>
        `;
    }
}

