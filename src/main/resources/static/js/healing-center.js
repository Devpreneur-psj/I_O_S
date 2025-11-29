// 정령 병원 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    const feedbackForm = document.getElementById('feedbackForm');
    const contentTextarea = document.getElementById('content');
    const charCount = document.getElementById('charCount');
    
    // 글자 수 카운트
    if (contentTextarea && charCount) {
        contentTextarea.addEventListener('input', function() {
            charCount.textContent = this.value.length;
        });
    }
    
    // 폼 제출
    if (feedbackForm) {
        feedbackForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const formData = new FormData(feedbackForm);
            const category = formData.get('category');
            const title = formData.get('title');
            const content = formData.get('content');
            const email = formData.get('email');
            
            // 유효성 검사
            if (!category || !title || !content) {
                alert('모든 필수 항목을 입력해주세요.');
                return;
            }
            
            try {
                const response = await fetch('/healing-center/submit-feedback', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    credentials: 'include',
                    body: new URLSearchParams({
                        category: category,
                        title: title,
                        content: content,
                        email: email || ''
                    })
                });
                
                const result = await response.json();
                
                if (result.success) {
                    alert(result.message);
                    feedbackForm.reset();
                    charCount.textContent = '0';
                } else {
                    alert(result.message || '피드백 제출 중 오류가 발생했습니다.');
                }
            } catch (error) {
                console.error('피드백 제출 오류:', error);
                alert('피드백 제출 중 오류가 발생했습니다. 다시 시도해주세요.');
            }
        });
    }
});

