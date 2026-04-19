(function () {
    const SELECTOR = "select:not([multiple]):not([data-native-select]):not(.form-control--hidden)";
    const openWrappers = new Set();

    function closeAllSelects(exceptWrapper) {
        openWrappers.forEach((wrapper) => {
            if (wrapper !== exceptWrapper) {
                wrapper.classList.remove("is-open");
                wrapper.querySelector(".app-select-trigger")?.setAttribute("aria-expanded", "false");
                openWrappers.delete(wrapper);
            }
        });
    }

    function syncSelect(select) {
        const wrapper = select.parentElement?.classList.contains("app-select-wrap") ? select.parentElement : null;
        if (!wrapper) return;

        const trigger = wrapper.querySelector(".app-select-trigger");
        const valueNode = wrapper.querySelector(".app-select-value");
        const menu = wrapper.querySelector(".app-select-menu");
        if (!trigger || !valueNode || !menu) return;

        const selectedOption = select.options[select.selectedIndex];
        valueNode.textContent = selectedOption ? selectedOption.text : "";

        wrapper.classList.toggle("is-disabled", !!select.disabled);
        trigger.disabled = !!select.disabled;

        menu.querySelectorAll(".app-select-option").forEach((button) => {
            const selected = button.dataset.value === select.value;
            button.classList.toggle("is-selected", selected);
            button.setAttribute("aria-selected", selected ? "true" : "false");
        });
    }

    function buildOptions(select) {
        const wrapper = select.parentElement?.classList.contains("app-select-wrap") ? select.parentElement : null;
        if (!wrapper) return;

        const trigger = wrapper.querySelector(".app-select-trigger");
        const menu = wrapper.querySelector(".app-select-menu");
        if (!menu || !trigger) return;

        menu.innerHTML = "";

        Array.from(select.options).forEach((option) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "app-select-option";
            button.dataset.value = option.value;
            button.role = "option";
            button.textContent = option.text;

            if (option.disabled) {
                button.classList.add("is-disabled");
                button.disabled = true;
                button.setAttribute("aria-disabled", "true");
            }

            button.addEventListener("click", function () {
                if (option.disabled) return;
                if (select.value !== option.value) {
                    select.value = option.value;
                    select.dispatchEvent(new Event("change", { bubbles: true }));
                    select.dispatchEvent(new Event("input", { bubbles: true }));
                } else {
                    syncSelect(select);
                }

                wrapper.classList.remove("is-open");
                trigger?.setAttribute("aria-expanded", "false");
                openWrappers.delete(wrapper);
            });

            menu.appendChild(button);
        });

        syncSelect(select);
    }

    function enhanceSelect(select) {
        if (select.dataset.customSelectReady === "true") return;
        if (!select.options || select.size > 1) return;

        const wrapper = document.createElement("div");
        wrapper.className = "app-select-wrap";

        const trigger = document.createElement("button");
        trigger.type = "button";
        trigger.className = "app-select-trigger";
        trigger.setAttribute("aria-haspopup", "listbox");
        trigger.setAttribute("aria-expanded", "false");

        const valueNode = document.createElement("span");
        valueNode.className = "app-select-value";

        const icon = document.createElement("i");
        icon.className = "bi bi-chevron-down app-select-icon";
        icon.setAttribute("aria-hidden", "true");

        trigger.appendChild(valueNode);
        trigger.appendChild(icon);

        const menu = document.createElement("div");
        menu.className = "app-select-menu";
        menu.role = "listbox";

        const parent = select.parentNode;
        parent.insertBefore(wrapper, select);
        wrapper.appendChild(select);
        wrapper.appendChild(trigger);
        wrapper.appendChild(menu);

        select.classList.add("app-select-native");
        select.dataset.customSelectReady = "true";

        trigger.addEventListener("click", function () {
            if (select.disabled) return;
            const willOpen = !wrapper.classList.contains("is-open");
            closeAllSelects(wrapper);
            wrapper.classList.toggle("is-open", willOpen);
            trigger.setAttribute("aria-expanded", willOpen ? "true" : "false");

            if (willOpen) {
                openWrappers.add(wrapper);
            } else {
                openWrappers.delete(wrapper);
            }
        });

        select.addEventListener("change", function () {
            syncSelect(select);
        });

        const observer = new MutationObserver(function () {
            buildOptions(select);
        });

        observer.observe(select, { childList: true, subtree: true, attributes: true });

        let lastValue = select.value;
        let lastOptionCount = select.options.length;
        window.setInterval(function () {
            if (!document.body.contains(select)) return;
            if (select.value !== lastValue || select.options.length !== lastOptionCount) {
                lastValue = select.value;
                lastOptionCount = select.options.length;
                buildOptions(select);
            }
        }, 250);

        buildOptions(select);
    }

    function initCustomSelects(root) {
        (root || document).querySelectorAll(SELECTOR).forEach(enhanceSelect);
    }

    document.addEventListener("click", function (event) {
        openWrappers.forEach((wrapper) => {
            if (!wrapper.contains(event.target)) {
                wrapper.classList.remove("is-open");
                wrapper.querySelector(".app-select-trigger")?.setAttribute("aria-expanded", "false");
                openWrappers.delete(wrapper);
            }
        });
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closeAllSelects();
        }
    });

    document.addEventListener("DOMContentLoaded", function () {
        initCustomSelects(document);
    });

    window.initializeCustomSelects = initCustomSelects;
})();
