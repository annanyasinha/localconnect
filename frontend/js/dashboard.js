function getCurrentUser() {
    try {
        const user = localStorage.getItem("user");
        return user ? JSON.parse(user) : null;
    } catch (err) {
        console.error("Failed to parse user:", err);
        return null;
    }
}

function goToAddService() {
    window.location.href = "add-service.html";
}

function goToServices() {
    window.location.href = "services.html";
}

function getDisplayName(user) {
    return user?.name || user?.fullName || "User";
}

function getDisplayEmail(user) {
    return user?.email || "-";
}

function getDisplayPhone(user) {
    return user?.phone || "-";
}

function getDisplayRole(user) {
    return user?.role || "USER";
}

function setDashboardStats({ role = "USER", bookings = 0, services = 0, status = "Active" }) {
    const roleEl = document.getElementById("statRole");
    const bookingsEl = document.getElementById("statBookings");
    const servicesEl = document.getElementById("statServices");
    const statusEl = document.getElementById("statStatus");

    if (roleEl) roleEl.innerText = role;
    if (bookingsEl) bookingsEl.innerText = bookings;
    if (servicesEl) servicesEl.innerText = services;
    if (statusEl) statusEl.innerText = status;
}

function getStatusBadge(status) {
    const value = (status || "").toUpperCase();

    let cls = "status-badge status-info";
    if (value.includes("CONFIRMED") || value.includes("APPROVED") || value.includes("ACTIVE")) {
        cls = "status-badge status-success";
    } else if (value.includes("PENDING")) {
        cls = "status-badge status-warning";
    } else if (value.includes("CANCELLED") || value.includes("REJECTED")) {
        cls = "status-badge status-danger";
    }

    return `<span class="${cls}">${status || "-"}</span>`;
}

function loadProfile() {
    const user = getCurrentUser();

    if (!user) {
        window.location.href = "login.html";
        return null;
    }

    const displayName = getDisplayName(user);
    const displayEmail = getDisplayEmail(user);
    const displayPhone = getDisplayPhone(user);
    const displayRole = getDisplayRole(user);

    const welcomeText = document.getElementById("welcomeText");
    const profileCard = document.getElementById("profileCard");
    const addServiceBtn = document.getElementById("addServiceBtn");
    const providerServicesSection = document.getElementById("providerServicesSection");
    const bookingTitle = document.getElementById("bookingTitle");

    if (welcomeText) {
        welcomeText.innerText = `Welcome, ${displayName}`;
    }

    if (profileCard) {
        profileCard.innerHTML = `
            <div class="dashboard-profile-top">
                <div class="dashboard-avatar">${displayName.charAt(0).toUpperCase()}</div>
                <div>
                    <h3>${displayName}</h3>
                    <p>${displayEmail}</p>
                </div>
            </div>

            <div class="dashboard-profile-grid">
                <div class="dashboard-mini-card">
                    <span>Phone</span>
                    <strong>${displayPhone}</strong>
                </div>
                <div class="dashboard-mini-card">
                    <span>Role</span>
                    <strong>${displayRole}</strong>
                </div>
                <div class="dashboard-mini-card">
                    <span>Status</span>
                    <strong>Active</strong>
                </div>
                <div class="dashboard-mini-card">
                    <span>Platform</span>
                    <strong>LocalConnect</strong>
                </div>
            </div>
        `;
    }

    if (displayRole === "PROVIDER") {
        if (addServiceBtn) addServiceBtn.style.display = "inline-block";
        if (providerServicesSection) providerServicesSection.style.display = "block";
        if (bookingTitle) bookingTitle.innerText = "Bookings On My Services";
    } else {
        if (addServiceBtn) addServiceBtn.style.display = "none";
        if (providerServicesSection) providerServicesSection.style.display = "none";
        if (bookingTitle) bookingTitle.innerText = "My Bookings";
    }

    setDashboardStats({
        role: displayRole,
        bookings: 0,
        services: displayRole === "PROVIDER" ? 0 : "-",
        status: "Active"
    });

    return user;
}

async function loadMyServices() {
    const user = getCurrentUser();
    const container = document.getElementById("myServices");

    if (!container) return 0;

    if (!user || user.role !== "PROVIDER") {
        container.innerHTML = "";
        return 0;
    }

    container.innerHTML = "<p>Loading your services...</p>";

    try {
        const data = await apiFetch(`/services/my?providerEmail=${encodeURIComponent(user.email)}`);
        container.innerHTML = "";

        if (!Array.isArray(data) || !data.length) {
            container.innerHTML = `<div class="empty-state-card"><p>No services added yet.</p></div>`;
            return 0;
        }

        data.forEach(service => {
            const card = document.createElement("div");
            card.className = "dashboard-service-card";

            card.innerHTML = `
                <div class="dashboard-card-head">
                    <div>
                        <span class="small-chip">${service.category || "Service"}</span>
                        <h3>${service.title || "Untitled Service"}</h3>
                    </div>
                    ${getStatusBadge(service.approvalStatus || "PENDING")}
                </div>

                <p class="dashboard-card-desc">${service.description || "Provider service listing."}</p>

                <div class="dashboard-card-grid-mini">
                    <div><strong>Sub-category:</strong> ${service.subCategory || "-"}</div>
                    <div><strong>Price:</strong> ₹${service.price ?? "-"}</div>
                    <div><strong>Location:</strong> ${service.city || "-"}, ${service.area || "-"}</div>
                    <div><strong>Available:</strong> ${service.available ?? "-"}</div>
                </div>
            `;

            container.appendChild(card);
        });

        return data.length;
    } catch (err) {
        console.error("Failed to load my services:", err);
        container.innerHTML = `<div class="empty-state-card"><p>Failed to load services.</p></div>`;
        return 0;
    }
}

async function loadBookings() {
    const user = getCurrentUser();
    const container = document.getElementById("myBookings");

    if (!container) return 0;

    if (!user) {
        container.innerHTML = `<div class="empty-state-card"><p>Please login first.</p></div>`;
        return 0;
    }

    container.innerHTML = "<p>Loading bookings...</p>";

    try {
        const url = user.role === "PROVIDER"
            ? `/bookings/provider?providerEmail=${encodeURIComponent(user.email)}`
            : `/bookings/my?userEmail=${encodeURIComponent(user.email)}`;

        const data = await apiFetch(url);
        container.innerHTML = "";

        if (!Array.isArray(data) || !data.length) {
            container.innerHTML = `<div class="empty-state-card"><p>No bookings found.</p></div>`;
            return 0;
        }

        data.forEach(booking => {
            const card = document.createElement("div");
            card.className = "dashboard-booking-card";

            card.innerHTML = `
                <div class="dashboard-card-head">
                    <div>
                        <span class="small-chip">Booking</span>
                        <h3>${booking.serviceTitle || "Service Booking"}</h3>
                    </div>
                    ${getStatusBadge(booking.status || "-")}
                </div>

                <div class="dashboard-card-grid-mini">
                    ${user.role === "PROVIDER" ? `<div><strong>User:</strong> ${booking.userName || "-"}</div>` : ""}
                    <div><strong>Message:</strong> ${booking.message || "-"}</div>
                    <div><strong>Booking Date:</strong> ${booking.bookingDate || "-"}</div>
                </div>
            `;

            container.appendChild(card);
        });

        return data.length;
    } catch (err) {
        console.error("Failed to load bookings:", err);
        container.innerHTML = `<div class="empty-state-card"><p>Failed to load bookings.</p></div>`;
        return 0;
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    const user = loadProfile();
    if (!user) return;

    const [serviceCount, bookingCount] = await Promise.all([
        loadMyServices(),
        loadBookings()
    ]);

    setDashboardStats({
        role: getDisplayRole(user),
        bookings: bookingCount,
        services: getDisplayRole(user) === "PROVIDER" ? serviceCount : "-",
        status: "Active"
    });
});