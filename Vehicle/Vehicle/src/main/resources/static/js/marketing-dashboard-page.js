const token = sessionStorage.getItem("jwtToken");
const role = sessionStorage.getItem("userRole");
const userName = sessionStorage.getItem("userName");
const MARKETING_BADGE_STATE_KEY = `marketing-dashboard-badges:${userName || "marketing-manager"}`;

let promotionsCache = [];
let totalActivePromos = 0;
let vehicleTargetCatalog = [];
let marketingChartInstance = null;

document.addEventListener("DOMContentLoaded", initMarketingDashboard);

async function initMarketingDashboard() {
    if (!token || (role !== "MARKETING_MANAGER" && role !== "MAIN_ADMIN")) {
        window.location.href = "login.html";
        return;
    }

    const welcomeEl = document.getElementById("welcomeMessage");
    if (welcomeEl) {
        const workspaceLabel = role === "MAIN_ADMIN" ? "promotion control desk" : "campaign studio";
        welcomeEl.textContent = userName ? `${userName}'s ${workspaceLabel}` : "Marketing campaign studio";
    }

    const roleBadge = document.querySelector(".marketing-role-badge");
    if (roleBadge && role === "MAIN_ADMIN") {
        roleBadge.innerHTML = `<i class="fas fa-crown"></i> Main Admin Access`;
    }

    const panelTitle = document.querySelector(".marketing-sidebar-panel h2");
    const panelCopy = document.querySelector(".marketing-sidebar-panel p");
    if (panelTitle && panelCopy && role === "MAIN_ADMIN") {
        panelTitle.textContent = "Promotion control";
        panelCopy.textContent = "Manage promotions directly while covering campaign operations or during marketing staff handovers.";
    }

    setDateConstraints();
    bindTargetingInteractions();

    await Promise.all([
        fetchVehicleTargetOptions(),
        fetchPromotions()
    ]);

    fetchMarketingStats();
}

function switchTab(tabId, element) {
    document.querySelectorAll(".marketing-section").forEach(section => section.classList.remove("active"));
    document.querySelectorAll(".marketing-sidebar-link").forEach(link => link.classList.remove("active"));

    const section = document.getElementById(tabId);
    if (section) {
        section.classList.add("active");
    }

    if (element) {
        element.classList.add("active");
    }

    markMarketingSectionSeen(tabId);
    updateMarketingSidebarBadges();
}

function getMarketingBadgeState() {
    try {
        return JSON.parse(sessionStorage.getItem(MARKETING_BADGE_STATE_KEY)) || {};
    } catch (error) {
        return {};
    }
}

function saveMarketingBadgeState(state) {
    sessionStorage.setItem(MARKETING_BADGE_STATE_KEY, JSON.stringify(state));
}

function buildMarketingOverviewSignatures() {
    return promotionsCache
        .filter(promotion =>
            promotion.showOnInventoryBanner === true &&
            promotion.status !== "Disabled" &&
            promotion.status !== "Expired"
        )
        .map(promotion => `${promotion.id}:${promotion.status || ""}:${promotion.startDate || ""}:${promotion.endDate || ""}:${promotion.discountPercentage || ""}`)
        .sort();
}

function buildMarketingPromoSignatures() {
    return promotionsCache
        .map(promotion => `${promotion.id}:${promotion.status || ""}:${promotion.updatedDate || ""}:${promotion.startDate || ""}:${promotion.endDate || ""}:${promotion.discountPercentage || ""}`)
        .sort();
}

function ensureMarketingBadgeStateInitialized() {
    const state = getMarketingBadgeState();
    if (state.__initialized) {
        return;
    }

    state.__initialized = true;
    state.overviewSection = buildMarketingOverviewSignatures();
    state.managePromosSection = buildMarketingPromoSignatures();
    saveMarketingBadgeState(state);
}

function markMarketingSectionSeen(tabId) {
    const state = getMarketingBadgeState();
    state.__initialized = true;

    if (tabId === "overviewSection") {
        state.overviewSection = buildMarketingOverviewSignatures();
    }

    if (tabId === "managePromosSection") {
        state.managePromosSection = buildMarketingPromoSignatures();
    }

    saveMarketingBadgeState(state);
}

function markActiveMarketingSectionSeen() {
    const activeSection = document.querySelector(".marketing-section.active");
    if (!activeSection) {
        return;
    }

    markMarketingSectionSeen(activeSection.id);
}

function countUnreadMarketingItems(tabId) {
    const state = getMarketingBadgeState();
    const seenSignatures = new Set(Array.isArray(state[tabId]) ? state[tabId] : []);
    const currentSignatures = tabId === "overviewSection"
        ? buildMarketingOverviewSignatures()
        : buildMarketingPromoSignatures();

    return currentSignatures.filter(signature => !seenSignatures.has(signature)).length;
}

function setDateConstraints() {
    const today = getLocalDateInputValue();
    const addStartField = document.getElementById("pStart");
    if (addStartField) {
        addStartField.min = today;
        if (!addStartField.value) {
            addStartField.value = today;
        }
    }

    const addEndField = document.getElementById("pEnd");
    if (addEndField && !addEndField.value) {
        addEndField.value = addDaysToDateString(today, 7);
    }

    syncEndDateConstraint("p");
}

function configureEditDateConstraints(existingStartDate = "", existingEndDate = "") {
    const today = getLocalDateInputValue();
    const startField = document.getElementById("eStart");
    const endField = document.getElementById("eEnd");

    if (!startField || !endField) {
        return;
    }

    const effectiveMinStartDate = existingStartDate && existingStartDate < today
        ? existingStartDate
        : today;

    startField.min = effectiveMinStartDate;

    if (!startField.value) {
        startField.value = existingStartDate || today;
    }

    if (!endField.value) {
        endField.value = existingEndDate || addDaysToDateString(startField.value || today, 7);
    }

    syncEndDateConstraint("e");
}

function bindTargetingInteractions() {
    ["p", "e"].forEach(prefix => {
        const allVehicles = document.getElementById(prefix + "AllVehicles");
        const brandField = document.getElementById(prefix + "Brand");
        const startField = document.getElementById(prefix + "Start");
        const discountField = document.getElementById(prefix + "Discount");

        if (allVehicles) {
            allVehicles.addEventListener("change", () => toggleTargetingFields(prefix));
        }

        if (brandField) {
            brandField.addEventListener("input", () => updateModelOptions(prefix));
            brandField.addEventListener("change", () => updateModelOptions(prefix));
        }

        if (startField) {
            startField.addEventListener("change", () => syncEndDateConstraint(prefix));
        }

        if (discountField) {
            discountField.addEventListener("input", () => sanitizeDiscountField(discountField));
        }

        toggleTargetingFields(prefix);
    });
}

function toggleTargetingFields(prefix) {
    const checkbox = document.getElementById(prefix + "AllVehicles");
    const disabled = checkbox && checkbox.checked;

    document.querySelectorAll(".target-field-" + prefix).forEach(field => {
        field.disabled = disabled;
        if (disabled) {
            field.value = "";
        }
    });

    updateModelOptions(prefix);
}

function syncEndDateConstraint(prefix) {
    const startField = document.getElementById(prefix + "Start");
    const endField = document.getElementById(prefix + "End");
    if (!startField || !endField) {
        return;
    }

    const today = getLocalDateInputValue();
    endField.min = startField.value || today;
    if (endField.value && endField.value < endField.min) {
        endField.value = endField.min;
    }
}

function sanitizeDiscountField(field) {
    if (!field) {
        return;
    }

    const value = field.value;
    if (value === "") {
        return;
    }

    const numericValue = Number(value);
    if (!Number.isFinite(numericValue)) {
        field.value = "";
        return;
    }

    if (numericValue < 0) {
        field.value = "0";
    }
}

async function fetchVehicleTargetOptions() {
    try {
        const res = await fetch("/api/vehicles/all");
        if (!res.ok) {
            return;
        }

        vehicleTargetCatalog = await res.json();
        const brands = [...new Set(vehicleTargetCatalog.map(vehicle => (vehicle.brand || "").trim()).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b));

        document.getElementById("promoBrandOptions").innerHTML = brands
            .map(brand => `<option value="${escapeHtml(brand)}"></option>`)
            .join("");

        updateModelOptions("p");
        updateModelOptions("e");
    } catch (error) {
        console.error("Failed to load vehicle targeting options", error);
    }
}

function updateModelOptions(prefix) {
    const checkbox = document.getElementById(prefix + "AllVehicles");
    const brandField = document.getElementById(prefix + "Brand");
    const modelField = document.getElementById(prefix + "Model");
    const datalist = document.getElementById(prefix === "p" ? "promoModelOptions" : "promoEditModelOptions");

    if (!brandField || !modelField || !datalist) {
        return;
    }

    const brandValue = (brandField.value || "").trim().toLowerCase();
    const models = [...new Set(vehicleTargetCatalog
        .filter(vehicle => !brandValue || (vehicle.brand || "").trim().toLowerCase() === brandValue)
        .map(vehicle => (vehicle.model || "").trim())
        .filter(Boolean))]
        .sort((a, b) => a.localeCompare(b));

    datalist.innerHTML = models
        .map(model => `<option value="${escapeHtml(model)}"></option>`)
        .join("");

    if (checkbox && checkbox.checked) {
        modelField.value = "";
    }
}

async function fetchPromotions() {
    try {
        const res = await fetch("/api/promotions/all", {
            headers: { Authorization: "Bearer " + token }
        });

        if (!res.ok) {
            throw new Error("Failed to load promotions.");
        }

        promotionsCache = await res.json();
        totalActivePromos = promotionsCache.filter(promotion => promotion.status === "Active").length;

        ensureMarketingBadgeStateInitialized();
        markActiveMarketingSectionSeen();
        updateHeroMetrics();
        updateMarketingSidebarBadges();
        renderPromotionsTable();
        renderOverviewCampaigns();
    } catch (error) {
        console.error(error);
        showToast("Failed to load promotions.", "error");
        document.getElementById("promosTableBody").innerHTML =
            `<tr><td colspan="7"><div class="marketing-empty-state">Failed to load campaigns.</div></td></tr>`;
    }
}

function updateHeroMetrics(totalUsers = null, totalVehicles = null) {
    document.getElementById("heroActivePromos").textContent = String(totalActivePromos);
    document.getElementById("heroTargetedPromos").textContent = String(
        promotionsCache.filter(promotion => !promotion.appliesToAllVehicles).length
    );
    document.getElementById("heroBannerPromos").textContent = String(
        promotionsCache.filter(promotion => promotion.showOnInventoryBanner && promotion.status !== "Disabled" && promotion.status !== "Expired").length
    );
    document.getElementById("heroTotalVehicles").textContent = String(totalVehicles ?? vehicleTargetCatalog.length);

    document.getElementById("statTotalUsers").textContent = String(totalUsers ?? 0);
    document.getElementById("statTotalPromos").textContent = String(totalActivePromos);
    document.getElementById("statTotalVehicles").textContent = String(totalVehicles ?? vehicleTargetCatalog.length);
}

function updateMarketingSidebarBadges() {
    setMarketingSidebarBadge("marketingOverviewBadge", countUnreadMarketingItems("overviewSection"));
    setMarketingSidebarBadge("marketingPromosBadge", countUnreadMarketingItems("managePromosSection"));
}

function setMarketingSidebarBadge(id, count) {
    const badge = document.getElementById(id);
    if (!badge) {
        return;
    }

    const safeCount = Math.max(0, Number(count) || 0);
    badge.textContent = safeCount > 99 ? "99+" : String(safeCount);
    badge.hidden = safeCount === 0;
}

function renderOverviewCampaigns() {
    const container = document.getElementById("overviewCampaignGrid");
    const activeBannerPromotions = promotionsCache
        .filter(promotion =>
            promotion.showOnInventoryBanner === true &&
            promotion.status === "Active"
        )
        .slice(0, 4);

    const scheduledBannerPromotions = promotionsCache
        .filter(promotion =>
            promotion.showOnInventoryBanner === true &&
            promotion.status === "Scheduled"
        )
        .sort((left, right) => {
            const leftTime = Date.parse(left?.startDate || "") || 0;
            const rightTime = Date.parse(right?.startDate || "") || 0;
            return leftTime - rightTime;
        })
        .slice(0, 4);

    const overviewPromotions = activeBannerPromotions.length
        ? activeBannerPromotions
        : scheduledBannerPromotions;

    if (!overviewPromotions.length) {
        container.innerHTML = `<div class="marketing-empty-state">No live campaigns yet. Launch your first promotion from the promotion management tab.</div>`;
        return;
    }

    container.innerHTML = overviewPromotions.map(promotion => {
        const chips = [];
        if (promotion.appliesToAllVehicles) {
            chips.push("All vehicles");
        } else {
            if (promotion.targetBrand) {
                chips.push(promotion.targetBrand);
            }
            if (promotion.targetModel) {
                chips.push(promotion.targetModel);
            }
        }

        return `
            <article class="marketing-live-item">
                <div class="marketing-live-item-top">
                    <h4>${escapeHtml(promotion.title)}</h4>
                    <span class="marketing-status-pill ${getStatusClass(promotion.status)}">${escapeHtml(promotion.status)}</span>
                </div>
                <p>${escapeHtml(promotion.description || "Campaign is live across the marketplace.")}</p>
                <div class="marketing-chip-row">
                    <span class="marketing-chip"><i class="fas fa-bolt"></i> ${escapeHtml(String(promotion.discountPercentage))}% off</span>
                    ${chips.map(chip => `<span class="marketing-chip">${escapeHtml(chip)}</span>`).join("")}
                    <span class="marketing-chip"><i class="far fa-calendar"></i> ${promotion.status === "Scheduled" ? "Starts" : "Ends"} ${escapeHtml(promotion.status === "Scheduled" ? (promotion.startDate || "") : (promotion.endDate || ""))}</span>
                </div>
            </article>
        `;
    }).join("");
}

function renderPromotionsTable() {
    const tbody = document.getElementById("promosTableBody");

    if (!promotionsCache.length) {
        tbody.innerHTML = `<tr><td colspan="7"><div class="marketing-empty-state">No campaigns found yet.</div></td></tr>`;
        return;
    }

    tbody.innerHTML = promotionsCache.map(promotion => {
        const statusClass = getStatusClass(promotion.status);
        const image = promotion.imageUrl
            ? `<img src="${promotion.imageUrl}" alt="${escapeHtml(promotion.title)}" class="marketing-banner-preview">`
            : `<div class="marketing-banner-placeholder">No banner</div>`;

        const visibility = [
            promotion.showOnInventoryBanner ? "Inventory banner" : null,
            promotion.showOnVehicleCard ? "Vehicle cards" : null,
            promotion.showOnVehicleDetails ? "Vehicle details" : null
        ].filter(Boolean).join(" • ");

        return `
            <tr>
                <td>${image}</td>
                <td>
                    <h4 class="marketing-table-title">${escapeHtml(promotion.title)}</h4>
                    <div class="marketing-discount"><i class="fas fa-bolt"></i> ${escapeHtml(String(promotion.discountPercentage))}% OFF</div>
                    ${promotion.highlightLabel ? `<div class="marketing-label-pill"><span>${escapeHtml(promotion.highlightLabel)}</span></div>` : ""}
                    <p class="marketing-table-copy">${escapeHtml(promotion.description || "")}</p>
                </td>
                <td>
                    <div class="marketing-target-summary">${escapeHtml(promotion.targetSummary || "All vehicles")}</div>
                    <div class="marketing-subtext">${promotion.appliesToAllVehicles ? "Global campaign" : "Brand-led targeted campaign"}</div>
                </td>
                <td>
                    <div class="marketing-target-summary">${escapeHtml(visibility || "Hidden from customer pages")}</div>
                    <div class="marketing-subtext">Created for guest and customer inventory visibility.</div>
                </td>
                <td>
                    <div class="marketing-target-summary">Starts ${escapeHtml(promotion.startDate || "")}</div>
                    <div class="marketing-subtext">Ends ${escapeHtml(promotion.endDate || "")}</div>
                </td>
                <td><span class="marketing-status-pill ${statusClass}">${escapeHtml(promotion.status || "Unknown")}</span></td>
                <td>
                    <div class="marketing-actions">
                        <button type="button" class="marketing-icon-btn edit" title="Edit campaign" onclick="openEditPromo(${promotion.id})"><i class="fas fa-pen"></i></button>
                        ${promotion.status === "Disabled" || promotion.status === "Expired"
                            ? `<button type="button" class="marketing-icon-btn resume" title="Activate campaign" onclick="updateStatus(${promotion.id}, 'Active')"><i class="fas fa-play"></i></button>`
                            : `<button type="button" class="marketing-icon-btn pause" title="Disable campaign" onclick="updateStatus(${promotion.id}, 'Disabled')"><i class="fas fa-pause"></i></button>`}
                        <button type="button" class="marketing-icon-btn delete" title="Delete campaign" onclick="deletePromo(${promotion.id})"><i class="fas fa-trash"></i></button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

async function fetchMarketingStats() {
    try {
        const res = await fetch("/api/admin/stats", {
            headers: { Authorization: "Bearer " + token }
        });

        if (!res.ok) {
            return;
        }

        const stats = await res.json();
        updateHeroMetrics(stats.totalUsers, stats.totalVehicles);
        drawMarketingChart(stats.totalUsers, stats.totalVehicles, totalActivePromos);
    } catch (error) {
        console.error("Failed to load marketing stats", error);
    }
}

function drawMarketingChart(users, vehicles, promotions) {
    const canvas = document.getElementById("marketingChart");
    if (!canvas) {
        return;
    }

    if (marketingChartInstance) {
        marketingChartInstance.destroy();
    }

    marketingChartInstance = new Chart(canvas.getContext("2d"), {
        type: "bar",
        data: {
            labels: ["Platform Users", "Inventory Vehicles", "Active Promotions"],
            datasets: [{
                data: [users, vehicles, promotions],
                backgroundColor: ["#3b82f6", "#1d4ed8", "#16a34a"],
                borderRadius: 12,
                maxBarThickness: 52
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: "rgba(148, 163, 184, 0.16)" },
                    ticks: { color: "#64748b" }
                },
                x: {
                    grid: { display: false },
                    ticks: { color: "#334155", font: { weight: 600 } }
                }
            }
        }
    });
}

function showAddForm() {
    const form = document.getElementById("promoForm");
    form.reset();
    document.getElementById("addPromoForm").classList.add("visible");
    document.getElementById("editPromoForm").classList.remove("visible");
    setDateConstraints();
    toggleTargetingFields("p");
    document.getElementById("addPromoForm").scrollIntoView({ behavior: "smooth", block: "start" });
}

function appendPromotionFields(formData, prefix) {
    formData.append("title", document.getElementById(prefix + "Title").value.trim());
    formData.append("description", document.getElementById(prefix + "Desc").value.trim());
    formData.append("discountPercentage", document.getElementById(prefix + "Discount").value);
    formData.append("targetListingType", document.getElementById(prefix + "ListingType").value);
    formData.append("startDate", document.getElementById(prefix + "Start").value);
    formData.append("endDate", document.getElementById(prefix + "End").value);
    formData.append("appliesToAllVehicles", document.getElementById(prefix + "AllVehicles").checked);
    formData.append("targetBrand", document.getElementById(prefix + "Brand").value.trim());
    formData.append("targetModel", document.getElementById(prefix + "Model").value.trim());
    formData.append("highlightLabel", document.getElementById(prefix + "HighlightLabel").value.trim());
    formData.append("showOnInventoryBanner", document.getElementById(prefix + "ShowBanner").checked);
    formData.append("showOnVehicleCard", document.getElementById(prefix + "ShowCard").checked);
    formData.append("showOnVehicleDetails", document.getElementById(prefix + "ShowDetails").checked);
}

function validatePromotionForm(prefix, imageRequired) {
    const title = document.getElementById(prefix + "Title").value.trim();
    const description = document.getElementById(prefix + "Desc").value.trim();
    const discount = Number(document.getElementById(prefix + "Discount").value);
    const listingType = document.getElementById(prefix + "ListingType").value;
    const startDate = document.getElementById(prefix + "Start").value;
    const endDate = document.getElementById(prefix + "End").value;
    const allVehicles = document.getElementById(prefix + "AllVehicles").checked;
    const brand = document.getElementById(prefix + "Brand").value.trim();
    const model = document.getElementById(prefix + "Model").value.trim();
    const image = document.getElementById(prefix + "Image").files[0];
    const showBanner = document.getElementById(prefix + "ShowBanner").checked;
    const showCard = document.getElementById(prefix + "ShowCard").checked;
    const showDetails = document.getElementById(prefix + "ShowDetails").checked;
    const today = getLocalDateInputValue();

    if (!title) {
        return "Campaign title is required.";
    }
    if (!description) {
        return "Campaign description is required.";
    }
    if (discount < 0) {
        return "Discount percentage cannot be a negative value.";
    }
    if (!Number.isFinite(discount) || discount < 1 || discount > 100) {
        return "Discount percentage must be between 1 and 100.";
    }
    if (!listingType) {
        return "Choose whether the promotion applies to sale vehicles or rent vehicles.";
    }
    if (!startDate || !endDate) {
        return "Start date and end date are required.";
    }
    if (prefix === "p" && startDate < today) {
        return "Promotion start date must be today or a future date.";
    }
    if (endDate < startDate) {
        return "Promotion end date cannot be before the start date.";
    }
    if (!allVehicles && !brand) {
        return "Choose a vehicle brand or select 'Apply to all vehicles'.";
    }
    if (model && !brand) {
        return "Choose a vehicle brand before selecting a model.";
    }
    if (!showBanner && !showCard && !showDetails) {
        return "Select at least one display location for the promotion.";
    }
    if (imageRequired && !image) {
        return "Promotion banner image is required.";
    }

    return null;
}

async function submitPromotion(event) {
    event.preventDefault();

    const validationError = validatePromotionForm("p", true);
    if (validationError) {
        showToast(validationError, "error");
        return;
    }

    const button = document.getElementById("submitPromoBtn");
    button.disabled = true;
    button.innerHTML = `<i class="fas fa-spinner fa-spin"></i> Launching campaign`;

    const formData = new FormData();
    appendPromotionFields(formData, "p");
    formData.append("image", document.getElementById("pImage").files[0]);

    try {
        const res = await fetch("/api/promotions/add", {
            method: "POST",
            headers: { Authorization: "Bearer " + token },
            body: formData
        });

        if (!res.ok) {
            throw new Error(await res.text());
        }

        document.getElementById("promoForm").reset();
        document.getElementById("addPromoForm").classList.remove("visible");
        toggleTargetingFields("p");
        showToast(buildPromotionSuccessMessage("created", await res.clone().json()), "success");
        await fetchPromotions();
        fetchMarketingStats();
    } catch (error) {
        console.error(error);
        showToast(cleanApiError(error.message, "Failed to launch campaign."), "error");
    } finally {
        button.disabled = false;
        button.innerHTML = `<i class="fas fa-rocket"></i> Launch campaign`;
    }
}

function openEditPromo(id) {
    const promo = promotionsCache.find(item => item.id === id);
    if (!promo) {
        return;
    }

    document.getElementById("ePromoId").value = promo.id;
    document.getElementById("eTitle").value = promo.title || "";
    document.getElementById("eDesc").value = promo.description || "";
    document.getElementById("eDiscount").value = promo.discountPercentage || "";
    document.getElementById("eListingType").value = promo.targetListingType || "";
    document.getElementById("eStart").value = promo.startDate || "";
    document.getElementById("eEnd").value = promo.endDate || "";
    document.getElementById("eAllVehicles").checked = promo.appliesToAllVehicles === true;
    document.getElementById("eBrand").value = promo.targetBrand || "";
    document.getElementById("eModel").value = promo.targetModel || "";
    document.getElementById("eHighlightLabel").value = promo.highlightLabel || "";
    document.getElementById("eShowBanner").checked = promo.showOnInventoryBanner === true;
    document.getElementById("eShowCard").checked = promo.showOnVehicleCard === true;
    document.getElementById("eShowDetails").checked = promo.showOnVehicleDetails === true;
    document.getElementById("eImage").value = "";

    configureEditDateConstraints(promo.startDate || "", promo.endDate || "");
    toggleTargetingFields("e");

    document.getElementById("addPromoForm").classList.remove("visible");
    document.getElementById("editPromoForm").classList.add("visible");
    document.getElementById("editPromoForm").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function submitPromoUpdate(event) {
    event.preventDefault();

    const validationError = validatePromotionForm("e", false);
    if (validationError) {
        showToast(validationError, "error");
        return;
    }

    const button = document.getElementById("submitEditPromoBtn");
    const promoId = document.getElementById("ePromoId").value;

    button.disabled = true;
    button.innerHTML = `<i class="fas fa-spinner fa-spin"></i> Saving changes`;

    const formData = new FormData();
    appendPromotionFields(formData, "e");

    const imageFile = document.getElementById("eImage").files[0];
    if (imageFile) {
        formData.append("image", imageFile);
    }

    try {
        const res = await fetch(`/api/promotions/update/${promoId}`, {
            method: "PUT",
            headers: { Authorization: "Bearer " + token },
            body: formData
        });

        if (!res.ok) {
            throw new Error(await res.text());
        }

        document.getElementById("editPromoFormElement").reset();
        document.getElementById("editPromoForm").classList.remove("visible");
        toggleTargetingFields("e");
        showToast(buildPromotionSuccessMessage("updated", await res.clone().json()), "success");
        await fetchPromotions();
        fetchMarketingStats();
    } catch (error) {
        console.error(error);
        showToast(cleanApiError(error.message, "Failed to update campaign."), "error");
    } finally {
        button.disabled = false;
        button.innerHTML = `<i class="fas fa-save"></i> Save changes`;
    }
}

async function updateStatus(id, newStatus) {
    try {
        const res = await fetch(`/api/promotions/status/${id}`, {
            method: "PUT",
            headers: {
                Authorization: "Bearer " + token,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ status: newStatus })
        });

        if (!res.ok) {
            throw new Error(await res.text());
        }

        showToast(`Campaign ${newStatus === "Active" ? "activated" : "disabled"} successfully.`, "success");
        await fetchPromotions();
        fetchMarketingStats();
    } catch (error) {
        console.error(error);
        showToast(cleanApiError(error.message, "Failed to update campaign status."), "error");
    }
}

async function deletePromo(id) {
    if (!await window.appConfirm("Delete this campaign permanently?", {
        title: "Delete Campaign",
        type: "danger",
        confirmText: "Delete Campaign",
        cancelText: "Cancel",
        danger: true,
        confirmIcon: "fa-trash"
    })) {
        return;
    }

    try {
        const res = await fetch(`/api/promotions/${id}`, {
            method: "DELETE",
            headers: { Authorization: "Bearer " + token }
        });

        if (!res.ok) {
            throw new Error("Failed to delete campaign.");
        }

        showToast("Campaign deleted successfully.", "success");
        await fetchPromotions();
        fetchMarketingStats();
    } catch (error) {
        console.error(error);
        showToast(cleanApiError(error.message, "Failed to delete campaign."), "error");
    }
}

function closeForm(formId) {
    const form = document.getElementById(formId);
    if (form) {
        form.classList.remove("visible");
    }
}

function cleanApiError(message, fallback) {
    if (!message) {
        return fallback;
    }

    return message
        .replace(/^Error:\s*/i, "")
        .replace(/^Failed to (create|update) promotion:\s*/i, "")
        .trim() || fallback;
}

function buildPromotionSuccessMessage(action, promotion) {
    const status = promotion?.status || "Active";
    if (status === "Scheduled") {
        return `Campaign ${action} successfully. It will appear on customer pages from ${promotion.startDate}.`;
    }
    return `Campaign ${action} successfully and is now visible on active promotion pages.`;
}

function getStatusClass(status) {
    const normalized = String(status || "").toLowerCase();
    if (normalized === "scheduled") {
        return "scheduled";
    }
    if (normalized === "active") {
        return "active";
    }
    if (normalized === "disabled") {
        return "disabled";
    }
    if (normalized === "expired") {
        return "expired";
    }
    return normalized;
}

function getLocalDateInputValue() {
    const now = new Date();
    const offsetMs = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - offsetMs).toISOString().split("T")[0];
}

function addDaysToDateString(dateString, daysToAdd) {
    const date = new Date(dateString + "T00:00:00");
    date.setDate(date.getDate() + daysToAdd);
    return getLocalDateInputValueFromDate(date);
}

function getLocalDateInputValueFromDate(date) {
    const offsetMs = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offsetMs).toISOString().split("T")[0];
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function showToast(message, type = "info") {
    const wrap = document.getElementById("marketingToastWrap");
    if (!wrap) {
        return;
    }

    const toast = document.createElement("div");
    toast.className = `marketing-toast ${type}`;
    toast.textContent = message;
    wrap.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 3200);
}

function logout() {
    ["jwtToken", "userName", "userRole"].forEach((key) => {
        sessionStorage.removeItem(key);
        localStorage.removeItem(key);
    });
    window.location.href = "index.html";
}

window.switchTab = switchTab;
window.showAddForm = showAddForm;
window.submitPromotion = submitPromotion;
window.openEditPromo = openEditPromo;
window.submitPromoUpdate = submitPromoUpdate;
window.updateStatus = updateStatus;
window.deletePromo = deletePromo;
window.logout = logout;
window.closeForm = closeForm;
