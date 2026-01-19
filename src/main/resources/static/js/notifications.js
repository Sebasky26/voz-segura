/**
 * Sistema de Notificaciones Voz Segura
 * Reemplaza las notificaciones del navegador por modales personalizados
 * siguiendo el estilo del sistema.
 */
(function() {
    "use strict";

    // Contenedor de notificaciones (toast stack)
    let toastContainer = null;

    /**
     * Inicializa el contenedor de notificaciones toast
     */
    function initToastContainer() {
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'vs-toast-container';
            toastContainer.className = 'vs-toast-container';
            document.body.appendChild(toastContainer);
        }
        return toastContainer;
    }

    /**
     * Muestra una notificación tipo toast
     * @param {string} message - Mensaje a mostrar
     * @param {string} type - Tipo: 'success', 'error', 'warning', 'info'
     * @param {number} duration - Duración en ms (default: 5000)
     */
    window.vsToast = function(message, type = 'info', duration = 5000) {
        const container = initToastContainer();
        
        const icons = {
            success: '✓',
            error: '✕',
            warning: '⚠',
            info: 'ℹ'
        };

        const toast = document.createElement('div');
        toast.className = `vs-toast vs-toast--${type}`;
        toast.setAttribute('role', 'alert');
        toast.setAttribute('aria-live', 'polite');
        toast.innerHTML = `
            <span class="vs-toast__icon">${icons[type] || icons.info}</span>
            <span class="vs-toast__message">${escapeHtml(message)}</span>
            <button type="button" class="vs-toast__close" aria-label="Cerrar">×</button>
        `;

        // Botón de cerrar
        const closeBtn = toast.querySelector('.vs-toast__close');
        closeBtn.addEventListener('click', function() {
            dismissToast(toast);
        });

        container.appendChild(toast);

        // Animación de entrada
        requestAnimationFrame(() => {
            toast.classList.add('vs-toast--visible');
        });

        // Auto-dismiss
        if (duration > 0) {
            setTimeout(() => {
                dismissToast(toast);
            }, duration);
        }

        return toast;
    };

    /**
     * Cierra un toast con animación
     */
    function dismissToast(toast) {
        toast.classList.remove('vs-toast--visible');
        toast.classList.add('vs-toast--hiding');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }

    /**
     * Muestra un modal de notificación
     * @param {string} title - Título del modal
     * @param {string} message - Mensaje a mostrar
     * @param {string} type - Tipo: 'success', 'error', 'warning', 'info'
     * @param {function} onClose - Callback al cerrar (opcional)
     */
    window.vsNotify = function(title, message, type = 'info', onClose = null) {
        // Remover modal anterior si existe
        const existing = document.getElementById('vsNotificationModal');
        if (existing) {
            existing.remove();
        }

        const icons = {
            success: '✓',
            error: '✕',
            warning: '⚠',
            info: 'ℹ'
        };

        const iconColors = {
            success: 'var(--success)',
            error: 'var(--danger)',
            warning: 'var(--warning)',
            info: 'var(--info)'
        };

        const modalHTML = `
            <div class="vs-modal-overlay vs-modal-overlay--notification" id="vsNotificationModal">
                <div class="vs-modal vs-modal--notification" role="dialog" aria-modal="true" aria-labelledby="vsNotifyTitle">
                    <div class="vs-modal__icon vs-modal__icon--${type}">
                        <span style="color: ${iconColors[type]}; font-size: 2.5rem;">${icons[type] || icons.info}</span>
                    </div>
                    <div class="vs-modal__header">
                        <h2 id="vsNotifyTitle" class="vs-h3">${escapeHtml(title)}</h2>
                    </div>
                    <div class="vs-modal__body">
                        <p class="vs-text">${escapeHtml(message)}</p>
                    </div>
                    <div class="vs-modal__footer">
                        <button type="button" class="vs-button vs-button--primary" id="vsNotifyCloseBtn">
                            Aceptar
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);

        const modal = document.getElementById('vsNotificationModal');
        const closeBtn = document.getElementById('vsNotifyCloseBtn');

        // Animación de entrada
        requestAnimationFrame(() => {
            modal.classList.add('vs-modal-overlay--visible');
        });

        function closeModal() {
            modal.classList.remove('vs-modal-overlay--visible');
            setTimeout(() => {
                modal.remove();
                if (typeof onClose === 'function') {
                    onClose();
                }
            }, 200);
        }

        // Cerrar con botón
        closeBtn.addEventListener('click', closeModal);

        // Cerrar con Escape
        function handleEscape(e) {
            if (e.key === 'Escape') {
                closeModal();
                document.removeEventListener('keydown', handleEscape);
            }
        }
        document.addEventListener('keydown', handleEscape);

        // Cerrar al hacer clic fuera
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeModal();
            }
        });

        // Focus en el botón
        closeBtn.focus();
    };

    /**
     * Muestra un modal de confirmación
     * @param {string} title - Título del modal
     * @param {string} message - Mensaje a mostrar
     * @param {function} onConfirm - Callback si confirma
     * @param {function} onCancel - Callback si cancela (opcional)
     */
    window.vsConfirm = function(title, message, onConfirm, onCancel = null) {
        const existing = document.getElementById('vsConfirmModal');
        if (existing) {
            existing.remove();
        }

        const modalHTML = `
            <div class="vs-modal-overlay vs-modal-overlay--notification" id="vsConfirmModal">
                <div class="vs-modal vs-modal--notification" role="dialog" aria-modal="true" aria-labelledby="vsConfirmTitle">
                    <div class="vs-modal__icon vs-modal__icon--warning">
                        <span style="color: var(--warning); font-size: 2.5rem;">⚠</span>
                    </div>
                    <div class="vs-modal__header">
                        <h2 id="vsConfirmTitle" class="vs-h3">${escapeHtml(title)}</h2>
                    </div>
                    <div class="vs-modal__body">
                        <p class="vs-text">${escapeHtml(message)}</p>
                    </div>
                    <div class="vs-modal__footer">
                        <button type="button" class="vs-button vs-button--secondary" id="vsConfirmCancelBtn">
                            Cancelar
                        </button>
                        <button type="button" class="vs-button vs-button--primary" id="vsConfirmOkBtn">
                            Confirmar
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);

        const modal = document.getElementById('vsConfirmModal');
        const okBtn = document.getElementById('vsConfirmOkBtn');
        const cancelBtn = document.getElementById('vsConfirmCancelBtn');

        requestAnimationFrame(() => {
            modal.classList.add('vs-modal-overlay--visible');
        });

        function closeModal(confirmed) {
            modal.classList.remove('vs-modal-overlay--visible');
            setTimeout(() => {
                modal.remove();
                if (confirmed && typeof onConfirm === 'function') {
                    onConfirm();
                } else if (!confirmed && typeof onCancel === 'function') {
                    onCancel();
                }
            }, 200);
        }

        okBtn.addEventListener('click', () => closeModal(true));
        cancelBtn.addEventListener('click', () => closeModal(false));

        document.addEventListener('keydown', function handleEscape(e) {
            if (e.key === 'Escape') {
                closeModal(false);
                document.removeEventListener('keydown', handleEscape);
            }
        });

        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeModal(false);
            }
        });

        cancelBtn.focus();
    };

    /**
     * Escapa HTML para prevenir XSS
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Cierra el modal de notificación actual (para compatibilidad)
     */
    window.closeNotificationModal = function() {
        const modal = document.getElementById('notificationModal');
        if (modal) {
            modal.classList.add('vs-modal--hidden');
            setTimeout(() => {
                modal.style.display = 'none';
            }, 200);
        }
        
        const vsModal = document.getElementById('vsNotificationModal');
        if (vsModal) {
            vsModal.classList.remove('vs-modal-overlay--visible');
            setTimeout(() => {
                vsModal.remove();
            }, 200);
        }
    };

    // Cerrar modales con Escape (global)
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeNotificationModal();
        }
    });

    // Auto-mostrar notificaciones de flash messages de Thymeleaf al cargar
    document.addEventListener('DOMContentLoaded', function() {
        const successAlert = document.querySelector('[data-vs-success]');
        const errorAlert = document.querySelector('[data-vs-error]');
        
        if (successAlert) {
            const message = successAlert.getAttribute('data-vs-success');
            if (message) {
                vsNotify('Operación exitosa', message, 'success');
            }
        }
        
        if (errorAlert) {
            const message = errorAlert.getAttribute('data-vs-error');
            if (message) {
                vsNotify('Error', message, 'error');
            }
        }
    });

})();
