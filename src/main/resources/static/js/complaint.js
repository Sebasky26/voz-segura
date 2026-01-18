(function () {
    const form = document.getElementById("complaintForm");
    const detail = document.getElementById("detail");
    const detailCount = document.getElementById("detailCount");
    const detailError = document.getElementById("detailError");

    const fileInput = document.getElementById("evidences");
    const fileList = document.getElementById("fileList");
    const filesError = document.getElementById("filesError");

    const formError = document.getElementById("formError");
    const backBtn = document.querySelector("[data-go-back]");

    if (backBtn) backBtn.addEventListener("click", () => history.back());

    // Reglas UI (NO reemplaza backend)
    const MIN_DETAIL = 50;
    const MAX_FILES = 5;
    const MAX_BYTES = 25 * 1024 * 1024; // 25MB

    let selectedFiles = [];

    function formatFileSize(bytes) {
        if (!bytes) return "0 Bytes";
        const k = 1024;
        const sizes = ["Bytes", "KB", "MB", "GB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        const val = Math.round((bytes / Math.pow(k, i)) * 100) / 100;
        return val + " " + sizes[i];
    }

    function getFileIcon(type) {
        if (!type) return "F";
        if (type.startsWith("image/")) return "▣";
        if (type.startsWith("video/")) return "▶";
        if (type.includes("pdf")) return "P";
        if (type.includes("word") || type.includes("document")) return "D";
        return "F";
    }

    function show(el, yes) {
        if (!el) return;
        el.classList.toggle("vs-hidden", !yes);
    }

    function validateDetail() {
        const len = (detail?.value || "").trim().length;
        if (detailCount) detailCount.textContent = `${len}/${MIN_DETAIL}`;
        const ok = len >= MIN_DETAIL;
        show(detailError, !ok);
        return ok;
    }

    function validateFiles(files) {
        if (!files) return true;

        if (files.length > MAX_FILES) return false;

        for (const f of files) {
            if (f.size > MAX_BYTES) return false;
        }
        return true;
    }

    function renderFiles() {
        if (!fileList) return;
        fileList.innerHTML = "";

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

    if (fileInput) {
        fileInput.addEventListener("change", () => {
            const files = Array.from(fileInput.files || []);

            selectedFiles = files;

            const ok = validateFiles(selectedFiles);
            show(filesError, !ok);

            if (!ok) {
                // Si no cumple, limpiamos para evitar envíos accidentales
                selectedFiles = [];
                fileInput.value = "";
            }

            renderFiles();
        });
    }

    if (form) {
        form.addEventListener("submit", (e) => {
            const okDetail = validateDetail();
            const okFiles = validateFiles(selectedFiles);

            show(filesError, !okFiles);
            show(formError, !(okDetail && okFiles));

            if (!(okDetail && okFiles)) {
                e.preventDefault();
                // foco suave al primer campo con problema
                if (!okDetail && detail) detail.focus();
                else if (!okFiles && fileInput) fileInput.focus();
            }
        });
    }
})();
