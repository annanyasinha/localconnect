const API_BASE =
    window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
        ? "http://localhost:8088/api"
        : "http://localhost:8088/api";

function getToken() {
    return localStorage.getItem("token");
}

async function apiFetch(url, options = {}) {
    const headers = {
        ...(options.headers || {})
    };

    const isFormData = options.body instanceof FormData;
    if (!isFormData) {
        headers["Content-Type"] = "application/json";
    }

    const token = getToken();
    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }

    let response;
    try {
        response = await fetch(API_BASE + url, {
            ...options,
            headers
        });
    } catch (err) {
        throw new Error("Server not reachable");
    }

    const text = await response.text();
    let data = {};

    try {
        data = text ? JSON.parse(text) : {};
    } catch (e) {
        data = { message: text || "Invalid server response" };
    }

    if (!response.ok) {
        throw new Error(data.message || "Request failed");
    }

    return data;
}