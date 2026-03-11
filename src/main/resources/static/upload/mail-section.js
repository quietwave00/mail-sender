// Excel 파일 선택 시 라벨 업데이트
document.getElementById('fileInput').addEventListener('change', function(e) {
    const fileName = e.target.files[0]?.name;
    const label = this.nextElementSibling.querySelector('.file-upload-text');
    if (fileName) {
        label.innerHTML = `선택된 파일: <strong>${fileName}</strong><br><small>다른 파일을 선택하려면 클릭하세요</small>`;
    } else {
        label.innerHTML = `Excel 파일을 선택<br><small>(.xls, .xlsx 파일만 지원)</small>`;
    }
    const result = document.getElementById('result');
    result.className = 'hide';
});

// 개별발송 input값 검증
const validateSeparateSendInputs = () => {
    const isSeparateSendEnabled = document.getElementById('separateSendEnabled')?.checked;
    if (!isSeparateSendEnabled) {
        return { valid: true };
    }

    const fromInput = document.getElementById('from');
    const toInput = document.getElementById('to');
    const fromValue = fromInput?.value?.trim() || '';
    const toValue = toInput?.value?.trim() || '';

    if (!fromValue || !toValue) {
        return { valid: false, message: '⚠️ 개별발송 시작/종료 값 모두 입력하세요! 모두요!' };
    }

    const numberPattern = /^\d+$/;
    if (!numberPattern.test(fromValue) || !numberPattern.test(toValue)) {
        return { valid: false, message: '⚠️ 개별발송 숫자를 입력해 주세요!!' };
    }

    if (Number(toValue) < Number(fromValue)) {
        return { valid: false, message: '⚠️ 개별발송 종료 값이 시작 값보다 클 수 없습니다...'};
    }

    return { valid: true };
};

// 메일 발송 폼 제출 처리
document.getElementById('uploadForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const fileInput = document.getElementById('fileInput');
    const result = document.getElementById('result');
    const progressWrapper = document.querySelector('.progress-wrapper');

    const separateSendValidation = validateSeparateSendInputs();
    if (!separateSendValidation.valid) {
        result.innerHTML = separateSendValidation.message;
        result.className = 'show';
        return;
    }

    if (!fileInput.files.length) {
       result.innerHTML = ('⚠️ Excel 파일을 선택하세요.');
       result.className = 'show';
        return;
    }
    result.innerHTML = '🚀 메일 발송을 처리중...';
    result.className = 'show';

    try {
        const previewData = await getPreviewData(fileInput);
        preview(JSON.stringify(previewData));
        progressWrapper.style.display = 'block';
        startProgressTracking(previewData.totalCount);

        const formData = new FormData();
        formData.append('file', fileInput.files[0]);
        formData.append('nameColumn', document.getElementById('nameColumn').value);
        formData.append('emailColumn', document.getElementById('emailColumn').value);
        formData.append('ticketColumn', document.getElementById('ticketColumn').value);
        if (document.getElementById('separateSendEnabled')?.checked) {
            formData.append('fromValue', document.getElementById('from').value.trim());
            formData.append('toValue', document.getElementById('to').value.trim());
        }

        const response = await fetch(`${server_host}/api/mail`, {
            method: 'POST',
            body: formData,
            credentials: "include"
        });

        const responseText = await response.text();
        if (response.ok) {
            result.innerHTML = `✅ ${responseText}`;
        } else {
            if(response.status == 401) {
                if(confirm('로그인 먼저 해주세요!!')) {
                    window.location.href = `${server_host}`;
                }
            }

            console.log(`❌ 오류가 발생했습니다: ${responseText}`);
        }
    } catch (error) {
        console.log(`❌ 네트워크 오류가 발생했습니다: ${error.message}`);
        console.error('Upload error:', error);
    }
});

// 메일 발송 대상 미리보기
const previewMail = async () => {
    const fileInput = document.getElementById('fileInput');
    const result = document.getElementById('result');

    const separateSendValidation = validateSeparateSendInputs();
    if (!separateSendValidation.valid) {
        result.innerHTML = separateSendValidation.message;
        result.className = 'show';
        return;
    }

    if (!fileInput.files.length) {
        result.innerHTML = ('⚠️ Excel 파일을 선택하세요.');
        result.className = 'show';
        return;
    }

    const modal = document.getElementById('previewModal');
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';

    try {
        const previewData = await getPreviewData(fileInput);
        preview(JSON.stringify(previewData));
    } catch (error) {
        alert(`미리보기를 불러올 수 없습니다: ${error.message}`);
    }
}

const closePreviewModal = () =>  {
    const modal = document.getElementById('previewModal');
    modal.classList.remove('active');
    document.body.style.overflow = 'auto'; // 배경 스크롤 복원
}

const closeModalOnOverlay = (event) => {
    if (!event) return;

    if (event.target === event.currentTarget) {
        closePreviewModal();
    }
}

document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closePreviewModal();
    }
});

// 미리보기
const preview = (response) => {
    try {
        if(response.status == 401) {
            if(confirm('로그인 먼저 해주세요!!')) {
                window.location.href = `${server_host}`;
            }
        }

        const data = JSON.parse(response);
        const container = document.getElementById('modalContent');
        const totalCountDisplay = document.getElementById('totalCountDisplay');

        totalCountDisplay.textContent = `총 ${data.totalCount}명에게 발송 예정`;

        // 파일 정보 HTML 생성
        let fileInfoHTML = '';
        if (data.templateFileName) {
            const fileName = data.templateFileName;
            const fileExtension = fileName.toLowerCase().substring(fileName.lastIndexOf('.') + 1);

            // 파일 타입별 아이콘
            let fileIcon = '📄';
            if (['jpg', 'jpeg', 'png'].includes(fileExtension)) {
                fileIcon = '🖼️';
            } else if (fileExtension === 'pdf') {
                fileIcon = '📕';
            }

            fileInfoHTML = `
                <div class="template-item">
                    <label class="template-label">📎 첨부파일:</label>
                    <div class="template-value" style="text-align: left !important; display: flex; align-items: center; gap: 8px;">
                        <span>${fileIcon}</span>
                        <strong>${escapeHtml(fileName)}</strong>
                    </div>
                </div>
            `;
        } else {
            fileInfoHTML = `
                <div class="template-item">
                    <label class="template-label">📎 첨부파일:</label>
                    <div class="template-value" style="color: #6b7280;">첨부파일 없음</div>
                </div>
            `;
        }

        container.innerHTML = `
                    <!-- 템플릿 정보 섹션 -->
                    <div class="template-section">
                        <div class="template-toggle">
                            <div class="toggle-header" onclick="toggleSection('template-content')">
                                <div class="toggle-title">
                                    <span class="icon">🎨</span>
                                    템플릿 정보
                                </div>
                                <div class="toggle-icon" id="template-icon">+</div>
                            </div>
                            <div class="toggle-content" id="template-content">
                                <div class="template-item">
                                    <label class="template-label">📝 메일 제목:</label>
                                    <div class="template-value">${escapeHtml(data.templateSubject)}</div>
                                </div>
                                <div class="template-item">
                                    <label class="template-label">📝 메일 내용:</label>
                                    <div class="html-preview">${data.templateBody}</div>
                                </div>
                                ${fileInfoHTML}
                            </div>
                        </div>
                    </div>
                    
                    <!-- 수신자 목록 섹션 -->
                    <div class="template-section">
                        <h2 class="section-title">
                            <span class="icon">👥</span>
                            수신자 목록
                        </h2>
                        
                        <div class="table-container">
                            <!-- 데스크톱용 테이블 -->
                            <table class="table">
                                <thead>
                                    <tr>
                                        <th>이메일</th>
                                        <th>이름</th>
                                        <th>예매번호</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${data.previews.map((preview, index) => `
                                        <tr>
                                            <td class="email-cell">${escapeHtml(preview.email)}</td>
                                            <td class="name-cell">${escapeHtml(preview.name)}</td>
                                            <td><span class="ticket-cell">${escapeHtml(preview.ticketNumbers)}</span></td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                            
                            <!-- 모바일용 카드 레이아웃 -->
                            <div class="mobile-cards">
                                ${data.previews.map((preview, index) => `
                                    <div class="mobile-card">
                                        <div class="card-row">
                                            <span class="card-label">이름</span>
                                            <span class="card-value card-name">${escapeHtml(preview.name)}</span>
                                        </div>
                                        <div class="card-row">
                                            <span class="card-label">이메일</span>
                                            <span class="card-value card-email">${escapeHtml(preview.email)}</span>
                                        </div>
                                        <div class="card-row">
                                            <span class="card-label">예매번호</span>
                                            <span class="card-value">
                                                <span class="card-tickets">${escapeHtml(preview.ticketNumbers)}</span>
                                            </span>
                                        </div>
                                    </div>
                                `).join('')}
                            </div>
                        </div>
                    </div>
                `;
    } catch (error) {
        console.error('미리보기 데이터 파싱 오류:', error);
        const container = document.getElementById('modalContent');
        container.innerHTML = `
                    <div style="text-align: center; padding: 40px; color: #ef4444;">
                        <h3>⚠️ 오류 발생</h3>
                        <p style="margin-top: 10px;">미리보기 데이터를 불러오는 중 오류가 발생했습니다.</p>
                        <pre style="background: #f3f4f6; padding: 20px; margin-top: 20px; border-radius: 8px; text-align: left; overflow-x: auto;">${escapeHtml(response)}</pre>
                    </div>
                `;
    }
};

const toggleSection = (contentId) => {
    const content = document.getElementById(contentId);
    const icon = document.getElementById(contentId.replace('-content', '-icon'));

    if (content.classList.contains('active')) {
        content.classList.remove('active');
        icon.textContent = '+';
        icon.classList.remove('rotate');
    } else {
        content.classList.add('active');
        icon.textContent = '−';
        icon.classList.add('rotate');
    }
}

const escapeHtml = (text) => {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 미리보기 API 호출 함수 (내부용)
const getPreviewData = async (fileInput) => {
    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('nameColumn', document.getElementById('nameColumn').value);
    formData.append('emailColumn', document.getElementById('emailColumn').value);
    formData.append('ticketColumn', document.getElementById('ticketColumn').value);
    if (document.getElementById('separateSendEnabled')?.checked) {
        formData.append('fromValue', document.getElementById('from').value.trim());
        formData.append('toValue', document.getElementById('to').value.trim());
    }

    const response = await fetch(`${server_host}/api/mail/preview`, {
        method: 'POST',
        body: formData,
        credentials: "include"
    });

    if (response.ok) {
        const responseText = await response.text();
        const previewData = JSON.parse(responseText);
        const totalCount = previewData.totalCount;
        return previewData;
    } else {
        if (response.status == 401) {
            if (confirm('로그인 먼저 해주세요!!')) {
                window.location.href = `${server_host}`;
            }
        }
        throw new Error(response.statusText);
    }
};

// progress
let progressInterval = null;
function startProgressTracking(totalCount) {
    if (progressInterval) {
        clearInterval(progressInterval);
    }

    progressInterval = setInterval(async () => {
        try {
            const response = await fetch(`${server_host}/api/mail/progress`, {
                method: 'GET',
                credentials: 'include'
            });
            const progress = await response.text();
            console.log("progress: " + progress);

            document.getElementById("progressDisplay").innerText = `${progress} / ${totalCount}`;

            // 진행률 바 업데이트
            const sentCount = parseInt(progress.trim());
            if (!isNaN(sentCount) && totalCount > 0) {
                const percentage = (sentCount / totalCount) * 100;
                document.getElementById("progressBar").style.width = `${percentage}%`;
            }

            if(progress == totalCount) stopProgressTracking();
        } catch (e) {
            console.error("Progress 조회 실패:", e);
        }
    }, 1000);


}

function stopProgressTracking() {
    if (progressInterval) {
        clearInterval(progressInterval);
        progressInterval = null;
    }
}
