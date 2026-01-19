(function () {
    "use strict";

    // =============================================
    // Modal de Notificaciones (usando notifications.js)
    // =============================================
    window.closeNotificationModal = function() {
        const modal = document.getElementById('notificationModal');
        if (modal) {
            modal.classList.add('vs-modal--hidden');
        }
        // También cerrar modales del nuevo sistema
        const vsModal = document.getElementById('vsNotificationModal');
        if (vsModal) {
            vsModal.remove();
        }
    };

    // Cerrar modal con tecla Escape
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeNotificationModal();
        }
    });

    // =============================================
    // Confirmaciones de formularios con modales personalizados
    // =============================================
    
    // Confirmación antes de rechazar
    const formRechazo = document.querySelector('form[action*="/rechazar"]');
    if (formRechazo) {
        formRechazo.addEventListener('submit', function(e) {
            e.preventDefault();
            const motivo = formRechazo.querySelector('textarea[name="motivo"]');
            if (!motivo || motivo.value.trim().length < 10) {
                if (typeof vsNotify === 'function') {
                    vsNotify('Validación', 'Por favor ingrese un motivo de rechazo (mínimo 10 caracteres).', 'warning');
                }
                motivo.focus();
                return false;
            }
            if (typeof vsConfirm === 'function') {
                vsConfirm(
                    'Confirmar rechazo',
                    '¿Está seguro de rechazar esta denuncia? Esta acción no se puede deshacer.',
                    function() {
                        formRechazo.submit();
                    }
                );
            } else {
                formRechazo.submit();
            }
        });
    }

    // Confirmación antes de aprobar y derivar
    const formAprobar = document.querySelector('form[action*="/aprobar-derivar"]');
    if (formAprobar) {
        formAprobar.addEventListener('submit', function(e) {
            e.preventDefault();
            if (typeof vsConfirm === 'function') {
                vsConfirm(
                    'Confirmar aprobación',
                    '¿Está seguro de aprobar y derivar esta denuncia a la entidad correspondiente?',
                    function() {
                        formAprobar.submit();
                    }
                );
            } else {
                formAprobar.submit();
            }
        });
    }

    // Validación de solicitar información
    const formInfo = document.querySelector('form[action*="/solicitar-info"]');
    if (formInfo) {
        formInfo.addEventListener('submit', function(e) {
            const motivo = formInfo.querySelector('textarea[name="motivo"]');
            if (!motivo || motivo.value.trim().length < 10) {
                e.preventDefault();
                if (typeof vsNotify === 'function') {
                    vsNotify('Validación', 'Por favor describa qué información necesita (mínimo 10 caracteres).', 'warning');
                }
                motivo.focus();
                return false;
            }
        });
    }

})();
