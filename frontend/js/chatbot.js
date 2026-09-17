/**
 * LocalConnect AI Chatbot Widget
 */

(function () {

    function getLoggedInUser() {

        try {
            return JSON.parse(
                localStorage.getItem("user") || "null"
            );
        } catch (error) {
            return null;
        }
    }

    function getConversationId(userEmail) {

        const key = "chatConversationId_" + userEmail;

        let conversationId = localStorage.getItem(key);

        if (!conversationId) {

            conversationId =
                typeof crypto !== "undefined"
                    && typeof crypto.randomUUID === "function"
                    ? crypto.randomUUID()
                    : "conv_" + Date.now()
                    + "_" + Math.random().toString(36).slice(2);

            localStorage.setItem(key, conversationId);
        }

        return conversationId;
    }

    function initChatbot() {

        if (document.getElementById("chat-widget-container")) {
            return;
        }

        // Load stylesheet.

        if (!document.querySelector('link[href*="chatbot.css"]')) {

            const link = document.createElement("link");

            link.rel = "stylesheet";

            link.href = window.location.pathname.includes("/pages/")
                ? "../css/chatbot.css"
                : "css/chatbot.css";

            document.head.appendChild(link);
        }

        // Create widget.

        const widgetHTML = `
            <button
                id="chat-launcher-btn"
                title="Open LocalConnect AI Assistant"
            >
                🤖
            </button>

            <div id="chat-widget-container">

                <div class="chat-header">

                    <div class="chat-header-info">

                        <div class="chat-avatar">🤖</div>

                        <div>
                            <h4 class="chat-header-title">
                                LocalConnect AI Assistant
                            </h4>

                            <div>
                                <span class="chat-status-text">
                                    Service Assistant
                                </span>
                            </div>
                        </div>

                    </div>

                    <button
                        class="chat-close-btn"
                        id="chat-close-btn"
                        title="Close chatbot"
                    >
                        ✕
                    </button>

                </div>

                <div
                    class="chat-messages"
                    id="chat-messages-body"
                >
                    <div class="chat-msg assistant">
                        👋 Hello! I can help you find local services
                        and manage your bookings.
                    </div>
                </div>

                <div class="chat-quick-actions">

                    <button
                        class="quick-chip"
                        data-query="Recommend top services"
                    >
                        ⭐ Top Services
                    </button>

                    <button
                        class="quick-chip"
                        data-query="Show available plumbers"
                    >
                        🔧 Find Plumbers
                    </button>

                    <button
                        class="quick-chip"
                        data-query="Show my bookings"
                    >
                        📋 My Bookings
                    </button>

                </div>

                <div class="chat-input-area">

                    <input
                        type="text"
                        id="chat-input-field"
                        class="chat-input"
                        placeholder="Type your message..."
                    />

                    <button
                        id="chat-send-btn"
                        class="chat-send-btn"
                        title="Send message"
                    >
                        ➤
                    </button>

                </div>

            </div>
        `;

        document.body.insertAdjacentHTML(
            "beforeend",
            widgetHTML
        );

        const launcher =
            document.getElementById("chat-launcher-btn");

        const container =
            document.getElementById("chat-widget-container");

        const closeBtn =
            document.getElementById("chat-close-btn");

        const sendBtn =
            document.getElementById("chat-send-btn");

        const inputField =
            document.getElementById("chat-input-field");

        const messagesBody =
            document.getElementById("chat-messages-body");

        launcher.addEventListener("click", () => {
            container.classList.toggle("active");
        });

        closeBtn.addEventListener("click", () => {
            container.classList.remove("active");
        });

        sendBtn.addEventListener("click", sendMessage);

        inputField.addEventListener("keydown", event => {

            if (event.key === "Enter") {
                sendMessage();
            }
        });

        document.querySelectorAll(".quick-chip")
            .forEach(chip => {

                chip.addEventListener("click", () => {

                    inputField.value =
                        chip.getAttribute("data-query");

                    sendMessage();
                });
            });

        // -----------------------------------------
        // SEND MESSAGE
        // -----------------------------------------

        async function sendMessage() {

            if (sendBtn.disabled) {
                return;
            }

            const text = inputField.value.trim();

            if (!text) {
                return;
            }

            const token = localStorage.getItem("token");

            const user = getLoggedInUser();

            const userEmail = user?.email;

            if (!token || !userEmail) {

                appendMessage(
                    "assistant",
                    "🔒 Please log in to use the AI assistant."
                );

                return;
            }

            const conversationId =
                getConversationId(userEmail);

            appendMessage("user", text);

            inputField.value = "";

            const typingElem = showTyping();

            sendBtn.disabled = true;

            try {

                const apiEndpoint =
                    typeof API_BASE !== "undefined"
                        ? API_BASE + "/chat"
                        : (
                            window.location.hostname === "localhost"
                                ||
                                window.location.hostname === "127.0.0.1"
                                ||
                                window.location.protocol === "file:"
                                ? "http://localhost:8088/api/chat"
                                : "https://localconnect-v4rp.onrender.com/api/chat"
                        );

                const response = await fetch(apiEndpoint, {

                    method: "POST",

                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + token
                    },

                    body: JSON.stringify({
                        message: text,
                        conversationId: conversationId
                    })
                });

                if (response.status === 401) {

                    appendMessage(
                        "assistant",
                        "🔒 Session expired. Please log in again."
                    );

                    return;
                }

                if (response.status === 403) {

                    localStorage.removeItem(
                        "chatConversationId_" + userEmail
                    );

                    appendMessage(
                        "assistant",
                        "⛔ Access denied to this conversation. "
                        + "Please send another message to start a new one."
                    );

                    return;
                }

                if (response.status === 429) {

                    appendMessage(
                        "assistant",
                        "⏳ Too many requests. Please wait "
                        + "a minute before trying again."
                    );

                    return;
                }

                if (!response.ok) {

                    console.error(
                        "Chat API error:",
                        response.status
                    );

                    appendMessage(
                        "assistant",
                        "⚠️ The chatbot couldn't process your request."
                    );

                    return;
                }

                const data = await response.json();

                if (data.conversationId) {

                    localStorage.setItem(
                        "chatConversationId_" + userEmail,
                        data.conversationId
                    );
                }

                appendMessage(
                    "assistant",
                    data.reply || "No response returned."
                );

            } catch (error) {

                console.error(
                    "Chatbot network error:",
                    error
                );

                appendMessage(
                    "assistant",
                    "⚠️ Cannot reach the chatbot server. "
                    + "Please try again later."
                );

            } finally {

                removeTyping(typingElem);

                sendBtn.disabled = false;
            }
        }

        // -----------------------------------------
        // DISPLAY CHAT MESSAGE
        // -----------------------------------------

        function appendMessage(sender, text) {

            const message = document.createElement("div");

            message.className = `chat-msg ${sender}`;

            message.innerText = text;

            messagesBody.appendChild(message);

            messagesBody.scrollTop =
                messagesBody.scrollHeight;
        }

        // -----------------------------------------
        // TYPING INDICATOR
        // -----------------------------------------

        function showTyping() {

            const indicator = document.createElement("div");

            indicator.className = "typing-indicator";

            indicator.innerHTML = `
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
            `;

            messagesBody.appendChild(indicator);

            messagesBody.scrollTop =
                messagesBody.scrollHeight;

            return indicator;
        }

        function removeTyping(element) {

            if (element && element.parentNode) {
                element.parentNode.removeChild(element);
            }
        }
    }

    if (document.readyState === "loading") {

        document.addEventListener(
            "DOMContentLoaded",
            initChatbot
        );

    } else {

        initChatbot();
    }

})();