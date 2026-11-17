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

1. Core domain foundation — current
2. Stadium structure and seat inventory
3. Match management
4. User accounts and roles
5. Booking workflow and temporary seat holds
6. Database persistence
7. Three.js stadium and seat-map experience
8. Digital tickets and QR codes
9. Admin console
10. Integration, security, testing and project documentation

## Important

This is a development starter, not a production ticketing platform. Payment processing, real authentication, persistent storage and concurrency-safe booking will be added in later modules. The demo API deliberately uses sample data.
