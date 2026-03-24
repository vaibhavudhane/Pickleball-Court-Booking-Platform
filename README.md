# 🏓 Pickleball Court Booking Platform

> Assignment 1 — Full-Stack Web Application  
> Submitted by: **Vaibhav Udhane**  
> GitHub: [vaibhavudhane/Pickleball-Court-Booking-Platform](https://github.com/vaibhavudhane/Pickleball-Court-Booking-Platform)

---

## 📌 AI Disclosure

Portions of this project were built with AI assistance (Claude by Anthropic). All code has been reviewed, tested, and understood by the author. I can walk through and explain every line of code in a follow-up session.

---

## 📋 Table of Contents

1. [Features Implemented](#features-implemented)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Step-by-Step Setup — Run Locally](#step-by-step-setup--run-locally)
6. [Environment Variables](#environment-variables)
7. [Pre-Seeded Test Credentials](#pre-seeded-test-credentials)
8. [API Documentation](#api-documentation)
9. [Business Rules](#business-rules)
10. [Known Bugs Fixed During Testing](#known-bugs-fixed-during-testing)

---

## ✅ Features Implemented

### Core Features
| Feature | Status |
|---|---|
| User accounts & JWT authentication | ✅ Complete |
| Colour-coded slot availability grid | ✅ Complete |
| Multi-slot cart & checkout | ✅ Complete |
| Conflict-safe concurrent booking | ✅ Complete |
| Weekday / weekend pricing | ✅ Complete |
| Booking history | ✅ Complete |

### Advanced Features
| Feature | Status |
|---|---|
| Dual-role system (Owner / Booker) | ✅ Complete |
| Venue creation & management | ✅ Complete |
| Photo uploads (up to 5 per venue) | ✅ Complete |
| Marketplace with availability filters | ✅ Complete |
| Per-venue detail & booking page | ✅ Complete |
| Booking rescheduling (12-hour rule) | ✅ Complete |

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Language |
| Spring Boot | 3.5.11 | Application framework |
| Spring Security 6 | — | JWT authentication & role-based access control |
| Spring Data JPA + Hibernate | — | Database ORM |
| PostgreSQL | 14+ | Production database |
| H2 | — | In-memory DB (development/testing) |
| JJWT | — | JWT generation and validation |
| Lombok | — | Boilerplate reduction |
| Bean Validation | — | Request DTO validation |
| SpringDoc OpenAPI | 2.8.5 | Swagger UI (`/swagger-ui/index.html`) |
| Maven | — | Build tool |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 19 | UI framework |
| Vite | 8 | Dev server and build tool |
| React Router | 7 | Client-side routing & role-based route guards |
| Axios | 1.13 | HTTP client with JWT interceptor |
| Tailwind CSS | 3.4 | Styling |
| react-datepicker | 9 | Date picker |
| date-fns | 4 | Date formatting utilities |

---

## 📁 Project Structure

```
Pickleball-Court-Booking-Platform/
│
├── src/main/java/com/pickleball/pickleball_backend/
│   ├── config/
│   │   ├── JacksonConfig.java            # JSON serialization settings
│   │   ├── SecurityConfig.java           # Spring Security, CORS, JWT filter chain
│   │   ├── SwaggerConfig.java            # OpenAPI / Swagger UI setup
│   │   └── WebMvcConfig.java             # Static resource handler for uploads/
│   │
│   ├── controller/
│   │   ├── AuthController.java           # POST /api/auth/register, /login
│   │   ├── AvailabilityController.java   # GET /api/venues/{id}/availability
│   │   ├── BookingController.java        # Checkout, My Bookings, Reschedule
│   │   ├── CartController.java           # Add, View, Remove, Clear cart
│   │   ├── OwnerController.java          # Venue CRUD, photos, bookings dashboard
│   │   └── VenueController.java          # Public marketplace + venue detail
│   │
│   ├── dto/
│   │   ├── request/                      # LoginRequest, RegisterRequest,
│   │   │                                 # AddToCartRequest, CartItemRequest,
│   │   │                                 # CreateVenueRequest, RescheduleRequest
│   │   └── response/                     # AuthResponseDTO, BookingDTO,
│   │                                     # CartResponseDTO, CartItemDTO,
│   │                                     # CheckoutResponseDTO, VenueCardDTO,
│   │                                     # VenueDetailDTO, AvailabilityResponseDTO,
│   │                                     # CourtAvailabilityDTO, SlotDTO, ErrorResponse
│   │
│   ├── entity/
│   │   ├── Booking.java                  # bookings table
│   │   ├── CartItem.java                 # cart_items table
│   │   ├── Court.java                    # courts table
│   │   ├── User.java                     # users table
│   │   ├── Venue.java                    # venues table
│   │   └── VenuePhoto.java              # venue_photos table
│   │
│   ├── enums/
│   │   ├── BookingStatus.java            # CONFIRMED, RESCHEDULED
│   │   ├── Role.java                     # OWNER, BOOKER (also accepts 1 / 0)
│   │   └── SlotStatus.java              # AVAILABLE, BOOKED, UNAVAILABLE
│   │
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java   # Maps all exceptions to JSON error responses
│   │   ├── CheckoutConflictException.java
│   │   ├── RescheduleNotAllowedException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── SlotAlreadyBookedException.java
│   │   └── UnauthorizedException.java
│   │
│   ├── repository/
│   │   ├── BookingRepository.java        # includes existsOverlappingBooking() JPQL
│   │   ├── CartItemRepository.java
│   │   ├── CourtRepository.java
│   │   ├── UserRepository.java
│   │   ├── VenuePhotoRepository.java
│   │   └── VenueRepository.java
│   │
│   ├── service/impl/
│   │   ├── AuthServiceImpl.java          # Register, login, BCrypt hashing
│   │   ├── AvailabilityServiceImpl.java  # Dynamic slot grid with boundary splitting
│   │   ├── BookingServiceImpl.java       # Atomic checkout + reschedule logic
│   │   ├── CartServiceImpl.java          # 13-step cart validation pipeline
│   │   ├── UserDetailsServiceImpl.java   # Spring Security user details
│   │   ├── VenuePhotoServiceImpl.java    # File upload + MIME type validation
│   │   └── VenueServiceImpl.java         # Venue CRUD + availability counting
│   │
│   └── util/
│       ├── JwtFilter.java               # Extracts + validates JWT on every request
│       ├── JwtUtil.java                 # Token generation, claim extraction
│       └── SecurityUtils.java           # Gets current authenticated userId
│
├── src/main/resources/
│   └── application.properties.example  # Template — copy to application.properties
│
├── src/ (React Frontend)
│   ├── api/
│   │   └── axiosInstance.js            # Axios with JWT interceptor + 401 auto-redirect
│   ├── context/
│   │   ├── AuthContext.jsx             # Auth state, login/logout, token expiry detection
│   │   └── ThemeContext.jsx            # Dark / light mode toggle
│   ├── components/
│   │   ├── Navbar.jsx                  # Role-aware navigation bar
│   │   └── ProtectedRoute.jsx          # Role-based route guard component
│   └── pages/
│       ├── auth/
│       │   ├── LoginPage.jsx
│       │   └── RegisterPage.jsx        # Includes mandatory role selection
│       ├── booker/
│       │   ├── MarketplacePage.jsx     # Venue cards with date + time slot filter
│       │   ├── VenueDetailPage.jsx     # Availability grid, slot selection, photo lightbox
│       │   ├── CartPage.jsx            # Cart items, pricing, checkout
│       │   └── MyBookingsPage.jsx      # Booking history + reschedule flow
│       └── owner/
│           ├── OwnerDashboard.jsx      # My venues + bookings filtered by date
│           ├── VenueCreatePage.jsx     # New venue form
│           └── VenueEditPage.jsx       # Edit venue + photo upload / delete
│
├── pom.xml
├── package.json
├── vite.config.js
└── README.md
```

---

## 🔧 Prerequisites

| Tool | Minimum Version | Check |
|---|---|---|
| Java | 17+ | `java -version` |
| Maven | 3.8+ | `./mvnw -version` |
| Node.js | 18+ | `node -version` |
| PostgreSQL | 14+ | `psql --version` |

> **No PostgreSQL?** You can use H2 in-memory database for quick local testing — see Step 2, Option B below.

---

## 🚀 Step-by-Step Setup — Run Locally

### Step 1 — Clone the repository

```bash
git clone https://github.com/vaibhavudhane/Pickleball-Court-Booking-Platform.git
cd Pickleball-Court-Booking-Platform
```

---

### Step 2 — Configure the backend

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Open `application.properties` and choose one option:

**Option A — PostgreSQL (recommended)**

First create the database:
```sql
CREATE DATABASE pickleball_db;
```

Then set in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pickleball_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
jwt.secret=MyPickleballSecretKey_AtLeast32Chars!
jwt.expiration=86400000
```

**Option B — H2 in-memory (no PostgreSQL needed, data resets on restart)**
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
jwt.secret=MyPickleballSecretKey_AtLeast32Chars!
jwt.expiration=86400000
```

---

### Step 3 — Start the backend

```bash
./mvnw spring-boot:run
```

Wait for this line in the console:
```
Started PickleballBackendApplication in X.XXX seconds
```

| Service | URL |
|---|---|
| REST API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |

---

### Step 4 — Configure the frontend

Create a `.env` file in the project root:

```bash
echo "VITE_API_URL=http://localhost:8080" > .env
```

---

### Step 5 — Start the frontend

```bash
npm install
npm run dev
```

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |

The app redirects to `/login` automatically. Use the test credentials below — no manual data entry needed.

---

## ⚙️ Environment Variables

### Backend — `application.properties`

| Property | Required | Description | Example |
|---|---|---|---|
| `spring.datasource.url` | ✅ | Database connection URL | `jdbc:postgresql://localhost:5432/pickleball_db` |
| `spring.datasource.username` | ✅ | Database username | `postgres` |
| `spring.datasource.password` | ✅ | Database password | `yourpassword` |
| `spring.datasource.driver-class-name` | ✅ | JDBC driver class | `org.postgresql.Driver` |
| `jwt.secret` | ✅ | Signing key — minimum 32 characters | `MyPickleballSecretKey_AtLeast32Chars!` |
| `jwt.expiration` | ✅ | Token expiry in milliseconds | `86400000` (24 hrs) |
| `server.port` | ❌ | Backend port | `8080` |
| `spring.servlet.multipart.max-file-size` | ❌ | Max photo upload size | `10MB` |
| `azure.storage.connection-string` | ❌ | Azure Blob (optional) | `UseDevelopmentStorage=true` |

### Frontend — `.env`

| Variable | Required | Description | Example |
|---|---|---|---|
| `VITE_API_URL` | ✅ | Backend base URL | `http://localhost:8080` |

---

## 🔑 Pre-Seeded Test Credentials

Register these accounts once via the app's Register page or Swagger UI — the application can then be tested immediately without any further setup.

### Step 1 — Register the Court Owner

```
URL:   http://localhost:5173/register
Name:  Vaibhav Owner
Email: owner@pickleball.com
Pass:  Owner@123
Role:  OWNER
```

Or via API:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Vaibhav Owner","email":"owner@pickleball.com","password":"Owner@123","role":"OWNER"}'
```

### Step 2 — Create the pre-seeded venue (log in as Owner first, then POST)

```json
POST http://localhost:8080/api/owner/venues
Authorization: Bearer <OWNER_TOKEN>

{
  "name": "Smash Arena Premium",
  "address": "Baner Road, Pune, Maharashtra",
  "description": "Premium pickleball courts with shower rooms and pro shop",
  "numCourts": 3,
  "openingTime": "06:00",
  "closingTime": "23:00",
  "weekdayRate": 550.00,
  "weekendRate": 750.00,
  "contactPhone": "9876543210",
  "contactEmail": "smash@arena.com"
}
```

This creates **3 courts** (Court 1, Court 2, Court 3) with slots from **6:00 AM to 11:00 PM**.

### Step 3 — Register the Booker

```
URL:   http://localhost:5173/register
Name:  Rahul Booker
Email: booker@pickleball.com
Pass:  Booker@123
Role:  BOOKER
```

### Quick Reference

| Role | Email | Password | Lands on |
|---|---|---|---|
| Court Owner | `owner@pickleball.com` | `Owner@123` | Owner Dashboard |
| Booker | `booker@pickleball.com` | `Booker@123` | Marketplace |

---

## 📡 API Documentation

Full interactive docs: **http://localhost:8080/swagger-ui/index.html**

All protected endpoints require: `Authorization: Bearer <JWT_TOKEN>`

---

### 🔐 Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | None | Register — returns JWT + role |
| `POST` | `/api/auth/login` | None | Login — returns JWT + role |

**Register body:**
```json
{
  "name": "John Smith",
  "email": "john@example.com",
  "password": "Test@1234",
  "role": "BOOKER"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "BOOKER",
  "name": "John Smith",
  "id": 1
}
```

---

### 🏟️ Venues — Public (no auth required)

| Method | Endpoint | Query Params | Description |
|---|---|---|---|
| `GET` | `/api/venues` | `date`, `startTime` (both optional) | List all venues. With filters: only venues with an available court in that slot |
| `GET` | `/api/venues/{id}` | — | Full venue detail — photos, courts, rates, hours |
| `GET` | `/api/venues/{venueId}/availability` | `date` (required) | Colour-coded slot grid for all courts |

**Availability slot status values:**

| Status | Meaning |
|---|---|
| `AVAILABLE` | Open — can be selected and booked |
| `BOOKED` | Confirmed by another user — cannot be selected |
| `UNAVAILABLE` | Past time slot or sub-30-min gap — cannot be selected |

---

### 🛒 Cart — BOOKER only

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/cart/add` | Add 1–10 slots. Full validation runs on every item |
| `GET` | `/api/cart` | View cart — itemised prices + running total |
| `DELETE` | `/api/cart/{itemId}` | Remove one item |
| `DELETE` | `/api/cart/clear` | Empty entire cart |

**Add to cart body:**
```json
{
  "items": [
    {
      "courtId": 1,
      "venueId": 1,
      "date": "2026-04-07",
      "startTime": "09:00",
      "endTime": "10:30"
    }
  ]
}
```

---

### ✅ Bookings — BOOKER only

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/bookings/checkout` | Atomically books all cart items. Rejects all if any slot is taken |
| `GET` | `/api/bookings/my` | Booking history — newest first |
| `PUT` | `/api/bookings/{bookingId}/reschedule` | Reschedule a confirmed booking |

**Checkout success (200):**
```json
{ "message": "Booking confirmed! 2 slot(s) booked.", "bookedCount": 2 }
```

**Checkout conflict (409):**
```json
{ "message": "These slots are no longer available: Court 1 on 2026-04-07 at 09:00 to 10:00", "status": 409 }
```

**Reschedule body:**
```json
{
  "newDate": "2026-04-15",
  "newStartTime": "10:00",
  "newEndTime": "11:30"
}
```

---

### 👤 Owner — OWNER only

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/owner/venues` | All venues I own |
| `POST` | `/api/owner/venues` | Create venue — courts auto-generated from `numCourts` |
| `PUT` | `/api/owner/venues/{id}` | Update venue — adding/removing courts handled automatically |
| `GET` | `/api/owner/venues/{venueId}/bookings` | All bookings at my venue. Optional: `?date=YYYY-MM-DD` |
| `POST` | `/api/owner/venues/{venueId}/photos` | Upload photos (`multipart/form-data`, max 5 total) |
| `DELETE` | `/api/owner/venues/{venueId}/photos/{photoId}` | Delete one photo |

---

### Error Response Format

```json
{ "message": "Human-readable error description", "status": 400 }
```

Validation errors return field-level detail:
```json
{
  "email": "Invalid email format — example: user@email.com",
  "password": "Password must contain at least one uppercase letter, one number and one special character"
}
```

---

## 📏 Business Rules

### Password Policy
Must be 8–50 characters containing at least one of each:
- Uppercase letter (A–Z)
- Lowercase letter (a–z)
- Number (0–9)
- Special character: `@ $ ! % * ? &`

### Pricing Formula
```
price = hourlyRate × durationMinutes ÷ 60   (rounded HALF_UP to 2 decimal places)
```
- Weekday rate (Mon–Fri): configured per venue by owner
- Weekend rate (Sat–Sun): configured per venue, must be ≥ weekday rate

### Cart — 13 Validation Checks (run in order)
1. Date must not be more than 90 days ahead
2. End time must be after start time
3. Minimum duration: 30 minutes
4. No overlap between slots in the same incoming request
5. Court must exist
6. Court must belong to the given venue
7. Venue must exist
8. Start time within venue operating hours (≥ 30 min before closing)
9. End time must not exceed venue closing time
10. No overlap with confirmed/rescheduled bookings in DB
11. No overlap with items already in user's cart
12. No exact duplicate slot in cart
13. Price calculated and stored at time of adding

### Checkout
- All items conflict-checked **before** any booking is saved
- If any slot taken → entire checkout rejected, conflicts listed, cart preserved
- On success → all bookings saved, cart cleared atomically

### Reschedule
- Only the booking owner can reschedule
- Original start must be **more than 12 hours from now**
- New slot must be available and within venue operating hours
- Minimum 30-minute duration
- `CONFIRMED` → `RESCHEDULED` status on success

### Photos
- Max 5 per venue
- Formats: JPG, JPEG, PNG — validated by extension AND MIME type
- Served at: `http://localhost:8080/uploads/venues/{venueId}/{filename}`

---

## 🗄️ Database Schema

```
users            id, name, email (unique), password (BCrypt hashed), role, created_at
venues           id, owner_id→users, name, address, description, num_courts,
                 opening_time, closing_time, weekday_rate, weekend_rate,
                 contact_phone, contact_email, created_at
courts           id, venue_id→venues, court_name, court_number
venue_photos     id, venue_id→venues, photo_url, display_order
cart_items       id, user_id→users, court_id→courts, venue_id→venues,
                 booking_date, start_time, end_time, price, added_at
bookings         id, user_id→users, court_id→courts, venue_id→venues,
                 booking_date, start_time, end_time, amount_paid,
                 status (CONFIRMED/RESCHEDULED), booked_at
```

---
