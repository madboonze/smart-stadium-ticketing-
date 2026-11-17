# Architecture decisions

## Java-first design

Java is the source of truth for all domain state and business rules. The browser-side 3D layer will request stadium data from Java APIs and send user interactions back to Java. It must not decide whether a seat can be booked or what a ticket costs.

## Why Three.js is still included

A browser 3D renderer provides better cross-platform WebGL support and visual quality for a realistic stadium scene. It is deliberately limited to rendering, camera controls, hit-testing and visual feedback. The Java backend remains authoritative.

## Module 1 domain model

- `Seat` is abstract and encapsulates identity, base price and status.
- `RegularSeat`, `PremiumSeat` and `VIPSeat` override pricing behavior.
- `Priceable` expresses the pricing contract.
- `Stadium` owns stands; `Stand` owns seats.
- `Match` references a stadium.
- `Booking` references a customer and match, temporarily holds selected seats, and creates tickets on confirmation.
- `Bookable` defines confirmation and cancellation operations.

## Important follow-up hardening

Module 1 uses in-memory demo state. Before production-like use, later modules must add database transactions, seat-hold expiry, concurrency control, authentication/authorization, audit records and payment-provider integration. Cancellation/refund policies must be implemented explicitly rather than assumed.
