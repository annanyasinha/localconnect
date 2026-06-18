const categoryMap = {
    "Plumber": [
        "Pipe Leakage Repair",
        "Tap Installation",
        "Bathroom Fitting",
        "Water Tank Cleaning",
        "Drain Blockage Fix"
    ],
    "Electrician": [
        "Switch Repair",
        "Fan Installation",
        "Wiring Work",
        "Light Fitting",
        "Inverter Support"
    ],
    "Tutor": [
        "Math Tuition",
        "Science Tuition",
        "English Tuition",
        "Exam Preparation",
        "Primary Classes"
    ],
    "Househelp": [
        "Dishwashing",
        "Brooming",
        "Mopping",
        "Dusting",
        "Laundry Help"
    ],
    "Cook": [
        "Daily Meal Cooking",
        "Tiffin Preparation",
        "Vegetarian Cooking",
        "Non-Veg Cooking",
        "Kitchen Assistance"
    ],
    "Babysitter": [
        "Infant Care",
        "Toddler Supervision",
        "Feeding Assistance",
        "Playtime Care",
        "Night Babysitting"
    ],
    "Carpenter": [
        "Furniture Repair",
        "Door Repair",
        "Window Fitting",
        "Shelf Installation",
        "Bed Assembly"
    ],
    "Car Cleaner": [
        "Exterior Wash",
        "Interior Vacuum",
        "Seat Cleaning",
        "Dashboard Cleaning",
        "Basic Detailing"
    ],
    "Mason": [
        "Wall Repair",
        "Brick Work",
        "Floor Tiling",
        "Plaster Work",
        "Small Construction"
    ],
    "Pest Control": [
        "Termite Control",
        "Mosquito Control",
        "Cockroach Treatment",
        "Rodent Control",
        "Home Sanitization"
    ],
    "Beautician": [
        "Bridal Makeup",
        "Party Makeup",
        "Hair Styling",
        "Facial Service",
        "Waxing / Grooming"
    ],
    "Senior Care Support": [
        "Daily Assistance",
        "Mobility Support",
        "Meal Support",
        "Medication Reminder",
        "Companionship Care"
    ]
};

function getCurrentUser() {
    try {
        const user = localStorage.getItem("user");
        return user ? JSON.parse(user) : null;
    } catch (err) {
        console.error("Failed to parse user from localStorage:", err);
        return null;
    }
}

function showMessage(text, isError = false) {
    const msg = document.getElementById("msg");
    if (!msg) return;

    msg.innerText = text;
    msg.style.color = isError ? "#f87171" : "#38bdf8";
}

function populateCategories() {
    const categorySelect = document.getElementById("category");
    if (!categorySelect) return;

    categorySelect.innerHTML = `<option value="">Select Category</option>`;

    Object.keys(categoryMap).forEach(category => {
        const option = document.createElement("option");
        option.value = category;
        option.textContent = category;
        categorySelect.appendChild(option);
    });
}

function resetSubCategories() {
    const subCategorySelect = document.getElementById("subCategory");
    if (!subCategorySelect) return;

    subCategorySelect.innerHTML = `<option value="">Select Sub-Category</option>`;
}

function updateSubCategories() {
    const category = document.getElementById("category")?.value;
    const subCategorySelect = document.getElementById("subCategory");

    if (!subCategorySelect) return;

    resetSubCategories();

    if (!category || !categoryMap[category]) return;

    categoryMap[category].forEach(sub => {
        const option = document.createElement("option");
        option.value = sub;
        option.textContent = sub;
        subCategorySelect.appendChild(option);
    });
}

function resetForm() {
    document.getElementById("title").value = "";
    document.getElementById("category").value = "";
    resetSubCategories();
    document.getElementById("description").value = "";
    document.getElementById("price").value = "";
    document.getElementById("city").value = "";
    document.getElementById("area").value = "";
    document.getElementById("imageUrl").value = "";
    document.getElementById("available").value = "true";
}

async function addService() {
    showMessage("");

    const user = getCurrentUser();

    if (!user) {
        showMessage("Please login first.", true);
        window.location.href = "login.html";
        return;
    }

    if (user.role !== "PROVIDER") {
        showMessage("Only providers can add services.", true);
        return;
    }

    const title = document.getElementById("title").value.trim();
    const category = document.getElementById("category").value.trim();
    const subCategory = document.getElementById("subCategory").value.trim();
    const description = document.getElementById("description").value.trim();
    const priceValue = document.getElementById("price").value;
    const city = document.getElementById("city").value.trim();
    const area = document.getElementById("area").value.trim();
    const imageUrl = document.getElementById("imageUrl").value.trim();
    const available = document.getElementById("available").value === "true";

    const price = parseFloat(priceValue);

    if (!title) {
        showMessage("Service title is required.", true);
        return;
    }

    if (!category) {
        showMessage("Please select a category.", true);
        return;
    }

    if (!subCategory) {
        showMessage("Please select a sub-category.", true);
        return;
    }

    if (!description) {
        showMessage("Description is required.", true);
        return;
    }

    if (isNaN(price) || price <= 0) {
        showMessage("Please enter a valid price greater than 0.", true);
        return;
    }

    if (!city) {
        showMessage("City is required.", true);
        return;
    }

    if (!area) {
        showMessage("Area is required.", true);
        return;
    }

    const data = {
        title,
        category,
        subCategory,
        description,
        price,
        city,
        area,
        imageUrl,
        available
    };

    try {
        await apiFetch(`/services?providerEmail=${encodeURIComponent(user.email)}`, {
            method: "POST",
            body: JSON.stringify(data)
        });

        showMessage("Service submitted successfully. Waiting for admin approval.");
        resetForm();
    } catch (err) {
        console.error("Add service error:", err);
        showMessage(err.message || "Failed to add service.", true);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    populateCategories();

    const categorySelect = document.getElementById("category");
    if (categorySelect) {
        categorySelect.addEventListener("change", updateSubCategories);
    }
});