const categoryData = {
    "Plumber": {
        image: "../assets/images/plumber.jpg",
        subs: [
            "Pipe Leakage Repair",
            "Tap Installation",
            "Bathroom Fitting",
            "Water Tank Cleaning",
            "Drain Blockage Fix"
        ]
    },
    "Electrician": {
        image: "../assets/images/electrician.jpg",
        subs: [
            "Switch Repair",
            "Fan Installation",
            "Wiring Work",
            "Light Fitting",
            "Inverter Support"
        ]
    },
    "Tutor": {
        image: "../assets/images/tutor.jpg",
        subs: [
            "Math Tuition",
            "Science Tuition",
            "English Tuition",
            "Exam Preparation",
            "Primary Classes"
        ]
    },
    "Househelp": {
        image: "../assets/images/househelp.jpg",
        subs: [
            "Dishwashing",
            "Brooming",
            "Mopping",
            "Dusting",
            "Laundry Help"
        ]
    },
    "Cook": {
        image: "../assets/images/cook.jpg",
        subs: [
            "Daily Meal Cooking",
            "Tiffin Preparation",
            "Vegetarian Cooking",
            "Non-Veg Cooking",
            "Kitchen Assistance"
        ]
    },
    "Babysitter": {
        image: "../assets/images/babysitter.jpg",
        subs: [
            "Infant Care",
            "Toddler Supervision",
            "Feeding Assistance",
            "Playtime Care",
            "Night Babysitting"
        ]
    },
    "Carpenter": {
        image: "../assets/images/carpenter.jpg",
        subs: [
            "Furniture Repair",
            "Door Repair",
            "Window Fitting",
            "Shelf Installation",
            "Bed Assembly"
        ]
    },
    "Car Cleaner": {
        image: "../assets/images/car-cleaner.jpg",
        subs: [
            "Exterior Wash",
            "Interior Vacuum",
            "Seat Cleaning",
            "Dashboard Cleaning",
            "Basic Detailing"
        ]
    },
    "Mason": {
        image: "../assets/images/mason.jpg",
        subs: [
            "Wall Repair",
            "Brick Work",
            "Floor Tiling",
            "Plaster Work",
            "Small Construction"
        ]
    },
    "Pest Control": {
        image: "../assets/images/pest-control.jpg",
        subs: [
            "Termite Control",
            "Mosquito Control",
            "Cockroach Treatment",
            "Rodent Control",
            "Home Sanitization"
        ]
    },
    "Beautician": {
        image: "../assets/images/beautician.jpg",
        subs: [
            "Bridal Makeup",
            "Party Makeup",
            "Hair Styling",
            "Facial Service",
            "Waxing / Grooming"
        ]
    },
    "Senior Care Support": {
        image: "../assets/images/senior-care.jpg",
        subs: [
            "Daily Assistance",
            "Mobility Support",
            "Meal Support",
            "Medication Reminder",
            "Companionship Care"
        ]
    }
};

let currentCategory = "";
let currentSubCategory = "";
const selectedServices = new Map();

function getQueryParam(name) {
    return new URLSearchParams(window.location.search).get(name);
}

function getCurrentUser() {
    try {
        const raw = localStorage.getItem("user");
        return raw ? JSON.parse(raw) : null;
    } catch (err) {
        console.error("User parse error:", err);
        return null;
    }
}

function getLocalDateTimeForJava() {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

function setBookingMessage(text, isError = false) {
    const msg = document.getElementById("bookingMsg");
    if (!msg) return;
    msg.innerText = text || "";
    msg.style.color = isError ? "#f87171" : "#38bdf8";
}

function updateSelectedCount() {
    const countEl = document.getElementById("selectedCount");
    if (countEl) {
        countEl.innerText = `Selected Services: ${selectedServices.size}`;
    }
}

function setPageHeader() {
    const titleEl = document.getElementById("servicesPageTitle");
    const subtitleEl = document.getElementById("servicesPageSubtitle");
    const subTextEl = document.getElementById("selectedSubCategoryText");

    if (titleEl) titleEl.innerText = currentCategory || "Services";
    if (subtitleEl) {
        subtitleEl.innerText = currentSubCategory
            ? `Showing providers for ${currentSubCategory}`
            : "Choose a sub-category and book services.";
    }
    if (subTextEl) {
        subTextEl.innerText = currentSubCategory
            ? `Showing providers for: ${currentSubCategory}`
            : "Select a sub-category to view providers.";
    }
}

function renderSubcategoryList() {
    const info = categoryData[currentCategory];
    const container = document.getElementById("subcategoryList");
    if (!container) return;

    container.innerHTML = "";

    if (!info) {
        container.innerHTML = "<p>No subcategories found.</p>";
        return;
    }

    info.subs.forEach(sub => {
        const item = document.createElement("button");
        item.type = "button";
        item.className = "subcategory-item";

        if (sub === currentSubCategory) {
            item.classList.add("active");
        }

        item.innerHTML = `
            <span class="subcategory-name">${sub}</span>
            <span class="subcategory-arrow">›</span>
        `;

        item.addEventListener("click", () => {
            currentSubCategory = sub;
            setPageHeader();
            renderSubcategoryList();
            loadServices();
        });

        container.appendChild(item);
    });
}

function getServiceImage(category, imageUrl) {
    if (imageUrl && imageUrl.trim() !== "") {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) return imageUrl;
        if (imageUrl.startsWith("assets/")) return "../" + imageUrl;
        if (imageUrl.startsWith("../assets/")) return imageUrl;
    }

    return categoryData[category]?.image || "../assets/images/default-service.jpg";
}

function toggleServiceSelection(serviceId, serviceTitle, checked) {
    if (checked) {
        selectedServices.set(serviceId, serviceTitle);
    } else {
        selectedServices.delete(serviceId);
    }

    updateSelectedCount();
}

function clearSelectedServices(showMessage = true) {
    selectedServices.clear();
    document.querySelectorAll(".service-select-checkbox").forEach(cb => {
        cb.checked = false;
    });
    updateSelectedCount();

    if (showMessage) {
        setBookingMessage("Selection cleared.");
    }
}

function saveSuccessPayload(serviceNames) {
    sessionStorage.setItem("bookedServices", JSON.stringify(serviceNames || []));
    sessionStorage.setItem("bookedCategory", currentCategory || "");
    sessionStorage.setItem("bookedSubCategory", currentSubCategory || "");
}

function renderServices(services) {
    const container = document.getElementById("services");
    if (!container) return;

    container.innerHTML = "";

    if (!services.length) {
        container.innerHTML = "<p>No approved providers available in this sub-category yet.</p>";
        return;
    }

    services.forEach(service => {
        const row = document.createElement("div");
        row.className = "service-row";

        const checked = selectedServices.has(service.id) ? "checked" : "";
        const img = getServiceImage(service.category, service.imageUrl);

        row.innerHTML = `
            <div class="service-row-image-wrap">
                <img src="${img}" alt="${service.title}" class="service-row-image"
                     onerror="this.src='../assets/images/default-service.jpg'">
            </div>

            <div class="service-row-content">
                <div class="service-row-top">
                    <div>
                        <span class="status-pill">${service.subCategory || "General"}</span>
                        <h3 class="service-row-title">${service.title}</h3>
                        <p class="service-provider-name">${service.providerName || "Provider"}</p>
                    </div>
                    <div class="service-price">₹${service.price ?? "-"}</div>
                </div>

                <div class="service-meta">
                    <span><strong>Location:</strong> ${service.city || "-"}, ${service.area || "-"}</span>
                </div>

                <p class="service-description">${service.description || ""}</p>

                <div class="service-row-actions">
                    <label class="checkbox-wrap">
                        <input type="checkbox" class="service-select-checkbox" ${checked}>
                        <span>Select this service</span>
                    </label>

                    <button type="button" class="row-book-btn">Book Now</button>
                </div>
            </div>
        `;

        const checkbox = row.querySelector(".service-select-checkbox");
        checkbox.addEventListener("change", (e) => {
            toggleServiceSelection(service.id, service.title, e.target.checked);
        });

        const bookBtn = row.querySelector(".row-book-btn");
        bookBtn.addEventListener("click", () => {
            singleBook(service.id, service.title);
        });

        container.appendChild(row);
    });

    updateSelectedCount();
}

async function loadServices() {
    const container = document.getElementById("services");
    if (container) container.innerHTML = "<p>Loading providers...</p>";
    setBookingMessage("");

    try {
        const data = await apiFetch(`/services?category=${encodeURIComponent(currentCategory)}&subCategory=${encodeURIComponent(currentSubCategory)}`);
        renderServices(Array.isArray(data) ? data : []);
    } catch (err) {
        console.error(err);
        if (container) container.innerHTML = "<p>Failed to load services.</p>";
        setBookingMessage(err.message || "Unable to load providers right now.", true);
    }
}

async function singleBook(serviceId, serviceTitle) {
    const user = getCurrentUser();

    if (!user) {
        setBookingMessage("Please login first.", true);
        window.location.href = "login.html";
        return;
    }

    try {
        await apiFetch(`/bookings?userEmail=${encodeURIComponent(user.email)}`, {
            method: "POST",
            body: JSON.stringify({
                serviceId: serviceId,
                message: `Booking request for ${serviceTitle}`,
                bookingDate: getLocalDateTimeForJava()
            })
        });

        saveSuccessPayload([serviceTitle]);
        window.location.href = "booking-success.html";
    } catch (err) {
        console.error("Booking error:", err);
        setBookingMessage("Booking failed: " + (err.message || "Unknown error"), true);
    }
}

async function bookSelectedServices() {
    const user = getCurrentUser();

    if (!user) {
        setBookingMessage("Please login first.", true);
        window.location.href = "login.html";
        return;
    }

    if (selectedServices.size === 0) {
        setBookingMessage("Select at least one service.", true);
        return;
    }

    const bookedNames = [];
    const failedNames = [];

    for (const [serviceId, serviceTitle] of selectedServices.entries()) {
        try {
            await apiFetch(`/bookings?userEmail=${encodeURIComponent(user.email)}`, {
                method: "POST",
                body: JSON.stringify({
                    serviceId: serviceId,
                    message: `Booking request for ${serviceTitle}`,
                    bookingDate: getLocalDateTimeForJava()
                })
            });

            bookedNames.push(serviceTitle);
        } catch (err) {
            console.error("Failed booking:", serviceTitle, err);
            failedNames.push(serviceTitle);
        }
    }

    if (bookedNames.length > 0) {
        saveSuccessPayload(bookedNames);
        selectedServices.clear();
        window.location.href = "booking-success.html";
    } else {
        setBookingMessage(`Booking failed${failedNames.length ? " for: " + failedNames.join(", ") : "."}`, true);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const categoryParam = getQueryParam("category");
    const subCategoryParam = getQueryParam("subCategory");

    console.log("Selected Category from URL:", categoryParam);
    console.log("Selected SubCategory from URL:", subCategoryParam);

    if (!categoryParam || !categoryData[categoryParam]) {
        setBookingMessage("Invalid category selected.", true);

        const container = document.getElementById("services");
        const subcategoryList = document.getElementById("subcategoryList");
        const titleEl = document.getElementById("servicesPageTitle");
        const subtitleEl = document.getElementById("servicesPageSubtitle");
        const subTextEl = document.getElementById("selectedSubCategoryText");

        if (titleEl) titleEl.innerText = "Invalid Category";
        if (subtitleEl) subtitleEl.innerText = "The selected category was not found.";
        if (subTextEl) subTextEl.innerText = "Please go back and choose a valid category.";
        if (subcategoryList) subcategoryList.innerHTML = "<p>No subcategories found.</p>";
        if (container) container.innerHTML = "<p>Please open this page from the Services page.</p>";

        updateSelectedCount();
        return;
    }

    currentCategory = categoryParam;
    currentSubCategory = subCategoryParam || (categoryData[currentCategory]?.subs?.[0] || "");

    setPageHeader();
    renderSubcategoryList();
    updateSelectedCount();
    loadServices();
});