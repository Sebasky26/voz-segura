(function () {
    const btn = document.getElementById("copyBtn");
    const text = document.getElementById("trackingText");
    if (!btn || !text) return;

    btn.addEventListener("click", async () => {
        const value = (text.textContent || "").trim();
        if (!value) return;

        try {
            await navigator.clipboard.writeText(value);
            btn.textContent = "Copiado";
            setTimeout(() => (btn.textContent = "Copiar código"), 1200);
        } catch (e) {
            // fallback suave (sin alert)
            btn.textContent = "No se pudo copiar";
            setTimeout(() => (btn.textContent = "Copiar código"), 1400);
        }
    });
})();
