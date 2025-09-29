// 새 파일 미리보기 생성 함수
function createNewFilePreview(file, parentContainer) {
    // 기존 미리보기 제거
    const existingPreview = document.getElementById('filePreview');
    if (existingPreview) {
        existingPreview.remove();
    }

    // 이미지 파일인지 확인
    const fileName = file.name.toLowerCase();
    if (fileName.includes('.jpg') || fileName.includes('.jpeg') || fileName.includes('.png')) {
        const previewContainer = document.createElement('div');
        previewContainer.id = 'filePreview';
        previewContainer.style.cssText = 'margin-top: 10px; text-align: center; padding: 10px; border: 1px solid #ddd; border-radius: 5px; background-color: #f9f9f9;';

        const img = document.createElement('img');
        img.style.cssText = 'max-width: 200px; max-height: 150px; border-radius: 3px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);';

        // FileReader를 사용해 파일을 읽어서 미리보기 생성
        const reader = new FileReader();
        reader.onload = function(e) {
            img.src = e.target.result;
        };
        reader.readAsDataURL(file);

        const caption = document.createElement('div');
        caption.style.cssText = 'margin-top: 8px; font-size: 12px; color: #666;';
        caption.textContent = `미리보기: ${file.name}`;

        previewContainer.appendChild(img);
        previewContainer.appendChild(caption);

        // 파일 입력 필드 다음에 미리보기 삽입
        const templateFileInput = document.getElementById('templateFileInput');
        templateFileInput.parentNode.insertBefore(previewContainer, templateFileInput.nextSibling.nextSibling);
    }
}

// 템플릿 이미지 파일 선택 시 라벨 업데이트
document.getElementById('templateFileInput').addEventListener('change', function(e) {
    const fileName = e.target.files[0]?.name;
    const selectedFile = e.target.files[0];
    const label = this.nextElementSibling?.querySelector('.file-upload-text') ||
        document.querySelector('label[for="templateFileInput"] .file-upload-text') ||
        document.querySelector('.file-upload-text');

    if (fileName && selectedFile) {
        if (label) {
            label.innerHTML = `선택된 파일: <strong>${fileName}</strong><br><small>다른 파일을 선택하려면 클릭하세요</small>`;
        }

        // 기존 정보 제거
        const existingFileInfo = document.getElementById('existingFileInfo');
        if (existingFileInfo) existingFileInfo.remove();

        // 새 파일 미리보기 생성
        createNewFilePreview(selectedFile, this.parentNode);

        // 삭제 버튼 추가
        addRemoveButton(this.parentNode);
    } else {
        if (label) {
            label.innerHTML = `이미지 파일을 선택<br><small>(.jpeg, .jpg, .png 파일만 지원)</small>`;
        }

        // 미리보기 제거
        const preview = document.getElementById('filePreview');
        if (preview) preview.remove();
    }
});

// 템플릿 설정 폼 제출 처리
document.getElementById('templateForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const subject = document.getElementById('subject').value;
    const templateFileInput = document.getElementById('templateFileInput');
    const result = document.getElementById('template-result');
    const existingFileInfo = document.getElementById('existingFileInfo');
    const bodyContent = quill.root.innerHTML
        .replace(/<p><br><\/p>/g, "<br>")
        .replace(/<p>(.*?)<\/p>/g, "$1<br>");
    const bodyText = quill.getText().trim();

    if (!subject || !bodyText) {
        result.innerHTML = '⚠️ 제목과 본문을 모두 입력해주세요.';
        result.className = 'show';
        return;
    }

    result.innerHTML = '⏳ 템플릿을 저장하는 중...';
    result.className = 'show';

    try {
        const formData = new FormData();
        formData.append('subject', subject);
        formData.append('body', bodyContent);

        // 새 파일이 선택된 경우
        if (templateFileInput.files.length > 0) {
            formData.append('templateFile', templateFileInput.files[0]);
            formData.append('useExistingFile', 'false');
        }
        // 기존 파일 정보가 있는 경우 (변경 없음)
        else if (existingFileInfo && existingFileInfo.value && existingFileInfo.value.trim() !== '') {
            try {
                const fileInfo = JSON.parse(existingFileInfo.value);
                if (fileInfo.hasFile && fileInfo.fileName && fileInfo.fileData) {
                    formData.append('existingFileName', fileInfo.fileName);
                    formData.append('existingFileData', fileInfo.fileData);
                    formData.append('useExistingFile', 'true');
                } else {
                    formData.append('useExistingFile', 'false');
                }
            } catch (parseError) {
                formData.append('useExistingFile', 'false');
            }
        } else {
            formData.append('useExistingFile', 'false');
        }

        const response = await fetch(`${server_host}/api/template`, {
            method: 'POST',
            body: formData,
            credentials: "include"
        });

        const responseText = await response.text();
        if (response.ok) {
            let message = '✅ 템플릿이 성공적으로 저장되었습니다 ㅇ ㅇb<br>';
            result.innerHTML = message;

            // 3초 후 결과 메시지 숨기기
            setTimeout(() => {
                result.className = '';
                result.innerHTML = '';
            }, 3000);
        } else {
            if(response.status == 401) {
                if(confirm('로그인 먼저 해주세요!!')) {
                    window.location.href = `${server_host}/login/login.html`;
                }
            }
            result.innerHTML = `❌ 템플릿 저장 중 오류가 발생했습니다: ${responseText}`;

            // 3초 후 에러 메시지 숨기기
            setTimeout(() => {
                result.className = '';
                result.innerHTML = '';
            }, 3000);
        }

    } catch (error) {
        result.innerHTML = `❌ 네트워크 오류가 발생했습니다: ${error.message}`;
        console.error('Template save error:', error);

        // 3초 후 에러 메시지 숨기기
        setTimeout(() => {
            result.className = '';
            result.innerHTML = '';
        }, 3000);
    }
});

// 템플릿 불러오기
const loadRecentTemplate = async () => {
    const loadLink = document.querySelector('.template-load-link');
    const originalText = loadLink.innerHTML;

    loadLink.classList.add('loading');
    loadLink.innerHTML = '<span class="load-icon">⏳</span>불러오는 중...';

    try {
        const response = await fetch(`${server_host}/api/template`, {
            method: 'GET',
            credentials: 'include',
            redirect: 'manual'
        });

        if (response.ok) {
            const textData = await response.text();
            if(textData.trim() == '') {
                loadLink.innerHTML = '저장된 템플릿이 없어요...';
                return;
            }

            let templateData = JSON.parse(textData);

            document.getElementById('subject').value = templateData.subject || '';
            let quillEditor = window.quill || window.quillEditor || quill;
            if (quillEditor) {
                insertTemplateBodyIntoQuill(quillEditor, templateData.body);
            }

            // 파일 입력 필드 초기화 (기존 선택 제거)
            const templateFileInput = document.getElementById('templateFileInput');
            templateFileInput.value = '';

            // 기존 파일 처리
            const label = templateFileInput.nextElementSibling?.querySelector('.file-upload-text');

            // 기존 hidden input과 preview 제거
            const existingHiddenInput = document.getElementById('existingFileInfo');
            if (existingHiddenInput) {
                existingHiddenInput.remove();
            }
            const existingPreview = document.getElementById('filePreview');
            if (existingPreview) {
                existingPreview.remove();
            }

            if (templateData.templateFileName && templateData.templateFileData) {
                // 시각적 피드백
                if (label) {
                    label.innerHTML = `<div style="color: #28a745;">📎 기존 파일: <strong>${templateData.templateFileName}</strong></div><small style="color: #6c757d;">새 파일을 선택하면 기존 파일이 교체됩니다</small>`;
                }

                // 기존 파일 정보 저장 (파일 데이터도 포함)
                const hiddenFileInfo = document.createElement('input');
                hiddenFileInfo.type = 'hidden';
                hiddenFileInfo.id = 'existingFileInfo';
                hiddenFileInfo.name = 'existingFileInfo';

                const fileInfoData = {
                    fileName: templateData.templateFileName,
                    fileData: templateData.templateFileData,
                    hasFile: true
                };

                hiddenFileInfo.value = JSON.stringify(fileInfoData);
                templateFileInput.parentNode.appendChild(hiddenFileInfo);

                // 이미지 미리보기
                const fileName = templateData.templateFileName.toLowerCase();
                if (fileName.includes('.jpg') || fileName.includes('.jpeg') || fileName.includes('.png')) {
                    const previewContainer = document.createElement('div');
                    previewContainer.id = 'filePreview';
                    previewContainer.style.cssText = 'margin-top: 10px; text-align: center; padding: 10px; border: 1px solid #ddd; border-radius: 5px; background-color: #f9f9f9;';

                    const img = document.createElement('img');
                    const extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                    const mimeType = extension === 'png' ? 'png' : 'jpeg';
                    img.src = `data:image/${mimeType};base64,${templateData.templateFileData}`;
                    img.style.cssText = 'max-width: 200px; max-height: 150px; border-radius: 3px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);';

                    const caption = document.createElement('div');
                    caption.style.cssText = 'margin-top: 8px; font-size: 12px; color: #666;';
                    caption.textContent = `미리보기: ${templateData.templateFileName}`;

                    previewContainer.appendChild(img);
                    previewContainer.appendChild(caption);
                    templateFileInput.parentNode.insertBefore(previewContainer, templateFileInput.nextSibling.nextSibling);
                }
                addRemoveButton(templateFileInput.parentNode);
            } else {
                // 파일이 없는 경우
                if (label) {
                    label.innerHTML = `이미지 파일을 선택<br><small>(.jpeg, .jpg, .png 파일만 지원)</small>`;
                }
            }

        } else {
            if(response.status == 401) {
                if(confirm('로그인 먼저 해주세요!!')) {
                    window.location.href = `${server_host}/login/login.html`;
                }
            }
        }
    } catch (error) {
        console.error('Template load error:', error);
        const result = document.getElementById('template-result');
        result.innerHTML = ('❌ 네트워크 오류가 발생했습니다.');
    } finally {
        setTimeout(() => {
            loadLink.classList.remove('loading');
            loadLink.innerHTML = originalText;
        }, 800);
    }
}

function insertTemplateBodyIntoQuill(quillEditor, bodyHtml) {
    if (!quillEditor || !bodyHtml) return;

    const variablePattern = /\[\[([^\]]+)\]\]/g;
    quillEditor.clipboard.dangerouslyPasteHTML(bodyHtml);

    let match;
    while ((match = variablePattern.exec(bodyHtml)) !== null) {
        const variable = match[1];
        const range = quillEditor.getText().indexOf(`[[${variable}]]`);
        if (range !== -1) {
            quillEditor.deleteText(range, variable.length + 4); // [[ ]]
            quillEditor.insertEmbed(range, 'template-variable', variable);
        }
    }
}

// 파일 삭제
function clearSelectedFile() {
    const templateFileInput = document.getElementById('templateFileInput');
    const label = templateFileInput.nextElementSibling?.querySelector('.file-upload-text') ||
        document.querySelector('label[for="templateFileInput"] .file-upload-text') ||
        document.querySelector('.file-upload-text');

    // input 초기화
    templateFileInput.value = '';

    // 라벨 초기화
    if (label) {
        label.innerHTML = `이미지 파일을 선택<br><small>(.jpeg, .jpg, .png 파일만 지원)</small>`;
    }

    // hidden input, 미리보기 제거
    const existingHiddenInput = document.getElementById('existingFileInfo');
    if (existingHiddenInput) existingHiddenInput.remove();

    const existingPreview = document.getElementById('filePreview');
    if (existingPreview) existingPreview.remove();

    // 삭제 버튼 제거
    const removeBtn = document.getElementById('fileRemoveBtn');
    if (removeBtn) removeBtn.remove();
}

function addRemoveButton(parentContainer) {
    // 기존 삭제 버튼 제거
    let removeBtn = document.getElementById('fileRemoveBtn');
    if (removeBtn) {
        removeBtn.remove();
    }

    // 삭제 버튼을 별도 컨테이너로 생성
    const buttonContainer = document.createElement('div');
    buttonContainer.id = 'fileRemoveContainer';
    buttonContainer.style.cssText = 'margin-top: 10px; text-align: right;';

    removeBtn = document.createElement('button');
    removeBtn.type = 'button'; // 중요: type="button"으로 form 제출 방지
    removeBtn.id = 'fileRemoveBtn';
    removeBtn.textContent = '파일 선택 취소 X';
    removeBtn.style.cssText = `
        background: none;
        border: none;
        color: #757575;
        cursor: pointer;
        font-size: 12px;
        padding: 0 !important;
        margin: 0;
    `;

    // 버튼 클릭 이벤트
    removeBtn.addEventListener('click', function(e) {
        e.preventDefault(); // 기본 동작 방지
        e.stopPropagation(); // 이벤트 전파 방지
        e.stopImmediatePropagation(); // 모든 이벤트 전파 방지
        clearSelectedFile();
    });

    buttonContainer.appendChild(removeBtn);

    // 부모 컨테이너의 맨 마지막에 추가 (label과 분리)
    parentContainer.appendChild(buttonContainer);
}