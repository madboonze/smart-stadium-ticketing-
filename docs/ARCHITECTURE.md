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

## Module 2 seating hierarchy

The seating structure is five levels deep:

```text
Stadium -> Stand -> Section -> SeatRow -> Seat
```

- `Section` is the block a customer picks before zooming in. It carries the `SeatCategory`
  and the base price, so one section decides the type and price of every seat inside it.
- `SeatRow` owns the seats of a single row and guards duplicate seat identities.
- `SeatFactory` turns a `SeatCategory` into the matching `Seat` subclass. The pricing rules
  stay on `RegularSeat`, `PremiumSeat` and `VIPSeat`; the factory only chooses the class.
- `Section.generateRows(rowCount, seatsPerRow)` builds a whole block in one call, labelling
  rows A, B, C ... and seats from 1.

Seat labels are `CODE-ROW-NN`, for example `N1-A-07`. Section codes are unique per stadium,
which makes every seat label unique stadium-wide and gives the 3D seat map a readable
identifier instead of an opaque UUID.

### Two ways a stand holds seats

`Stand.addSeat` attaches a seat directly to the stand, which is what the original Module 1
demo fixture does. `Stand.addSection` attaches a whole section structure. A `Seat` instance
is never in both places, so counts never double up:

- `getSeats()` returns only the directly attached seats, unchanged from Module 1.
- `getAllSeats()` returns the directly attached seats plus every seat inside the sections.
- `getAvailableSeatCount()` and `getCapacity()` cover both groups.

This is why `Stadium.getAvailableSeatCount()` needed no change: it already delegates to the
stands.

### Where inventory lives

`SeatInventoryService` owns section creation and seat generation, but it does not store
stadiums or stands. Those stay in `AdminCatalogService`, which already held them for the
admin API, so there is one registry rather than two copies of the same data.

`SeatInventoryService.findSeat(stadiumId, seatId)` is the intended seam for the booking
engine: it turns a seat id from the browser into the live domain object to hold or book,
keeping seat state owned by the inventory alone.

## Important follow-up hardening

Module 1 uses in-memory demo state. Before production-like use, later modules must add database transactions, seat-hold expiry, concurrency control, authentication/authorization, audit records and payment-provider integration. Cancellation/refund policies must be implemented explicitly rather than assumed.
