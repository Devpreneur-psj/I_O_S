// 기술 강의실 JavaScript

let selectedSpiritId = null;
let allSkills = new Map(); // skillId -> skill 정보
let learnedSkillCount = 0; // 배운 기술 개수
let skillUpdateInterval = null; // 기술 목록 자동 업데이트 인터벌

document.addEventListener('DOMContentLoaded', function() {
    const spiritSelect = document.getElementById('spiritSelect');
    
    spiritSelect.addEventListener('change', function() {
        selectedSpiritId = this.value;
        if (selectedSpiritId) {
            updateSelectedSpiritPreview(this);
            loadSpiritSkills(selectedSpiritId);
            loadLearnedSkillCount(selectedSpiritId);
            startSkillUpdateInterval();
        } else {
            hideSelectedSpiritPreview();
            hideAllSections();
            stopSkillUpdateInterval();
        }
    });
    
    // 페이지 언로드 시 인터벌 정리
    window.addEventListener('beforeunload', function() {
        stopSkillUpdateInterval();
    });
});

/**
 * 기술 목록 자동 업데이트 시작 (학습 완료 체크)
 */
function startSkillUpdateInterval() {
    stopSkillUpdateInterval(); // 기존 인터벌 정리
    
    // 0.1초마다 기술 목록 업데이트 (학습 완료 체크)
    skillUpdateInterval = setInterval(function() {
        if (selectedSpiritId) {
            // 학습 중인 기술이 있는지 먼저 확인
            const learnedSkillsGrid = document.getElementById('learnedSkillsGrid');
            const hasLearningSkills = learnedSkillsGrid && learnedSkillsGrid.querySelector('[data-is-learning="true"]');
            
            // 학습 중인 기술이 있을 때만 업데이트
            if (hasLearningSkills) {
                loadSpiritSkills(selectedSpiritId);
            }
        }
    }, 100); // 0.1초 (100ms)
}

/**
 * 기술 목록 자동 업데이트 중지
 */
function stopSkillUpdateInterval() {
    if (skillUpdateInterval) {
        clearInterval(skillUpdateInterval);
        skillUpdateInterval = null;
    }
}

/**
 * 선택된 정령 미리보기 업데이트
 */
function updateSelectedSpiritPreview(selectElement) {
    const selectedOption = selectElement.options[selectElement.selectedIndex];
    if (!selectedOption || !selectedOption.value) {
        hideSelectedSpiritPreview();
        return;
    }
    
    const spiritType = selectedOption.getAttribute('data-spirit-type');
    const evolutionStage = selectedOption.getAttribute('data-evolution-stage');
    const spiritName = selectedOption.text;
    
    // 이미지 경로 생성
    const typeCode = getTypeCode(spiritType);
    const stepNumber = evolutionStage === '0' ? '1' : evolutionStage === '1' ? '2' : '3';
    const imagePath = `/images/spirits/step${stepNumber}_${typeCode}.png`;
    
    // 미리보기 업데이트
    const preview = document.getElementById('selectedSpiritPreview');
    const previewImage = document.getElementById('selectedSpiritImage');
    const previewName = document.getElementById('selectedSpiritName');
    
    previewImage.src = imagePath;
    previewImage.alt = spiritName;
    previewName.textContent = spiritName;
    preview.style.display = 'block';
}

/**
 * 정령 타입 코드 가져오기
 */
function getTypeCode(spiritType) {
    if (spiritType === '불의 정령') return 'fire';
    if (spiritType === '물의 정령') return 'water';
    if (spiritType === '풀의 정령') return 'leaf';
    if (spiritType === '빛의 정령') return 'light';
    if (spiritType === '어둠의 정령') return 'dark';
    return 'fire'; // 기본값
}

/**
 * 선택된 정령 미리보기 숨기기
 */
function hideSelectedSpiritPreview() {
    const preview = document.getElementById('selectedSpiritPreview');
    preview.style.display = 'none';
}

/**
 * 정령의 기술 목록 로드
 */
async function loadSpiritSkills(spiritId) {
    try {
        // 배운 기술 로드
        const learnedResponse = await fetch(`/magic-academy/api/learned-skills/${spiritId}`, {
            credentials: 'include'
        });
        
        if (learnedResponse.ok) {
            const learnedSkills = await learnedResponse.json();
            // 카운트를 먼저 API에서 가져와서 설정 (displayLearnedSkills에서 덮어쓰지 않도록)
            await loadLearnedSkillCount(spiritId);
            displayLearnedSkills(learnedSkills);
        } else {
            // API 실패 시에도 카운트는 업데이트 시도
            await loadLearnedSkillCount(spiritId);
        }
        
        // 배울 수 있는 기술 로드
        const learnableResponse = await fetch(`/magic-academy/api/learnable-skills/${spiritId}`, {
            credentials: 'include'
        });
        
        if (learnableResponse.ok) {
            try {
                const response = await learnableResponse.json();
                console.log('Learnable skills response:', response);
                const learnableSkills = Array.isArray(response.skills) ? response.skills : (Array.isArray(response) ? response : []);
                // API 응답에서 isLearning 정보 사용 (중복 호출 제거)
                const isLearning = response.isLearning || false;
                displayLearnableSkills(learnableSkills, isLearning);
            } catch (error) {
                console.error('Error parsing learnable skills response:', error);
                displayLearnableSkills([], false);
            }
        } else {
            console.error('Failed to load learnable skills:', learnableResponse.status, learnableResponse.statusText);
            displayLearnableSkills([], false);
        }
        
        // 섹션 표시
        document.getElementById('learnedSkillsSection').style.display = 'block';
        document.getElementById('learnableSkillsSection').style.display = 'block';
        document.getElementById('emptyMessage').style.display = 'none';
        
    } catch (error) {
        console.error('Error loading spirit skills:', error);
        showError('기술 목록을 불러오는 중 오류가 발생했습니다.');
    }
}

/**
 * 배운 기술 표시
 */
async function displayLearnedSkills(learnedSkills) {
    const grid = document.getElementById('learnedSkillsGrid');
    grid.innerHTML = '';
    
    // 학습 완료 시간이 지난 기술이 있는지 확인하고 프론트엔드에서 즉시 완료 처리
    const now = new Date();
    let hasCompletedSkills = false;
    
    console.log('displayLearnedSkills 시작 - 전체 기술:', learnedSkills);
    
    // 학습 완료 시간이 지난 기술을 즉시 완료 상태로 변경
    const updatedSkills = learnedSkills.map(ss => {
        console.log('기술 체크:', {
            skillName: ss.skillName || ss.skillId,
            isLearning: ss.isLearning,
            learnedAt: ss.learnedAt,
            learningCompletionTime: ss.learningCompletionTime
        });
        
        // 학습 완료 시간이 있고, learnedAt이 없는 경우 처리
        if (ss.learningCompletionTime) {
            const completionTime = new Date(ss.learningCompletionTime);
            const isPast = now >= completionTime;
            
            console.log('학습 완료 시간 체크:', {
                skillName: ss.skillName || ss.skillId,
                completionTime: completionTime,
                now: now,
                isPast: isPast,
                hasLearnedAt: ss.learnedAt != null
            });
            
            // 학습 완료 시간이 지났고, learnedAt이 없는 경우
            if (isPast && (ss.learnedAt == null || ss.learnedAt === '')) {
                // 프론트엔드에서 즉시 완료 상태로 변경
                hasCompletedSkills = true;
                console.log('학습 완료 처리 (learnedAt 추가):', ss.skillName || ss.skillId);
                return {
                    ...ss,
                    isLearning: false,
                    learnedAt: now.toISOString() // 현재 시간을 ISO 문자열로 설정
                };
            }
        }
        
        return ss;
    });
    
    // 백엔드에도 완료 처리 요청 후 배울 수 있는 기술 목록 새로고침
    if (hasCompletedSkills) {
        // 학습 완료 처리와 동시에 모든 정보 새로고침
        Promise.all([
            fetch(`/magic-academy/api/check-learning-completion/${selectedSpiritId}`, {
                method: 'POST',
                credentials: 'include'
            }),
            loadLearnedSkillCount(selectedSpiritId)
        ]).then(() => {
            // 학습 완료 처리 후 배울 수 있는 기술 목록 새로고침
            if (selectedSpiritId) {
                fetch(`/magic-academy/api/learnable-skills/${selectedSpiritId}`, {
                    credentials: 'include'
                }).then(response => {
                    if (response.ok) {
                        return response.json();
                    }
                }).then(response => {
                    if (response) {
                        const learnableSkills = Array.isArray(response.skills) ? response.skills : (Array.isArray(response) ? response : []);
                        const isLearning = response.isLearning || false;
                        displayLearnableSkills(learnableSkills, isLearning);
                    }
                }).catch(error => {
                    console.error('배울 수 있는 기술 목록 새로고침 중 오류:', error);
                });
            }
        }).catch(error => {
            console.error('학습 완료 확인 중 오류:', error);
        });
    }
    
    // 카운트는 loadLearnedSkillCount에서 이미 설정되었으므로 여기서는 계산하지 않음
    // 학습 완료된 기술이 있으면 카운트만 다시 로드
    if (hasCompletedSkills) {
        // 학습 완료 후 카운트 다시 로드
        loadLearnedSkillCount(selectedSpiritId);
    }
    
    // 업데이트된 기술 목록으로 표시
    learnedSkills = updatedSkills;
    
    if (learnedSkills.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: rgba(255,255,255,0.5);">아직 배운 기술이 없습니다.</div>';
        return;
    }
    
    // 학습 중인 기술이 있는지 확인
    const isLearningInProgress = learnedSkills.some(ss => ss.isLearning);
    
    // 기술 정보 표시 (API에서 이미 Skill 정보를 포함해서 반환함)
    for (const spiritSkill of learnedSkills) {
        if (spiritSkill.skillName) {
            // Skill 정보가 이미 포함되어 있음
            const skill = {
                id: spiritSkill.skillId,
                skillName: spiritSkill.skillName,
                skillType: spiritSkill.skillType,
                elementType: spiritSkill.elementType,
                basePower: spiritSkill.basePower,
                description: spiritSkill.description,
                unlockEvolutionStage: spiritSkill.unlockEvolutionStage,
                learnTimeMinutes: spiritSkill.learnTimeMinutes,
                cooldownSeconds: spiritSkill.cooldownSeconds,
                effectType: spiritSkill.effectType,
                effectValue: spiritSkill.effectValue
            };
            allSkills.set(skill.id, skill);
            const card = createSkillCard(skill, true, spiritSkill.masteryLevel, spiritSkill, isLearningInProgress);
            grid.appendChild(card);
        } else {
            // Skill 정보가 없으면 가져오기
            const skill = await getSkillInfo(spiritSkill.skillId);
            if (skill) {
                const card = createSkillCard(skill, true, spiritSkill.masteryLevel, spiritSkill, isLearningInProgress);
                grid.appendChild(card);
            }
        }
    }
    
    // 학습 중인 기술이 있으면 타이머 시작
    const learningSkill = learnedSkills.find(ss => ss.isLearning);
    if (learningSkill) {
        startLearningTimer(learningSkill);
    }
}

/**
 * 배울 수 있는 기술 표시
 */
function displayLearnableSkills(learnableSkills, isLearning) {
    const grid = document.getElementById('learnableSkillsGrid');
    if (!grid) {
        console.error('learnableSkillsGrid element not found');
        return;
    }
    
    grid.innerHTML = '';
    
    // learnableSkills가 배열이 아니거나 undefined인 경우 처리
    if (learnableSkills === null || learnableSkills === undefined) {
        console.error('learnableSkills is null or undefined:', learnableSkills);
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: rgba(255,255,255,0.5);">배울 수 있는 기술을 불러올 수 없습니다.</div>';
        return;
    }
    
    if (!Array.isArray(learnableSkills)) {
        console.error('learnableSkills is not an array:', learnableSkills, typeof learnableSkills);
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: rgba(255,255,255,0.5);">배울 수 있는 기술을 불러올 수 없습니다.</div>';
        return;
    }
    
    if (learnableSkills.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: rgba(255,255,255,0.5);">배울 수 있는 기술이 없습니다.</div>';
        return;
    }
    
    // isLearning이 undefined일 수 있으므로 기본값 설정
    const isLearningInProgress = isLearning === true;
    
    try {
        for (const skill of learnableSkills) {
        // 기술 정보를 allSkills에 저장
        allSkills.set(skill.id, {
            id: skill.id,
            skillName: skill.skillName,
            skillType: skill.skillType,
            elementType: skill.elementType,
            basePower: skill.basePower,
            description: skill.description,
            unlockEvolutionStage: skill.unlockEvolutionStage,
            learnTimeMinutes: skill.learnTimeMinutes,
            cooldownSeconds: skill.cooldownSeconds,
            effectType: skill.effectType,
            effectValue: skill.effectValue
        });
            const card = createSkillCard(skill, false, null, null, isLearningInProgress);
            grid.appendChild(card);
        }
    } catch (error) {
        console.error('Error displaying learnable skills:', error);
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: rgba(255,255,255,0.5);">기술 목록을 표시하는 중 오류가 발생했습니다.</div>';
    }
}

/**
 * 기술 정보 가져오기
 */
async function getSkillInfo(skillId) {
    if (allSkills.has(skillId)) {
        return allSkills.get(skillId);
    }
    
    // API에서 가져오기 (필요시)
    // 현재는 learnableSkills에서 이미 가져온 정보 사용
    return null;
}

/**
 * 기술 카드 생성
 */
function createSkillCard(skill, isLearned, masteryLevel, spiritSkill, isLearningInProgress) {
    const card = document.createElement('div');
    card.className = `skill-card ${isLearned ? 'learned' : 'learnable'}`;
    if (spiritSkill && spiritSkill.isLearning) {
        card.classList.add('learning');
        card.setAttribute('data-learning-completion', spiritSkill.learningCompletionTime);
    }
    
    // 기술 타입 텍스트 결정
    let skillTypeText = '';
    let skillTypeClass = '';
    if (skill.skillType === 'RANGED_ATTACK') {
        skillTypeText = '원거리 공격';
        skillTypeClass = 'ranged';
    } else if (skill.skillType === 'MELEE_ATTACK') {
        skillTypeText = '근거리 공격';
        skillTypeClass = 'melee';
    } else if (skill.skillType === 'SUPPORT') {
        if (skill.effectType && (skill.effectType.startsWith('BUFF') || skill.effectType === 'HEAL')) {
            skillTypeText = '버프';
            skillTypeClass = 'buff';
        } else if (skill.effectType && skill.effectType.startsWith('DEBUFF')) {
            skillTypeText = '디버프';
            skillTypeClass = 'debuff';
        } else {
            skillTypeText = '보조 기술';
            skillTypeClass = 'support';
        }
    } else if (skill.effectType && (skill.effectType.startsWith('BUFF') || skill.effectType === 'HEAL')) {
        skillTypeText = '버프';
        skillTypeClass = 'buff';
    } else if (skill.effectType && skill.effectType.startsWith('DEBUFF')) {
        skillTypeText = '디버프';
        skillTypeClass = 'debuff';
    } else {
        skillTypeText = skill.skillType || '기술';
        skillTypeClass = 'other';
    }
    
    let masteryHtml = '';
    if (isLearned && masteryLevel && !(spiritSkill && spiritSkill.isLearning)) {
        masteryHtml = `
            <div class="mastery-level">
                <span style="color: rgba(255,255,255,0.7); font-size: 12px;">숙련도:</span>
                <div class="mastery-stars">
                    ${Array.from({length: 5}, (_, i) => 
                        `<div class="mastery-star ${i < masteryLevel ? 'filled' : ''}"></div>`
                    ).join('')}
                </div>
            </div>
        `;
    }
    
    let learningProgressHtml = '';
    if (spiritSkill && spiritSkill.isLearning) {
        const learnTimeMinutes = skill.learnTimeMinutes || 30;
        learningProgressHtml = `
            <div class="learning-progress">
                <div class="learning-label">학습 진행 중...</div>
                <div class="learning-timer" id="learning-timer-${spiritSkill.spiritSkillId}">계산 중...</div>
                <div class="learning-time-info">예상 소요 시간: ${learnTimeMinutes}분</div>
            </div>
        `;
    }
    
    card.innerHTML = `
        <div class="skill-header">
            <div class="skill-name">${skill.skillName}</div>
            <div class="skill-type-badge ${skillTypeClass}">${skillTypeText}</div>
        </div>
        <div class="skill-description">${skill.description || '설명 없음'}</div>
        <div class="skill-stats">
            ${skill.basePower > 0 ? `
            <div class="stat-item">
                <span class="stat-label">기본 위력</span>
                <span class="stat-value">${skill.basePower}</span>
            </div>
            ` : ''}
            <div class="stat-item">
                <span class="stat-label">해금 단계</span>
                <span class="stat-value">${skill.unlockEvolutionStage === 0 ? '기본' : skill.unlockEvolutionStage === 1 ? '1차' : '2차'}</span>
            </div>
            ${skill.learnTimeMinutes ? `
            <div class="stat-item">
                <span class="stat-label">학습 시간</span>
                <span class="stat-value">${skill.learnTimeMinutes}분</span>
            </div>
            ` : ''}
            ${skill.cooldownSeconds ? `
            <div class="stat-item">
                <span class="stat-label">쿨타임</span>
                <span class="stat-value">${skill.cooldownSeconds}초</span>
            </div>
            ` : ''}
            ${skill.effectType && skill.effectValue ? `
            <div class="stat-item">
                <span class="stat-label">효과</span>
                <span class="stat-value">${getEffectDescription(skill.effectType, skill.effectValue)}</span>
            </div>
            ` : ''}
        </div>
        ${learningProgressHtml}
        ${masteryHtml}
        ${!isLearned ? `
            <button class="learn-btn" onclick="learnSkill(${skill.id})" 
                    ${learnedSkillCount >= 4 ? 'disabled title="기술 슬롯이 가득 찼습니다. 기존 기술을 잊어야 합니다."' : ''}
                    ${isLearningInProgress ? 'disabled title="공부중인 기술이 있습니다."' : ''}>
                기술 배우기
            </button>
        ` : `
            ${spiritSkill && spiritSkill.isLearning ? `
                <button class="cancel-learning-btn" onclick="cancelLearning(${skill.id}, ${spiritSkill.spiritSkillId})" 
                        style="background: #FF6347; color: white; padding: 8px 16px; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; margin-top: 10px;">
                    학습 취소
                </button>
            ` : `
                <button class="forget-btn" onclick="forgetSkill(${skill.id}, ${spiritSkill.spiritSkillId})">
                    기술 잊기
                </button>
            `}
        `}
    `;
    
    return card;
}

/**
 * 효과 설명 가져오기
 */
function getEffectDescription(effectType, effectValue) {
    if (effectType === 'BUFF_MELEE_ATTACK') {
        return `물리공격 +${effectValue}`;
    } else if (effectType === 'BUFF_RANGED_ATTACK') {
        return `원거리공격 +${effectValue}`;
    } else if (effectType === 'BUFF_DEFENSE') {
        return `방어력 +${effectValue}`;
    } else if (effectType === 'BUFF_SPEED') {
        return `스피드 +${effectValue}`;
    } else if (effectType === 'BUFF_ALL') {
        return `전능력치 +${effectValue}`;
    } else if (effectType === 'DEBUFF_ATTACK') {
        return `상대 공격 -${effectValue}`;
    } else if (effectType === 'DEBUFF_DEFENSE') {
        return `상대 방어 -${effectValue}`;
    } else if (effectType === 'DEBUFF_SPEED') {
        return `상대 스피드 -${effectValue}`;
    } else if (effectType === 'DEBUFF_ALL') {
        return `상대 전능력치 -${effectValue}`;
    } else if (effectType === 'HEAL') {
        return `체력 회복 +${effectValue}`;
    }
    return `${effectType} ${effectValue > 0 ? '+' : ''}${effectValue}`;
}

/**
 * 기술 배우기
 */
async function learnSkill(skillId) {
    if (!selectedSpiritId) {
        showWarning('정령을 선택해주세요.');
        return;
    }
    
    // 학습 중인 기술이 있는지 확인
    const learnedResponse = await fetch(`/magic-academy/api/learned-skills/${selectedSpiritId}`, {
        credentials: 'include'
    });
    
    if (learnedResponse.ok) {
        const learnedSkills = await learnedResponse.json();
        const isLearning = learnedSkills.some(ss => ss.isLearning);
        if (isLearning) {
            showWarning('공부중인 기술이 있습니다.');
            return;
        }
    }
    
    const confirmed = await showConfirm('이 기술을 배우시겠습니까?', '기술 학습');
    if (!confirmed) {
        return;
    }
    
    try {
        const response = await fetch('/magic-academy/api/learn-skill', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${selectedSpiritId}&skillId=${skillId}`
        });
        
        const result = await response.json();
        
        if (result.success) {
            showSuccess('기술 학습을 시작했습니다! ' + (result.learnTimeMinutes || 30) + '분 후 완료됩니다.');
            
            // API 응답의 카운트로 먼저 설정 (학습 중인 기술은 카운트에 포함되지 않으므로 동일)
            if (result.learnedSkillCount !== undefined) {
                learnedSkillCount = result.learnedSkillCount;
                updateSkillSlotDisplay();
            }
            
            // 기술 목록 새로고침 (카운트는 이미 설정됨)
            await loadSpiritSkills(selectedSpiritId);
        } else {
            showError('기술 학습 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error learning skill:', error);
        showError('기술 학습 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 학습 취소
 */
async function cancelLearning(skillId, spiritSkillId) {
    if (!selectedSpiritId) {
        showWarning('정령을 선택해주세요.');
        return;
    }
    
    const confirmed = await showConfirm('기술 학습을 취소하시겠습니까? 학습 시간은 초기화됩니다.', '학습 취소');
    if (!confirmed) {
        return;
    }
    
    try {
        const response = await fetch('/magic-academy/api/cancel-learning', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${selectedSpiritId}&skillId=${skillId}`
        });
        
        const result = await response.json();
        
        if (result.success) {
            showSuccess('기술 학습을 취소했습니다.');
            // 응답에서 받은 개수로 먼저 업데이트
            if (result.learnedSkillCount !== undefined) {
                learnedSkillCount = result.learnedSkillCount;
                updateSkillSlotDisplay();
            }
            // 기술 목록 새로고침
            await loadSpiritSkills(selectedSpiritId);
        } else {
            showError('학습 취소 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error canceling learning:', error);
        showError('학습 취소 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 모든 섹션 숨기기
 */
function hideAllSections() {
    document.getElementById('learnedSkillsSection').style.display = 'none';
    document.getElementById('learnableSkillsSection').style.display = 'none';
    document.getElementById('emptyMessage').style.display = 'block';
}

/**
 * 기술 잊기
 */
async function forgetSkill(skillId, spiritSkillId) {
    if (!selectedSpiritId) {
        showWarning('정령을 선택해주세요.');
        return;
    }
    
    const confirmed = await showConfirm('이 기술을 잊으시겠습니까? 다시 배우려면 학습 시간이 필요합니다.', '기술 잊기');
    if (!confirmed) {
        return;
    }
    
    try {
        const response = await fetch('/magic-academy/api/forget-skill', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: `spiritId=${selectedSpiritId}&skillId=${skillId}`
        });
        
        const result = await response.json();
        
        if (result.success) {
            showSuccess('기술을 잊었습니다.');
            // 응답에서 받은 개수로 먼저 업데이트
            if (result.learnedSkillCount !== undefined) {
                learnedSkillCount = result.learnedSkillCount;
                updateSkillSlotDisplay();
            }
            // 기술 목록 새로고침
            await loadSpiritSkills(selectedSpiritId);
        } else {
            showError('기술 잊기 실패: ' + result.message);
        }
    } catch (error) {
        console.error('Error forgetting skill:', error);
        showError('기술 잊기 중 오류가 발생했습니다: ' + error.message);
    }
}

/**
 * 배운 기술 개수 로드
 */
async function loadLearnedSkillCount(spiritId) {
    try {
        // 배운 기술 목록을 가져와서 학습 완료된 기술만 카운트
        const response = await fetch(`/magic-academy/api/learned-skills/${spiritId}`, {
            credentials: 'include'
        });
        
        if (response.ok) {
            const learnedSkills = await response.json();
            
            // 학습 완료 시간이 지난 기술을 즉시 완료 상태로 변경
            const now = new Date();
            const updatedSkills = learnedSkills.map(ss => {
                // 학습 완료 시간이 있고, learnedAt이 없는 경우 처리
                if (ss.learningCompletionTime) {
                    const completionTime = new Date(ss.learningCompletionTime);
                    const isPast = now >= completionTime;
                    
                    // 학습 완료 시간이 지났고, learnedAt이 없는 경우
                    if (isPast && (ss.learnedAt == null || ss.learnedAt === '')) {
                        // 프론트엔드에서 즉시 완료 상태로 변경
                        return {
                            ...ss,
                            isLearning: false,
                            learnedAt: now.toISOString()
                        };
                    }
                }
                return ss;
            });
            
            // 학습 완료된 기술만 카운트 (displayLearnedSkills와 동일한 로직)
            const completedSkills = updatedSkills.filter(ss => {
                const isLearning = ss.isLearning === true;
                const hasLearnedAt = ss.learnedAt != null && ss.learnedAt !== '';
                return !isLearning && hasLearnedAt;
            });
            learnedSkillCount = completedSkills.length;
            updateSkillSlotDisplay();
        } else {
            console.error('Failed to load learned skills for count:', response.status);
        }
    } catch (error) {
        console.error('Error loading learned skill count:', error);
    }
}

/**
 * 기술 슬롯 표시 업데이트
 */
function updateSkillSlotDisplay() {
    const slotDisplay = document.getElementById('skillSlotDisplay');
    if (slotDisplay) {
        slotDisplay.textContent = `기술 슬롯: ${learnedSkillCount}/4`;
        if (learnedSkillCount >= 4) {
            slotDisplay.style.color = '#FF6347';
            slotDisplay.title = '기술 슬롯이 가득 찼습니다. 기존 기술을 잊어야 새로운 기술을 배울 수 있습니다.';
        } else {
            slotDisplay.style.color = '#A8E6CF';
            slotDisplay.title = '';
        }
    }
}

/**
 * 학습 타이머 시작
 */
function startLearningTimer(spiritSkill) {
    if (!spiritSkill.isLearning || !spiritSkill.learningCompletionTime) {
        return;
    }
    
    const timerElement = document.getElementById(`learning-timer-${spiritSkill.spiritSkillId}`);
    if (!timerElement) {
        return;
    }
    
    // 기존 타이머가 있으면 정리
    const timerId = `learning-timer-${spiritSkill.spiritSkillId}`;
    if (window[`timer_${timerId}`]) {
        clearTimeout(window[`timer_${timerId}`]);
    }
    
    function updateTimer() {
        const completionTime = new Date(spiritSkill.learningCompletionTime);
        const now = new Date();
        const diff = completionTime - now;
        
        if (diff <= 0) {
            timerElement.textContent = '학습 완료!';
            timerElement.style.color = '#4CAF50';
            
            // 학습 완료 즉시 처리 (백엔드 동기화)
            if (selectedSpiritId) {
                // 백엔드에 학습 완료 처리 요청
                fetch(`/magic-academy/api/check-learning-completion/${selectedSpiritId}`, {
                    method: 'POST',
                    credentials: 'include'
                }).then(() => {
                    // 모든 정보 즉시 새로고침
                    return Promise.all([
                        loadSpiritSkills(selectedSpiritId),
                        loadLearnedSkillCount(selectedSpiritId)
                    ]);
                }).then(() => {
                    // 배울 수 있는 기술 목록도 새로고침
                    return fetch(`/magic-academy/api/learnable-skills/${selectedSpiritId}`, {
                        credentials: 'include'
                    });
                }).then(response => {
                    if (response.ok) {
                        return response.json();
                    }
                }).then(response => {
                    if (response) {
                        const learnableSkills = Array.isArray(response.skills) ? response.skills : (Array.isArray(response) ? response : []);
                        const isLearning = response.isLearning || false;
                        displayLearnableSkills(learnableSkills, isLearning);
                    }
                }).catch(error => {
                    console.error('학습 완료 후 새로고침 중 오류:', error);
                });
            }
            
            // 타이머 정리
            if (window[`timer_${timerId}`]) {
                clearTimeout(window[`timer_${timerId}`]);
                delete window[`timer_${timerId}`];
            }
            return;
        }
        
        const minutes = Math.floor(diff / 60000);
        const seconds = Math.floor((diff % 60000) / 1000);
        timerElement.textContent = `${minutes}분 ${seconds}초 남음`;
        
        // 다음 업데이트까지 남은 시간 계산 (최대 1초)
        const nextUpdate = Math.min(1000, diff);
        window[`timer_${timerId}`] = setTimeout(updateTimer, nextUpdate);
    }
    
    // 즉시 시작하고, 남은 시간에 맞춰 다음 업데이트 스케줄링
    updateTimer();
}

