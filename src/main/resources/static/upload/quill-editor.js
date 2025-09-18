const Delta = Quill.import('delta');
// Quill 에디터 초기화 및 변수 삽입 기능
const quill = new Quill('#editor', {
    theme: 'snow',
    placeholder: '메일 본문을 입력하세요...',
    modules: {
        toolbar: [
            ['bold', 'italic', 'underline'],   // 굵게, 기울임, 밑줄
            [{ 'list': 'ordered'}, { 'list': 'bullet' }] // 리스트
        ],
        clipboard: {
            // 클립보드 처리 개선
            matchVisual: false
        }
    }
});
document.dispatchEvent(new Event('quill-ready'));

// Custom Blot 개선
const Embed = Quill.import('blots/embed');
class VariableBlot extends Embed {
    static create(value) {
        const node = super.create();
        node.setAttribute('data-variable', value);
        node.setAttribute('contenteditable', false);

        // 텍스트 노드 생성 방식 개선
        const textNode = document.createTextNode(`[[${value}]]`);
        node.appendChild(textNode);

        // 스타일 적용
        node.style.background = '#d8ebfc';
        node.style.color = '#acc5e0';
        node.style.borderRadius = '2px';
        node.style.padding = '2px 4px';
        node.style.margin = '0 2px';
        node.style.display = 'inline';

        return node;
    }

    static value(node) {
        return node.getAttribute('data-variable');
    }

    // 클립보드 처리를 위한 추가 메서드
    static formats(node) {
        return node.getAttribute('data-variable');
    }

    // DOM 노드 검증
    static match(node) {
        return node.tagName === 'SPAN' && node.hasAttribute('data-variable');
    }
}

VariableBlot.blotName = 'template-variable';
VariableBlot.tagName = 'span';
VariableBlot.className = 'template-variable';
Quill.register(VariableBlot);

// 붙여넣기 이벤트 처리 개선
quill.clipboard.addMatcher(Node.ELEMENT_NODE, function(node, delta) {
    try {
        // 기존 템플릿 변수 span 태그 처리
        if (node.tagName === 'SPAN' && node.hasAttribute('data-variable')) {
            const variableName = node.getAttribute('data-variable');
            return new Delta().insert({ 'template-variable': variableName });
        }

        // 중첩된 span 태그에서 템플릿 변수 추출
        if (node.tagName === 'SPAN') {
            const innerSpans = node.querySelectorAll('span[contenteditable="false"]');
            if (innerSpans.length > 0) {
                const variableText = node.textContent.match(/\[\[([^\]]+)\]\]/);
                if (variableText && node.hasAttribute('data-variable')) {
                    const variableName = node.getAttribute('data-variable');
                    return new Delta().insert({ 'template-variable': variableName });
                }
            }
        }
        return delta;
    } catch (error) {
        console.warn('클립보드 처리 중 오류:', error);
        return delta;
    }
});

// 텍스트 붙여넣기 시 템플릿 변수 변환
quill.clipboard.addMatcher(Node.TEXT_NODE, function(node, delta) {
    try {
        const text = node.data;
        const variablePattern = /\[\[([^\]]+)\]\]/g;
        let newDelta = new Delta();
        let lastIndex = 0;
        let match;

        while ((match = variablePattern.exec(text)) !== null) {
            // 템플릿 변수 앞의 텍스트 추가
            if (match.index > lastIndex) {
                newDelta = newDelta.insert(text.slice(lastIndex, match.index));
            }

            // 템플릿 변수 추가
            newDelta = newDelta.insert({ 'template-variable': match[1] });
            lastIndex = variablePattern.lastIndex;
        }

        // 남은 텍스트 추가
        if (lastIndex < text.length) {
            newDelta = newDelta.insert(text.slice(lastIndex));
        }

        return newDelta.ops.length > 0 ? newDelta : delta;
    } catch (error) {
        console.warn('텍스트 처리 중 오류:', error);
        return delta;
    }
});

// 변수 삽입 함수 개선
const insertVariable = (variableName) => {
    try {
        const selection = quill.getSelection();
        if (!selection) {
            quill.focus();
            return;
        }

        const index = selection.index;

        // 안전한 삽입을 위한 검증
        const currentLength = quill.getLength();
        if (index > currentLength) {
            return;
        }

        // embed 삽입
        quill.insertEmbed(index, 'template-variable', variableName);

        // 커서 위치 조정
        const newIndex = index + 1;
        quill.setSelection(newIndex, 0);

        // 포커스 유지
        setTimeout(() => {
            quill.focus();
        }, 10);

    } catch (error) {
        console.error('변수 삽입 중 오류:', error);
    }
};

// 에러 핸들링을 위한 전역 리스너
quill.on('text-change', function(delta, oldDelta, source) {
    try {
        // 변경사항 검증 및 정리
        const contents = quill.getContents();
        // 필요시 추가 검증 로직
    } catch (error) {
        console.warn('텍스트 변경 처리 중 오류:', error);
    }
});

// DOM 조작 오류 방지를 위한 안전장치
quill.on('selection-change', function(range, oldRange, source) {
    try {
        if (range && source === 'user') {
            // 선택 영역이 유효한지 검증
            const length = quill.getLength();
            if (range.index > length || range.index + range.length > length) {
                quill.setSelection(Math.min(range.index, length - 1), 0);
            }
        }
    } catch (error) {
        console.warn('선택 영역 변경 처리 중 오류:', error);
    }
});

// 추가: 안전한 HTML 파싱을 위한 유틸리티 함수
function safeParseHTML(htmlString) {
    try {
        const parser = new DOMParser();
        const doc = parser.parseFromString(htmlString, 'text/html');
        return doc.body;
    } catch (error) {
        console.warn('HTML 파싱 오류:', error);
        return null;
    }
}