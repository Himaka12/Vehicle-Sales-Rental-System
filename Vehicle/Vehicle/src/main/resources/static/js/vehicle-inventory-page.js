const vehicleCacheKeyPrefix = "vehicle-preview-";
const pageSize = 8;
const token = sessionStorage.getItem("jwtToken");

let allVehicles = [];
let filteredVehicles = [];
let livePromotions = [];
let displayedCount = pageSize;
let activeQuickChip = "all";
let currentView = "grid";
let myWishlistIds = [];
let isPremiumUser = false;
let promotionRotationIndex = 0;
let promotionRotationTimer = null;
const promoImageMetaCache = new Map();
const promoImageMetaPending = new Set();

document.addEventListener("DOMContentLoaded", initVehicleInventoryPage);

async function initVehicleInventoryPage() {
    initNavScroll();
    initNavUser();
    bindQuickChips();
    bindControlListeners();
    bindFilterDrawer();
    bindDesktopFilterPin();
    bindWishlistRefresh();
    await Promise.allSettled([fetchCustomerPricingState(), fetchWishlistIds(), fetchVehicles(), fetchPromotionShowcase()]);
}

function initNavScroll() {
    const nav = document.getElementById("mainNav");
    let lastScrollY = window.scrollY;
    const updateScrollState = () => {
        const currentScrollY = window.scrollY;
        nav.classList.toggle("scrolled", currentScrollY > 18);

        if (currentScrollY <= 24) {
            nav.classList.remove("hidden");
        } else if (currentScrollY > lastScrollY + 4) {
            nav.classList.add("hidden");
        } else if (currentScrollY < lastScrollY - 4) {
            nav.classList.remove("hidden");
        }

        lastScrollY = currentScrollY;
    };
    updateScrollState();
    window.addEventListener("scroll", updateScrollState, { passive: true });
}

function initNavUser() {
    const userName = sessionStorage.getItem("userName");
    document.getElementById("navUserName").textContent = userName ? `Hi, ${userName.split(" ")[0]}` : "My Account";
}

function handleLogout() {
    ["jwtToken", "userName", "userRole"].forEach((key) => {
        sessionStorage.removeItem(key);
        localStorage.removeItem(key);
    });
    window.location.href = "login.html";
}

function bindQuickChips() {
    document.querySelectorAll("#quickChipBar .chip").forEach((chip) => {
        chip.addEventListener("click", () => setQuickChip(chip.dataset.chip || "all"));
    });
}

function bindControlListeners() {
    document.getElementById("filterBrand").addEventListener("change", syncModelOptions);
    document.getElementById("sortOption").addEventListener("change", () => applyFilters(false));
}

function bindFilterDrawer() {
    window.addEventListener("resize", () => {
        if (window.innerWidth > 980) {
            closeFilterDrawer();
        }
    });
}

function bindWishlistRefresh() {
    window.addEventListener("pageshow", () => {
        if (token) {
            fetchWishlistIds();
        }
    });

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden && token) {
            fetchWishlistIds();
        }
    });
}

function bindDesktopFilterPin() {
    const updateFilterPin = () => syncDesktopFilterPin();
    updateFilterPin();
    window.addEventListener("scroll", updateFilterPin, { passive: true });
    window.addEventListener("resize", updateFilterPin);
}

async function fetchWishlistIds() {
    if (!token) {
        return;
    }

    try {
        const res = await fetch("/api/wishlist/my-wishlist-ids", {
            headers: { "Authorization": "Bearer " + token }
        });
        if (res.ok) {
            myWishlistIds = await res.json();
            if (filteredVehicles.length || allVehicles.length) {
                renderVehicles();
            }
        }
    } catch (error) {
        console.error("Could not fetch wishlist", error);
    }
}

async function fetchCustomerPricingState() {
    if (!token) {
        return;
    }

    try {
        const res = await fetch("/api/customer/profile", {
            headers: { "Authorization": "Bearer " + token }
        });

        if (res.ok) {
            const profileData = await res.json();
            isPremiumUser = isPremiumProfile(profileData);
            if (filteredVehicles.length || allVehicles.length) {
                renderVehicles();
            }
        }
    } catch (error) {
        console.error("Could not fetch customer pricing state", error);
    }
}

function isPremiumProfile(profileData) {
    return profileData?.isPremium === true || profileData?.premium === true;
}

async function fetchVehicles() {
    const grid = document.getElementById("vehicleGrid");

    try {
        const response = await fetch("/api/vehicles/all");
        if (!response.ok) {
            throw new Error("Failed to fetch vehicles.");
        }

    allVehicles = await response.json();
    populateFilterOptions();
    applyFilters(true);
    renderPromotionShowcase();
    updateHeroStats();
    syncDesktopFilterPin();
    } catch (error) {
        grid.innerHTML = `
            <div class="state-card">
                <div>
                    <i class="bi bi-exclamation-triangle"></i>
                    <h3>Inventory unavailable</h3>
                    <p>We could not load the inventory right now. Please refresh and try again.</p>
                </div>
            </div>
        `;
        updateHeroStats();
    }
}

async function fetchPromotionShowcase() {
    try {
        const response = await fetch("/api/promotions/showcase");
        if (!response.ok) {
            throw new Error("Failed to fetch promotions.");
        }

        livePromotions = await response.json();
        renderPromotionShowcase();
        updateHeroStats();
    } catch (error) {
        livePromotions = [];
        renderPromotionShowcase();
        updateHeroStats();
    }
}

function updateHeroStats() {
    const ecoVehicles = allVehicles.filter((vehicle) => {
        const fuelType = (vehicle.fuelType || "").toLowerCase();
        return fuelType === "hybrid" || fuelType === "electric";
    }).length;

    const heroVehicleCount = document.getElementById("heroVehicleCount");
    const heroOfferCount = document.getElementById("heroOfferCount");
    const heroEcoCount = document.getElementById("heroEcoCount");
    const promoMiniCount = document.getElementById("promoMiniCount");

    if (heroVehicleCount) heroVehicleCount.textContent = allVehicles.length;
    if (heroOfferCount) heroOfferCount.textContent = livePromotions.length;
    if (heroEcoCount) heroEcoCount.textContent = ecoVehicles;
    if (promoMiniCount) promoMiniCount.textContent = `${livePromotions.length} offer${livePromotions.length === 1 ? "" : "s"}`;
}

function populateFilterOptions() {
    populateSelect("filterBrand", uniqueSorted(allVehicles.map((vehicle) => vehicle.brand)), "All Brands");
    populateSelect("filterFuel", uniqueSorted(allVehicles.map((vehicle) => vehicle.fuelType)), "Any Fuel Type");
    syncModelOptions();
}

function populateSelect(selectId, values, defaultLabel) {
    const select = document.getElementById(selectId);
    const currentValue = select.value;
    select.innerHTML = `<option value="any">${escapeHtml(defaultLabel)}</option>`;

    values.forEach((value) => {
        if (!value) {
            return;
        }
        select.insertAdjacentHTML("beforeend", `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`);
    });

    select.value = values.includes(currentValue) ? currentValue : "any";
}

function syncModelOptions() {
    const brand = document.getElementById("filterBrand").value;
    const modelSelect = document.getElementById("filterModel");
    const currentValue = modelSelect.value;

    const models = uniqueSorted(
        allVehicles
            .filter((vehicle) => brand === "any" || (vehicle.brand || "").toLowerCase() === brand.toLowerCase())
            .map((vehicle) => vehicle.model)
    );

    modelSelect.innerHTML = `<option value="any">All Models</option>`;
    models.forEach((model) => {
        modelSelect.insertAdjacentHTML("beforeend", `<option value="${escapeHtml(model)}">${escapeHtml(model)}</option>`);
    });
    modelSelect.value = models.includes(currentValue) ? currentValue : "any";
}

function setQuickChip(chipKey) {
    activeQuickChip = chipKey;
    document.querySelectorAll("#quickChipBar .chip").forEach((chip) => {
        chip.classList.toggle("active", chip.dataset.chip === chipKey);
    });

    if (chipKey === "budget") {
        document.getElementById("filterPrice").value = "budget";
    } else if (document.getElementById("filterPrice").value === "budget") {
        document.getElementById("filterPrice").value = "any";
    }

    applyFilters(true);
}

function setView(view) {
    currentView = view === "list" ? "list" : "grid";
    document.getElementById("vehicleGrid").classList.toggle("list-view", currentView === "list");
    document.getElementById("gridViewBtn").classList.toggle("active", currentView === "grid");
    document.getElementById("listViewBtn").classList.toggle("active", currentView === "list");
}

function applyFilters(resetDisplayed) {
    if (resetDisplayed) {
        displayedCount = pageSize;
    }

    const purpose = document.getElementById("filterPurpose").value;
    const brand = document.getElementById("filterBrand").value;
    const model = document.getElementById("filterModel").value;
    const fuel = document.getElementById("filterFuel").value;
    const condition = document.getElementById("filterCondition").value;
    const priceRange = document.getElementById("filterPrice").value;
    const sort = document.getElementById("sortOption").value;

    filteredVehicles = allVehicles.filter((vehicle) => {
        if (purpose !== "any" && (vehicle.listingType || "") !== purpose) return false;
        if (brand !== "any" && (vehicle.brand || "").toLowerCase() !== brand.toLowerCase()) return false;
        if (model !== "any" && (vehicle.model || "").toLowerCase() !== model.toLowerCase()) return false;
        if (fuel !== "any" && (vehicle.fuelType || "").toLowerCase() !== fuel.toLowerCase()) return false;
        if (condition !== "any" && (vehicle.vehicleCondition || "").toLowerCase() !== condition.toLowerCase()) return false;
        if (!matchesPriceRange(vehicle, priceRange)) return false;
        if (!matchesQuickChip(vehicle)) return false;
        return true;
    });

    sortVehicles(filteredVehicles, sort);
    renderVehicles();
    syncDesktopFilterPin();
    closeFilterDrawer();
}

function sortVehicles(vehicles, sort) {
    if (sort === "lowHigh") {
        vehicles.sort((a, b) => getDisplayPrice(a) - getDisplayPrice(b));
        return;
    }
    if (sort === "highLow") {
        vehicles.sort((a, b) => getDisplayPrice(b) - getDisplayPrice(a));
        return;
    }
    if (sort === "promo") {
        vehicles.sort((a, b) => {
            const promoA = a.appliedPromotion || {};
            const promoB = b.appliedPromotion || {};
            return (promoB.discountPercentage || 0) - (promoA.discountPercentage || 0)
                || (Number(b.id) || 0) - (Number(a.id) || 0);
        });
        return;
    }

    vehicles.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
}

function renderVehicles() {
    const grid = document.getElementById("vehicleGrid");
    const visibleVehicles = filteredVehicles.slice(0, displayedCount);
    setView(currentView);
    updateResultsHeader(visibleVehicles.length);

    if (!allVehicles.length) {
        return;
    }

    if (!filteredVehicles.length) {
        grid.innerHTML = `
            <div class="state-card">
                <div>
                    <i class="bi bi-search"></i>
                    <h3>No vehicles match this search</h3>
                    <p>Adjust your filters, quick chips, or price range to discover more vehicles and live promotions.</p>
                </div>
            </div>
        `;
        updateLoadMoreButton();
        return;
    }

    grid.innerHTML = visibleVehicles.map(buildVehicleCard).join("");
    updateLoadMoreButton();
    syncDesktopFilterPin();
}

function updateResultsHeader(visibleCount) {
    const resultsCount = document.getElementById("resultsCount");
    const resultsCaption = document.getElementById("resultsCaption");
    if (resultsCount) {
        resultsCount.textContent = filteredVehicles.length;
    }
    if (resultsCaption) {
        resultsCaption.textContent = "";
    }
}

function updateLoadMoreButton() {
    document.getElementById("loadMoreBtn").hidden = displayedCount >= filteredVehicles.length;
}

function loadMoreVehicles() {
    displayedCount += pageSize;
    renderVehicles();
}

function resetFilters() {
    document.getElementById("filterPurpose").value = "any";
    document.getElementById("filterBrand").value = "any";
    document.getElementById("filterFuel").value = "any";
    document.getElementById("filterCondition").value = "any";
    document.getElementById("filterPrice").value = "any";
    document.getElementById("sortOption").value = "newest";
    syncModelOptions();
    setQuickChip("all");
    closeFilterDrawer();
}

function openFilterDrawer() {
    if (window.innerWidth > 980) {
        return;
    }
    document.getElementById("filterSidebar").classList.add("open");
    document.getElementById("filterDrawerBackdrop").classList.add("visible");
    document.body.classList.add("drawer-open");
}

function closeFilterDrawer() {
    const sidebar = document.getElementById("filterSidebar");
    const backdrop = document.getElementById("filterDrawerBackdrop");
    if (sidebar) {
        sidebar.classList.remove("open");
    }
    if (backdrop) {
        backdrop.classList.remove("visible");
    }
    document.body.classList.remove("drawer-open");
}

function syncDesktopFilterPin() {
    const sidebar = document.getElementById("filterSidebar");
    const panel = sidebar?.querySelector(".filter-sidebar-sticky");
    const layout = document.querySelector(".inventory-layout");
    const nav = document.getElementById("mainNav");

    if (!sidebar || !panel || !layout || !nav) {
        return;
    }

    if (window.innerWidth <= 980) {
        resetDesktopFilterStyles(sidebar, panel);
        return;
    }

    const navOffset = nav.getBoundingClientRect().bottom + 12;
    const scrollY = window.scrollY;
    const sidebarRect = sidebar.getBoundingClientRect();
    const sidebarTop = sidebarRect.top + scrollY;
    const panelHeight = panel.scrollHeight;
    const pinStart = sidebarTop - navOffset;

    sidebar.style.minHeight = `${panelHeight}px`;

    if (scrollY <= pinStart) {
        resetDesktopFilterStyles(sidebar, panel);
        return;
    }

    panel.style.position = "fixed";
    panel.style.top = `${navOffset}px`;
    panel.style.left = `${Math.max(16, sidebarRect.left - 20)}px`;
    panel.style.width = `${sidebarRect.width}px`;
    panel.style.maxHeight = `${Math.max(220, window.innerHeight - navOffset - 24)}px`;
    panel.style.overflowY = "auto";
}

function resetDesktopFilterStyles(sidebar, panel) {
    sidebar.style.minHeight = "";
    panel.style.position = "";
    panel.style.top = "";
    panel.style.left = "";
    panel.style.width = "";
    panel.style.maxHeight = "";
    panel.style.overflowY = "";
}

function renderPromotionShowcase() {
    const featuredContainer = document.getElementById("featuredPromoCard");
    stopPromotionRotation();
    const showcasePromotions = getShowcasePromotions();

    if (!showcasePromotions.length) {
        featuredContainer.classList.remove("has-image");
        featuredContainer.innerHTML = `
            <div class="promo-layout promo-layout-empty">
                <div class="promo-copy">
                    <div class="promo-heading">
                        <div class="promo-top">
                            <span class="promo-kicker"><i class="bi bi-stars"></i> Featured promotion</span>
                        </div>
                        <h2>No live campaigns available right now.</h2>
                        <p class="promo-description">As soon as the next dealership campaign goes live, this space will update automatically with its timing, eligible vehicles, and offer details.</p>
                    </div>
                </div>
            </div>
        `;
        return;
    }

    const sortedPromotions = getSortedPromotions(showcasePromotions);
    promotionRotationIndex = 0;
    renderFeaturedPromotionCard(featuredContainer, sortedPromotions, promotionRotationIndex);
    if (sortedPromotions.length > 1) {
        promotionRotationTimer = window.setInterval(() => {
            promotionRotationIndex = (promotionRotationIndex + 1) % sortedPromotions.length;
            renderFeaturedPromotionCard(featuredContainer, sortedPromotions, promotionRotationIndex);
        }, 5000);
    }

}

function renderFeaturedPromotionCard(container, promotions, index) {
    const featured = promotions[index];
    const featuredChips = buildPromotionCriteria(featured);
    const heroVehicle = getPromotionHeroVehicle(featured);
    const vehicleImage = heroVehicle ? getVehicleImage(heroVehicle) : null;
    const showcaseImage = featured.imageUrl || vehicleImage;
    const showcaseImageClass = featured.imageUrl ? "promo-visual-image promo-visual-image--banner" : "promo-visual-image promo-visual-image--vehicle";
    const showcaseAlt = featured.imageUrl
        ? (featured.title || featured.highlightLabel || "Promotion banner")
        : buildPromotionVehicleAlt(heroVehicle, featured);
    const hasImage = Boolean(showcaseImage);
    const imageMeta = featured.imageUrl ? promoImageMetaCache.get(featured.imageUrl) : null;
    const bannerShapeClass = featured.imageUrl ? getPromoBannerShapeClass(imageMeta) : "";
    const isUpcoming = isPromotionUpcoming(featured);
    const promoKicker = isUpcoming ? "Upcoming promotion" : "Featured promotion";
    const promoStatus = isUpcoming ? "Starts soon" : "Limited deal";
    const timelineMarkup = [
        featured.startDate
            ? `<div class="promo-detail-item promo-detail-item--starts"><span>Starts</span><strong>${escapeHtml(formatPromoDate(featured.startDate))}</strong></div>`
            : "",
        featured.endDate
            ? `<div class="promo-detail-item promo-detail-item--ends"><span>Ends</span><strong>${escapeHtml(formatPromoDate(featured.endDate))}</strong></div>`
            : ""
    ].join("");
    const applicabilityLabel = featured.targetSummary || "Selected vehicles";

    container.classList.toggle("has-image", hasImage);
    container.innerHTML = `
        <div class="promo-layout">
            <div class="promo-copy">
                <div class="promo-heading">
                    <div class="promo-top">
                        <span class="promo-kicker"><i class="bi bi-stars"></i> ${escapeHtml(promoKicker)}</span>
                        <span class="promo-status">${escapeHtml(promoStatus)}</span>
                    </div>
                    <h2>${escapeHtml(featured.highlightLabel || featured.title || "Live promotion")}</h2>
                    <p class="promo-description">${escapeHtml(featured.description || "A premium dealership offer is currently available across selected inventory.")}</p>
                </div>
                <div class="promo-chips">
                    ${featuredChips.map((chip) => `<span class="promo-chip">${escapeHtml(chip)}</span>`).join("")}
                </div>
                <div class="promo-detail-grid">
                    ${timelineMarkup}
                    <div class="promo-detail-item promo-detail-item-wide"><span>Applies to</span><strong>${escapeHtml(applicabilityLabel)}</strong></div>
                </div>
                <div class="promo-bottom">
                    <div class="promo-actions">
                        <button type="button" class="promo-action primary" onclick="viewPromotionOffer(${Number(featured.id) || 0})">
                            <i class="bi bi-stars"></i> View Offer
                        </button>
                        <button type="button" class="promo-action secondary" onclick="scrollToInventoryResults()">
                            <i class="bi bi-arrow-down-right"></i> Explore Vehicles
                        </button>
                    </div>
                    ${promotions.length > 1 ? `<div class="promo-rotation-dots">${promotions.map((_, dotIndex) => `<button type="button" class="promo-dot ${dotIndex === index ? "active" : ""}" onclick="jumpToPromotionSlide(${dotIndex})" aria-label="Promotion ${dotIndex + 1}"></button>`).join("")}</div>` : ""}
                </div>
            </div>
                <div class="promo-visual-shell">
                    <span class="promo-feature-badge"><i class="bi bi-stars"></i> Featured Promo</span>
                    <div class="promo-discount">
                        <strong>${escapeHtml(String(Math.round(Number(featured.discountPercentage) || 0)))}%</strong>
                        <span>${isUpcoming ? "SOON" : "OFF"}</span>
                    </div>
                    <div class="promo-visual-stage ${featured.imageUrl ? "promo-visual-stage--banner" : "promo-visual-stage--vehicle"}">
                        ${showcaseImage ? `
                            <div class="promo-visual-frame ${bannerShapeClass}">
                                <img class="${showcaseImageClass}" src="${showcaseImage}" alt="${escapeHtml(showcaseAlt)}">
                            </div>
                        ` : ""}
                    </div>
                </div>
            </div>
    `;

    if (featured.imageUrl) {
        ensurePromoImageMeta(featured.imageUrl, () => {
            if (!promotions.length) return;
            renderFeaturedPromotionCard(container, promotions, index);
        });
    }
}

function getPromoBannerShapeClass(imageMeta) {
    if (!imageMeta || !imageMeta.ratio) {
        return "is-wide";
    }

    if (imageMeta.ratio >= 1.55) {
        return "is-wide";
    }

    if (imageMeta.ratio <= 0.95) {
        return "is-tall";
    }

    return "is-standard";
}

function ensurePromoImageMeta(url, onReady) {
    if (!url || promoImageMetaCache.has(url) || promoImageMetaPending.has(url)) {
        return;
    }

    promoImageMetaPending.add(url);
    const image = new Image();
    image.onload = () => {
        promoImageMetaCache.set(url, {
            width: image.naturalWidth,
            height: image.naturalHeight,
            ratio: image.naturalWidth && image.naturalHeight ? image.naturalWidth / image.naturalHeight : 0
        });
        promoImageMetaPending.delete(url);
        if (typeof onReady === "function") {
            onReady();
        }
    };
    image.onerror = () => {
        promoImageMetaPending.delete(url);
    };
    image.src = url;
}

function jumpToPromotionSlide(index) {
    const promotions = getSortedPromotions(getShowcasePromotions());
    if (!promotions.length) {
        return;
    }
    promotionRotationIndex = index % promotions.length;
    renderFeaturedPromotionCard(document.getElementById("featuredPromoCard"), promotions, promotionRotationIndex);
    restartPromotionRotation(promotions);
}

function restartPromotionRotation(promotions) {
    stopPromotionRotation();
    if (promotions.length > 1) {
        promotionRotationTimer = window.setInterval(() => {
            promotionRotationIndex = (promotionRotationIndex + 1) % promotions.length;
            renderFeaturedPromotionCard(document.getElementById("featuredPromoCard"), promotions, promotionRotationIndex);
        }, 5000);
    }
}

function stopPromotionRotation() {
    if (promotionRotationTimer) {
        window.clearInterval(promotionRotationTimer);
        promotionRotationTimer = null;
    }
}

function getSortedPromotions(promotions) {
    return [...promotions].sort((a, b) =>
        (Number(b.discountPercentage) || 0) - (Number(a.discountPercentage) || 0)
        || (Number(b.id) || 0) - (Number(a.id) || 0)
    );
}

function getShowcasePromotions() {
    const bannerPromotions = livePromotions.filter((promotion) => promotion.showOnInventoryBanner !== false);
    return bannerPromotions.length ? bannerPromotions : livePromotions;
}

function viewPromotionOffer(promotionId) {
    const promotion = livePromotions.find((item) => Number(item.id) === Number(promotionId));
    if (!promotion) {
        scrollToInventoryResults();
        return;
    }

    document.getElementById("filterPurpose").value = mapPromotionListingType(promotion.targetListingType);
    document.getElementById("filterBrand").value = hasMatchingBrandOption(promotion.targetBrand) ? promotion.targetBrand : "any";
    syncModelOptions();
    document.getElementById("filterModel").value = hasMatchingModelOption(promotion.targetModel) ? promotion.targetModel : "any";
    activeQuickChip = "all";
    document.querySelectorAll("#quickChipBar .chip").forEach((chip) => chip.classList.toggle("active", chip.dataset.chip === "all"));
    applyFilters(true);
    scrollToInventoryResults();
}

function scrollToInventoryResults() {
    const resultsShell = document.querySelector(".results-shell");
    if (resultsShell) {
        resultsShell.scrollIntoView({ behavior: "smooth", block: "start" });
    }
}

function mapPromotionListingType(value) {
    if (!value) {
        return "any";
    }
    return String(value).toLowerCase() === "rent" ? "Rent" : "Sale";
}

function hasMatchingBrandOption(brand) {
    const select = document.getElementById("filterBrand");
    return !!brand && Array.from(select.options).some((option) => option.value.toLowerCase() === String(brand).toLowerCase());
}

function hasMatchingModelOption(model) {
    const select = document.getElementById("filterModel");
    return !!model && Array.from(select.options).some((option) => option.value.toLowerCase() === String(model).toLowerCase());
}

function isPromotionUpcoming(promotion) {
    if (!promotion?.startDate) {
        return false;
    }
    return promotion.startDate > getTodayString();
}

function getPromotionHeroVehicle(promotion) {
    if (!promotion || !allVehicles.length) {
        return null;
    }

    const matchingVehicles = allVehicles.filter((vehicle) => doesVehicleMatchPromotion(vehicle, promotion));
    const imageReadyVehicle = matchingVehicles.find((vehicle) => Boolean(getVehicleImageSource(vehicle)));
    return imageReadyVehicle || matchingVehicles[0] || null;
}

function doesVehicleMatchPromotion(vehicle, promotion) {
    if (!vehicle || !promotion) {
        return false;
    }

    if (promotion.targetListingType && String(vehicle.listingType || "").toLowerCase() !== String(promotion.targetListingType).toLowerCase()) {
        return false;
    }
    if (promotion.appliesToAllVehicles) {
        return true;
    }
    if (promotion.targetBrand && String(vehicle.brand || "").toLowerCase() !== String(promotion.targetBrand).toLowerCase()) {
        return false;
    }
    if (promotion.targetModel && String(vehicle.model || "").toLowerCase() !== String(promotion.targetModel).toLowerCase()) {
        return false;
    }
    return true;
}

function buildPromotionVehicleAlt(vehicle, promotion) {
    if (vehicle) {
        return `${vehicle.brand || ""} ${vehicle.model || ""}`.trim();
    }
    return promotion?.title || "Promotion vehicle";
}

function buildVehicleCard(vehicle) {
    const pricing = getVehiclePricing(vehicle);
    const promoted = pricing.hasCampaign;
    const displayPrice = pricing.finalPrice;
    const originalPrice = pricing.originalPrice;
    const isRent = (vehicle.listingType || "").toLowerCase() === "rent";
    const badgeClass = isRent ? "badge-rent" : "badge-sale";
    const listingLabel = isRent ? "For Rent" : "For Sale";
    const primaryActionLabel = "View Details";
    const priceLabel = isRent && pricing.hasPremiumRate ? "Member Daily Rate" : (isRent ? "Daily Rate" : "Total Price");
    const targetPage = isRent ? "vehicle-details-rent.html" : "vehicle-details-sale.html";
    const stockLabel = Number(vehicle.quantity || 0) > 0 ? `${vehicle.quantity} in stock` : "Out of stock";
    const promoRibbon = promoted ? escapeHtml(pricing.appliedPromotion.highlightLabel || pricing.appliedPromotion.title || "Special Offer") : "";
    const wishlistActive = myWishlistIds.includes(Number(vehicle.id));
    const priceCaptionMarkup = [];

    if (pricing.hasCampaign) {
        priceCaptionMarkup.push(`<span class="price-caption">${escapeHtml(`${Math.round(Number(pricing.appliedPromotion.discountPercentage) || 0)}% promo applied`)}</span>`);
    }
    if (pricing.hasPremiumRate) {
        priceCaptionMarkup.push(`<span class="price-caption price-caption-premium">${escapeHtml(pricing.hasCampaign ? "Premium extra 10% applied" : "Premium member 10% off")}</span>`);
    }

    return `
        <article class="vehicle-card ${promoted ? "promoted" : ""}">
            <div class="card-media">
                <img src="${getVehicleImage(vehicle)}" alt="${escapeHtml(`${vehicle.brand || ""} ${vehicle.model || ""}`.trim())}">
                <div class="card-overlay"></div>
                <button class="card-media-link" type="button" onclick="openVehicleDetails(event, ${Number(vehicle.id) || 0}, '${targetPage}')" aria-label="Open vehicle details"></button>
                <div class="card-badges">
                    <div class="badge-stack">
                        <span class="vehicle-badge ${badgeClass}">${escapeHtml(listingLabel)}</span>
                        <span class="vehicle-badge badge-condition">${escapeHtml(normalizeCondition(vehicle.vehicleCondition))}</span>
                    </div>
                    <button class="wishlist-btn ${wishlistActive ? "active" : ""}" type="button" onclick="toggleWishlist(event, ${Number(vehicle.id) || 0})" aria-label="Save to wishlist">
                        <i class="${wishlistActive ? "fas" : "far"} fa-heart"></i>
                    </button>
                </div>
                ${promoted ? `<div class="card-promo-note"><i class="bi bi-lightning-charge-fill"></i> ${promoRibbon}</div>` : ""}
            </div>

            <div class="card-body">
                <div class="card-topline">
                    <span class="card-stock">${escapeHtml(stockLabel)}</span>
                </div>

                <div class="card-title-wrap">
                    <h3>${escapeHtml(vehicle.brand || "")} ${escapeHtml(vehicle.model || "")}</h3>
                    <p>${escapeHtml(buildSubtitle(vehicle))}</p>
                </div>

                <div class="meta-grid">
                    <div class="meta-item"><i class="bi bi-calendar-event"></i><span>${escapeHtml(String(vehicle.manufactureYear || "N/A"))}</span></div>
                    <div class="meta-item"><i class="bi bi-speedometer2"></i><span>${escapeHtml(formatMileage(vehicle.mileage))}</span></div>
                    <div class="meta-item"><i class="bi bi-fuel-pump-fill"></i><span>${escapeHtml(vehicle.fuelType || "N/A")}</span></div>
                    <div class="meta-item"><i class="bi bi-gear-wide-connected"></i><span>${escapeHtml(vehicle.transmission || "N/A")}</span></div>
                </div>

                <div class="card-pricing">
                    <div>
                        <div class="price-label">${escapeHtml(priceLabel)}</div>
                        ${pricing.hasDiscount ? `<div class="price-original">${escapeHtml(formatCurrency(originalPrice))}</div>` : ""}
                        ${pricing.isStacked ? `<div class="price-intermediate">${escapeHtml(`Campaign rate ${formatCurrency(pricing.campaignPrice)}`)}</div>` : ""}
                        <div class="price-current">${escapeHtml(formatCurrency(pricing.hasDiscount ? displayPrice : originalPrice))}</div>
                        ${priceCaptionMarkup.length ? `<div class="price-caption-row">${priceCaptionMarkup.join("")}</div>` : ""}
                    </div>

                    <div class="card-actions">
                        <button class="card-btn primary" type="button" onclick="openVehicleDetails(event, ${Number(vehicle.id) || 0}, '${targetPage}')">
                            <i class="bi bi-arrow-up-right-circle"></i> ${escapeHtml(primaryActionLabel)}
                        </button>
                    </div>
                </div>
            </div>
        </article>
    `;
}

async function toggleWishlist(event, vehicleId) {
    event.preventDefault();
    event.stopPropagation();

    if (!token) {
        window.location.href = "login.html";
        return;
    }

    try {
        const res = await fetch(`/api/wishlist/toggle/${vehicleId}`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });

        if (res.ok) {
            const status = await res.text();
            if (status === "added") {
                if (!myWishlistIds.includes(vehicleId)) {
                    myWishlistIds.push(vehicleId);
                }
            } else {
                myWishlistIds = myWishlistIds.filter((id) => id !== vehicleId);
            }
            renderVehicles();
        }
    } catch (error) {
        console.error("Wishlist error", error);
    }
}

function openVehicleDetails(event, vehicleId, targetPage) {
    event.preventDefault();
    const selectedVehicle = allVehicles.find((vehicle) => Number(vehicle.id) === Number(vehicleId));
    if (selectedVehicle) {
        cacheVehicleForDetails(selectedVehicle);
    }
    window.location.href = `${targetPage}?id=${vehicleId}`;
}

function cacheVehicleForDetails(vehicle) {
    try {
        sessionStorage.setItem(vehicleCacheKeyPrefix + vehicle.id, JSON.stringify(vehicle));
    } catch (error) {
        console.warn("Could not cache vehicle details:", error);
    }
}

function getDisplayPrice(vehicle) {
    return getVehiclePricing(vehicle).finalPrice;
}

function isPromoted(vehicle) {
    return getVehiclePricing(vehicle).hasCampaign;
}

function shouldShowCardPromotion(vehicle) {
    return getVehiclePricing(vehicle).hasCampaign;
}

function getVehiclePricing(vehicle) {
    const originalPrice = Number(vehicle?.price ?? 0) || 0;
    const promotion = vehicle?.appliedPromotion;
    const isRent = (vehicle?.listingType || "").toLowerCase() === "rent";
    const hasCampaign = Boolean(promotion && promotion.showOnVehicleCard !== false && Number(vehicle?.effectivePrice || 0) < originalPrice);
    const campaignPrice = hasCampaign ? (Number(vehicle?.effectivePrice ?? vehicle?.price ?? 0) || originalPrice) : originalPrice;
    const hasPremiumRate = isRent && isPremiumUser;
    const finalPrice = hasPremiumRate ? campaignPrice * 0.90 : campaignPrice;

    return {
        originalPrice,
        appliedPromotion: promotion,
        hasCampaign,
        hasPremiumRate,
        campaignPrice,
        finalPrice,
        hasDiscount: finalPrice < originalPrice,
        isStacked: hasCampaign && hasPremiumRate
    };
}

function matchesPriceRange(vehicle, priceRange) {
    if (priceRange === "any") return true;

    const price = getDisplayPrice(vehicle);
    const isRent = (vehicle.listingType || "").toLowerCase() === "rent";

    if (priceRange === "budget") return isRent ? price <= 8000 : price <= 8000000;
    if (priceRange === "mid") return isRent ? (price > 8000 && price <= 18000) : (price > 8000000 && price <= 16000000);
    if (priceRange === "premium") return isRent ? price > 18000 : price > 16000000;
    return true;
}

function matchesQuickChip(vehicle) {
    const listingType = (vehicle.listingType || "").toLowerCase();
    const fuelType = (vehicle.fuelType || "").toLowerCase();
    const category = (vehicle.category || "").toLowerCase();

    switch (activeQuickChip) {
        case "sale": return listingType === "sale";
        case "rent": return listingType === "rent";
        case "hybrid": return fuelType === "hybrid";
        case "electric": return fuelType === "electric";
        case "suv": return category === "suv";
        case "sedan": return category === "sedan";
        case "budget": return matchesPriceRange(vehicle, "budget");
        case "promo": return isPromoted(vehicle);
        default: return true;
    }
}

function buildPromotionCriteria(promotion) {
    if (!promotion) return ["Selected Vehicles"];
    if (promotion.appliesToAllVehicles) {
        return [promotion.targetListingType === "Rent" ? "All Rent Vehicles" : "All Sale Vehicles"];
    }

    const chips = [];
    if (promotion.targetBrand) chips.push(promotion.targetBrand);
    if (promotion.targetModel) chips.push(promotion.targetModel);
    if (promotion.targetListingType) chips.push(promotion.targetListingType === "Rent" ? "For Rent" : "For Sale");
    if (promotion.targetVehicleCondition) chips.push(normalizeCondition(promotion.targetVehicleCondition));
    return chips.length ? chips : ["Selected Vehicles"];
}

function buildSubtitle(vehicle) {
    const parts = [];
    if (vehicle.color) parts.push(vehicle.color);
    if (vehicle.engineCapacity) parts.push(vehicle.engineCapacity);
    if (vehicle.listingType) parts.push(vehicle.listingType === "Rent" ? "Rental option" : "Purchase ready");
    return parts.length ? parts.join(" | ") : "Premium dealership listing";
}

function buildVehicleBlurb(vehicle, promoted) {
    if (promoted && vehicle.appliedPromotion) {
        const promotion = vehicle.appliedPromotion;
        const offerText = promotion.highlightLabel || promotion.title || "Limited-time deal";
        const deadline = promotion.endDate ? ` Ends ${formatPromoEndDate(promotion.endDate)}.` : "";
        return `${offerText} for ${promotion.targetSummary || "selected vehicles"}.${deadline}`;
    }

    if (vehicle.description) {
        return truncateText(vehicle.description, 112);
    }

    const fuel = vehicle.fuelType || "flexible powertrain";
    const category = vehicle.category || "premium";
    return `A ${category.toLowerCase()} vehicle with ${fuel.toLowerCase()} performance and a clean dealership presentation.`;
}

function buildVehicleHighlight(vehicle) {
    const listingType = (vehicle.listingType || "").toLowerCase() === "rent" ? "Ready for rental" : "Ready to purchase";
    const fuel = vehicle.fuelType || "Flexible powertrain";
    return `${listingType} | ${fuel}`;
}

function normalizeCondition(value) {
    if (!value) return "Condition";
    return value === "BrandNew" ? "Brand New" : value;
}

function formatMileage(value) {
    return `${new Intl.NumberFormat("en-LK").format(Number(value || 0))} km`;
}

function formatCurrency(value) {
    return `LKR ${new Intl.NumberFormat("en-LK").format(Math.round(Number(value || 0)))}`;
}

function formatPromoEndDate(value) {
    if (!value) return "Limited time";
    return new Date(value).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

function formatPromoDate(value) {
    if (!value) return "Limited time";
    return new Date(value).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

function getTodayString() {
    const now = new Date();
    const offsetMs = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - offsetMs).toISOString().split("T")[0];
}

function getVehicleImage(vehicle) {
    const image = getVehicleImageSource(vehicle);
    if (image) return image;
    return buildPlaceholderImage(`${vehicle.brand || "Vehicle"} ${vehicle.model || ""}`.trim());
}

function getVehicleImageSource(vehicle) {
    if (!vehicle) {
        return "";
    }
    return vehicle.image1 || vehicle.image2 || vehicle.image3 || vehicle.image4 || vehicle.image5 || "";
}

function buildPlaceholderImage(label) {
    return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(`
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 420">
            <defs>
                <linearGradient id="g" x1="0" x2="1" y1="0" y2="1">
                    <stop offset="0%" stop-color="#0f2b55"/>
                    <stop offset="100%" stop-color="#2563eb"/>
                </linearGradient>
            </defs>
            <rect width="640" height="420" fill="url(#g)"/>
            <text x="50%" y="45%" text-anchor="middle" fill="#ffffff" font-size="34" font-family="Inter, Arial, sans-serif" font-weight="700">${escapeHtml(label || "Vehicle")}</text>
            <text x="50%" y="58%" text-anchor="middle" fill="#dbeafe" font-size="20" font-family="Inter, Arial, sans-serif">Image coming soon</text>
        </svg>
    `)}`;
}

function uniqueSorted(values) {
    return [...new Set(values.filter(Boolean))].sort((a, b) => String(a).localeCompare(String(b)));
}

function truncateText(value, maxLength) {
    if (!value || value.length <= maxLength) return value || "";
    return `${value.slice(0, maxLength - 1).trim()}...`;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

window.jumpToPromotionSlide = jumpToPromotionSlide;
window.viewPromotionOffer = viewPromotionOffer;
window.scrollToInventoryResults = scrollToInventoryResults;
window.openFilterDrawer = openFilterDrawer;
window.closeFilterDrawer = closeFilterDrawer;
