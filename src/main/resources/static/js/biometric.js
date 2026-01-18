(function () {
    const pickBtn = document.getElementById("pickPhotoBtn");
    const input = document.getElementById("photoInput");
    const preview = document.getElementById("photoPreview");
    const placeholder = document.getElementById("cameraPlaceholder");
    const submitBtn = document.getElementById("submitBtn");
    const form = document.getElementById("biometricForm");
    const uiError = document.getElementById("bioUiError");

    if (!input || !preview || !submitBtn || !form || !pickBtn) return;

    function showError(show) {
        if (!uiError) return;
        uiError.classList.toggle("vs-hidden", !show);
    }

    function isValidImage(file) {
        if (!file) return false;
        if (!file.type || !file.type.startsWith("image/")) return false;

        // límite UI (no reemplaza backend). Ajusta si quieres.
        const maxBytes = 3 * 1024 * 1024; // 3MB
        if (file.size > maxBytes) return false;

        return true;
    }

    function setReady(ready) {
        submitBtn.disabled = !ready;
        showError(false);
    }

    pickBtn.addEventListener("click", () => input.click());

    input.addEventListener("change", (event) => {
        const file = event.target.files && event.target.files[0];
        if (!isValidImage(file)) {
            // limpia selección y bloquea continuar
            input.value = "";
            setReady(false);
            showError(true);
            return;
        }

        const reader = new FileReader();
        reader.onload = function (e) {
            preview.src = e.target.result;
            preview.classList.add("active");
            if (placeholder) placeholder.classList.add("hidden");
            setReady(true);
        };
        reader.readAsDataURL(file);
    });

    form.addEventListener("submit", (e) => {
        const file = input.files && input.files[0];
        if (!isValidImage(file)) {
            e.preventDefault();
            setReady(false);
            showError(true);
            pickBtn.focus();
        }
    });

    setReady(false);
})();
