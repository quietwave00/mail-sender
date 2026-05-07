(function () {
    const STORAGE_KEY = 'update-note:account-template-management:v1';

    const createModalMarkup = () => `
    <div class="update-modal-overlay" id="updateNoteOverlay" aria-hidden="true">
        <div class="update-modal-dialog" role="dialog" aria-modal="true" aria-labelledby="updateNoteTitle">
            <div class="update-modal-content">
                <div class="update-modal-header">
                    <div class="update-modal-badge">업데이트 노트</div>
                    <h2 class="update-modal-title" id="updateNoteTitle">계정별 관리</h2>
                    <p class="update-modal-subtitle"><b>ㅎㅇ요!!! 오늘도 조은 하루 보내시어!!</b><br> 로그인한 계정별로 최근 설정이 분리됩니다.</p>
                </div>
                
                <div class="update-modal-body">
                    <ul class="update-feature-list">
                        <li>
                            <span class="feature-icon">✓</span>
                            <div class="feature-text">
                                <strong>개별 템플릿 저장</strong>
                                <p>계정별로 가장 최근에 저장한 템플릿을 관리합니다.</p>
                            </div>
                        </li>
                        <li>
                            <span class="feature-icon">✓</span>
                            <div class="feature-text">
                                <strong>자동 열 설정</strong>
                                <p>계정별로 이름, 메일, 예매번호 설정을 기억합니다.</p>
                            </div>
                        </li>
                    </ul>
                </div>

                <div class="update-modal-footer">
                    <button type="button" class="update-modal-button" id="updateNoteConfirmButton">시작하기</button>
                </div>
            </div>
        </div>
    </div>
`;

    const shouldShowModal = () => {
        try {
            return window.localStorage.getItem(STORAGE_KEY) !== 'dismissed';
        } catch (error) {
            return true;
        }
    };

    const markDismissed = () => {
        try {
            window.localStorage.setItem(STORAGE_KEY, 'dismissed');
        } catch (error) {
            console.error('업데이트 노트 상태 저장 오류:', error);
        }
    };

    const closeModal = (overlay) => {
        overlay.classList.remove('active');
        overlay.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('update-note-open');
        markDismissed();
    };

    const openModal = (overlay) => {
        overlay.classList.add('active');
        overlay.setAttribute('aria-hidden', 'false');
        document.body.classList.add('update-note-open');
    };

    const initializeUpdateNoteModal = () => {
        if (!window.location.pathname.startsWith('/upload')) {
            return;
        }

        if (!shouldShowModal()) {
            return;
        }

        document.body.insertAdjacentHTML('beforeend', createModalMarkup());

        const overlay = document.getElementById('updateNoteOverlay');
        const confirmButton = document.getElementById('updateNoteConfirmButton');
        if (!overlay || !confirmButton) {
            return;
        }

        confirmButton.addEventListener('click', () => closeModal(overlay));
        overlay.addEventListener('click', (event) => {
            if (event.target === overlay) {
                closeModal(overlay);
            }
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && overlay.classList.contains('active')) {
                closeModal(overlay);
            }
        });

        openModal(overlay);
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeUpdateNoteModal);
        return;
    }

    initializeUpdateNoteModal();
})();
