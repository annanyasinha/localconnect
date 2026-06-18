async function register() {
    const msg = document.getElementById("msg");

    const data = {
        fullName: document.getElementById("name").value.trim(),
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value.trim(),
        phone: document.getElementById("phone").value.trim(),
        role: document.getElementById("role").value
    };

    try {
        const res = await apiFetch("/auth/register", {
            method: "POST",
            body: JSON.stringify(data)
        });

        localStorage.setItem("token", res.token);
        localStorage.setItem("user", JSON.stringify(res.user));
        msg.innerText = "Registration successful";
        window.location.href = "dashboard.html";
    } catch (err) {
        console.error(err);
        msg.innerText = err.message || "Registration failed";
    }
}

async function login() {
    const msg = document.getElementById("msg");

    const data = {
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value.trim()
    };

    try {
        const res = await apiFetch("/auth/login", {
            method: "POST",
            body: JSON.stringify(data)
        });

        localStorage.setItem("token", res.token);
        localStorage.setItem("user", JSON.stringify(res.user));
        msg.innerText = "Login successful";

        if (res.user.role === "ADMIN") {
            window.location.href = "admin.html";
        } else {
            window.location.href = "dashboard.html";
        }
    } catch (err) {
        console.error(err);
        msg.innerText = err.message || "Login failed";
    }
}