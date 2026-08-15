/**
 * LocalConnect AI Chatbot Widget JS
 */

(function () {
    let conversationId = localStorage.getItem('chatConversationId') || ('conv_' + Math.random().toString(36).substring(2, 9));
    localStorage.setItem('chatConversationId', conversationId);

    function initChatbot() {
        if (document.getElementById('chat-widget-container')) return;

        // 1. Inject Stylesheet if not included
        if (!document.querySelector('link[href*="chatbot.css"]')) {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = (window.location.pathname.includes('/pages/') ? '../css/chatbot.css' : 'css/chatbot.css');
            document.head.appendChild(link);
        }

        // 2. Inject HTML Widget Structure
        const widgetHTML = `
            <button id="chat-launcher-btn" title="Open LocalConnect AI Assistant">🤖</button>
            
            <div id="chat-widget-container">
                <div class="chat-header">
                    <div class="chat-header-info">
                        <div class="chat-avatar">🤖</div>
                        <div>
                            <h4 class="chat-header-title">LocalConnect AI Assistant</h4>
                            <div><span class="chat-status-dot"></span><span class="chat-status-text">Online (English & Hindi)</span></div>
                        </div>
                    </div>
                    <button class="chat-close-btn" id="chat-close-btn">✕</button>
                </div>
                
                <div class="chat-messages" id="chat-messages-body">
                    <div class="chat-msg assistant">👋 Hello! I am your LocalConnect AI Assistant.\nI can help you book services, manage bookings, check status, and answer questions.</div>
                </div>

                <div class="chat-quick-actions">
                    <button class="quick-chip" data-query="Recommend top services">⭐ Top Services</button>
                    <button class="quick-chip" data-query="Book service 51">📅 Book Service 51</button>
                    <button class="quick-chip" data-query="Show my bookings">📋 My Bookings</button>
                    <button class="quick-chip" data-query="नमस्ते help me">🇮🇳 हिंदी सहाय</button>
                </div>

                <div class="chat-input-area">
                    <input type="text" id="chat-input-field" class="chat-input" placeholder="Type a message (e.g. Book service #1)..." />
                    <button id="chat-send-btn" class="chat-send-btn">➤</button>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', widgetHTML);

        // 3. Event Listeners
        const launcher = document.getElementById('chat-launcher-btn');
        const container = document.getElementById('chat-widget-container');
        const closeBtn = document.getElementById('chat-close-btn');
        const sendBtn = document.getElementById('chat-send-btn');
        const inputField = document.getElementById('chat-input-field');
        const messagesBody = document.getElementById('chat-messages-body');

        launcher.addEventListener('click', () => container.classList.toggle('active'));
        closeBtn.addEventListener('click', () => container.classList.remove('active'));

        sendBtn.addEventListener('click', sendMessage);
        inputField.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') sendMessage();
        });

        document.querySelectorAll('.quick-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const query = chip.getAttribute('data-query');
                inputField.value = query;
                sendMessage();
            });
        });

        async function sendMessage() {
            const text = inputField.value.trim();
            if (!text) return;

            // Render User Bubble
            appendMessage('user', text);
            inputField.value = '';

            // Show Typing Indicator
            const typingElem = showTyping();

            // Get logged in user email if available
            let userEmail = localStorage.getItem('userEmail') || localStorage.getItem('email') || 'guest@localconnect.com';

            try {
                const apiEndpoint = window.location.pathname.includes('/pages/') 
                    ? 'http://localhost:8088/api/chat' 
                    : 'http://localhost:8088/api/chat';

                const response = await fetch(apiEndpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + (localStorage.getItem('token') || '')
                    },
                    body: JSON.stringify({
                        message: text,
                        conversationId: conversationId,
                        userEmail: userEmail
                    })
                });

                removeTyping(typingElem);

                if (response.ok) {
                    const data = await response.json();
                    appendMessage('assistant', data.reply);
                } else {
                    appendMessage('assistant', '⚠️ Unable to connect to AI server. Please verify backend is running on port 8088.');
                }
            } catch (err) {
                removeTyping(typingElem);
                appendMessage('assistant', '👋 Hello! AI backend is connecting... You can ask me to book service #1 or view active bookings!');
            }
        }

        function appendMessage(sender, text) {
            const msgDiv = document.createElement('div');
            msgDiv.className = `chat-msg ${sender}`;
            msgDiv.innerText = text;
            messagesBody.appendChild(msgDiv);
            messagesBody.scrollTop = messagesBody.scrollHeight;
        }

        function showTyping() {
            const typingDiv = document.createElement('div');
            typingDiv.className = 'typing-indicator';
            typingDiv.innerHTML = '<div class="typing-dot"></div><div class="typing-dot"></div><div class="typing-dot"></div>';
            messagesBody.appendChild(typingDiv);
            messagesBody.scrollTop = messagesBody.scrollHeight;
            return typingDiv;
        }

        function removeTyping(elem) {
            if (elem && elem.parentNode) {
                elem.parentNode.removeChild(elem);
            }
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initChatbot);
    } else {
        initChatbot();
    }
})();
