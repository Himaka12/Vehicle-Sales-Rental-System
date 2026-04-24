/**
 * Home page interactions and live marketplace rendering.
 */

const mainNav = document.getElementById("mainNav");
let lastNavScrollY = window.scrollY;

window.addEventListener("scroll", () => {
    if (!mainNav) return;

    const currentScrollY = window.scrollY;
    mainNav.classList.toggle("scrolled", currentScrollY > 18);

    if (currentScrollY <= 24) {
        mainNav.classList.remove("hidden");
    } else if (currentScrollY > lastNavScrollY + 4) {
        mainNav.classList.add("hidden");
    } else if (currentScrollY < lastNavScrollY - 4) {
        mainNav.classList.remove("hidden");
    }

    lastNavScrollY = currentScrollY;
    updateActiveNavLink();
}, { passive: true });

(function initSlider() {
    const slides = document.querySelectorAll(".hero-slide");
    const indicators = document.querySelectorAll(".indicator");
    const prevBtn = document.getElementById("sliderPrev");
    const nextBtn = document.getElementById("sliderNext");

    if (!slides.length) return;

    let current = 0;
    let timer = null;
    const delay = 4000;

    function goTo(index) {
        slides[current].classList.remove("active");
        slides[current].classList.add("exiting");
        if (indicators[current]) indicators[current].classList.remove("active");

        const exiting = slides[current];
        exiting.addEventListener("transitionend", function cleanup(event) {
            if (event.target !== exiting) return;
            exiting.classList.remove("exiting");
            exiting.removeEventListener("transitionend", cleanup);
        });

        current = (index + slides.length) % slides.length;
        slides[current].classList.add("active");
        if (indicators[current]) indicators[current].classList.add("active");
    }

    function startTimer() {
        stopTimer();
        timer = setInterval(() => goTo(current + 1), delay);
    }

    function stopTimer() {
        if (!timer) return;
        clearInterval(timer);
        timer = null;
    }

    if (prevBtn) {
        prevBtn.addEventListener("click", () => {
            goTo(current - 1);
            startTimer();
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", () => {
            goTo(current + 1);
            startTimer();
        });
    }

    indicators.forEach((indicator, index) => {
        indicator.addEventListener("click", () => {
            goTo(index);
            startTimer();
        });
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "ArrowLeft") {
            goTo(current - 1);
            startTimer();
        }
        if (event.key === "ArrowRight") {
            goTo(current + 1);
            startTimer();
        }
    });

    const hero = document.getElementById("home");
    if (hero) {
        let touchStartX = 0;
        hero.addEventListener("touchstart", (event) => {
            touchStartX = event.touches[0].clientX;
            stopTimer();
        }, { passive: true });

        hero.addEventListener("touchend", (event) => {
            const delta = event.changedTouches[0].clientX - touchStartX;
            if (Math.abs(delta) > 50) {
                if (delta < 0) {
                    goTo(current + 1);
                } else {
                    goTo(current - 1);
                }
            }
            startTimer();
        }, { passive: true });
    }

    startTimer();
})();

function initScrollReveal() {
    const selectors = [
        ".feature-card",
        ".vehicle-card",
        ".step-card",
        ".promo-card",
        ".review-card",
        ".about-text",
        ".section-header",
        ".footer-column"
    ];

    selectors.forEach((selector) => {
        document.querySelectorAll(selector).forEach((element, index) => {
            element.classList.add("reveal");
            element.style.transitionDelay = `${(index % 4) * 0.08}s`;
        });
    });

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            entry.target.classList.add("visible");
            observer.unobserve(entry.target);
        });
    }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" });

    document.querySelectorAll(".reveal").forEach((element) => observer.observe(element));
}

initScrollReveal();

function updateActiveNavLink() {
    const sections = document.querySelectorAll("section[id], footer[id]");
    const navLinks = document.querySelectorAll(".nav-links a[href^='#'], .navbar-nav .nav-link");
    const offset = (mainNav?.offsetHeight || 70) + 20;

    let activeId = "";
    sections.forEach((section) => {
        if (section.getBoundingClientRect().top <= offset) {
            activeId = section.id;
        }
    });

    navLinks.forEach((link) => {
        link.classList.toggle("active", link.getAttribute("href") === `#${activeId}`);
    });
}

document.querySelectorAll("a[href^='#']").forEach((anchor) => {
    anchor.addEventListener("click", function onClick(event) {
        const targetId = this.getAttribute("href");
        if (targetId === "#") return;
        const target = document.querySelector(targetId);
        if (!target) return;

        event.preventDefault();
        const top = target.getBoundingClientRect().top + window.scrollY - (mainNav?.offsetHeight || 70);
        window.scrollTo({ top, behavior: "smooth" });
    });
});

document.querySelectorAll(".btn-promo").forEach((button) => {
    button.addEventListener("click", (event) => {
        event.preventDefault();
        if (button.disabled) return;

        button.textContent = "Claimed";
        button.style.cssText = "background:rgba(6,182,212,0.2);border-color:rgba(6,182,212,0.5);color:#22d3ee;";
        button.disabled = true;

        setTimeout(() => {
            button.disabled = false;
            button.textContent = "Claim Offer";
            button.style.cssText = "";
        }, 3000);
    });
});

(function initHomeMarketplace() {
    const featuredVehiclesGrid = document.getElementById("featuredVehiclesGrid");
    const homePromotionsGrid = document.getElementById("homePromotionsGrid");

    if (!featuredVehiclesGrid && !homePromotionsGrid) return;

    document.addEventListener("DOMContentLoaded", loadHomeMarketplaceData);

    async function loadHomeMarketplaceData() {
        try {
            const [vehiclesResponse, promotionsResponse] = await Promise.all([
                fetch("/api/vehicles/all"),
                fetch("/api/promotions/showcase")
            ]);

            const vehicles = vehiclesResponse.ok ? await vehiclesResponse.json() : [];
            const promotions = promotionsResponse.ok ? await promotionsResponse.json() : [];

            renderFeaturedVehicles(Array.isArray(vehicles) ? vehicles : []);
            renderHomePromotions(Array.isArray(promotions) ? promotions : []);
            updateHomeSummary(Array.isArray(vehicles) ? vehicles : [], Array.isArray(promotions) ? promotions : []);
            initScrollReveal();
        } catch (error) {
            console.error("Failed to load home marketplace data:", error);
            renderFeaturedVehicles([]);
            renderHomePromotions([]);
            updateHomeSummary([], []);
        }
    }

    function renderFeaturedVehicles(vehicles) {
        if (!featuredVehiclesGrid) return;

        const liveVehicles = vehicles
            .filter((vehicle) => String(vehicle?.status || "").toLowerCase() === "available")
            .sort((left, right) => {
                const leftPromoted = left?.appliedPromotion ? 1 : 0;
                const rightPromoted = right?.appliedPromotion ? 1 : 0;
                if (rightPromoted !== leftPromoted) return rightPromoted - leftPromoted;

                const leftDate = Date.parse(left?.listedDate || "") || 0;
                const rightDate = Date.parse(right?.listedDate || "") || 0;
                return rightDate - leftDate;
            })
            .slice(0, 4);

        if (!liveVehicles.length) {
            featuredVehiclesGrid.innerHTML = `
                <div class="home-empty-state reveal">
                    Featured vehicles will appear here automatically once inventory is available.
                </div>
            `;
            return;
        }

        featuredVehiclesGrid.innerHTML = liveVehicles.map((vehicle) => {
            const image = getVehicleImage(vehicle);
            const price = formatLkr(vehicle.effectivePrice || vehicle.price || 0);
            const title = [vehicle.brand, vehicle.model].filter(Boolean).join(" ");
            const stockCount = Number(vehicle.quantity || 0);
            const stockLabel = stockCount > 0 ? `Stock: ${stockCount}` : "Out of stock";
            const subtitleParts = [vehicle.color, vehicle.engineCapacity, vehicle.fuelType, stockLabel].filter(Boolean);
            const listingType = String(vehicle.listingType || "").toLowerCase();
            const listingLabel = listingType === "rent" ? "For Rent" : "For Sale";
            const listingBadgeClass = listingType === "rent" ? "home-badge-rent" : "home-badge-sale";
            const detailLink = `guest-vehicle-details.html?id=${Number(vehicle.id) || 0}`;
            const hasOfferPrice = Boolean(vehicle.appliedPromotion && Number(vehicle.effectivePrice || 0) > 0 && Number(vehicle.effectivePrice || 0) < Number(vehicle.price || 0));
            const originalPrice = hasOfferPrice ? formatLkr(vehicle.price || 0) : "";
            const subtitleMarkup = subtitleParts.length
                ? subtitleParts.map((part) => escapeHtml(part)).join(' <span class="featured-vehicle-sub-sep">&bull;</span> ')
                : escapeHtml(stockLabel);

            return `
                <article class="featured-vehicle-card vehicle-card reveal">
                    <a class="featured-vehicle-media" href="${detailLink}">
                        ${image ? `<img src="${image}" alt="${escapeHtml(title || "Vehicle image")}">` : ""}
                        <div class="featured-vehicle-badges">
                            <div class="featured-vehicle-badges-left">
                                <span class="home-badge ${listingBadgeClass}">${escapeHtml(listingLabel)}</span>
                                <span class="home-badge home-badge-condition">${escapeHtml(formatCondition(vehicle.vehicleCondition))}</span>
                            </div>
                        </div>
                    </a>
                    <div class="featured-vehicle-body">
                        <div class="featured-vehicle-copy">
                            <h3>${escapeHtml(title || "Vehicle")}</h3>
                            <p class="featured-vehicle-sub">${subtitleMarkup}</p>
                        </div>
                        <div class="featured-vehicle-specs">
                            <div class="featured-vehicle-spec">
                                <span>Year</span>
                                <strong>${escapeHtml(String(vehicle.manufactureYear || "N/A"))}</strong>
                            </div>
                            <div class="featured-vehicle-spec">
                                <span>Transmission</span>
                                <strong>${escapeHtml(vehicle.transmission || "N/A")}</strong>
                            </div>
                            <div class="featured-vehicle-spec">
                                <span>Mileage</span>
                                <strong>${escapeHtml(formatMileage(vehicle.mileage))}</strong>
                            </div>
                            <div class="featured-vehicle-spec">
                                <span>Category</span>
                                <strong>${escapeHtml(vehicle.category || "Vehicle")}</strong>
                            </div>
                        </div>
                        <div class="featured-vehicle-footer">
                            <div class="featured-vehicle-price-panel">
                                <span class="featured-vehicle-price-label">${vehicle.appliedPromotion ? "Offer Price" : "Listed Price"}</span>
                                ${hasOfferPrice ? `<div class="featured-vehicle-price-original">${escapeHtml(originalPrice)}</div>` : ""}
                                <strong class="featured-vehicle-price-value">${price}</strong>
                            </div>
                        </div>
                    </div>
                </article>
            `;
        }).join("");
    }

    function renderHomePromotions(promotions) {
        if (!homePromotionsGrid) return;

        const livePromotions = promotions.filter(Boolean).slice(0, 3);

        if (!livePromotions.length) {
            homePromotionsGrid.innerHTML = `
                <div class="home-empty-state reveal">
                    No live promotion banners are available right now. As soon as marketing activates a campaign, it will appear here automatically.
                </div>
            `;
            return;
        }

        homePromotionsGrid.innerHTML = livePromotions.map((promotion) => {
            const isUpcoming = String(promotion.status || "").toLowerCase() === "scheduled";
            const title = promotion.highlightLabel || promotion.title || "Featured campaign";
            const summary = promotion.targetSummary || buildPromotionSummary(promotion);
            const discount = Math.round(Number(promotion.discountPercentage) || 0);

            return `
                <article class="home-promo-card promo-card reveal">
                    <div class="home-promo-visual">
                        ${promotion.imageUrl ? `<img src="${promotion.imageUrl}" alt="${escapeHtml(title)}">` : ""}
                        <div class="home-promo-discount">
                            <strong>${escapeHtml(String(discount))}%</strong>
                            <span>${isUpcoming ? "SOON" : "OFF"}</span>
                        </div>
                    </div>
                    <div class="home-promo-body">
                        <div class="home-promo-top">
                            <span class="section-kicker"><i class="bi bi-stars"></i> Promotion</span>
                            <span class="home-promo-status">${escapeHtml(isUpcoming ? "Starts Soon" : "Live Offer")}</span>
                        </div>
                        <div>
                            <h3>${escapeHtml(title)}</h3>
                            <p class="home-promo-copy">${escapeHtml(promotion.description || "Campaign details are available in the inventory page.")}</p>
                        </div>
                        <div class="home-promo-meta">
                            ${promotion.startDate ? `
                                <div class="home-promo-meta-item home-promo-meta-item--starts">
                                    <span>Starts</span>
                                    <strong>${escapeHtml(formatPromoDate(promotion.startDate))}</strong>
                                </div>
                            ` : ""}
                            ${promotion.endDate ? `
                                <div class="home-promo-meta-item home-promo-meta-item--ends">
                                    <span>Ends</span>
                                    <strong>${escapeHtml(formatPromoDate(promotion.endDate))}</strong>
                                </div>
                            ` : ""}
                            <div class="home-promo-meta-item">
                                <span>Applies To</span>
                                <strong>${escapeHtml(summary)}</strong>
                            </div>
                        </div>
                        <div class="home-promo-actions">
                            <a class="home-promo-link primary" href="guest-vehicle-inventory.html"><i class="bi bi-stars"></i> View Offer</a>
                            <a class="home-promo-link secondary" href="guest-vehicle-inventory.html"><i class="bi bi-arrow-right"></i> Explore Vehicles</a>
                        </div>
                    </div>
                </article>
            `;
        }).join("");
    }

    function updateHomeSummary(vehicles, promotions) {
        const availableVehicles = vehicles.filter((vehicle) => String(vehicle?.status || "").toLowerCase() === "available");
        const saleCount = availableVehicles.filter((vehicle) => String(vehicle?.listingType || "").toLowerCase() === "sale").length;
        const rentCount = availableVehicles.filter((vehicle) => String(vehicle?.listingType || "").toLowerCase() === "rent").length;

        setText("homeVehicleCount", String(availableVehicles.length));
        setText("homeSaleCount", String(saleCount));
        setText("homeRentCount", String(rentCount));
        setText("homePromoCount", String(promotions.length));
        setText("footerVehicleCount", String(availableVehicles.length));
        setText("footerPromoCount", String(promotions.length));
        setText("footerRentalCount", String(rentCount));
    }

    function getVehicleImage(vehicle) {
        return vehicle?.image1 || vehicle?.image2 || vehicle?.image3 || vehicle?.image4 || vehicle?.image5 || "";
    }

    function formatLkr(value) {
        const amount = Number(value) || 0;
        return `LKR ${amount.toLocaleString("en-US")}`;
    }

    function formatMileage(value) {
        const mileage = Number(value);
        if (!Number.isFinite(mileage) || mileage <= 0) return "0 km";
        return `${mileage.toLocaleString("en-US")} km`;
    }

    function formatCondition(value) {
        if (!value) return "Condition";
        return String(value).replace(/BrandNew/gi, "Brand New");
    }

    function formatPromoDate(value) {
        if (!value) return "TBA";
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) return String(value);
        return parsed.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
    }

    function buildPromotionSummary(promotion) {
        const parts = [
            promotion.targetBrand && `Brand: ${promotion.targetBrand}`,
            promotion.targetModel && `Model: ${promotion.targetModel}`,
            promotion.targetListingType && `Listing: ${promotion.targetListingType}`
        ].filter(Boolean);

        return parts.length ? parts.join(" | ") : "Selected vehicles";
    }

    function setText(id, value) {
        const node = document.getElementById(id);
        if (node) node.textContent = value;
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }
})();
