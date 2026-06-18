function loadBookedServices() {
    const list = document.getElementById("bookedServicesList");
    const backBtn = document.getElementById("backToSubcategoryBtn");

    if (!list) return;

    list.innerHTML = "";

    let services = [];
    let category = "";
    let subCategory = "";

    try {
        services = JSON.parse(sessionStorage.getItem("bookedServices")) || [];
        category = sessionStorage.getItem("bookedCategory") || "";
        subCategory = sessionStorage.getItem("bookedSubCategory") || "";
    } catch (err) {
        console.error("Failed to read booking success payload:", err);
    }

    if (backBtn) {
        if (category && subCategory) {
            backBtn.href = `services-page.html?category=${encodeURIComponent(category)}&subCategory=${encodeURIComponent(subCategory)}`;
        } else if (category) {
            backBtn.href = `services-page.html?category=${encodeURIComponent(category)}`;
        } else {
            backBtn.href = "services.html";
        }
    }

    if (!Array.isArray(services) || services.length === 0) {
        const li = document.createElement("li");
        li.textContent = "Your booking has been placed successfully.";
        list.appendChild(li);
        return;
    }

    services.forEach(name => {
        const li = document.createElement("li");
        li.textContent = name;
        list.appendChild(li);
    });
}

document.addEventListener("DOMContentLoaded", loadBookedServices);