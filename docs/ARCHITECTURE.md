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

## Module 6 persistence

SQLite, accessed through Spring Data JPA, with Flyway owning the schema.

### Why the domain classes are not entities

None of them can be. Every field in `Seat`, `Stadium`, `Stand`, `Match`, `Section` and
`SeatRow` is `final` and none has a no-argument constructor, both of which Hibernate
requires. `DigitalTicket` is a `record`, which the JPA specification forbids as an entity
outright. Annotating the domain would have meant stripping `final` off every field and
weakening the encapsulation Module 1 was built around, purely to satisfy an ORM.

So persistence lives in its own layer:

```text
domain/        unchanged, framework-free, final fields
persistence/
  entity/      @Entity classes, one per table
  repository/  Spring Data repositories
  InventoryMapper   domain <-> rows, pure Java, unit tested
  InventoryStore    the only door between services and the database
```

Services depend on `InventoryStore` and never see an entity. Everything crossing that
boundary is a domain object.

### Restoring state through the domain's own rules

`Seat` has no status setter: the only way in is `hold()`, `confirmBooking()` and friends.
Rather than reach past that with reflection, the mapper replays the transitions, so a seat
that was BOOKED is rebuilt by calling `hold()` then `confirmBooking()`. State coming out
of the database is therefore validated by the same invariants that guarded it going in.

`MatchStatus` has `SOLD_OUT` and `COMPLETED`, but `Match` exposes no transition that
reaches them, so nothing can currently produce or restore those values; such a row comes
back as `SCHEDULED`. Module 3 should add the transitions and the matching mapper cases.

### Flat id columns instead of @OneToMany

The entities carry plain `stadium_id` / `stand_id` / `row_id` columns rather than JPA
associations. The whole inventory is read in six queries and assembled in Java, which
removes lazy loading, fetch joins and cascade behaviour from the picture entirely. The
relationships are still enforced where it counts: the schema has real foreign keys, plus
a CHECK constraint ensuring a seat hangs off exactly one parent.

### Memory and database together

The live aggregate stays in memory because the rest of the application passes `Seat`
objects around and expects to see one another's changes. Every change is written as it
happens and read back at startup, so the database is what survives and memory holds the
single live copy.

That is why seat status has dedicated methods on `SeatInventoryService`
(`holdSeat`, `releaseSeatHold`, `confirmSeatBooking`, `cancelSeatBooking`). Calling
`findSeat(...).hold()` changes memory only; calling `holdSeat(...)` changes memory and the
database together. **The booking engine must use these.** `findSeat` remains for reads.

### Writes are kept narrow

Ids are assigned by the application rather than generated by the database, which makes
every `save` a merge with a SELECT in front of it. Writing the whole stadium to add one
section would be thousands of needless statements, so there are three widths of write:
`persistStadiumRow` for a new stadium, `persistStand` and `persistSection` for structural
additions, and `updateSeatStatus` for the single-row hot path. `persistStadium` writes
everything and is used only for the one-off seed.

`persistStadium` inserts and updates but never deletes. Nothing in the application removes
a stand, section or seat yet; when something does, it will need an explicit delete.

### Column types

Every value column is TEXT or INTEGER on purpose. SQLite has dynamic typing and will turn
a numeric money column into a floating point REAL, which loses precision on prices. Prices
are stored as exact `BigDecimal` strings, timestamps as ISO-8601 text, and enums as their
Java names, with the mapper converting. Nothing here depends on how a particular driver or
dialect handles decimals and dates.

### What is not persisted yet

Accounts, bearer tokens, issued tickets and gate check-ins are still in memory and reset on
restart. They belong to other modules' owners and are planned as follow-up changes so each
author reviews the conversion of their own code. Login attempt counters are transient by
nature and should stay in memory. The demo fixture in `DemoDataService` is rebuilt
identically on every start, so it is deliberately never persisted.

## Important follow-up hardening

Module 1 uses in-memory demo state. Before production-like use, later modules must add database transactions, seat-hold expiry, concurrency control, authentication/authorization, audit records and payment-provider integration. Cancellation/refund policies must be implemented explicitly rather than assumed.
