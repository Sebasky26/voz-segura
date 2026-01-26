/**
 * reglas.js - Gestión de Reglas de Derivación
 *
 * Funcionalidades:
 * - Activar/desactivar reglas con confirmación
 * - Notificaciones visuales (sin alert/confirm del navegador)
 * - Validación de formularios
 *
 * @author Voz Segura Team
 * @since 2026-01
 */

(function() {
    'use strict';

    /**
     * Muestra una notificación temporal
     */
    function showNotification(message, type = 'info') {
        // Eliminar notificaciones previas
        const prev = document.querySelector('.vs-notification');
        if (prev) {
            prev.remove();
        }

        const notification = document.createElement('div');
        notification.className = `vs-notification vs-notification--${type}`;
        notification.textContent = message;
        notification.setAttribute('role', 'alert');
        notification.setAttribute('aria-live', 'polite');

        document.body.appendChild(notification);

        // Forzar reflow para que la animación funcione
        notification.offsetHeight;
        notification.classList.add('vs-notification--show');

        // Auto-cerrar después de 4 segundos
        setTimeout(() => {
            notification.classList.remove('vs-notification--show');
            setTimeout(() => notification.remove(), 300);
        }, 4000);
    }

    /**
     * Muestra un modal de confirmación personalizado
     */
    function showConfirmModal(message, onConfirm) {
        // Crear overlay
        const overlay = document.createElement('div');
        overlay.className = 'vs-modal-overlay';
        overlay.setAttribute('role', 'dialog');
        overlay.setAttribute('aria-modal', 'true');
        overlay.setAttribute('aria-labelledby', 'modal-title');

        // Crear modal
        const modal = document.createElement('div');
        modal.className = 'vs-modal';
        modal.innerHTML = `
            <div class="vs-modal__header">
                <h3 id="modal-title" class="vs-modal__title">Confirmación</h3>
            </div>
            <div class="vs-modal__body">
                <p>${message}</p>
            </div>
            <div class="vs-modal__footer">
                <button type="button" class="vs-button vs-button--secondary" data-action="cancel">
                    Cancelar
                </button>
                <button type="button" class="vs-button vs-button--primary" data-action="confirm">
                    Confirmar
                </button>
            </div>
        `;

        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        // Forzar reflow para animación
        overlay.offsetHeight;
        overlay.classList.add('vs-modal-overlay--show');

        // Event listeners
        const btnCancel = modal.querySelector('[data-action="cancel"]');
        const btnConfirm = modal.querySelector('[data-action="confirm"]');

        function closeModal() {
            overlay.classList.remove('vs-modal-overlay--show');
            setTimeout(() => overlay.remove(), 300);
        }

        btnCancel.addEventListener('click', closeModal);
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) closeModal();
        });

        btnConfirm.addEventListener('click', () => {
            closeModal();
            if (onConfirm) onConfirm();
        });

        // Focus en el botón de confirmar
        btnConfirm.focus();

        // Cerrar con ESC
        function handleEsc(e) {
            if (e.key === 'Escape') {
                closeModal();
                document.removeEventListener('keydown', handleEsc);
            }
        }
        document.addEventListener('keydown', handleEsc);
    }

    /**
     * Manejador de activar/desactivar reglas
     */
    function initRuleActions() {
        const actionForms = document.querySelectorAll('form[action*="/reglas/"][action*="/activar"], form[action*="/reglas/"][action*="/desactivar"]');

        actionForms.forEach(form => {
            form.addEventListener('submit', function(e) {
                e.preventDefault();

                const isActivate = this.action.includes('/activar');
                const message = isActivate
                    ? '¿Está seguro de activar esta regla de derivación?'
                    : '¿Está seguro de desactivar esta regla de derivación?';

                showConfirmModal(message, () => {
                    // Mostrar indicador de carga
                    const btn = this.querySelector('button[type="submit"]');
                    const originalText = btn.textContent;
                    btn.disabled = true;
                    btn.textContent = 'Procesando...';

                    // Enviar formulario
                    this.submit();
                });
            });
        });
    }

    /**
     * Validación del formulario de crear regla
     */
    function initFormValidation() {
        const form = document.querySelector('form[action*="/reglas/crear"]');
        if (!form) return;

        form.addEventListener('submit', function(e) {
            const name = form.querySelector('#name').value.trim();
            const destinationId = form.querySelector('#destinationId').value;
            const priorityOrder = form.querySelector('#priorityOrder').value;
            const slaHours = form.querySelector('#slaHours').value;

            if (!name) {
                e.preventDefault();
                showNotification('El nombre de la regla es requerido', 'error');
                form.querySelector('#name').focus();
                return;
            }

            if (!destinationId) {
                e.preventDefault();
                showNotification('Debe seleccionar una entidad destino', 'error');
                form.querySelector('#destinationId').focus();
                return;
            }

            if (!priorityOrder || priorityOrder < 1 || priorityOrder > 100) {
                e.preventDefault();
                showNotification('El orden de prioridad debe estar entre 1 y 100', 'error');
                form.querySelector('#priorityOrder').focus();
                return;
            }

            if (!slaHours || slaHours < 1) {
                e.preventDefault();
                showNotification('El SLA debe ser al menos 1 hora', 'error');
                form.querySelector('#slaHours').focus();
                return;
            }

            // Mostrar indicador de carga
            const btn = form.querySelector('button[type="submit"]');
            btn.disabled = true;
            btn.textContent = 'Creando...';
        });
    }

    /**
     * Inicialización cuando el DOM está listo
     */
    function init() {
        // Mostrar notificaciones de Thymeleaf (si existen)
        const successMsg = document.querySelector('.vs-alert--success');
        if (successMsg) {
            showNotification(successMsg.textContent.trim(), 'success');
            successMsg.remove();
        }

        const errorMsg = document.querySelector('.vs-alert--danger');
        if (errorMsg) {
            showNotification(errorMsg.textContent.trim(), 'error');
            errorMsg.remove();
        }

        // Inicializar funcionalidades
        initRuleActions();
        initFormValidation();
    }

    // Ejecutar cuando el DOM esté listo
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
