const COLUMN_DEFAULTS = {
    nameColumn: 2,
    emailColumn: 1,
    ticketColumn: 3
};

const COLUMN_MAX = 26;
const INPUT_SOURCE = {
    EXCEL: 'excel',
    SPREADSHEET: 'spreadsheet'
};
let currentPreviewSource = INPUT_SOURCE.EXCEL;
let currentSpreadsheetRequestPayload = null;
let currentSpreadsheetSnapshotId = null;
let selectedSpreadsheetRowIds = [];
let currentSpreadsheetPreviewRowIds = [];
let hasSpreadsheetSelectionState = false;
let isApplyingSavedColumnMapping = false;
let isApplyingSavedSpreadsheetUrl = false;
let spreadsheetUrlSaveTimeoutId = null;
let hasSpreadsheetUrlUserInteracted = false;

const populateColumnOptions = () => {
    ['nameColumn', 'emailColumn', 'ticketColumn'].forEach((selectId) => {
        const select = document.getElementById(selectId);
        const defaultValue = COLUMN_DEFAULTS[selectId];

        select.innerHTML = '';
        for (let index = 0; index < COLUMN_MAX; index += 1) {
            const option = document.createElement('option');
            option.value = String(index);
            option.textContent = `${String.fromCharCode(65 + index)}열`;
            option.selected = index === defaultValue;
            select.appendChild(option);
        }
    });
};

const getCurrentColumnMapping = () => ({
    nameColumn: Number(document.getElementById('nameColumn').value),
    emailColumn: Number(document.getElementById('emailColumn').value),
    ticketColumn: Number(document.getElementById('ticketColumn').value)
});

const applyColumnMapping = (columnMapping) => {
    if (!columnMapping) {
        return;
    }

    isApplyingSavedColumnMapping = true;
    document.getElementById('nameColumn').value = String(columnMapping.nameColumn);
    document.getElementById('emailColumn').value = String(columnMapping.emailColumn);
    document.getElementById('ticketColumn').value = String(columnMapping.ticketColumn);
    isApplyingSavedColumnMapping = false;
};

const saveColumnMapping = async () => {
    const response = await fetch(`${server_host}/api/column-mapping`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(getCurrentColumnMapping())
    });

    if (!response.ok) {
        const responseText = await response.text();
        throw new Error(responseText || '열 매핑 저장에 실패했습니다.');
    }
};

const persistColumnMappingSafely = async () => {
    try {
        await saveColumnMapping();
    } catch (error) {
        console.error('열 매핑 저장 오류:', error);
    }
};

const loadSavedColumnMapping = async () => {
    const response = await fetch(`${server_host}/api/column-mapping`, {
        method: 'GET',
        credentials: 'include'
    });

    if (response.status === 204) {
        return;
    }

    if (!response.ok) {
        const responseText = await response.text();
        throw new Error(responseText || '열 매핑을 불러오지 못했습니다.');
    }

    applyColumnMapping(await response.json());
};

const initializeColumnMappingPersistence = () => {
    ['nameColumn', 'emailColumn', 'ticketColumn'].forEach((selectId) => {
        document.getElementById(selectId).addEventListener('change', async () => {
            if (isApplyingSavedColumnMapping) {
                return;
            }

            await persistColumnMappingSafely();
        });
    });
};

const getCurrentSpreadsheetUrl = () =>
    document.getElementById('spreadsheetUrl')?.value ?? '';

const applySpreadsheetUrl = (spreadsheetUrl) => {
    const input = document.getElementById('spreadsheetUrl');
    if (!input || typeof spreadsheetUrl !== 'string') {
        return;
    }

    isApplyingSavedSpreadsheetUrl = true;
    input.value = spreadsheetUrl;
    isApplyingSavedSpreadsheetUrl = false;
};

const saveSpreadsheetUrl = async () => {
    const response = await fetch(`${server_host}/api/spreadsheet-url`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
            spreadsheetUrl: getCurrentSpreadsheetUrl()
        })
    });

    if (!response.ok) {
        const responseText = await response.text();
        throw new Error(responseText || '스프레드시트 URL 저장에 실패했습니다.');
    }
};

const persistSpreadsheetUrlSafely = async () => {
    try {
        await saveSpreadsheetUrl();
    } catch (error) {
        console.error('스프레드시트 URL 저장 오류:', error);
    }
};

const loadSavedSpreadsheetUrl = async () => {
    const response = await fetch(`${server_host}/api/spreadsheet-url`, {
        method: 'GET',
        credentials: 'include'
    });

    if (response.status === 204) {
        return;
    }

    if (!response.ok) {
        const responseText = await response.text();
        throw new Error(responseText || '스프레드시트 URL을 불러오지 못했습니다.');
    }

    const data = await response.json();
    const currentValue = getCurrentSpreadsheetUrl().trim();
    if (hasSpreadsheetUrlUserInteracted || currentValue) {
        return;
    }
    applySpreadsheetUrl(data.spreadsheetUrl || '');
};

const initializeSpreadsheetUrlPersistence = () => {
    const input = document.getElementById('spreadsheetUrl');
    if (!input) {
        return;
    }

    input.addEventListener('input', () => {
        const result = document.getElementById('result');
        result.className = 'hide';
        hasSpreadsheetUrlUserInteracted = true;

        if (isApplyingSavedSpreadsheetUrl) {
            return;
        }

        if (spreadsheetUrlSaveTimeoutId) {
            clearTimeout(spreadsheetUrlSaveTimeoutId);
        }

        spreadsheetUrlSaveTimeoutId = setTimeout(() => {
            persistSpreadsheetUrlSafely();
        }, 300);
    });
};

const setActiveSource = (source) => {
    document.querySelectorAll('[data-source-option]').forEach((option) => {
        option.classList.toggle('active', option.dataset.sourceOption === source);
    });

    document.querySelectorAll('[data-source-panel]').forEach((panel) => {
        panel.classList.toggle('active', panel.dataset.sourcePanel === source);
    });

    const fileInput = document.getElementById('fileInput');
    if (fileInput) {
        fileInput.required = source === INPUT_SOURCE.EXCEL;
    }

    updateSheetSelectionSummary();
    document.getElementById('result').className = 'hide';
};

const getSelectedSource = () =>
    document.querySelector('input[name="inputSource"]:checked')?.value || INPUT_SOURCE.EXCEL;

populateColumnOptions();
initializeColumnMappingPersistence();
initializeSpreadsheetUrlPersistence();

document.querySelectorAll('input[name="inputSource"]').forEach((input) => {
    input.addEventListener('change', function() {
        setActiveSource(this.value);
    });
});

setActiveSource(getSelectedSource());
loadSavedColumnMapping().catch((error) => {
    console.error('저장된 열 매핑 불러오기 오류:', error);
});
loadSavedSpreadsheetUrl().catch((error) => {
    console.error('저장된 스프레드시트 URL 불러오기 오류:', error);
});

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

    if (getSelectedSource() === INPUT_SOURCE.SPREADSHEET) {
        await sendSelectedSpreadsheetMails();
        return;
    }

    if (getSelectedSource() !== INPUT_SOURCE.EXCEL) {
        return;
    }

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
        await persistColumnMappingSafely();
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
    if (getSelectedSource() !== INPUT_SOURCE.EXCEL) {
        return;
    }

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
        await persistColumnMappingSafely();
        currentPreviewSource = INPUT_SOURCE.EXCEL;
        currentSpreadsheetRequestPayload = null;
        currentSpreadsheetSnapshotId = null;
        const previewData = await getPreviewData(fileInput);
        preview(JSON.stringify(previewData));
    } catch (error) {
        alert(`미리보기를 불러올 수 없습니다: ${error.message}`);
    }
}

const previewSpreadsheetMail = async () => {
    if (getSelectedSource() !== INPUT_SOURCE.SPREADSHEET) {
        return;
    }

    const result = document.getElementById('result');
    const spreadsheetUrl = document.getElementById('spreadsheetUrl')?.value?.trim() || '';

    if (!spreadsheetUrl) {
        result.innerHTML = '⚠️ 스프레드시트 URL을 입력하세요.';
        result.className = 'show';
        return;
    }

    try {
        await persistColumnMappingSafely();
        currentPreviewSource = INPUT_SOURCE.SPREADSHEET;
        currentSpreadsheetRequestPayload = createSpreadsheetRequestPayload(spreadsheetUrl);
        const previewData = await getSpreadsheetPreviewData(spreadsheetUrl);
        const modal = document.getElementById('previewModal');
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
        currentSpreadsheetSnapshotId = previewData.snapshotId || null;
        currentSpreadsheetPreviewRowIds = previewData.previews
            .map((previewItem) => Number(previewItem.rowId))
            .filter((rowId) => !Number.isNaN(rowId));

        if (!hasSpreadsheetSelectionState) {
            selectedSpreadsheetRowIds = [];
            hasSpreadsheetSelectionState = true;
        } else {
            selectedSpreadsheetRowIds = selectedSpreadsheetRowIds
                .filter((rowId) => currentSpreadsheetPreviewRowIds.includes(rowId));
        }

        updateSheetSelectionSummary();
        preview(JSON.stringify(previewData));
    } catch (error) {
        closePreviewModal();
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
        const isSpreadsheetPreview = currentPreviewSource === INPUT_SOURCE.SPREADSHEET;

        totalCountDisplay.textContent = isSpreadsheetPreview
            ? `총 ${selectedSpreadsheetRowIds.length}명에게 발송 예정`
            : `총 ${data.totalCount}명에게 발송 예정`;

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

        const spreadsheetSelectionHTML = isSpreadsheetPreview ? `
                    <div class="selection-toolbar">
                        <label class="selection-check">
                            <input type="checkbox" id="selectAllRecipients" onchange="toggleAllSpreadsheetRecipients(this.checked)">
                            <span>전체 선택</span>
                        </label>
                        <div class="selection-actions">
                            <div class="selection-range">
                                <input type="number" id="rangeStartRow" min="1" placeholder="시작 행">
                                <span>~</span>
                                <input type="number" id="rangeEndRow" min="1" placeholder="끝 행">
                                <button type="button" class="range-action-button" onclick="applySpreadsheetRangeSelection(true)">범위 선택</button>
                                <button type="button" class="range-action-button secondary" onclick="applySpreadsheetRangeSelection(false)">범위 해제</button>
                            </div>
                            <span id="selectedRecipientCount" class="selection-count">0명 선택</span>
                        </div>
                    </div>
        ` : '';

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
                        ${spreadsheetSelectionHTML}
                        
                        <div class="table-container">
                            <!-- 데스크톱용 테이블 -->
                            <table class="table">
                                <thead>
                                    <tr>
                                        ${isSpreadsheetPreview ? '<th class="checkbox-column">선택</th>' : ''}
                                        ${isSpreadsheetPreview ? '<th class="row-number-column">번호</th>' : ''}
                                        <th>이메일</th>
                                        <th>이름</th>
                                        <th>예매번호</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${data.previews.map((preview, index) => `
                                        <tr>
                                            ${isSpreadsheetPreview ? `
                                                <td class="checkbox-column">
                                                    <input
                                                        type="checkbox"
                                                        class="recipient-checkbox"
                                                        data-row-id="${preview.rowId ?? ''}"
                                                        ${selectedSpreadsheetRowIds.includes(Number(preview.rowId)) ? 'checked' : ''}
                                                        onchange="updateSpreadsheetSelectionState()">
                                                </td>
                                            ` : ''}
                                            ${isSpreadsheetPreview ? `<td class="row-number-cell">${escapeHtml(String(preview.rowId ?? ''))}</td>` : ''}
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
                                        ${isSpreadsheetPreview ? `
                                            <label class="mobile-card-check">
                                                <input
                                                    type="checkbox"
                                                    class="recipient-checkbox"
                                                    data-row-id="${preview.rowId ?? ''}"
                                                    ${selectedSpreadsheetRowIds.includes(Number(preview.rowId)) ? 'checked' : ''}
                                                    onchange="updateSpreadsheetSelectionState()">
                                                <span>선택</span>
                                            </label>
                                        ` : ''}
                                        ${isSpreadsheetPreview ? `
                                            <div class="card-row">
                                                <span class="card-label">번호</span>
                                                <span class="card-value card-row-number">${escapeHtml(String(preview.rowId ?? ''))}</span>
                                            </div>
                                        ` : ''}
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

        if (isSpreadsheetPreview) {
            updateSpreadsheetSelectionState();
        }
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

const createSpreadsheetRequestPayload = (spreadsheetUrl) => ({
    spreadsheetUrl,
    columnMapping: getCurrentColumnMapping()
});

const handlePreviewAction = async () => {
    if (getSelectedSource() === INPUT_SOURCE.SPREADSHEET) {
        await previewSpreadsheetMail();
        return;
    }

    await previewMail();
};

window.handlePreviewAction = handlePreviewAction;

const getUniqueRecipientCheckboxes = () => {
    const seen = new Set();
    return Array.from(document.querySelectorAll('.recipient-checkbox')).filter((checkbox) => {
        const rowId = checkbox.dataset.rowId;
        if (!rowId || seen.has(rowId)) {
            return false;
        }
        seen.add(rowId);
        return true;
    });
};

const getSelectedSpreadsheetRowIds = () =>
    getUniqueRecipientCheckboxes()
        .filter((checkbox) => checkbox.checked)
        .map((checkbox) => Number(checkbox.dataset.rowId))
        .filter((rowId) => !Number.isNaN(rowId));

const updateSpreadsheetSelectionState = () => {
    if (currentPreviewSource !== INPUT_SOURCE.SPREADSHEET) {
        return;
    }

    const uniqueCheckboxes = getUniqueRecipientCheckboxes();
    const selectedRowIds = getSelectedSpreadsheetRowIds();
    const selectAllCheckbox = document.getElementById('selectAllRecipients');
    const selectedCountLabel = document.getElementById('selectedRecipientCount');
    const totalCountDisplay = document.getElementById('totalCountDisplay');

    document.querySelectorAll('.recipient-checkbox').forEach((checkbox) => {
        checkbox.checked = selectedRowIds.includes(Number(checkbox.dataset.rowId));
    });

    if (selectAllCheckbox) {
        selectAllCheckbox.checked = uniqueCheckboxes.length > 0 && selectedRowIds.length === uniqueCheckboxes.length;
    }

    if (selectedCountLabel) {
        selectedCountLabel.textContent = `${selectedRowIds.length}명 선택`;
    }

    if (totalCountDisplay) {
        totalCountDisplay.textContent = `총 ${selectedRowIds.length}명에게 발송 예정`;
    }

    selectedSpreadsheetRowIds = selectedRowIds;
    hasSpreadsheetSelectionState = true;
    updateSheetSelectionSummary();
};

const toggleAllSpreadsheetRecipients = (checked) => {
    document.querySelectorAll('.recipient-checkbox').forEach((checkbox) => {
        checkbox.checked = checked;
    });
    updateSpreadsheetSelectionState();
};

const applySpreadsheetRangeSelection = (shouldSelect) => {
    const startInput = document.getElementById('rangeStartRow');
    const endInput = document.getElementById('rangeEndRow');
    const result = document.getElementById('result');

    if (!startInput || !endInput) {
        return;
    }

    const startRow = Number(startInput.value);
    const endRow = Number(endInput.value);

    if (!Number.isInteger(startRow) || !Number.isInteger(endRow) || startRow <= 0 || endRow <= 0) {
        result.innerHTML = '⚠️ 범위 선택 시작 행과 끝 행을 올바르게 입력해 주세요.';
        result.className = 'show';
        return;
    }

    const rangeStart = Math.min(startRow, endRow);
    const rangeEnd = Math.max(startRow, endRow);

    let matchedCount = 0;
    document.querySelectorAll('.recipient-checkbox').forEach((checkbox) => {
        const rowId = Number(checkbox.dataset.rowId);
        if (Number.isNaN(rowId) || rowId < rangeStart || rowId > rangeEnd) {
            return;
        }
        checkbox.checked = shouldSelect;
        matchedCount += 1;
    });

    if (matchedCount === 0) {
        result.innerHTML = `⚠️ ${rangeStart}행부터 ${rangeEnd}행 사이에 선택할 수 있는 대상이 없습니다.`;
        result.className = 'show';
        return;
    }

    result.className = 'hide';
    updateSpreadsheetSelectionState();
};

window.toggleAllSpreadsheetRecipients = toggleAllSpreadsheetRecipients;
window.updateSpreadsheetSelectionState = updateSpreadsheetSelectionState;
window.applySpreadsheetRangeSelection = applySpreadsheetRangeSelection;

const sendSelectedSpreadsheetMails = async () => {
    const result = document.getElementById('result');
    const progressWrapper = document.querySelector('.progress-wrapper');
    const selectedRowIds = [...selectedSpreadsheetRowIds];

    if (!currentSpreadsheetRequestPayload) {
        result.innerHTML = '⚠️ 먼저 스프레드시트 미리보기를 불러오세요.';
        result.className = 'show';
        return;
    }

    if (!currentSpreadsheetSnapshotId) {
        result.innerHTML = '⚠️ 스프레드시트 미리보기 정보가 없습니다. 다시 미리보기를 불러와 주세요.';
        result.className = 'show';
        return;
    }

    if (!selectedRowIds.length) {
        result.innerHTML = '⚠️ 발송할 메일을 한 명 이상 선택해 주세요.';
        result.className = 'show';
        return;
    }

    closePreviewModal();
    progressWrapper.style.display = 'block';
    startProgressTracking(selectedRowIds.length);

    result.innerHTML = '🚀 선택한 메일 발송을 처리중...';
    result.className = 'show';

    try {
        await persistColumnMappingSafely();
        const response = await fetch(`${server_host}/api/mail/send-sheet`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
                snapshotId: currentSpreadsheetSnapshotId,
                selectedRowIds
            })
        });

        const responseText = await response.text();
        if (response.ok) {
            result.innerHTML = `✅ ${responseText}`;
            selectedSpreadsheetRowIds = [];
            currentSpreadsheetPreviewRowIds = [];
            hasSpreadsheetSelectionState = false;
            currentSpreadsheetRequestPayload = null;
            currentSpreadsheetSnapshotId = null;
            updateSheetSelectionSummary();
            return;
        }

        if (response.status == 401) {
            if (confirm('로그인 먼저 해주세요!!')) {
                window.location.href = `${server_host}`;
            }
        }

        throw new Error(responseText || response.statusText);
    } catch (error) {
        result.innerHTML = `❌ ${error.message}`;
        result.className = 'show';
        stopProgressTracking();
        progressWrapper.style.display = 'none';
    }
};

function updateSheetSelectionSummary() {
    const summary = document.getElementById('sheetSelectionSummary');
    if (!summary) {
        return;
    }

    if (getSelectedSource() !== INPUT_SOURCE.SPREADSHEET || !selectedSpreadsheetRowIds.length) {
        summary.textContent = '';
        summary.className = 'sheet-selection-summary';
        return;
    }

    summary.textContent = `총 ${selectedSpreadsheetRowIds.length}명에게 발송 예정`;
    summary.className = 'sheet-selection-summary show';
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

const getSpreadsheetPreviewData = async (spreadsheetUrl) => {
    const response = await fetch(`${server_host}/api/mail/preview-sheet`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(createSpreadsheetRequestPayload(spreadsheetUrl))
    });

    if (response.ok) {
        return await response.json();
    }

    if (response.status == 401) {
        if (confirm('로그인 먼저 해주세요!!')) {
            window.location.href = `${server_host}`;
        }
    }

    const errorText = await response.text();
    throw new Error(errorText || response.statusText);
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
