# LocalConnect - Smart Local Services Platform with Conversational AI Agent

**LocalConnect** is a state-of-the-art, full-stack local services platform designed to seamlessly connect service seekers with trusted local service providers (Plumbers, Tutors, Electricians, Cooks, Househelp, Carpenters, and Babysitters) in Jamshedpur. 

Powered by **Spring Boot 3**, **MySQL**, and an advanced **Spring AI Conversational Agent with Tool Calling**, LocalConnect offers a friction-free experience for searching, booking, and managing home services through both web interfaces and natural language AI conversations.

---

## 🌟 Key Highlights & AI Agent Architecture

### 🤖 1. LLM-First Conversational AI Agent
- **Spring AI Framework Integration**: Built on Spring AI (`1.0.0-M1`) with `@Tool` method annotations and dynamic function callbacks.
- **Natural Language Understanding (NLU)**: Understands complex natural language requests in English and Hindi (e.g. *"i need to get ready for my wedding"*, *"i have a problem in my washroom i need someone to fix it"*, *"teach me class 10 maths"*).
- **Conversational Memory**: Retains multi-turn conversation history and injects user profile context (Name, Email) into every prompt execution.
- **Dynamic Tool Calling**: The LLM autonomously invokes Java backend functions (`recommendServicesFunction`, `bookServiceFunction`, `cancelBookingFunction`, `rescheduleBookingFunction`, `checkBookingStatusFunction`) to query and mutate MySQL database records in real time.
- **Smart Ordinal Selection**: Resolves list-index references (e.g. *"service 3"*, *"3rd option"*, *"book the first one"*) to the exact item from recent search results.
- **Strict Date/Time Validation**: Ensures bookings are scheduled only with valid date/time expressions (e.g. *"Tomorrow at 5 PM"*), prompting users clearly if missing.

---

## 🛠️ Technology Stack

| Layer | Technology / Framework |
| :--- | :--- |
| **Backend Core** | Java 17, Spring Boot 3.3.2 |
| **AI & LLM Integration** | Spring AI (`1.0.0-M1`), Function Callbacks, `@Tool` Annotations |
| **Security & Auth** | Spring Security 6, JWT (`jjwt`), BCrypt Password Encoder |
| **Persistence & Database**| MySQL 8.0, Spring Data JPA, Hibernate ORM, H2 (Test Profile) |
| **DTO & Mapping** | Lombok, MapStruct |
| **Frontend** | HTML5, Vanilla CSS3 (Dark Mode & Glassmorphism Aesthetics), JavaScript (Fetch API) |
| **Testing** | JUnit 5, Mockito, Spring Boot Test |
| **Build & Tooling** | Apache Maven |

---

## 🚀 Core Features

### 1. User & Authentication Management
- Role-Based Access Control (RBAC): `USER`, `PROVIDER`, and `ADMIN`.
- Guest Fallback Account Generation: Allows guest users to seamlessly converse and book services without upfront registration hurdles.
- Secure JWT Token authentication with BCrypt password hashing.

### 2. Service Catalog & Discovery
- Detailed categories: **Tutor**, **Plumber**, **Electrician**, **Cook**, **Househelp**, **Carpenter**, and **Babysitter**.
- Provider approval workflow (`PENDING`, `APPROVED`, `REJECTED`).
- Filtering and search by category, city, area, price, and availability.

### 3. Booking Engine
- Real-time booking creation, rescheduling, and cancellation.
- Booking status tracking (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`).
- Automatic guest user provisioning on instant booking execution.

### 4. Interactive Floating AI Chat Widget
- Sleek, modern floating chat interface embedded across all frontend pages (`index.html`, `services.html`).
- Features typing indicators, quick action buttons, markdown formatting, and mobile responsiveness.

---

## 📡 Key API Endpoints

### 🤖 AI Assistant Chat
- `POST /api/chat/message` - Sends user message and conversation ID; returns AI reply, action type, and payload data.

### 🔐 Authentication
- `POST /api/auth/register` - User/Provider Registration
- `POST /api/auth/login` - Authenticates user and returns JWT Token

### 🛠️ Service Listings
- `GET /api/services` - List all approved service listings
- `GET /api/services/recommend` - Query service listings by category and city
- `POST /api/services` - Create a new service listing (Providers)

### 📋 Bookings
- `POST /api/bookings` - Create a service booking
- `GET /api/bookings/my` - Fetch current user's bookings
- `GET /api/bookings/{id}/status` - Check booking status
- `PUT /api/bookings/{id}/cancel` - Cancel a booking
- `PUT /api/bookings/{id}/reschedule` - Reschedule a booking

---

## 🧪 Testing & Quality Assurance

- **100% Automated Test Suite Passed** (16/16 Unit and Integration Tests):
  - `ChatServiceTest`: Verifies greeting pipeline, booking state resets, ordinal selections, and date parsing.
  - `BookingServiceImplTest`: Verifies booking persistence, guest account creation, and security constraints.
  - `ServiceListingServiceImplTest` & `UserServiceImplTest`: Tests CRUD workflows and password encoding.
- Execute unit tests using Maven:
  ```bash
  ./mvnw test
  ```

---

## 💻 Running the Application Locally

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/annanyasinha/localconnect.git
   cd localconnect
   ```

2. **Start the Backend Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   *The server starts on `http://localhost:8088`.*

3. **Open the Web Application**:
   Open [`frontend/index.html`](file:///Users/annanyasinha/Documents/localconnect/frontend/index.html) in your browser.
