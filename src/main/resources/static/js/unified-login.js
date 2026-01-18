(function () {
    "use strict";

    // ======= Turnstile state =======
    let turnstileValid = false;

    // Turnstile callbacks (deben ser globales porque Turnstile los llama por nombre)
    window.onTurnstileSuccess = function () {
        turnstileValid = true;
        hide(document.getElementById("turnstileError"));
        updateSubmit();
    };

    window.onTurnstileExpired = function () {
        turnstileValid = false;
        show(document.getElementById("turnstileError"));
        updateSubmit();
    };

    window.onTurnstileError = function () {
        turnstileValid = false;
        show(document.getElementById("turnstileError"));
        updateSubmit();
    };

    // ======= Elements =======
    const form = document.getElementById("loginForm");
    const submitBtn = document.getElementById("submitBtn");
    const uiError = document.getElementById("uiError");

    const cedula = document.getElementById("cedula");
    const cedulaError = document.getElementById("cedulaError");

    const cd = document.getElementById("codigoDactilar");
    const cdError = document.getElementById("cdError");

    const termsCheck = document.getElementById("termsAcceptance");

    // Modal
    const modal = document.getElementById("termsModal");
    const openTermsBtn = document.getElementById("openTermsBtn");
    const acceptTermsBtn = document.getElementById("acceptTermsBtn");
    const closeBtns = document.querySelectorAll("[data-terms-close]");

    // ======= Helpers =======
    function show(el) { if (el) el.classList.remove("vs-hidden"); }
    function hide(el) { if (el) el.classList.add("vs-hidden"); }

    function openModal() {
        if (!modal) return;
        modal.classList.remove("vs-modal--hidden");
        // foco al botón de cerrar o aceptar
        const close = modal.querySelector("[data-terms-close]");
        if (close) close.focus();
    }

    function closeModal() {
        if (!modal) return;
        modal.classList.add("vs-modal--hidden");
        if (openTermsBtn) openTermsBtn.focus();
    }

    function validateCedula() {
        if (!cedula) return true;
        const v = (cedula.value || "").replace(/\D/g, "").slice(0, 10);
        if (cedula.value !== v) cedula.value = v;

        const ok = v.length === 10;
        if (cedulaError) cedulaError.classList.toggle("vs-hidden", ok);
        cedula.classList.toggle("vs-field--invalid", !ok);
        cedula.classList.toggle("vs-field--valid", ok);

        return ok;
    }

    function validateCodigoDactilar() {
        if (!cd) return true;

        // Limpiamos a letras/números, y mayúsculas
        let v = (cd.value || "").replace(/[^A-Za-z0-9]/g, "").slice(0, 10).toUpperCase();
        cd.value = v;

        // Tu regla (sin 0): 1 letra + 4 números (1-9) + 1 letra + 4 números (1-9)
        const ok = /^[A-Z][1-9]{4}[A-Z][1-9]{4}$/.test(v);

        // si aún no completa 10, no lo marco como error “rojo”, solo neutro
        const isComplete = v.length === 10;

        if (cdError) cdError.classList.toggle("vs-hidden", ok || !isComplete);
        cd.classList.toggle("vs-field--invalid", isComplete && !ok);
        cd.classList.toggle("vs-field--valid", isComplete && ok);

        return ok;
    }

    function updateSubmit() {
        if (!submitBtn || !termsCheck) return;
        const ok = termsCheck.checked && turnstileValid;
        submitBtn.disabled = !ok;
    }

    // ======= Events =======
    if (openTermsBtn) openTermsBtn.addEventListener("click", openModal);
    if (acceptTermsBtn) {
        acceptTermsBtn.addEventListener("click", () => {
            if (termsCheck) termsCheck.checked = true;
            updateSubmit();
            closeModal();
        });
    }
    closeBtns.forEach(btn => btn.addEventListener("click", closeModal));

    // Cerrar modal con ESC / click overlay
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") closeModal();
    });
    if (modal) {
        modal.addEventListener("click", (e) => {
            if (e.target === modal) closeModal();
        });
    }

    if (termsCheck) termsCheck.addEventListener("change", updateSubmit);

    if (cedula) {
        cedula.addEventListener("input", validateCedula);
        cedula.addEventListener("blur", validateCedula);
    }

    if (cd) {
        cd.addEventListener("input", validateCodigoDactilar);
        cd.addEventListener("blur", validateCodigoDactilar);
    }

    if (form) {
        form.addEventListener("submit", (e) => {
            hide(uiError);

            const okCed = validateCedula();
            const okCd = validateCodigoDactilar();
            const okTerms = !!(termsCheck && termsCheck.checked);
            const okTurn = turnstileValid;

            // Si algo falla, bloquea y muestra mensajes
            if (!(okCed && okCd && okTerms && okTurn)) {
                e.preventDefault();
                show(uiError);
                if (!okTurn) show(document.getElementById("turnstileError"));
                if (!okCed && cedula) cedula.focus();
                else if (!okCd && cd) cd.focus();
                else if (!okTerms && termsCheck) termsCheck.focus();
            }
        });
    }

    // Mostrar términos una sola vez por sesión (opcional, como tú querías)
    try {
        const seen = sessionStorage.getItem("termsShown");
        if (!seen) {
            openModal();
            sessionStorage.setItem("termsShown", "true");
        }
    } catch (_) {}

    // Inicial
    validateCedula();
    validateCodigoDactilar();
    updateSubmit();
})();
