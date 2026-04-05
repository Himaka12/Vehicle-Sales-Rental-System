function openAdminShellSidebar() {
    document.body.classList.add("admin-sidebar-open");
}

function closeAdminShellSidebar() {
    document.body.classList.remove("admin-sidebar-open");
}

function toggleAdminShellSidebar() {
    document.body.classList.toggle("admin-sidebar-open");
}

document.addEventListener("DOMContentLoaded", () => {
    const body = document.body;
    if (!body.classList.contains("admin-shell-page")) {
        return;
    }

    const sidebar = document.querySelector(".sidebar");
    const mainContent = document.querySelector(".main-content");
    if (!sidebar || !mainContent) {
        return;
    }

    if (!document.querySelector(".admin-shell-overlay")) {
        const overlay = document.createElement("div");
        overlay.className = "admin-shell-overlay";
        overlay.addEventListener("click", closeAdminShellSidebar);
        body.appendChild(overlay);
    }

    if (!document.querySelector(".admin-shell-mobile-bar")) {
        const mobileBar = document.createElement("div");
        mobileBar.className = "admin-shell-mobile-bar";
        mobileBar.innerHTML = `
            <div class="admin-shell-mobile-bar__inner">
                <button class="admin-shell-menu-btn" type="button" aria-label="Open admin menu">
                    <i class="bi bi-list"></i>
                </button>
                <a href="admin-dashboard.html" class="admin-shell-mobile-brand">
                    <span class="admin-shell-mobile-brand-icon"><i class="bi bi-car-front-fill"></i></span>
                    <span class="admin-shell-mobile-brand-text">
                        <span class="admin-shell-mobile-brand-title">K.D. Auto <span>Traders</span></span>
                        <span class="admin-shell-mobile-brand-sub">Admin workspace</span>
                    </span>
                </a>
                <span class="admin-shell-mobile-pill">Admin</span>
            </div>
        `;
        mobileBar.querySelector(".admin-shell-menu-btn").addEventListener("click", toggleAdminShellSidebar);
        body.insertBefore(mobileBar, body.firstChild);
    }

    sidebar.querySelectorAll("a").forEach((link) => {
        link.addEventListener("click", () => {
            if (window.innerWidth <= 1100) {
                closeAdminShellSidebar();
            }
        });
    });

    window.addEventListener("resize", () => {
        if (window.innerWidth > 1100) {
            closeAdminShellSidebar();
        }
    });
});
