(function () {
    "use strict";

    const btn = document.querySelector("[data-toggle-password]");
    const input = document.getElementById("password");

    if (!btn || !input) return;

    btn.addEventListener("click", function () {
        const isHidden = input.type === "password";
        input.type = isHidden ? "text" : "password";
        btn.textContent = isHidden ? "Ocultar" : "Mostrar";
    });
})();
