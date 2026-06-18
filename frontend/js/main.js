function getCurrentUser() {
    try {
        const user = localStorage.getItem("user");
        return user ? JSON.parse(user) : null;
    } catch (err) {
        console.error("Failed to parse user:", err);
        return null;
    }
}

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    localStorage.removeItem("selectedServicesDraft");
    sessionStorage.removeItem("bookedServices");
    sessionStorage.removeItem("bookedCategory");
    sessionStorage.removeItem("bookedSubCategory");

    const currentPath = window.location.pathname.replace(/\\/g, "/").toLowerCase();
    const isInsidePagesFolder = currentPath.includes("/pages/");

    if (isInsidePagesFolder) {
        window.location.href = "../index.html";
    } else {
        window.location.href = "index.html";
    }
}

function renderNavbar(containerId, rootPath = "") {
    const user = getCurrentUser();
    const container = document.getElementById(containerId);
    if (!container) return;

    let links = `
        <a href="${rootPath}index.html">Home</a>
        <a href="${rootPath}pages/services.html">Services</a>
    `;

    if (!user) {
        links += `
            <a href="${rootPath}pages/login.html">Login</a>
            <a href="${rootPath}pages/register.html">Register</a>
        `;
    } else {
        links += `<a href="${rootPath}pages/dashboard.html">Dashboard</a>`;

        if (user.role === "PROVIDER") {
            links += `<a href="${rootPath}pages/add-service.html">Add Service</a>`;
        }

        if (user.role === "ADMIN") {
            links += `<a href="${rootPath}pages/admin.html">Admin Panel</a>`;
        }

        links += `<button type="button" onclick="logout()">Logout</button>`;
    }

    container.innerHTML = `
        <nav class="navbar">
            <div class="logo">LocalConnect</div>
            <div class="nav-links">${links}</div>
        </nav>
    `;
}
document.addEventListener("DOMContentLoaded", () => {
    const videos = document.querySelectorAll(".bg-video");

    if (!videos.length) return;

    let current = 0;

    function changeVideo() {
        videos[current].classList.remove("active");

        current = (current + 1) % videos.length;

        videos[current].classList.add("active");
    }

    setInterval(changeVideo, 5000); // change every 5 sec
});