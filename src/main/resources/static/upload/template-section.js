// 템플릿 이미지 파일 선택 시 라벨 업데이트
document.getElementById('templateFileInput').addEventListener('change', function(e) {
    const fileName = e.target.files[0]?.name;
    const label = this.nextElementSibling?.querySelector('.file-upload-text') ||
        document.querySelector('label[for="templateFileInput"] .file-upload-text') ||
        document.querySelector('.file-upload-text');

    if (fileName) {
        if (label) {
            label.innerHTML = `선택된 파일: <strong>${fileName}</strong><br><small>다른 파일을 선택하려면 클릭하세요</small>`;
        }

        // 새 파일 선택 시 기존 파일 정보와 미리보기 제거
        const existingFileInfo = document.getElementById('existingFileInfo');
        if (existingFileInfo) existingFileInfo.remove();
        const preview = document.getElementById('filePreview');
        if (preview) preview.remove();
    } else {
        if (label) {
            label.innerHTML = `이미지 파일을 선택<br><small>(.jpeg, .jpg, .png 파일만 지원)</small>`;
        }
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
        }
        // 기존 파일 정보가 있는 경우 (변경 없음)
        else if (existingFileInfo) {
            const fileInfo = JSON.parse(existingFileInfo.value);
            formData.append('existingFileName', fileInfo.fileName);
            formData.append('existingFileData', fileInfo.fileData);
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
        } else {
            if(response.status == 401) {
                if(confirm('로그인 먼저 해주세요!!')) {
                    window.location.href = `${client_host}/login/login.html`;
                }
            }
            result.innerHTML = `❌ 템플릿 저장 중 오류가 발생했습니다: ${responseText}`;
        }

    } catch (error) {
        result.innerHTML = `❌ 네트워크 오류가 발생했습니다: ${error.message}`;
        console.error('Template save error:', error);
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
                loadLink.innerHTML = '저장된 템플릿이 엄서요...';
                return;
            }

            let templateData = JSON.parse(textData);
            document.getElementById('subject').value = templateData.subject || '';
            let quillEditor = window.quill || window.quillEditor || quill;
            if (quillEditor) {
                insertTemplateBodyIntoQuill(quillEditor, templateData.body);
            }

            // 기존 파일 처리
            const templateFileInput = document.getElementById('templateFileInput');
            const label = templateFileInput.nextElementSibling?.querySelector('.file-upload-text');

            if (templateData.templateFileName && templateData.templateFileData) {
                // 시각적 피드백
                if (label) {
                    label.innerHTML = `<div style="color: #28a745;">📎 기존 파일: <strong>${templateData.templateFileName}</strong></div><small style="color: #6c757d;">새 파일을 선택하면 기존 파일이 교체됩니다</small>`;
                }

                // 기존 파일 정보 저장 (파일 데이터도 포함)
                let hiddenFileInfo = document.getElementById('existingFileInfo');
                if (!hiddenFileInfo) {
                    hiddenFileInfo = document.createElement('input');
                    hiddenFileInfo.type = 'hidden';
                    hiddenFileInfo.id = 'existingFileInfo';
                    hiddenFileInfo.name = 'existingFileInfo';
                    templateFileInput.parentNode.appendChild(hiddenFileInfo);
                }
                hiddenFileInfo.value = JSON.stringify({
                    fileName: templateData.templateFileName,
                    fileData: templateData.templateFileData,
                    hasFile: true
                });

                // 이미지 미리보기
                const fileName = templateData.templateFileName.toLowerCase();
                if (fileName.includes('.jpg') || fileName.includes('.jpeg') || fileName.includes('.png')) {
                    const existingPreview = document.getElementById('filePreview');
                    if (existingPreview) existingPreview.remove();

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
            } else {
                // 파일이 없는 경우
                if (label) {
                    label.innerHTML = `이미지 파일을 선택<br><small>(.jpeg, .jpg, .png 파일만 지원)</small>`;
                }
                const existingFileInfo = document.getElementById('existingFileInfo');
                if (existingFileInfo) existingFileInfo.remove();
                const existingPreview = document.getElementById('filePreview');
                if (existingPreview) existingPreview.remove();
            }

        } else {
            if(response.status == 401) {
                if(confirm('로그인 먼저 해주세요!!')) {
                    window.location.href = `${client_host}/login/login.html`;
                }
            }
        }
    } catch (error) {
        console.error('Template load error');
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