-- =====================================================================
--  TriCity Connect - MySQL database schema (reference)
--  The app creates these tables automatically on first start
--  (spring.jpa.hibernate.ddl-auto=update), and DataSeeder fills them.
--  This script is for the project report / viva, or to create the
--  database by hand:   mysql -u root -p < database/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS tricity_connect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tricity_connect;

-- Passengers, conductors and admins
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100)  NOT NULL,
    mobile          VARCHAR(10)   NOT NULL UNIQUE,
    email           VARCHAR(120)  UNIQUE,
    gender          VARCHAR(10)   NOT NULL,          -- MALE / FEMALE / OTHER (used for ladies seats)
    home_city       VARCHAR(60),
    password_hash   VARCHAR(255)  NOT NULL,          -- BCrypt hash, never the plain password
    role            VARCHAR(12)   NOT NULL,          -- PASSENGER / CONDUCTOR / ADMIN
    wallet_balance  DECIMAL(10,2) NOT NULL DEFAULT 0,
    emergency_name  VARCHAR(80),                     -- SOS / share-trip contact
    emergency_mobile VARCHAR(10),
    created_at      DATETIME(6)
);

-- Bus stops across UT Chandigarh, Punjab (Mohali side) and Haryana (Panchkula)
CREATE TABLE stops (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name   VARCHAR(80) NOT NULL UNIQUE,
    city   VARCHAR(40) NOT NULL,
    state  VARCHAR(30) NOT NULL
);

-- Corridors; each direction is a route, linked by return_route_id
CREATE TABLE routes (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    code               VARCHAR(12)  NOT NULL UNIQUE,   -- TC-104, TC-104R ...
    name               VARCHAR(120) NOT NULL,
    tagline            VARCHAR(60),
    distance_km        DOUBLE       NOT NULL,
    frequency_minutes  INT          NOT NULL,
    return_route_id    BIGINT,
    FOREIGN KEY (return_route_id) REFERENCES routes(id)
);

-- Ordered stops on each route (seq 1, 2, 3 ...)
CREATE TABLE route_stops (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id            BIGINT NOT NULL,
    stop_id             BIGINT NOT NULL,
    seq                 INT    NOT NULL,
    km_from_start       DOUBLE NOT NULL,
    minutes_from_start  INT    NOT NULL,
    UNIQUE (route_id, seq),
    FOREIGN KEY (route_id) REFERENCES routes(id),
    FOREIGN KEY (stop_id)  REFERENCES stops(id)
);

CREATE TABLE buses (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    reg_no    VARCHAR(20) NOT NULL UNIQUE,              -- CH01-GA-2041
    type      VARCHAR(15) NOT NULL,                     -- AC_ELECTRIC / AC_CNG / NON_AC
    capacity  INT         NOT NULL,                     -- 36 = 9 rows x 4 seats
    depot     VARCHAR(60),
    model     VARCHAR(60),                              -- e.g. Olectra K9 12m
    status    VARCHAR(12) NOT NULL DEFAULT 'ACTIVE'     -- ACTIVE / MAINTENANCE / CHARGING / STANDBY

);

-- Hibernate sequence tables: trips and bookings take ids in blocks of 50
-- so thousands of rows can be inserted in JDBC batches.
CREATE TABLE trip_seq    (next_val BIGINT);  INSERT INTO trip_seq    VALUES (1);
CREATE TABLE booking_seq (next_val BIGINT);  INSERT INTO booking_seq VALUES (1);

-- One scheduled run of a bus on a route
CREATE TABLE trips (
    id              BIGINT PRIMARY KEY,                 -- from trip_seq
    route_id        BIGINT      NOT NULL,
    bus_id          BIGINT      NOT NULL,
    departure_time  DATETIME(6) NOT NULL,               -- from the first stop
    status          VARCHAR(12) NOT NULL,               -- SCHEDULED / DISRUPTED / CANCELLED / COMPLETED
    delay_minutes   INT         NOT NULL DEFAULT 0,
    bay             VARCHAR(40),
    driver_name     VARCHAR(60),
    terminates_at_seq INT,                              -- set when a disruption ends the bus early
    disruption_id   BIGINT,                             -- FK added at the end (disruptions table)
    INDEX idx_trip_route_time (route_id, departure_time),
    FOREIGN KEY (route_id) REFERENCES routes(id),
    FOREIGN KEY (bus_id)   REFERENCES buses(id)
);

-- Stop-wise seat bookings: the seat is held only from from_seq to to_seq
CREATE TABLE bookings (
    id                 BIGINT PRIMARY KEY,              -- from booking_seq
    pnr                VARCHAR(10)  NOT NULL UNIQUE,
    user_id            BIGINT       NOT NULL,
    trip_id            BIGINT       NOT NULL,
    seat_no            VARCHAR(4)   NOT NULL,
    from_seq           INT          NOT NULL,
    to_seq             INT          NOT NULL,
    passenger_name     VARCHAR(100) NOT NULL,
    passenger_age      INT          NOT NULL,
    passenger_gender   VARCHAR(10),
    passenger_mobile   VARCHAR(10),
    passenger_email    VARCHAR(120),
    base_fare          DECIMAL(10,2),
    cgst               DECIMAL(10,2),
    sgst               DECIMAL(10,2),
    total_fare         DECIMAL(10,2),
    payment_method     VARCHAR(12),                     -- UPI / WALLET / CARD / NET_BANKING
    status             VARCHAR(12)  NOT NULL,           -- CONFIRMED / CANCELLED
    whatsapp_ticket    BIT          NOT NULL,
    emergency_contact  VARCHAR(10),                     -- women's safety: share trip with family
    refund_amount      DECIMAL(10,2),
    cancel_reason      VARCHAR(120),
    concession         VARCHAR(10)  NOT NULL DEFAULT 'GENERAL',   -- GENERAL / STUDENT / SENIOR
    boarded            BIT          NOT NULL DEFAULT 0,           -- validated by conductor ETM
    boarded_at         DATETIME(6),
    disruption_choice  VARCHAR(12),                     -- CONNECTOR / REBOOKED / REFUNDED / KEPT
    created_at         DATETIME(6),
    cancelled_at       DATETIME(6),
    INDEX idx_booking_trip_seat (trip_id, seat_no),
    INDEX idx_booking_user (user_id),
    CHECK (from_seq < to_seq),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (trip_id) REFERENCES trips(id)
);

-- Monthly commuter passes: fixed seat on a fixed morning bus and evening return bus
CREATE TABLE commuter_passes (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    pass_no            VARCHAR(20) NOT NULL UNIQUE,
    user_id            BIGINT      NOT NULL,
    pass_type          VARCHAR(16) NOT NULL,            -- CORRIDOR / STUDENT_SENIOR
    route_id           BIGINT      NOT NULL,
    seat_no            VARCHAR(4)  NOT NULL,
    morning_departure  TIME        NOT NULL,
    evening_departure  TIME        NOT NULL,
    valid_from         DATE        NOT NULL,
    valid_to           DATE        NOT NULL,
    months             INT         NOT NULL,
    price              DECIMAL(10,2),
    full_price         DECIMAL(10,2),
    status             VARCHAR(10) NOT NULL,            -- ACTIVE / EXPIRED / CANCELLED
    trips_used         INT         NOT NULL DEFAULT 0,
    created_at         DATETIME(6),
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (route_id) REFERENCES routes(id)
);

-- Service bulletins (detours, disruptions, festival services, offers)
CREATE TABLE alerts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    type          VARCHAR(12)   NOT NULL,
    title         VARCHAR(160)  NOT NULL,
    message       VARCHAR(1000) NOT NULL,
    route_code    VARCHAR(12),
    tags          VARCHAR(300),
    valid_window  VARCHAR(60),
    active        BIT           NOT NULL,
    priority      INT           NOT NULL,
    created_at    DATETIME(6)
);

CREATE TABLE alert_subscriptions (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                  BIGINT      NOT NULL,
    route_id                 BIGINT      NOT NULL,
    whatsapp_number          VARCHAR(10) NOT NULL,
    notify_before_departure  BIT         NOT NULL,
    critical_alerts          BIT         NOT NULL,
    created_at               DATETIME(6),
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (route_id) REFERENCES routes(id)
);

-- =====================================================================
--  Useful queries for the viva
-- =====================================================================

-- 1) Is seat 4A free on trip 25 between stop 2 and stop 5?
--    (a booking clashes only if its stop range overlaps ours)
-- SELECT COUNT(*) FROM bookings
--  WHERE trip_id = 25 AND seat_no = '4A' AND status = 'CONFIRMED'
--    AND from_seq < 5      -- existing booking starts before our drop stop
--    AND 2 < to_seq;       -- and ends after our boarding stop
--  -> 0 means free

-- 2) Busiest routes today (seats sold per route)
-- SELECT r.code, r.name, COUNT(b.id) AS seats_sold
--   FROM bookings b JOIN trips t ON b.trip_id = t.id JOIN routes r ON t.route_id = r.id
--  WHERE DATE(t.departure_time) = CURDATE() AND b.status = 'CONFIRMED'
--  GROUP BY r.code, r.name ORDER BY seats_sold DESC;

-- 3) Revenue by payment method
-- SELECT payment_method, COUNT(*) AS tickets, SUM(total_fare) AS revenue
--   FROM bookings WHERE status = 'CONFIRMED' GROUP BY payment_method;

-- =====================================================================
--  Operations (admin console)
-- =====================================================================

-- Incidents published by the admin: suspension, short-termination, delay, diversion
CREATE TABLE disruptions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(20)   NOT NULL UNIQUE,   -- DIS-2026-104A
    type                VARCHAR(20)   NOT NULL,          -- SUSPENDED / SHORT_TERMINATED / DELAYED / DIVERTED
    title               VARCHAR(160)  NOT NULL,
    reason              VARCHAR(1000) NOT NULL,
    route_codes         VARCHAR(200)  NOT NULL,          -- TC-104, TC-104R
    border_impact       VARCHAR(40),                     -- Chandigarh–Mohali / Chandigarh–Panchkula ...
    new_last_stop_id    BIGINT,                          -- where buses now end (short-termination)
    delay_minutes       INT           NOT NULL DEFAULT 0,
    start_time          DATETIME(6)   NOT NULL,
    end_time            DATETIME(6)   NOT NULL,
    offer_connector     BIT           NOT NULL,
    offer_rebook        BIT           NOT NULL,
    offer_refund        BIT           NOT NULL,
    affected_trips      INT           NOT NULL DEFAULT 0,
    affected_passengers INT           NOT NULL DEFAULT 0,
    active              BIT           NOT NULL DEFAULT 1,
    alert_id            BIGINT,
    resolution_note     VARCHAR(300),
    created_at          DATETIME(6),
    resolved_at         DATETIME(6),
    FOREIGN KEY (new_last_stop_id) REFERENCES stops(id),
    FOREIGN KEY (alert_id)         REFERENCES alerts(id)
);

ALTER TABLE trips ADD CONSTRAINT fk_trip_disruption FOREIGN KEY (disruption_id) REFERENCES disruptions(id);

-- TriCity Wallet ledger: every top-up, payment and refund
CREATE TABLE wallet_transactions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    kind          VARCHAR(20)   NOT NULL,   -- TOPUP / BOOKING / REFUND / DISRUPTION_REFUND / PASS
    description   VARCHAR(160)  NOT NULL,
    reference     VARCHAR(30),              -- PNR or pass number
    route_code    VARCHAR(12),
    method        VARCHAR(40),              -- UPI, Wallet, Card ...
    amount        DECIMAL(10,2) NOT NULL,
    wallet_impact BIT           NOT NULL,   -- true if the wallet balance changed
    created_at    DATETIME(6),
    INDEX idx_wallet_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- One row (id = 1): the live fare rules edited on the Fare Rules page
CREATE TABLE fare_settings (
    id                   BIGINT       PRIMARY KEY,
    rate_per_km          DECIMAL(6,2) NOT NULL,   -- 2.00
    peak_multiplier      DECIMAL(4,2) NOT NULL,   -- 1.20 (07-10, 17-20)
    off_peak_multiplier  DECIMAL(4,2) NOT NULL,   -- 1.00
    min_fare             DECIMAL(6,2) NOT NULL,   -- 10.00
    gst_percent          DECIMAL(4,2) NOT NULL,   -- 5.00, split CGST/SGST
    student_discount_pct INT NOT NULL,            -- 50
    senior_discount_pct  INT NOT NULL,            -- 50
    pass1month_pct       INT NOT NULL,            -- 25
    pass3month_pct       INT NOT NULL,            -- 30
    pass6month_pct       INT NOT NULL,            -- 35
    updated_by           VARCHAR(100),
    updated_at           DATETIME(6)
);

-- Complaints filed from the Help Centre
CREATE TABLE grievances (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no  VARCHAR(20)   NOT NULL UNIQUE,
    user_id    BIGINT,
    category   VARCHAR(40)   NOT NULL,
    pnr        VARCHAR(10),
    message    VARCHAR(1000) NOT NULL,
    status     VARCHAR(12)   NOT NULL DEFAULT 'OPEN',
    created_at DATETIME(6),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT INTO fare_settings VALUES (1, 2.00, 1.20, 1.00, 10.00, 5.00, 50, 50, 25, 30, 35, 'system', NOW());

