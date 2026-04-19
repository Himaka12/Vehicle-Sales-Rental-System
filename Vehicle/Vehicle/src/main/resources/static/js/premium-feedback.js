(function () {
    const STYLE_ID = "premium-feedback-style";
    const ROOT_ID = "premiumFeedbackRoot";

    const toneMap = {
        info: { icon: "fa-circle-info", accent: "#2563eb" },
        success: { icon: "fa-circle-check", accent: "#16a34a" },
        warning: { icon: "fa-triangle-exclamation", accent: "#d97706" },
        danger: { icon: "fa-circle-xmark", accent: "#dc2626" }
    };

    function ensureMounted() {
        if (!document.getElementById(STYLE_ID)) {
            const style = document.createElement("style");
            style.id = STYLE_ID;
            style.textContent = `
                .pf-toast-wrap {
                    position: fixed;
                    top: 28px;
                    right: 28px;
                    z-index: 5000;
                    display: grid;
                    gap: 12px;
                    max-width: min(420px, calc(100vw - 32px));
                }

                .pf-toast {
                    position: relative;
                    overflow: hidden;
                    padding: 16px 18px;
                    border-radius: 22px;
                    border: 1px solid rgba(191, 219, 254, 0.72);
                    background: rgba(255, 255, 255, 0.92);
                    backdrop-filter: blur(18px);
                    box-shadow: 0 24px 42px rgba(15, 23, 42, 0.16);
                    color: #0f172a;
                    transform: translateY(-6px);
                    opacity: 0;
                    transition: opacity 0.22s ease, transform 0.22s ease;
                }

                .pf-toast.is-visible {
                    opacity: 1;
                    transform: translateY(0);
                }

                .pf-toast::before {
                    content: "";
                    position: absolute;
                    inset: 0 auto 0 0;
                    width: 5px;
                    background: var(--pf-accent, #2563eb);
                }

                .pf-toast-head {
                    display: flex;
                    align-items: flex-start;
                    gap: 12px;
                }

                .pf-toast-icon {
                    width: 38px;
                    height: 38px;
                    border-radius: 14px;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    background: color-mix(in srgb, var(--pf-accent, #2563eb) 14%, white);
                    color: var(--pf-accent, #2563eb);
                    flex-shrink: 0;
                    font-size: 1rem;
                }

                .pf-toast-title {
                    font-family: "Outfit", "Inter", sans-serif;
                    font-size: 1rem;
                    font-weight: 800;
                    letter-spacing: -0.03em;
                }

                .pf-toast-message {
                    margin-top: 4px;
                    font-size: 0.92rem;
                    line-height: 1.6;
                    color: #334155;
                }

                .pf-overlay {
                    position: fixed;
                    inset: 0;
                    z-index: 5100;
                    display: none;
                    align-items: center;
                    justify-content: center;
                    padding: 20px;
                    background: rgba(15, 23, 42, 0.30);
                    backdrop-filter: blur(12px);
                }

                .pf-overlay.is-open {
                    display: flex;
                }

                .pf-dialog {
                    width: min(100%, 540px);
                    padding: 28px;
                    border-radius: 28px;
                    background: rgba(255, 255, 255, 0.96);
                    border: 1px solid rgba(191, 219, 254, 0.76);
                    box-shadow: 0 32px 64px rgba(15, 23, 42, 0.22);
                    color: #0f172a;
                }

                .pf-dialog-head {
                    display: flex;
                    gap: 14px;
                    align-items: flex-start;
                }

                .pf-dialog-icon {
                    width: 46px;
                    height: 46px;
                    border-radius: 16px;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    flex-shrink: 0;
                    background: color-mix(in srgb, var(--pf-accent, #2563eb) 14%, white);
                    color: var(--pf-accent, #2563eb);
                    font-size: 1.15rem;
                }

                .pf-dialog-title {
                    font-family: "Outfit", "Inter", sans-serif;
                    font-size: 1.3rem;
                    font-weight: 800;
                    letter-spacing: -0.04em;
                    line-height: 1.15;
                }

                .pf-dialog-message {
                    margin-top: 10px;
                    font-size: 0.95rem;
                    line-height: 1.7;
                    color: #475569;
                    white-space: pre-wrap;
                }

                .pf-dialog-actions {
                    margin-top: 24px;
                    display: flex;
                    flex-wrap: wrap;
                    gap: 12px;
                    justify-content: flex-end;
                }

                .pf-btn {
                    min-height: 50px;
                    padding: 0 20px;
                    border-radius: 18px;
                    border: 1px solid transparent;
                    font-family: "Inter", sans-serif;
                    font-size: 0.95rem;
                    font-weight: 800;
                    cursor: pointer;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    gap: 10px;
                    transition: transform 0.18s ease, filter 0.18s ease, background 0.18s ease;
                }

                .pf-btn:hover {
                    transform: translateY(-1px);
                    filter: brightness(1.03);
                }

                .pf-btn-primary {
                    background: linear-gradient(135deg, #3b82f6, #2563eb);
                    color: #ffffff;
                    box-shadow: 0 16px 30px rgba(37, 99, 235, 0.24);
                }

                .pf-btn-danger {
                    background: linear-gradient(135deg, #ef4444, #dc2626);
                    color: #ffffff;
                    box-shadow: 0 16px 30px rgba(220, 38, 38, 0.22);
                }

                .pf-btn-secondary {
                    background: #ffffff;
                    color: #334155;
                    border-color: rgba(191, 219, 254, 0.8);
                }

                @media (max-width: 640px) {
                    .pf-toast-wrap {
                        top: 16px;
                        right: 16px;
                        left: 16px;
                        max-width: none;
                    }

                    .pf-dialog {
                        padding: 22px;
                        border-radius: 24px;
                    }

                    .pf-dialog-actions {
                        flex-direction: column-reverse;
                    }

                    .pf-btn {
                        width: 100%;
                    }
                }
            `;
            document.head.appendChild(style);
        }

        let root = document.getElementById(ROOT_ID);
        if (!root) {
            root = document.createElement("div");
            root.id = ROOT_ID;
            root.innerHTML = `
                <div class="pf-toast-wrap" id="pfToastWrap"></div>
                <div class="pf-overlay" id="pfOverlay" aria-hidden="true">
                    <div class="pf-dialog" role="dialog" aria-modal="true" aria-live="assertive">
                        <div class="pf-dialog-head">
                            <div class="pf-dialog-icon" id="pfDialogIcon"></div>
                            <div>
                                <div class="pf-dialog-title" id="pfDialogTitle"></div>
                                <div class="pf-dialog-message" id="pfDialogMessage"></div>
                            </div>
                        </div>
                        <div class="pf-dialog-actions" id="pfDialogActions"></div>
                    </div>
                </div>
            `;
            document.body.appendChild(root);
        }

        return {
            toastWrap: document.getElementById("pfToastWrap"),
            overlay: document.getElementById("pfOverlay"),
            dialogIcon: document.getElementById("pfDialogIcon"),
            dialogTitle: document.getElementById("pfDialogTitle"),
            dialogMessage: document.getElementById("pfDialogMessage"),
            dialogActions: document.getElementById("pfDialogActions")
        };
    }

    function getTone(type) {
        return toneMap[type] || toneMap.info;
    }

    function buildButton(label, className, iconClass, onClick) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `pf-btn ${className}`;
        button.innerHTML = `${iconClass ? `<i class="fas ${iconClass}"></i>` : ""}<span>${escapeHtml(label)}</span>`;
        button.addEventListener("click", onClick);
        return button;
    }

    function openDialog(options) {
        const ui = ensureMounted();
        const tone = getTone(options.type);

        ui.overlay.style.setProperty("--pf-accent", tone.accent);
        ui.dialogIcon.innerHTML = `<i class="fas ${tone.icon}"></i>`;
        ui.dialogTitle.textContent = options.title;
        ui.dialogMessage.textContent = options.message;
        ui.dialogActions.innerHTML = "";

        return new Promise((resolve) => {
            const close = (result) => {
                ui.overlay.classList.remove("is-open");
                ui.overlay.setAttribute("aria-hidden", "true");
                document.removeEventListener("keydown", onKeyDown);
                ui.overlay.onclick = null;
                resolve(result);
            };

            const onKeyDown = (event) => {
                if (event.key === "Escape") {
                    close(false);
                }
            };

            if (options.mode === "confirm") {
                ui.dialogActions.appendChild(buildButton(
                    options.cancelText || "Cancel",
                    "pf-btn-secondary",
                    "fa-xmark",
                    () => close(false)
                ));
            }

            ui.dialogActions.appendChild(buildButton(
                options.confirmText || "OK",
                options.danger ? "pf-btn-danger" : "pf-btn-primary",
                options.confirmIcon || "fa-check",
                () => close(true)
            ));

            ui.overlay.onclick = (event) => {
                if (event.target === ui.overlay) {
                    close(options.mode === "confirm" ? false : true);
                }
            };

            document.addEventListener("keydown", onKeyDown);
            ui.overlay.classList.add("is-open");
            ui.overlay.setAttribute("aria-hidden", "false");
        });
    }

    function showToast(message, options = {}) {
        const ui = ensureMounted();
        const tone = getTone(options.type || "info");
        const toast = document.createElement("div");
        toast.className = "pf-toast";
        toast.style.setProperty("--pf-accent", tone.accent);
        toast.innerHTML = `
            <div class="pf-toast-head">
                <div class="pf-toast-icon"><i class="fas ${tone.icon}"></i></div>
                <div>
                    <div class="pf-toast-title">${escapeHtml(options.title || defaultToastTitle(options.type || "info"))}</div>
                    <div class="pf-toast-message">${escapeHtml(message)}</div>
                </div>
            </div>
        `;

        ui.toastWrap.appendChild(toast);
        requestAnimationFrame(() => toast.classList.add("is-visible"));

        const duration = Number(options.duration) > 0 ? Number(options.duration) : 3200;
        setTimeout(() => {
            toast.classList.remove("is-visible");
            setTimeout(() => toast.remove(), 220);
        }, duration);
    }

    function defaultToastTitle(type) {
        switch (type) {
            case "success":
                return "Success";
            case "warning":
                return "Attention";
            case "danger":
                return "Something went wrong";
            default:
                return "Notice";
        }
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    window.appAlert = function (message, options = {}) {
        return openDialog({
            title: options.title || defaultToastTitle(options.type || "info"),
            message: String(message ?? ""),
            type: options.type || "info",
            confirmText: options.confirmText || "OK",
            confirmIcon: options.confirmIcon || "fa-check",
            danger: options.danger === true,
            mode: "alert"
        });
    };

    window.appConfirm = function (message, options = {}) {
        return openDialog({
            title: options.title || "Please confirm",
            message: String(message ?? ""),
            type: options.type || "warning",
            confirmText: options.confirmText || "Yes",
            cancelText: options.cancelText || "Cancel",
            confirmIcon: options.confirmIcon || "fa-check",
            danger: options.danger === true,
            mode: "confirm"
        });
    };

    window.appToast = function (message, options = {}) {
        showToast(message, options);
    };

    window.alert = function (message) {
        const normalized = String(message ?? "").toLowerCase();
        const looksSuccessful = [
            "success",
            "successfully",
            "completed",
            "generated",
            "cancelled",
            "payment successful",
            "marked as completed",
            "deleted from the system"
        ].some(keyword => normalized.includes(keyword)) && !normalized.includes("error");

        if (looksSuccessful) {
            window.appToast(String(message ?? ""), { type: "success", title: "Success" });
            return;
        }

        window.appAlert(message, {
            title: normalized.includes("error") || normalized.includes("failed") ? "Attention Required" : "Notice",
            type: normalized.includes("error") || normalized.includes("failed") ? "danger" : "info",
            confirmText: "OK",
            danger: normalized.includes("error") || normalized.includes("failed")
        });
    };
})();
