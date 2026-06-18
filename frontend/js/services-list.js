function getQueryParam(name) {
    const params = new URLSearchParams(window.location.search);
    return params.get(name);
}

const category = getQueryParam("category");
const subCategory = getQueryParam("subCategory");

document.getElementById("title").innerText =
    `${category} → ${subCategory}`;

function loadServices() {
    apiFetch(`/services?category=${encodeURIComponent(category)}&subCategory=${encodeURIComponent(subCategory)}`)
        .then(data => {
            const container = document.getElementById("services");
            container.innerHTML = "";

            if (!data.length) {
                container.innerHTML = "<p>No providers found.</p>";
                return;
            }

            data.forEach(service => {
                const card = document.createElement("div");
                card.className = "card";

                card.innerHTML = `
                    <h3>${service.title}</h3>
                    <p><strong>Provider:</strong> ${service.providerName}</p>
                    <p><strong>Price:</strong> ₹${service.price}</p>
                    <p><strong>Location:</strong> ${service.city}, ${service.area}</p>
                    <button onclick="book(${service.id})">Book</button>
                `;

                container.appendChild(card);
            });
        });
}

function book(id) {
    const user = JSON.parse(localStorage.getItem("user"));

    if (!user) {
        alert("Login first");
        return;
    }

    apiFetch(`/bookings?userEmail=${user.email}`, {
        method: "POST",
        body: JSON.stringify({
            serviceId: id,
            bookingDate: new Date().toISOString()
        })
    }).then(() => alert("Booked!"));
}

loadServices();