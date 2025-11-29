// 사서의 탑 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    const settingsForm = document.getElementById('settingsForm');
    const soundVolumeSlider = document.getElementById('soundVolume');
    const musicVolumeSlider = document.getElementById('musicVolume');
    const soundVolumeValue = document.getElementById('soundVolumeValue');
    const musicVolumeValue = document.getElementById('musicVolumeValue');
    
    // 볼륨 슬라이더 값 업데이트
    if (soundVolumeSlider && soundVolumeValue) {
        soundVolumeSlider.addEventListener('input', function() {
            soundVolumeValue.textContent = this.value;
        });
    }
    
    if (musicVolumeSlider && musicVolumeValue) {
        musicVolumeSlider.addEventListener('input', function() {
            musicVolumeValue.textContent = this.value;
        });
    }
    
    // 폼 제출
    if (settingsForm) {
        settingsForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const formData = new FormData(settingsForm);
            const soundEnabled = formData.get('soundEnabled') === 'on';
            const musicEnabled = formData.get('musicEnabled') === 'on';
            const soundVolume = formData.get('soundVolume') || 70;
            const musicVolume = formData.get('musicVolume') || 50;
            const notificationsEnabled = formData.get('notificationsEnabled') === 'on';
            const autoSaveEnabled = formData.get('autoSaveEnabled') === 'on';
            
            try {
                const response = await fetch('/tower-settings/save-settings', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    credentials: 'include',
                    body: new URLSearchParams({
                        soundEnabled: soundEnabled,
                        musicEnabled: musicEnabled,
                        soundVolume: soundVolume,
                        musicVolume: musicVolume,
                        notificationsEnabled: notificationsEnabled,
                        autoSaveEnabled: autoSaveEnabled
                    })
                });
                
                const result = await response.json();
                
                if (result.success) {
                    alert(result.message);
                    // 실제로는 localStorage나 서버에서 설정을 불러와서 적용
                    // localStorage.setItem('gameSettings', JSON.stringify({...}));
                } else {
                    alert(result.message || '설정 저장 중 오류가 발생했습니다.');
                }
            } catch (error) {
                console.error('설정 저장 오류:', error);
                alert('설정 저장 중 오류가 발생했습니다. 다시 시도해주세요.');
            }
        });
    }
});

