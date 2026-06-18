function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    window.location.href = "login.html";
}

async function loadPendingServices() {
    try {
        const data = await apiFetch("/admin/pending-services");
        const container = document.getElementById("pendingServices");
        container.innerHTML = "";

        if (!data.length) {
            container.innerHTML = "<p>No pending services.</p>";
            return;
        }

        data.forEach(service => {
            const card = document.createElement("div");
            card.className = "card";

            card.innerHTML = `
                <h3>${service.title}</h3>
                <p><strong>Provider:</strong> ${service.providerName}</p>
                <p><strong>Category:</strong> ${service.category}</p>
                <p><strong>Sub-Category:</strong> ${service.subCategory}</p>
                <p><strong>Price:</strong> ₹${service.price}</p>
                <p><strong>Location:</strong> ${service.city}, ${service.area}</p>
                <p>${service.description || ""}</p>
                <div class="actions">
                    <button onclick="approveService(${service.id})">Approve</button>
                    <button class="danger" onclick="rejectService(${service.id})">Reject</button>
                </div>
            `;

            container.appendChild(card);
        });
    } catch (err) {
        console.error(err);
        document.getElementById("pendingServices").innerHTML = "<p>Failed to load pending services.</p>";
    }
}

async function loadUsers() {
    try {
        const data = await apiFetch("/admin/users");
        const container = document.getElementById("users");
        container.innerHTML = "";

        if (!data.length) {
            container.innerHTML = "<p>No users found.</p>";
            return;
        }

        data.forEach(user => {
            const card = document.createElement("div");
            card.className = "card";

            card.innerHTML = `
                <h3>${user.fullName}</h3>
                <p><strong>Email:</strong> ${user.email}</p>
                <p><strong>Phone:</strong> ${user.phone || "-"}</p>
                <p><strong>Role:</strong> ${user.role}</p>
                <p><strong>Enabled:</strong> ${user.enabled}</p>
            `;

            container.appendChild(card);
        });
    } catch (err) {
        console.error(err);
        document.getElementById("users").innerHTML = "<p>Failed to load users.</p>";
    }
}

async function approveService(id) {
    try {
        await apiFetch(`/admin/services/${id}/approve`, {
            method: "PUT"
        });
        alert("Service approved");
        loadPendingServices();
    } catch (err) {
        console.error(err);
        alert("Failed to approve service");
    }
}

async function rejectService(id) {
    try {
        await apiFetch(`/admin/services/${id}/reject`, {
            method: "PUT"
        });
        alert("Service rejected");
        loadPendingServices();
    } catch (err) {
        console.error(err);
        alert("Failed to reject service");
    }
}

loadPendingServices();
loadUsers();