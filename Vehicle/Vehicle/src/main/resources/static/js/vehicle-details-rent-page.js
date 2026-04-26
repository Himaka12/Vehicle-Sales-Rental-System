const token = sessionStorage.getItem("jwtToken");
const vehicleCacheKeyPrefix = "vehicle-preview-";

let vehicleImages = [];
let currentImageIndex = 0;
let imageRotationTimer = null;
let currentVehicleData = null;
let dailyRentPrice = 0;
let isPremiumUser = false;
let loggedInUserName = "";
let editingReviewId = null;
let isSubmittingReview = false;

document.addEventListener("DOMContentLoaded", initRentVehicleDetails);

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

function getVehicleCacheKey(vehicleId) {
    return vehicleCacheKeyPrefix + vehicleId;
}

function getCachedVehicle(vehicleId) {
    try {
        const cachedData = sessionStorage.getItem(getVehicleCacheKey(vehicleId));
        return cachedData ? JSON.parse(cachedData) : null;
    } catch (error) {
        console.warn("Could not read cached vehicle details:", error);
        return null;
    }
}

function cacheVehicle(vehicle) {
    try {
        sessionStorage.setItem(getVehicleCacheKey(vehicle.id), JSON.stringify(vehicle));
    } catch (error) {
        console.warn("Could not cache vehicle details:", error);
    }
}

function clearCachedVehicle(vehicleId) {
    try {
        sessionStorage.removeItem(getVehicleCacheKey(vehicleId));
    } catch (error) {
        console.warn("Could not clear cached vehicle details:", error);
    }
}

function showVehicleDetails() {
    document.getElementById("loading-container").style.display = "none";
    document.getElementById("details-container").style.display = "block";
}

function stopImageRotation() {
    if (imageRotationTimer) {
        clearInterval(imageRotationTimer);
        imageRotationTimer = null;
    }
}

function setActiveThumbnail(activeIndex) {
    document.querySelectorAll("#thumbnail-container .thumb").forEach((thumb, index) => {
        thumb.classList.toggle("active", index === activeIndex);
    });
}

function showImageAt(index) {
    if (!vehicleImages.length) {
        return;
    }

    currentImageIndex = index;
    document.getElementById("mainImage").src = vehicleImages[currentImageIndex];
    setActiveThumbnail(currentImageIndex);
}

function startImageRotation() {
    stopImageRotation();
    if (vehicleImages.length <= 1) {
        return;
    }

    imageRotationTimer = window.setInterval(() => {
        const nextIndex = (currentImageIndex + 1) % vehicleImages.length;
        showImageAt(nextIndex);
    }, 4000);
}

function renderVehicleImages(vehicle) {
    stopImageRotation();
    vehicleImages = [vehicle.image1, vehicle.image2, vehicle.image3, vehicle.image4, vehicle.image5].filter(Boolean);
    currentImageIndex = 0;
    const thumbContainer = document.getElementById("thumbnail-container");
    thumbContainer.innerHTML = "";

    if (vehicleImages.length > 0) {
        vehicleImages.forEach((imgUrl, index) => {
            const thumb = document.createElement("button");
            thumb.type = "button";
            thumb.className = "thumb" + (index === 0 ? " active" : "");
            thumb.onclick = () => changeImage(index);
            thumb.innerHTML = `<img src="${imgUrl}" alt="Vehicle image ${index + 1}">`;
            thumbContainer.appendChild(thumb);
        });
        showImageAt(0);
        startImageRotation();
    } else {
        document.getElementById("mainImage").src = buildPlaceholderImage(`${vehicle.brand || "Vehicle"} ${vehicle.model || ""}`.trim());
    }
}

function changeImage(index) {
    showImageAt(index);
    startImageRotation();
}

function setReviewRating(rating) {
    const normalizedRating = Math.max(1, Math.min(5, Number(rating) || 5));
    const ratingInput = document.getElementById("reviewRating");
    if (ratingInput) {
        ratingInput.value = String(normalizedRating);
    }

    document.querySelectorAll("#reviewRatingPicker .star-rating-btn").forEach((button) => {
        const buttonRating = Number(button.dataset.rating || 0);
        button.classList.toggle("active", buttonRating <= normalizedRating);
        button.setAttribute("aria-checked", buttonRating === normalizedRating ? "true" : "false");
    });
}

function renderRentVehicle(vehicle) {
    currentVehicleData = vehicle;
    const title = `${vehicle.brand || ""} ${vehicle.model || ""}`.trim();

    const heroTitle = document.getElementById("heroTitle");
    if (heroTitle) {
        heroTitle.innerText = title || "Vehicle for Rent";
    }

    document.getElementById("vTitle").innerText = title;
    const conditionEl = document.getElementById("vCondition");
    const isBrandNew = vehicle.vehicleCondition === "BrandNew";
    conditionEl.innerText = isBrandNew ? "Brand New" : (vehicle.vehicleCondition || "Condition");
    conditionEl.classList.toggle("condition-brand-new", isBrandNew);
    document.getElementById("detailSubtitle").innerText = [vehicle.category, vehicle.color, vehicle.engineCapacity].filter(Boolean).join(" | ") || "Ready-to-rent listing";
    document.getElementById("vYear").innerText = vehicle.manufactureYear || "N/A";
    document.getElementById("vMileage").innerText = `${new Intl.NumberFormat("en-LK").format(Number(vehicle.mileage || 0))} km`;
    document.getElementById("vColor").innerText = vehicle.color || "N/A";
    document.getElementById("vFuel").innerText = vehicle.fuelType || "N/A";
    document.getElementById("vTrans").innerText = vehicle.transmission || "N/A";
    document.getElementById("vDescription").innerText = vehicle.description || "No description available.";

    const quantity = Number(vehicle.quantity || 0);
    const quantityEl = document.getElementById("vQuantity");
    quantityEl.innerText = quantity > 0 ? `${quantity} units` : "0 units";
    quantityEl.style.color = quantity > 0 ? "#16a34a" : "#ef4444";
    document.getElementById("actionButtons").style.display = quantity > 0 ? "grid" : "none";
    document.getElementById("outOfStock").style.display = quantity > 0 ? "none" : "block";

    setReviewRating(document.getElementById("reviewRating")?.value || 5);
    applyRentPrice();
    renderVehicleImages(vehicle);
}

function applyRentPrice() {
    if (!currentVehicleData) {
        return;
    }

    const priceContainer = document.getElementById("priceContainer");
    const promoInfoBox = document.getElementById("promoInfoBox");
    const pricing = calculateRentPricing(currentVehicleData);
    dailyRentPrice = pricing.finalPrice;

    if (pricing.hasDiscount) {
        const noteMarkup = [];
        if (pricing.hasCampaign) {
            noteMarkup.push(`<span class="price-note-chip promo">${escapeHtml(`${Math.round(Number(pricing.appliedPromotion.discountPercentage) || 0)}% promo applied`)}</span>`);
        }
        if (pricing.hasPremiumRate) {
            noteMarkup.push(`<span class="price-note-chip premium">${escapeHtml(pricing.hasCampaign ? "Premium extra 10% applied" : "Premium member 10% off")}</span>`);
        }

        priceContainer.innerHTML = `
            <span class="price-original">${formatCurrency(pricing.originalPrice)}</span>
            ${pricing.isStacked ? `<span class="price-intermediate">Campaign rate: ${formatCurrency(pricing.campaignPrice)}</span>` : ""}
            <span class="price-current">${formatCurrency(pricing.finalPrice)}</span>
            <div class="price-note-row">${noteMarkup.join("")}</div>
        `;
        promoInfoBox.style.display = "block";
        promoInfoBox.innerHTML = `
            ${pricing.hasCampaign && pricing.appliedPromotion.imageUrl ? `<div class="offer-banner"><img src="${pricing.appliedPromotion.imageUrl}" alt="${escapeHtml(pricing.appliedPromotion.title || "Promotion banner")}"></div>` : ""}
            <div class="offer-title"><i class="bi bi-stars"></i> ${escapeHtml(pricing.offerTitle)}</div>
            <div class="offer-meta">${escapeHtml(pricing.offerBody)}</div>
        `;
    } else {
        priceContainer.innerHTML = `<span class="price-current">${formatCurrency(pricing.finalPrice)}</span>`;
        promoInfoBox.style.display = "none";
        promoInfoBox.innerHTML = "";
    }

    checkAndCalculateAvailability();
}

function isPremiumProfile(profileData) {
    return profileData?.isPremium === true || profileData?.premium === true;
}

function calculateRentPricing(vehicle) {
    const originalPrice = Number(vehicle?.price || 0);
    const appliedPromotion = vehicle?.appliedPromotion;
    const campaignPriceRaw = Number(vehicle?.effectivePrice ?? vehicle?.price ?? 0);
    const hasCampaign = Boolean(appliedPromotion && appliedPromotion.showOnVehicleDetails && campaignPriceRaw < originalPrice);
    const campaignPrice = hasCampaign ? campaignPriceRaw : originalPrice;
    const hasPremiumRate = isPremiumUser === true;
    const premiumBasePrice = campaignPrice;
    const finalPrice = hasPremiumRate ? premiumBasePrice * 0.90 : campaignPrice;
    const isStacked = hasCampaign && hasPremiumRate;

    let offerTitle = "";
    let offerBody = "";

    if (isStacked) {
        offerTitle = "Promotion + Premium Rate Applied";
        offerBody = `${appliedPromotion.title} is active for ${appliedPromotion.targetSummary || "selected vehicles"}, and your premium membership adds an extra 10% off the campaign rate.`;
        if (appliedPromotion.endDate) {
            offerBody += ` Ends: ${formatPromoDeadline(appliedPromotion.endDate)}`;
        }
    } else if (hasCampaign) {
        offerTitle = "Promotion Applied";
        offerBody = `${appliedPromotion.title} is active for ${appliedPromotion.targetSummary || "selected vehicles"}.`;
        if (appliedPromotion.endDate) {
            offerBody += ` Ends: ${formatPromoDeadline(appliedPromotion.endDate)}`;
        }
    } else if (hasPremiumRate) {
        offerTitle = "VIP Member Rate Applied";
        offerBody = "Your premium membership applies an extra 10% discount to this rental.";
    }

    return {
        originalPrice,
        appliedPromotion,
        hasCampaign,
        hasPremiumRate,
        campaignPrice,
        finalPrice,
        isStacked,
        hasDiscount: finalPrice < originalPrice,
        offerTitle,
        offerBody
    };
}

async function loadRentUserState(vehicleId) {
    if (!token) {
        return;
    }

    try {
        const profileRes = await fetch("/api/customer/profile", {
            headers: { "Authorization": "Bearer " + token }
        });

        if (profileRes.ok) {
            const profileData = await profileRes.json();
            isPremiumUser = isPremiumProfile(profileData);
            loggedInUserName = profileData.fullName || "";
            applyRentPrice();
        }

        const wishRes = await fetch("/api/wishlist/my-wishlist-ids", {
            headers: { "Authorization": "Bearer " + token }
        });
        if (wishRes.ok) {
            const savedIds = await wishRes.json();
            const wishlistButton = document.getElementById("wishlistButton");
            if (savedIds.includes(parseInt(vehicleId, 10))) {
                wishlistButton.classList.add("danger");
                wishlistButton.innerHTML = `<i class="fas fa-heart"></i> Saved to Wishlist`;
            } else {
                wishlistButton.classList.remove("danger");
                wishlistButton.innerHTML = `<i class="far fa-heart"></i> Add to Wishlist`;
            }
        }
    } catch (error) {
        console.error("Error fetching user data:", error);
    }
}

async function initRentVehicleDetails() {
    initNavScroll();
    initNavUser();

    const urlParams = new URLSearchParams(window.location.search);
    const vehicleId = urlParams.get("id");

    if (!vehicleId) {
        window.location.href = "vehicle-inventory.html";
        return;
    }

    const todayStr = new Date().toISOString().split("T")[0];
    document.getElementById("startDate").min = todayStr;
    document.getElementById("endDate").min = todayStr;
    document.getElementById("startDate").addEventListener("change", syncEndDateWithStartDate);
    document.getElementById("endDate").addEventListener("focus", syncEndDateWithStartDate);

    const cachedVehicle = getCachedVehicle(vehicleId);
    if (cachedVehicle) {
        renderRentVehicle(cachedVehicle);
        showVehicleDetails();
    }

    try {
        const response = await fetch(`/api/vehicles/${vehicleId}`);
        if (!response.ok) {
            throw new Error("Vehicle not found");
        }

        const vehicle = await response.json();
        cacheVehicle(vehicle);
        renderRentVehicle(vehicle);
        showVehicleDetails();
        fetchVehicleReviews(vehicleId);
        checkReviewEligibility(vehicleId);
        loadRentUserState(vehicleId);
    } catch (error) {
        clearCachedVehicle(vehicleId);
        if (cachedVehicle) {
            alert("This listing is no longer available.");
            window.location.replace("vehicle-inventory.html");
            return;
        }
        if (!cachedVehicle) {
            document.getElementById("loading-container").innerHTML = `
                <i class="bi bi-exclamation-triangle"></i>
                <h2>Could not load this vehicle</h2>
                <p>Please return to inventory and try again.</p>
            `;
        }
    }
}

function syncEndDateWithStartDate() {
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");
    const todayStr = new Date().toISOString().split("T")[0];
    const minimumEndDate = startDateInput.value || todayStr;

    endDateInput.min = minimumEndDate;

    if (endDateInput.value && endDateInput.value < minimumEndDate) {
        endDateInput.value = minimumEndDate;
    }
}

async function checkAndCalculateAvailability() {
    const startDateVal = document.getElementById("startDate").value;
    const endDateVal = document.getElementById("endDate").value;
    const startTimeVal = document.getElementById("startTime").value;
    const endTimeVal = document.getElementById("endTime").value;
    const vehicleId = new URLSearchParams(window.location.search).get("id");

    const totalBox = document.getElementById("dynamicTotalBox");
    const warningBox = document.getElementById("availabilityWarning");
    const bankBox = document.getElementById("bankDetailsBox");
    const slipGroup = document.getElementById("slipUploadGroup");
    const submitBtn = document.getElementById("requestBookingBtn");

    syncEndDateWithStartDate();

    if (startDateVal && endDateVal) {
        const start = new Date(startDateVal);
        const end = new Date(endDateVal);
        if (end < start) {
            document.getElementById("endDate").value = startDateVal;
            end.setTime(start.getTime());
        }

        const timeDiff = end.getTime() - start.getTime();
        const daysDiff = Math.ceil(timeDiff / (1000 * 3600 * 24)) + 1;
        const totalFee = daysDiff * dailyRentPrice;

        document.getElementById("daysLabel").innerText = `Total for ${daysDiff} Day${daysDiff > 1 ? "s" : ""}`;
        document.getElementById("totalFeeAmount").innerText = formatCurrency(totalFee);
        totalBox.style.display = "block";
    } else {
        totalBox.style.display = "none";
    }

    if (startDateVal && endDateVal && startTimeVal && endTimeVal) {
        try {
            const url = `/api/bookings/check-availability?vehicleId=${vehicleId}&startDate=${startDateVal}&endDate=${endDateVal}&startTime=${startTimeVal}&endTime=${endTimeVal}`;
            const res = await fetch(url);

            if (res.ok) {
                const data = await res.json();
                if (data.available === true) {
                    warningBox.style.display = "none";
                    bankBox.style.display = "block";
                    slipGroup.style.display = "grid";
                    submitBtn.style.display = "inline-flex";
                } else {
                    warningBox.style.display = "block";
                    bankBox.style.display = "none";
                    slipGroup.style.display = "none";
                    submitBtn.style.display = "none";
                }
            }
        } catch (error) {
            console.error("Live availability check failed:", error);
        }
    } else {
        warningBox.style.display = "none";
        bankBox.style.display = "none";
        slipGroup.style.display = "none";
        submitBtn.style.display = "none";
    }
}

async function fetchVehicleReviews(vehicleId) {
    try {
        const res = await fetch(`/api/reviews/vehicle/${vehicleId}`);
        if (res.ok) {
            const reviews = await res.json();
            renderReviews(reviews);
            renderReviewInsights(reviews);
        }
    } catch (error) {
        console.error("Error fetching reviews", error);
    }
}

function renderReviews(reviews) {
    const list = document.getElementById("reviewsList");
    const summary = document.getElementById("ratingSummary");

    if (!reviews.length) {
        summary.innerHTML = `<span style="color:var(--muted); font-weight:500;">No ratings yet</span>`;
        list.innerHTML = `
            <div class="review-card">
                <p class="review-comment">No reviews yet. Rent this vehicle to be the first reviewer.</p>
            </div>
        `;
        return;
    }

    const totalStars = reviews.reduce((sum, review) => sum + review.rating, 0);
    const avgRating = (totalStars / reviews.length).toFixed(1);
    summary.innerHTML = `<span style="color:#fbbf24; font-weight:800;"><i class="bi bi-star-fill"></i> ${avgRating} / 5</span><span style="color:var(--muted);"> (${reviews.length} verified)</span>`;

    list.innerHTML = reviews.map((review) => {
        const displayDate = new Date(review.reviewDate).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
        const starsHtml = Array.from({ length: 5 }, (_, index) =>
            `<i class="fas fa-star" style="color:${index < review.rating ? "#fbbf24" : "#e2e8f0"}; font-size:11px;"></i>`
        ).join("");

        const actionMarkup = loggedInUserName && review.customerName === loggedInUserName ? `
            <div class="action-stack" style="margin-top:12px;">
                <button class="action-btn secondary" type="button" onclick="prepareEditReview(${review.id}, ${review.rating}, '${escapeJs(review.comment || "")}')">
                    <i class="bi bi-pencil-square"></i> Edit Review
                </button>
                <button class="action-btn danger" type="button" onclick="deleteReview(${review.id})">
                    <i class="bi bi-trash3-fill"></i> Delete Review
                </button>
            </div>
        ` : "";
        const sentiment = String(review.aiSentiment || "NEGATIVE").toUpperCase();
        const sentimentClass = sentiment === "POSITIVE" ? "positive" : (sentiment === "CRITICAL" ? "critical" : "negative");
        const replyLabel = review.replySource === "AdminManual" ? "Admin response" : "System reply";
        const replyMarkup = review.businessReply ? `
            <div class="review-response-note">
                <strong>${escapeHtml(replyLabel)}</strong>
                ${escapeHtml(review.businessReply || "")}
                ${review.businessReplyDate ? `<br><span class="review-date">${escapeHtml(review.businessReplyDate)}</span>` : ""}
            </div>
        ` : "";
        const pendingAdminMarkup = review.requiresAdminAttention === true && review.adminAttentionStatus !== "Resolved" ? `
            <div class="review-response-note critical">
                <strong>Support follow-up in progress</strong>
                ${escapeHtml(review.adminAttentionReason || "Our team is reviewing this feedback and will follow up manually.")}
            </div>
        ` : "";

        return `
            <div class="review-card">
                <div class="review-head">
                    <div>
                        <div class="reviewer">${escapeHtml(review.customerName || "Verified Renter")} <i class="fas fa-check-circle" style="color:#16a34a; font-size:11px;"></i></div>
                        <div style="margin-top:4px;">${starsHtml}</div>
                        <div class="review-meta-row">
                            <span class="review-meta-pill ${sentimentClass}">${escapeHtml(sentiment)}</span>
                            ${review.businessReply ? `<span class="review-meta-pill reply">${escapeHtml(replyLabel)}</span>` : ""}
                        </div>
                    </div>
                    <div class="review-date">${escapeHtml(displayDate)}</div>
                </div>
                <p class="review-comment">"${escapeHtml(review.comment || "")}"</p>
                ${pendingAdminMarkup}
                ${replyMarkup}
                ${actionMarkup}
            </div>
        `;
    }).join("");
}

function renderReviewInsights(reviews) {
    const chart = document.getElementById("reviewSentimentChart");
    if (!chart) {
        return;
    }

    const counts = {
        positive: reviews.filter((review) => String(review.aiSentiment || "").toUpperCase() === "POSITIVE").length,
        negative: reviews.filter((review) => String(review.aiSentiment || "").toUpperCase() === "NEGATIVE").length,
        critical: reviews.filter((review) => String(review.aiSentiment || "").toUpperCase() === "CRITICAL").length
    };

    const total = reviews.length;
    chart.innerHTML = [
        buildReviewInsightRow("Positive", "positive", counts.positive, total),
        buildReviewInsightRow("Negative", "negative", counts.negative, total),
        buildReviewInsightRow("Critical", "critical", counts.critical, total)
    ].join("");
}

function buildReviewInsightRow(label, tone, count, total) {
    const percentage = total > 0 ? Math.round((count / total) * 100) : 0;
    const fillWidth = count > 0 ? Math.max(percentage, 8) : 0;
    return `
        <div class="review-insight-row">
            <div class="review-insight-head">
                <span class="review-insight-label"><span class="review-insight-dot ${tone}"></span>${escapeHtml(label)}</span>
                <span class="review-insight-value">${escapeHtml(String(count))} review${count === 1 ? "" : "s"} | ${escapeHtml(String(percentage))}%</span>
            </div>
            <div class="review-insight-bar">
                <div class="review-insight-fill ${tone}" style="width:${fillWidth}%"></div>
            </div>
        </div>
    `;
}

async function checkReviewEligibility(vehicleId) {
    const writeReviewSection = document.getElementById("writeReviewSection");
    const cannotReviewMessage = document.getElementById("cannotReviewMessage");
    if (!writeReviewSection) {
        return;
    }

    if (!token) {
        writeReviewSection.style.display = "none";
        if (cannotReviewMessage) cannotReviewMessage.style.display = "none";
        return;
    }
    try {
        const res = await fetch(`/api/reviews/can-review/${vehicleId}`, {
            headers: { "Authorization": "Bearer " + token }
        });
        if (res.ok) {
            const canReview = await res.json();
            if (canReview === true) {
                writeReviewSection.style.display = "block";
                if (cannotReviewMessage) cannotReviewMessage.style.display = "none";
            } else if (!editingReviewId) {
                writeReviewSection.style.display = "none";
                if (cannotReviewMessage) cannotReviewMessage.style.display = "none";
            }
        }
    } catch (error) {
        console.error("Error checking eligibility", error);
    }
}

async function submitReview() {
    if (isSubmittingReview) {
        if (window.appToast) {
            window.appToast("Your review is already being submitted. Please wait a moment.", { type: "info", duration: 2800 });
        }
        return;
    }

    const vehicleId = new URLSearchParams(window.location.search).get("id");
    const rating = document.getElementById("reviewRating").value;
    const comment = document.getElementById("reviewComment").value;
    const submitBtn = document.getElementById("submitReviewBtn");
    const originalSubmitMarkup = editingReviewId
        ? `<i class="bi bi-pencil-square"></i> Update Review`
        : `<i class="bi bi-send-fill"></i> Submit Review`;

    if (!comment.trim()) {
        alert("Please write a comment for your review.");
        return;
    }

    const data = { vehicleId, rating: parseInt(rating, 10), comment };
    let apiEndpoint = "/api/reviews/add";
    let httpMethod = "POST";

    if (editingReviewId) {
        apiEndpoint = `/api/reviews/update/${editingReviewId}`;
        httpMethod = "PUT";
    }

    try {
        isSubmittingReview = true;
        submitBtn.disabled = true;
        submitBtn.innerHTML = editingReviewId
            ? `<i class="fas fa-spinner fa-spin"></i> Updating Review...`
            : `<i class="fas fa-spinner fa-spin"></i> Submitting Review...`;

        if (window.appToast) {
            window.appToast(
                editingReviewId
                    ? "Your review update was sent. Please wait while we process it."
                    : "Your review was submitted. Please wait while we process it.",
                { type: "info", duration: 4200 }
            );
        }

        const res = await fetch(apiEndpoint, {
            method: httpMethod,
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify(data)
        });

        if (res.ok) {
            document.getElementById("reviewComment").value = "";
            setReviewRating(5);
            editingReviewId = null;
            submitBtn.innerHTML = `<i class="bi bi-send-fill"></i> Submit Review`;
            if (window.appToast) {
                window.appToast(
                    "Review saved successfully. Analysis and response have been updated.",
                    { type: "success", duration: 3200 }
                );
            }
            fetchVehicleReviews(vehicleId);
            checkReviewEligibility(vehicleId);
        } else {
            const errorText = await res.text();
            alert("Error submitting review: " + errorText);
        }
    } catch (error) {
        console.error(error);
        alert("Network error. Could not submit review.");
    } finally {
        isSubmittingReview = false;
        submitBtn.disabled = false;
        if (editingReviewId) {
            submitBtn.innerHTML = `<i class="bi bi-pencil-square"></i> Update Review`;
        } else {
            submitBtn.innerHTML = originalSubmitMarkup;
        }
    }
}

function prepareEditReview(reviewId, currentRating, currentComment) {
    editingReviewId = reviewId;
    setReviewRating(currentRating);
    document.getElementById("reviewComment").value = currentComment;
    document.getElementById("writeReviewSection").style.display = "block";
    const cannotReviewMessage = document.getElementById("cannotReviewMessage");
    if (cannotReviewMessage) cannotReviewMessage.style.display = "none";
    const submitBtn = document.getElementById("submitReviewBtn");
    submitBtn.innerHTML = `<i class="bi bi-pencil-square"></i> Update Review`;
    document.getElementById("writeReviewSection").scrollIntoView({ behavior: "smooth" });
}

async function deleteReview(reviewId) {
    if (!await window.appConfirm("Are you sure you want to permanently delete your review?", {
        title: "Delete Review",
        type: "danger",
        confirmText: "Delete Review",
        cancelText: "Cancel",
        danger: true,
        confirmIcon: "fa-trash"
    })) {
        return;
    }

    const vehicleId = new URLSearchParams(window.location.search).get("id");

    try {
        const res = await fetch(`/api/reviews/delete/${reviewId}`, {
            method: "DELETE",
            headers: { "Authorization": "Bearer " + token }
        });
        if (res.ok) {
            fetchVehicleReviews(vehicleId);
            checkReviewEligibility(vehicleId);
        } else {
            const errorText = await res.text();
            alert("Error deleting review: " + errorText);
        }
    } catch (error) {
        console.error(error);
        alert("Network error. Could not delete review.");
    }
}

async function submitRentalBooking() {
    if (!token) {
        window.location.href = "login.html";
        return;
    }

    const vehicleId = new URLSearchParams(window.location.search).get("id");
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const startTime = document.getElementById("startTime").value;
    const endTime = document.getElementById("endTime").value;
    const slipFile = document.getElementById("paymentSlip").files[0];

    if (!startDate || !endDate || !startTime || !endTime || !slipFile) {
        alert("Please complete the booking details and upload the payment slip.");
        return;
    }

    const formData = new FormData();
    formData.append("vehicleId", vehicleId);
    formData.append("startDate", startDate);
    formData.append("endDate", endDate);
    formData.append("startTime", startTime);
    formData.append("endTime", endTime);
    formData.append("paymentSlip", slipFile);

    const button = document.getElementById("requestBookingBtn");
    const originalText = button.innerHTML;
    button.innerHTML = `<i class="fas fa-spinner fa-spin"></i> Processing...`;
    button.disabled = true;

    try {
        const res = await fetch("/api/bookings/rent", {
            method: "POST",
            headers: { "Authorization": "Bearer " + token },
            body: formData
        });

        if (res.ok) {
            window.location.href = "customer-dashboard.html";
        } else {
            const errorText = await res.text();
            alert("Error: " + errorText);
        }
    } catch (error) {
        console.error(error);
        alert("Error submitting booking.");
    } finally {
        button.innerHTML = originalText;
        button.disabled = false;
    }
}

async function toggleWishlist(event) {
    event.preventDefault();

    if (!token) {
        window.location.href = "login.html";
        return;
    }

    const vehicleId = new URLSearchParams(window.location.search).get("id");
    const button = event.currentTarget;
    button.disabled = true;

    try {
        const res = await fetch(`/api/wishlist/toggle/${vehicleId}`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });

        if (res.ok) {
            const status = await res.text();
            if (status === "added") {
                button.classList.add("danger");
                button.innerHTML = `<i class="fas fa-heart"></i> Saved to Wishlist`;
            } else {
                button.classList.remove("danger");
                button.innerHTML = `<i class="far fa-heart"></i> Add to Wishlist`;
            }
        }
    } catch (error) {
        console.error(error);
    } finally {
        button.disabled = false;
    }
}

function formatCurrency(value) {
    return `LKR ${new Intl.NumberFormat("en-LK").format(Math.round(Number(value || 0)))}`;
}

function formatPromoDeadline(value) {
    if (!value) {
        return "Limited time";
    }
    return new Date(value).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
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

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function escapeJs(value) {
    return String(value ?? "")
        .replace(/\\/g, "\\\\")
        .replace(/'/g, "\\'");
}
