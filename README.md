# Smart Stadium

A Java-first stadium ticketing application with an interactive 3D stadium interface.

## Technology

- Java 21
- Maven
- Spring Boot 3.5.0
- Three.js (3D rendering only)
- SQLite (planned in a later module)

The Java backend owns domain rules, seat pricing, booking rules, validation and persistence. Three.js is used only for the 3D visual layer.

## Current status

**Module 1 — Core domain foundation** is implemented:
- Seat hierarchy (`Seat`, `RegularSeat`, `PremiumSeat`, `VIPSeat`)
- Encapsulation and controlled state changes
- Pricing polymorphism
- Stadium / stand / match / user / booking / ticket domain models
- Interfaces for pricing and booking behavior
- A small REST API exposing sample domain data
- Unit tests for pricing and booking invariants

**Module 2 — Stadium structure and seat inventory** is implemented:
- Full seating hierarchy: `Stadium` → `Stand` → `Section` → `SeatRow` → `Seat`
- `Section` carries the seat category and base price; `SeatFactory` builds the matching seat type
- `Section.generateRows(rows, seatsPerRow)` generates individually bookable seats in one call
- Readable stadium-wide seat labels such as `N1-A-07`
- `SeatInventoryService` for inventory management and seat lookup
- Public read APIs for the seat map, plus an admin API to create sections
- A seeded demo stadium with 4 stands, 12 sections and 1,978 seats

**Module 6 — Database persistence** is implemented for the seat inventory:
- SQLite via Spring Data JPA, with Flyway owning the schema
- Stadiums, stands, sections, rows, seats (including seat status) and matches survive restart
- The domain classes are untouched: a separate `persistence` layer holds the entities, with
  a mapper converting both ways
- Real foreign keys plus a CHECK constraint that a seat has exactly one parent
- `SeatInventoryService.holdSeat/releaseSeatHold/confirmSeatBooking/cancelSeatBooking`
  change memory and the database together

Accounts, tokens, tickets and check-ins are still in memory and reset on restart; those
conversions are planned as separate changes so each module's author reviews their own.

**Module 4 — User accounts and roles** is implemented:
- Customer and admin registration, login and logout
- Password hashing and bearer-token sessions
- Role checks (customer vs admin)
- `GET /api/auth/me` for the current account

**Module 9 — Admin console (API)** is implemented:
- Protected admin APIs to create/list stadiums, add stands, and create/list matches
- Admin dashboard summary endpoint
- A small admin dashboard page at `/admin.html`

Both modules use in-memory stores (accounts, tokens, stadiums, matches) so they can be integrated before the database module. Data resets on server restart; see Module 6.
## Requirements

- JDK 21
- Maven 3.9+
- Git

Check installations:

```bash
java -version
mvn -version
git --version
```

## Run

From the repository root:

```bash
mvn spring-boot:run
```

Then open:
- `http://localhost:8080/api/health`
- `http://localhost:8080/api/demo/stadium`
- `http://localhost:8080/api/stadiums` — stadiums with capacity and availability
- `http://localhost:8080/api/stadiums/{stadiumId}/inventory` — stands and sections
- `http://localhost:8080/api/stadiums/{stadiumId}/inventory?includeSeats=true` — every row and seat

Seat inventory endpoints (Module 2):

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/stadiums` | Stadiums that can be browsed |
| GET | `/api/stadiums/{stadiumId}/inventory` | Stands and sections with counts |
| GET | `/api/stadiums/{stadiumId}/stands/{standId}/sections/{sectionId}` | One section with all rows and seats |
| GET | `/api/stadiums/{stadiumId}/seats/{seatId}` | A single seat |
| POST | `/api/admin/inventory/stadiums/{stadiumId}/stands/{standId}/sections` | Create a section and generate its seats (admin) |

The seeded demo stadium can be switched off with
`smartstadium.inventory.seed-demo-bowl=false`. It is only ever seeded when the database
holds no sectioned inventory, so it is not recreated on later starts.

## Database

The SQLite file lives at `data/smart-stadium.db`, which `.gitignore` excludes. Flyway
creates the schema on first start from `src/main/resources/db/migration`.

To start from scratch, stop the app and delete the file:

```bash
rm -f data/smart-stadium.db        # Windows: del data\smart-stadium.db
```

The seed inventory is rebuilt on the next start. Point somewhere else with
`SMART_STADIUM_DB=/path/to/other.db`.

Run tests:

```bash
mvn test
```

Package:

```bash
mvn clean package
```

## Project layout

```text
smart-stadium/
├── src/main/java/com/smartstadium/
│   ├── SmartStadiumApplication.java
│   ├── api/
│   ├── domain/
│   │   ├── booking/
│   │   ├── match/
│   │   ├── seat/
│   │   ├── stadium/
│   │   └── user/
│   └── service/
├── src/test/java/com/smartstadium/
├── docs/
└── pom.xml
```

## Planned modules

1. Core domain foundation — done
2. Stadium structure and seat inventory — done
3. Match management
4. User accounts and roles — done
5. Booking workflow and temporary seat holds
6. Database persistence — done for seat inventory; accounts and tickets pending
7. Three.js stadium and seat-map experience
8. Digital tickets and QR codes
9. Admin console — done
10. Integration, security, testing and project documentation
## Important

This is a development starter, not a production ticketing platform. Payment processing, real authentication, persistent storage and concurrency-safe booking will be added in later modules. The demo API deliberately uses sample data.
