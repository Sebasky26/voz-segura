(function () {
    "use strict";

    const retryBtn = document.getElementById("retryBtn");

    if (!retryBtn) return;

    retryBtn.addEventListener("click", function () {
        location.reload();
    });
})();
