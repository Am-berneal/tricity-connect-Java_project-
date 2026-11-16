# TriCity Connect — Intercity Bus Seat Reservation System

Seat booking and monthly commuter passes for express buses between **Chandigarh (UT), Mohali (Punjab) and Panchkula (Haryana)**.
Java 17 · Spring Boot 3 · Thymeleaf · MySQL · JUnit 5

---

## 1. Problem statement
Daily commuters across Chandigarh, Mohali and Panchkula can't book a guaranteed seat on crowded inter-city buses.
Women's reserved seats aren't enforced, and passengers get no warning when disputes between the three governments stop services.

## 2. What the system does
| Feature | Where in the code |
|---|---|
| Search buses between any two stops, filter by time/bus type/fare, sort | `SearchService`, `results.html` |
| **Stop-wise seat booking**: a seat is held only between the passenger's own stops and can be sold again after they get off | `core/SeatSegments`, `SeatService`, `BookingService` |
| No double booking: the trip row is locked while booking (`SELECT … FOR UPDATE`) inside one transaction | `TripRepository.lockById`, `BookingService.book` |
| Ladies seats (rows 1–2) only bookable by women | `core/SeatLayout`, `BookingService` |
| Peak/off-peak fares, GST split (CGST + SGST) | `core/FareCalculator` |
| Payment (UPI / card / net banking simulated, TriCity Wallet real balance) | `BookingService` |
| E-ticket with QR code + PDF download | `QrService`, `TicketPdfService`, `confirmation.html` |
| Cancellation with refund rules (90% / 50% / 0%) to wallet | `core/RefundPolicy`, `bookings.html` |
| Live bus tracking (simulated from timetable + delay) | `TrackingService`, `tracking.html` |
| Monthly commuter pass with a fixed seat on fixed buses | `PassService`, `core/PassPricing`, `pass-new.html` |
| Service alerts + WhatsApp alert subscription | `AlertController`, `alerts.html` |
| Women's safety: share trip with family, SOS 112 | `checkout.html`, `tracking.html` |
| English / हिंदी / ਪੰਜਾਬੀ | `messages*.properties`, `WebConfig` |
| Student / senior concession (50%) chosen at checkout | `core/Concession`, `checkout.html` |
| **Disruption handling**: when a border closure or strike cuts a route, affected passengers choose free connector bus / free rebook (same seat if free) / 100% refund | `DisruptionService`, `alerts.html` |
| Profile & TriCity Wallet: top-up, full ledger with filters, CSV statement, emergency contact, password | `ProfileController`, `WalletService`, `profile.html` |
| Help Centre: FAQ, rule-based chat assistant ("next bus from Kharar to Sector 17"), grievance tickets | `HelpService`, `help.html` |
| Login with roles (Passenger / Conductor / Admin), BCrypt passwords | `SecurityConfig`, `UserService` |

**Admin console** (`/admin`, log in as admin)
| Screen | What it does | Code |
|---|---|---|
| Dashboard | Today's trips, seats booked, passes, occupancy, 8-day trend, corridor × hour load heatmap, auto "add a bus / reduce frequency" recommendations | `StatsService.dashboard`, `admin/dashboard.html` |
| Routes & stops | View every corridor's stops, km and fares; insert a stop in the middle (all bookings renumbered safely); remove unused stops; create a corridor + return route | `RouteAdminService`, `admin/routes.html` |
| Fleet | Add buses, change status (active / maintenance / charging / standby) | `FleetService`, `admin/buses.html` |
| Trip scheduling | Add one trip or bulk-schedule a date range, swap the bus, cancel a trip (everyone refunded 100%), passenger manifest | `TripAdminService`, `admin/trips.html`, `admin/manifest.html` |
| Disruptions | Publish suspension / short-termination / delay / diversion for chosen routes and time window; resolve it | `DisruptionService`, `admin/disruptions.html` |
| Fare rules | Edit ₹/km, peak multiplier, GST, concessions, pass discounts (stored in DB, used instantly); festival demand calendar; fare preview calculator | `FareService`, `core/DemandForecast`, `admin/fares.html` |
| Reports | Revenue, passengers, occupancy by hour, revenue by corridor, boardings by state (UT / Punjab / Haryana), ladies-seat use, ledger, CSV export, print to PDF | `StatsService.report`, `admin/reports.html` |

**Conductor ETM** (`/conductor`, open on a phone): choose today's trip → see current stop, who boards here, live seat map for this stretch (validated / booked / pass / gets off next / ladies), validate tickets by camera QR scan or typed PNR / pass number. | `ConductorService`, `conductor/*.html`

The six corridors are the busiest Tricity routes identified in the RITES Comprehensive Mobility Plan:
ISBT-43 → New Chandigarh, ISBT-43 → Mansa Devi, ISBT-43 → Panchkula Sec 5, ISBT-17 ⇄ Kharar, PGI → Zirakpur, PGI → Panchkula (both directions).

## 3. How to run

**You need:** JDK 17 or newer, Maven 3.9+ (or the Maven built into IntelliJ / Eclipse), and MySQL 8 (optional).

### Option A — with MySQL (for the final demo)
1. Start MySQL. The database `tricity_connect` is created automatically.
2. Open `src/main/resources/application.properties` and set your MySQL username/password:
   ```
   spring.datasource.username=root
   spring.datasource.password=root
   ```
3. Run:
   ```
   mvn spring-boot:run
   ```
4. Open **http://localhost:8080**

### Option B — no MySQL needed (quick test)
```
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Uses an in-memory database (reset on every restart). Database console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:tricity`, user `sa`).

### In IntelliJ IDEA
File → Open → select the `tricity-connect` folder → wait for Maven to load → run `TriCityConnectApplication`.
(For H2: Run → Edit Configurations → Active profiles: `h2`.)

On first start, demo data is created: stops, 12 routes, 28 buses, a 14-day timetable (last 7 days + next 7, ≈2,700 trips) and ≈32,000 sample bookings so the dashboard, reports and seat maps look realistic. With MySQL **the first start takes about 1–2 minutes** while this is inserted; later starts are fast (data is kept). To start fresh, drop the database: `DROP DATABASE tricity_connect;`

Also seeded on purpose:
- A **live disruption**: TC-104 tomorrow 07:00–09:00 ends at Mohali Phase 3B2 (NH-5 work). Harpreet's 07:45 booking is affected, so his Alerts page shows the connector / rebook / refund choices.
- A resolved delay on TC-312 yesterday (incident log).
- TC-104 08:35 today is completely full.

### Demo logins
| Role | Login | Password |
|---|---|---|
| Passenger (male) | `9876543210` | `password123` |
| Passenger (female — can book ladies seats) | `9812345678` | `password123` |
| Admin | `admin@tricity.in` | `admin123` |
| Conductor | `conductor@tricity.in` | `conductor123` |

## 4. Run the tests
```
mvn test
```
Tests use an in-memory H2 database, so MySQL is not needed.
- `core/*Test` — fares, refunds, stop overlap, pass pricing, seat layout (pure unit tests)
- `core/FareRulesTest`, `core/DemandForecastTest` — concessions, pass discounts, demand index
- `service/BookingFlowTest` — search, stop-wise resale, overlap rejection, ladies rule, wallet charge + refund, pass seat blocking
- `service/OperationsFlowTest` — disruption rebook keeps seat, suspension = 100% refund, conductor check-in, trip cancel refunds, reports, saved fare rules change prices
- `web/PagesTest` — every passenger, admin and conductor page renders, languages work, login/role rules work

### Manual test cases (for the report)
| # | Test | Steps | Expected |
|---|---|---|---|
| 1 | Search | Home → Kharar Bus Stand → Sector 17 ISBT → Find Buses | List of TC-104 buses with times, fares, seats left |
| 2 | Full bus | Search Kharar → Sector 17 today, morning | 08:35 bus shows "Bus Full", no Select button |
| 3 | Stop-wise booking | Book seat X Kharar → Mohali Phase 7 as 9876543210; then as 9812345678 book seat X Phase 7 → Sector 17 | Both succeed |
| 4 | Overlap blocked | Try seat X Kharar → Phase 3B2 on the same bus | Seat shows as Booked |
| 5 | Ladies seat | Log in as 9876543210, open a seat map | Rows 1–2 locked (pink) |
| 6 | Wallet payment | Pay with TriCity Wallet | Balance in navbar goes down by the fare |
| 7 | Cancel & refund | My Bookings → Cancel (trip > 2 h away) | 90% credited to wallet, booking in Cancelled tab |
| 8 | PDF ticket | Confirmation → Download PDF | PDF with PNR, seat and QR |
| 9 | Pass | My Passes → Get New Pass → choose corridor, times, seat, Buy | Pass card with QR; that seat now shows "Pass" on those buses |
| 10 | Security | Open /bookings while logged out | Redirected to login |
| 11 | Roles | Log in as passenger, open /admin | 403 Forbidden |
| 12 | Language | Click हिंदी / ਪੰਜਾਬੀ in the navbar | Menu and home page switch language |
| 13 | Disruption choice | As 9876543210 open Alerts → affected booking → "Rebook free" | Moved to next bus, same seat if free, no extra charge |
| 14 | Publish disruption | Admin → Disruptions → Suspended, TC-206, next 2 h → Publish | Trips marked, count of affected passengers shown, alert visible on /alerts |
| 15 | Fare rule change | Admin → Fare Rules → ₹/km = 2.50 → Save; search again | Fares go up; old tickets unchanged |
| 16 | Cancel a trip | Admin → Trips → Cancel on a booked trip | All its passengers refunded 100% to wallet |
| 17 | Conductor check-in | Log in as conductor → pick a trip → type a PNR → Validate | "✓ Valid…", second time "Already checked in" |
| 18 | Wrong bus | Validate a PNR from another trip | "Ticket is for another bus" |
| 19 | Reports | Admin → Reports → change dates → CSV | Numbers update; CSV downloads |
| 20 | Help bot | Help → ask "next bus from Kharar to Sector 17" | Shows next bus, time, seats and fare |

## 5. Project structure
```
src/main/java/com/tricityconnect
├── core/          Plain-Java business rules (fares, refunds, seat overlap, pass price, timetable)
├── model/         JPA entities = database tables (AppUser, Stop, Route, RouteStop, Bus, Trip, Booking, CommuterPass, Alert ...)
├── repository/    Spring Data JPA interfaces (database queries)
├── service/       Business logic (SearchService, SeatService, BookingService, PassService, TrackingService, PDF, QR)
├── dto/           Form objects and view data
├── web/           Controllers (one per group of pages)
└── config/        Security, language switch, demo data seeder
src/main/resources
├── templates/     Thymeleaf pages (designed from the Google Stitch screens)
├── static/        logo, images, CSS
└── messages*.properties   English, Hindi, Punjabi texts
database/schema.sql        MySQL schema + useful queries
```
**Architecture:** MVC + layered (Controller → Service → Repository → MySQL).

## 6. Using your Google Stitch HTML
The pages already follow the Stitch designs. To swap in an exported Stitch page:
1. Copy the content inside Stitch's `<body>` into the matching template (e.g. `seats.html`).
2. Keep the `th:` attributes on elements that show data (e.g. `th:text="${fare.total()}"`) — they are what connect the page to Java.
3. Keep `<head th:replace=...>`, the navbar and footer lines so every page stays consistent.

## 7. Viva quick points
- **Why stop-wise booking?** Buses are short of capacity (CTU is the only Tricity public bus operator). Selling a seat again after a passenger gets off carries more people with the same fleet. Two bookings clash only if `newFrom < oldTo AND oldFrom < newTo`.
- **How is double booking prevented?** `BookingService.book` is `@Transactional` and starts with `lockById` (pessimistic write lock = `SELECT … FOR UPDATE`). A second request for the same bus waits, then re-checks and finds the seat taken. If anything fails, the whole transaction rolls back (wallet not charged).
- **Why is the fare GST-inclusive?** The passenger sees one round number (₹45); the system splits it into base + CGST 2.5% + SGST 2.5% for the invoice.
- **Passwords?** BCrypt hashes, never stored as plain text.
- **Why Spring Boot + Thymeleaf?** The UI from Google Stitch is HTML; Thymeleaf lets those pages show live data from Java.

- **What happens in a disruption?** The admin publishes it once; `DisruptionService` marks every trip in the window (cancelled, short-terminated at a stop, or delayed). Search stops selling seats beyond the new last stop, and each affected passenger picks connector / rebook / full refund. Resolving it restores the trips.
- **How does the dashboard know where to add buses?** It averages each corridor's peak load per hour over the last 7 days; cells ≥ 50% suggest an extra bus, ≤ 30% suggest sending one to charge.
- **Why keep fare rules in the database?** The admin can change the fare without redeploying; tickets already sold store their own fare, so they never change.

## 8. Future scope
Real payment gateway (Razorpay/UPI intent), SMS/WhatsApp gateway for OTP and alerts, real GPS feed from CTU's AIS-140 devices, Android app, ML-based demand forecasting.
