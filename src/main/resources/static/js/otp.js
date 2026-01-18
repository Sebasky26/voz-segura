(function () {
    const otp = document.getElementById("otpCode");
    const form = document.getElementById("otpForm");
    const uiError = document.getElementById("otpUiError");

    const countdownEl = document.getElementById("countdown");
    const timerText = document.getElementById("timerText");
    const resendForm = document.getElementById("resendForm");

    if (otp) otp.focus();

    function onlyDigits(v) {
        return (v || "").replace(/[^0-9]/g, "");
    }

    function showUiError(show) {
        if (!uiError) return;
        uiError.classList.toggle("vs-hidden", !show);
    }

    if (otp) {
        otp.addEventListener("input", function () {
            const clean = onlyDigits(this.value);
            if (this.value !== clean) this.value = clean;
            showUiError(false);
        });
    }

    if (form) {
        form.addEventListener("submit", function (e) {
            const code = otp ? onlyDigits(otp.value) : "";
            if (code.length !== 6) {
                e.preventDefault();
                showUiError(true);
                if (otp) otp.focus();
            }
        });
    }

    // Reenvío: countdown
    let countdown = 60;
    if (countdownEl && timerText && resendForm) {
        const timer = setInterval(function () {
            countdown--;
            countdownEl.textContent = String(countdown);

            if (countdown <= 0) {
                clearInterval(timer);
                timerText.classList.add("vs-hidden");
                resendForm.classList.remove("vs-hidden");
            }
        }, 1000);
    }
})();
