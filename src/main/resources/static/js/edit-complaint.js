(function () {
    "use strict";

    const textarea = document.getElementById('additionalInfo');
    const countSpan = document.getElementById('infoCount');
    const fileInput = document.getElementById('evidences');
    const fileList = document.getElementById('fileList');

    // Contador de caracteres
    function updateCount() {
        if (!textarea || !countSpan) return;
        const len = textarea.value.length;
        countSpan.textContent = len + ' caracteres' + (len < 20 ? ' (mínimo 20)' : '');
    }

    if (textarea) {
        textarea.addEventListener('input', updateCount);
        updateCount();
    }

    // Lista de archivos seleccionados
    function updateFileList() {
        if (!fileInput || !fileList) return;

        fileList.innerHTML = '';
        const files = fileInput.files;

        if (files.length === 0) return;

        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            const size = (file.size / 1024).toFixed(1);

            const item = document.createElement('div');
            item.className = 'vs-file-item';
            item.innerHTML =
                '<div class="vs-file-item__info">' +
                    '<span class="vs-file-item__icon">📄</span>' +
                    '<div>' +
                        '<div class="vs-file-item__name">' + file.name + '</div>' +
                        '<div class="vs-file-item__size">' + size + ' KB</div>' +
                    '</div>' +
                '</div>';
            fileList.appendChild(item);
        }
    }

    if (fileInput) {
        fileInput.addEventListener('change', updateFileList);
    }

    // Validación del formulario
    const form = document.getElementById('editForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            if (textarea && textarea.value.trim().length < 20) {
                e.preventDefault();
                alert('La información debe tener al menos 20 caracteres.');
                textarea.focus();
            }
        });
    }

})();
