const categoryData = {
    "Plumber": {
        subtitle: "Pipe, fitting and water-related repair services",
        image: "../assets/images/plumber.jpg",
        video: "../assets/videos/plumber.mp4",
        subs: [
            "Pipe Leakage Repair",
            "Tap Installation",
            "Bathroom Fitting",
            "Water Tank Cleaning",
            "Drain Blockage Fix"
        ]
    },
    "Electrician": {
        subtitle: "Wiring and electrical repair services",
        image: "../assets/images/electrician.jpg",
        video: "../assets/videos/electrician.mp4",
        subs: [
            "Switch Repair",
            "Fan Installation",
            "Wiring Work",
            "Light Fitting",
            "Inverter Support"
        ]
    },
    "Tutor": {
        subtitle: "Home tuition and academic support services",
        image: "../assets/images/tutor.jpg",
        video: "../assets/videos/tutor.mp4",
        subs: [
            "Math Tuition",
            "Science Tuition",
            "English Tuition",
            "Exam Preparation",
            "Primary Classes"
        ]
    },
    "Househelp": {
        subtitle: "Daily household cleaning and maintenance support",
        image: "../assets/images/househelp.jpg",
        video: "../assets/videos/househelp.mp4",
        subs: [
            "Dishwashing",
            "Brooming",
            "Mopping",
            "Dusting",
            "Laundry Help"
        ]
    },
    "Cook": {
        subtitle: "Home meal preparation and kitchen support services",
        image: "../assets/images/cook.jpg",
        video: "../assets/videos/cook.mp4",
        subs: [
            "Daily Meal Cooking",
            "Tiffin Preparation",
            "Vegetarian Cooking",
            "Non-Veg Cooking",
            "Kitchen Assistance"
        ]
    },
    "Babysitter": {
        subtitle: "Trusted support for baby care and child assistance",
        image: "../assets/images/babysitter.jpg",
        video: "../assets/videos/babysitter.mp4",
        subs: [
            "Infant Care",
            "Toddler Supervision",
            "Feeding Assistance",
            "Playtime Care",
            "Night Babysitting"
        ]
    },
    "Carpenter": {
        subtitle: "Furniture repair and wooden installation services",
        image: "../assets/images/carpenter.jpg",
        video: "../assets/videos/carpenter.mp4",
        subs: [
            "Furniture Repair",
            "Door Repair",
            "Window Fitting",
            "Shelf Installation",
            "Bed Assembly"
        ]
    },
    "Car Cleaner": {
        subtitle: "Doorstep car cleaning and detailing support",
        image: "../assets/images/car-cleaner.jpg",
        video: "../assets/videos/car-cleaner.mp4",
        subs: [
            "Exterior Wash",
            "Interior Vacuum",
            "Seat Cleaning",
            "Dashboard Cleaning",
            "Basic Detailing"
        ]
    },
    "Mason": {
        subtitle: "Construction and repair support for home spaces",
        image: "../assets/images/mason.jpg",
        video: "../assets/videos/mason.mp4",
        subs: [
            "Wall Repair",
            "Brick Work",
            "Floor Tiling",
            "Plaster Work",
            "Small Construction"
        ]
    },
    "Pest Control": {
        subtitle: "Home pest removal and sanitation services",
        image: "../assets/images/pest-control.jpg",
        video: "../assets/videos/pest-control.mp4",
        subs: [
            "Termite Control",
            "Mosquito Control",
            "Cockroach Treatment",
            "Rodent Control",
            "Home Sanitization"
        ]
    },
    "Beautician": {
        subtitle: "Beauty, grooming and home salon services",
        image: "../assets/images/beautician.jpg",
        video: "../assets/videos/beautician.mp4",
        subs: [
            "Bridal Makeup",
            "Party Makeup",
            "Hair Styling",
            "Facial Service",
            "Waxing / Grooming"
        ]
    },
    "Senior Care Support": {
        subtitle: "Routine support and companionship care for adults",
        image: "../assets/images/senior-care.jpg",
        video: "../assets/videos/senior-care.mp4",
        subs: [
            "Daily Assistance",
            "Mobility Support",
            "Meal Support",
            "Medication Reminder",
            "Companionship Care"
        ]
    }
};

function getQueryParam(name) {
    const params = new URLSearchParams(window.location.search);
    return params.get(name);
}

function renderCategoryHeader(categoryName) {
    const info = categoryData[categoryName];
    document.getElementById("categoryTitle").innerText = categoryName || "Category";
    document.getElementById("categorySubtitle").innerText =
        info ? info.subtitle : "Explore services";
}

function setCategoryVideo(categoryName) {
    const video = document.getElementById("categoryVideo");
    const videoSource = document.getElementById("categoryVideoSource");
    const info = categoryData[categoryName];

    if (!video || !videoSource || !info || !info.video) return;

    videoSource.src = info.video;
    video.load();

    video.addEventListener("error", () => {
        video.style.display = "none";
    }, { once: true });

    video.style.display = "block";
}

function renderSubcategories(categoryName) {
    const info = categoryData[categoryName];
    const container = document.getElementById("subcategoryGrid");
    container.innerHTML = "";

    if (!info) {
        container.innerHTML = "<p>No subcategories found.</p>";
        return;
    }

    info.subs.forEach(sub => {
        const card = document.createElement("div");
        card.className = "card subcategory-card";

        card.innerHTML = `
            <img src="${info.image}" alt="${sub}" class="card-image">
            <h3>${sub}</h3>
            <p>${categoryName} service under LocalConnect.</p>
        `;

        card.onclick = () => {
            window.location.href =
                `services-page.html?category=${encodeURIComponent(categoryName)}&subCategory=${encodeURIComponent(sub)}`;
        };

        container.appendChild(card);
    });
}

const currentCategory = getQueryParam("name") || "Plumber";
renderCategoryHeader(currentCategory);
setCategoryVideo(currentCategory);
renderSubcategories(currentCategory);