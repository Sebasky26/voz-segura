(function () {
    const form = document.getElementById("complaintForm");
    const detail = document.getElementById("detail");
    const detailCount = document.getElementById("detailCount");
    const detailError = document.getElementById("detailError");

    const companyContact = document.getElementById("companyContact");
    const contactError = document.getElementById("contactError");

    const fileInput = document.getElementById("evidences");
    const fileList = document.getElementById("fileList");
    const filesError = document.getElementById("filesError");

    const formError = document.getElementById("formError");

    // Reglas UI (NO reemplaza backend)
    const MIN_DETAIL = 50;
    const MAX_DETAIL = 4000;
    const MAX_FILES = 5;
    const MAX_BYTES = 25 * 1024 * 1024; // 25MB

    let selectedFiles = [];

    // Expresiones regulares para validación de contacto
    const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const PHONE_REGEX = /^(0[2-9]\d{7,8}|0[9]\d{8})$/; // Teléfono Ecuador: convencional 0X-XXXXXXX o celular 09XXXXXXXX

    function formatFileSize(bytes) {
        if (!bytes) return "0 Bytes";
        const k = 1024;
        const sizes = ["Bytes", "KB", "MB", "GB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        const val = Math.round((bytes / Math.pow(k, i)) * 100) / 100;
        return val + " " + sizes[i];
    }

    function getFileIcon(type) {
        if (!type) return "[F]";
        if (type.startsWith("image/")) return "[IMG]";
        if (type.startsWith("video/")) return "[VID]";
        if (type.includes("pdf")) return "[PDF]";
        if (type.includes("word") || type.includes("document")) return "[DOC]";
        return "[F]";
    }

    function show(el, yes) {
        if (!el) return;
        el.classList.toggle("vs-hidden", !yes);
    }

    function validateContact() {
        if (!companyContact) return true;
        
        const value = (companyContact.value || "").trim();
        if (!value) return false;

        // Limpiar guiones y espacios para la validación de teléfono
        const cleanValue = value.replace(/[-\s]/g, "");
        
        // Validar si es email o teléfono
        const isEmail = EMAIL_REGEX.test(value);
        const isPhone = PHONE_REGEX.test(cleanValue);
        
        const ok = isEmail || isPhone;
        
        show(contactError, !ok);
        companyContact.classList.toggle("vs-field--invalid", !ok);
        companyContact.classList.toggle("vs-field--valid", ok);
        
        return ok;
    }

    function validateDetail() {
        const len = (detail?.value || "").trim().length;
        const ok = len >= MIN_DETAIL;

        // Mostrar contador con formato claro
        if (detailCount) {
            if (len < MIN_DETAIL) {
                detailCount.textContent = `${len} caracteres (mínimo ${MIN_DETAIL})`;
                detailCount.classList.add("vs-text-warning");
                detailCount.classList.remove("vs-text-success");
            } else {
                detailCount.textContent = `${len} / ${MAX_DETAIL} caracteres`;
                detailCount.classList.remove("vs-text-warning");
                detailCount.classList.add("vs-text-success");
            }
        }

        show(detailError, !ok);
        return ok;
    }

    function validateFiles(files) {
        if (!files || files.length === 0) return true;

        if (files.length > MAX_FILES) return false;

        for (const f of files) {
            if (f.size > MAX_BYTES) return false;
        }
        return true;
    }

    function renderFiles() {
        if (!fileList) return;
        fileList.innerHTML = "";

        if (selectedFiles.length === 0) {
            fileList.innerHTML = '<p class="vs-small-muted">No hay archivos seleccionados</p>';
            return;
        }

        selectedFiles.forEach((file, index) => {
            const item = document.createElement("div");
            item.className = "vs-file-item";

            const info = document.createElement("div");
            info.className = "vs-file-item__info";

            const icon = document.createElement("span");
            icon.className = "vs-file-item__icon";
            icon.textContent = getFileIcon(file.type);

            const details = document.createElement("div");

            const name = document.createElement("div");
            name.className = "vs-file-item__name";
            name.textContent = file.name;

            const size = document.createElement("div");
            size.className = "vs-file-item__size";
            size.textContent = formatFileSize(file.size);

            details.appendChild(name);
            details.appendChild(size);

            info.appendChild(icon);
            info.appendChild(details);

            const removeBtn = document.createElement("button");
            removeBtn.className = "vs-file-item__remove";
            removeBtn.type = "button";
            removeBtn.setAttribute("aria-label", "Quitar archivo");
            removeBtn.textContent = "×";
            removeBtn.addEventListener("click", () => removeFile(index));

            item.appendChild(info);
            item.appendChild(removeBtn);
            fileList.appendChild(item);
        });

        // Mostrar contador de archivos
        const counter = document.createElement("p");
        counter.className = "vs-small-muted vs-mt-2";
        counter.textContent = `${selectedFiles.length} de ${MAX_FILES} archivos seleccionados`;
        fileList.appendChild(counter);
    }

    function syncInputFiles() {
        if (!fileInput) return;
        const dt = new DataTransfer();
        selectedFiles.forEach(f => dt.items.add(f));
        fileInput.files = dt.files;
    }

    function removeFile(index) {
        selectedFiles.splice(index, 1);
        syncInputFiles();
        renderFiles();
        show(filesError, false);
    }

    if (detail) {
        detail.addEventListener("input", validateDetail);
        detail.addEventListener("blur", validateDetail);
        validateDetail();
    }

    if (companyContact) {
        companyContact.addEventListener("input", validateContact);
        companyContact.addEventListener("blur", validateContact);
    }

    if (fileInput) {
        fileInput.addEventListener("change", () => {
            const newFiles = Array.from(fileInput.files || []);

            // Si es la primera selección, limpiar array anterior
            // ACUMULAR archivos en lugar de reemplazar
            newFiles.forEach(newFile => {
                // Evitar duplicados por nombre
                const exists = selectedFiles.some(f => f.name === newFile.name && f.size === newFile.size);
                if (!exists && selectedFiles.length < MAX_FILES) {
                    selectedFiles.push(newFile);
                }
            });

            const ok = validateFiles(selectedFiles);
            show(filesError, !ok);

            if (!ok) {
                // Si excede el límite, mostrar error pero mantener los archivos válidos
                while (selectedFiles.length > MAX_FILES) {
                    selectedFiles.pop();
                }
            }

            // IMPORTANTE: Sincronizar inmediatamente después de agregar archivos
            syncInputFiles();
            renderFiles();
        });

        // Sincronizar archivos antes de enviar el formulario
        form?.addEventListener("submit", (e) => {
            // Asegurarse de que los archivos estén sincronizados antes de enviar
            syncInputFiles();
        });
    }

    if (form) {
        form.addEventListener("submit", (e) => {
            // Sincronizar archivos antes de validar
            syncInputFiles();
            
            const okDetail = validateDetail();
            const okContact = validateContact();
            const okFiles = validateFiles(selectedFiles);

            show(filesError, !okFiles);
            show(formError, !(okDetail && okContact && okFiles));

            if (!(okDetail && okContact && okFiles)) {
                e.preventDefault();
                // foco suave al primer campo con problema
                if (!okDetail && detail) detail.focus();
                else if (!okContact && companyContact) companyContact.focus();
                else if (!okFiles && fileInput) fileInput.focus();
            }
        });
    }
})();
