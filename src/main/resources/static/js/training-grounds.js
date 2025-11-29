// 정령 수련장 JavaScript

let selectedSpiritId = null;
let selectedTrainingType = null;

document.addEventListener('DOMContentLoaded', function() {
    const spiritSelect = document.getElementById('spiritSelect');
    
    // URL 파라미터에서 spiritId 확인
    const urlParams = new URLSearchParams(window.location.search);
    const spiritIdParam = urlParams.get('spiritId');
    
    if (spiritIdParam) {
        // 정령 자동 선택
        spiritSelect.value = spiritIdParam;
        selectedSpiritId = spiritIdParam;
        document.getElementById('trainingSection').style.display = 'block';
        loadSpiritInfo(spiritIdParam);
        updateSpiritPreview(spiritIdParam);
    }
    
    spiritSelect.addEventListener('change', function() {
        selectedSpiritId = this.value;
        if (selectedSpiritId) {
            document.getElementById('trainingSection').style.display = 'block';
            // 정령 정보 표시
            loadSpiritInfo(selectedSpiritId);
            // 정령 이미지 표시
            updateSpiritPreview(selectedSpiritId);
        } else {
            document.getElementById('trainingSection').style.display = 'none';
            document.getElementById('trainingResultSection').style.display = 'none';
            document.getElementById('selectedSpiritPreview').style.display = 'none';
        }
    });
});

/**
 * 정령 정보 로드
 */
async function loadSpiritInfo(spiritId) {
    try {
        const response = await fetch(`/spirit/api/${spiritId}`, {
            credentials: 'include'
        });
        
        if (response.ok) {
            const spirit = await response.json();
            const trainingInfo = document.getElementById('trainingInfo');
            trainingInfo.innerHTML = `
                <p><strong>${spirit.name}</strong> (${spirit.spiritType} Lv.${spirit.level})</p>
                <p>현재 에너지: ${spirit.energy}/100</p>
                <p class="hint">훈련은 정령의 에너지를 소모하며 능력치를 향상시킵니다.</p>
            `;
        }
    } catch (error) {
        console.error('Error loading spirit info:', error);
    }
}

/**
 * 훈련 타입 선택
 */
function selectTraining(trainingType, element) {
    selectedTrainingType = trainingType;
    
    // 모든 훈련 카드에서 선택 상태 제거
    document.querySelectorAll('.training-card').forEach(card => {
        card.classList.remove('selected');
    });
    
    // 선택한 훈련 카드에 선택 상태 추가
    if (element) {
        element.classList.add('selected');
    } else {
        const card = document.querySelector(`[data-training="${trainingType}"]`);
        if (card) {
            card.classList.add('selected');
        }
    }
    
    // 훈련 시작 버튼 활성화
    const trainingBtn = document.getElementById('trainingBtn');
    const trainingNames = {
        'ATTACK': '공격 훈련',
        'DEFENSE': '방어 훈련',
        'SPEED': '스피드 훈련',
        'BALANCED': '균형 훈련'
    };
    trainingBtn.disabled = false;
    trainingBtn.textContent = `${trainingNames[trainingType]} 시작`;
}

/**
 * 훈련 시작
 */
async function startTraining() {
    if (!selectedSpiritId) {
        showWarning('정령을 선택해주세요.');
        return;
    }
    
    if (!selectedTrainingType) {
        showWarning('훈련 타입을 선택해주세요.');
        return;
    }
    
    try {
        // 로딩 표시
        const trainingBtn = document.getElementById('trainingBtn');
        trainingBtn.disabled = true;
        trainingBtn.textContent = '훈련 중...';
        
        const response = await fetch('/training/api/train', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${selectedSpiritId}&trainingType=${selectedTrainingType}`
        });
        
        const result = await response.json();
        
        trainingBtn.disabled = false;
        trainingBtn.textContent = '훈련 타입을 선택하세요';
        
        if (result.success) {
            displayTrainingResult(result);
        } else {
            showError('훈련 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error starting training:', error);
        showError('훈련 중 오류가 발생했습니다: ' + error.message);
        const trainingBtn = document.getElementById('trainingBtn');
        trainingBtn.disabled = false;
        trainingBtn.textContent = '훈련 타입을 선택하세요';
    }
}

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
 * 훈련 결과 표시
 */
function displayTrainingResult(result) {
    const resultSection = document.getElementById('trainingResultSection');
    const resultDiv = document.getElementById('trainingResult');
    
    resultSection.style.display = 'block';
    resultSection.scrollIntoView({ behavior: 'smooth' });
    
    resultDiv.innerHTML = `
        <h3 style="color: #FFD700; margin-bottom: 20px;">✅ 훈련 완료!</h3>
        <p style="font-size: 18px; margin-bottom: 10px;">${result.message}</p>
        <p style="color: rgba(255,255,255,0.7); margin-top: 20px;">정령의 능력치가 향상되었습니다!</p>
    `;
    
    // 3초 후 페이지 새로고침
    setTimeout(() => {
        location.reload();
    }, 3000);
}

