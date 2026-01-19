(function () {
    "use strict";

    // Confirmación antes de rechazar
    const formRechazo = document.querySelector('form[action*="/rechazar"]');
    if (formRechazo) {
        formRechazo.addEventListener('submit', function(e) {
            const motivo = formRechazo.querySelector('textarea[name="motivo"]');
            if (!motivo || motivo.value.trim().length < 10) {
                e.preventDefault();
                alert('Por favor ingrese un motivo de rechazo (mínimo 10 caracteres).');
                motivo.focus();
                return false;
            }
            if (!confirm('¿Está seguro de rechazar esta denuncia? Esta acción no se puede deshacer.')) {
                e.preventDefault();
                return false;
            }
        });
    }

    // Confirmación antes de aprobar y derivar
    const formAprobar = document.querySelector('form[action*="/aprobar-derivar"]');
    if (formAprobar) {
        formAprobar.addEventListener('submit', function(e) {
            if (!confirm('¿Está seguro de aprobar y derivar esta denuncia?')) {
                e.preventDefault();
                return false;
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
                alert('Por favor describa qué información necesita (mínimo 10 caracteres).');
                motivo.focus();
                return false;
            }
        });
    }

})();
