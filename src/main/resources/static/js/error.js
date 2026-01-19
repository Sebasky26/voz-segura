(function () {
    "use strict";

    const retryBtn = document.getElementById("retryBtn");

    if (!retryBtn) return;

    retryBtn.addEventListener("click", function () {
        // Intentar ir a la página anterior o al login
        if (document.referrer && document.referrer.includes(window.location.host)) {
            window.history.back();
        } else {
            window.location.href = "/auth/login";
        }
    });
})();
