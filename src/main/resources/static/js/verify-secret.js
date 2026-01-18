(function () {
    const form = document.getElementById('secretForm');
    const input = document.getElementById('secretKey');
    const check = document.getElementById('confirmCheck');
    const btn = document.getElementById('submitBtn');
    const err = document.getElementById('secretError');

    if (!form || !input || !check || !btn) return;

    function isValidKey() {
        return (input.value || '').trim().length >= 8;
    }

    function updateUI() {
        const ok = isValidKey();
        if (err) err.classList.toggle('vs-hidden', ok);
        btn.disabled = !(check.checked && ok);
    }

    // mostrar/ocultar
    document.querySelectorAll('[data-toggle-password]').forEach((b) => {
        const targetId = b.getAttribute('data-toggle-password');
        const target = document.getElementById(targetId);
        if (!target) return;

        b.addEventListener('click', () => {
            const hidden = target.type === 'password';
            target.type = hidden ? 'text' : 'password';
            b.textContent = hidden ? 'Ocultar' : 'Mostrar';
        });
    });

    check.addEventListener('change', updateUI);
    input.addEventListener('input', updateUI);
    input.addEventListener('blur', updateUI);

    form.addEventListener('submit', function (e) {
        // valida en UI (sin alert)
        if (!isValidKey() || !check.checked) {
            e.preventDefault();
            updateUI();
            input.focus();
        }
    });

    updateUI();
})();
