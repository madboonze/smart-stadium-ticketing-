# Module 7 — Stadium and Seat Map

This module adds the customer-facing stadium page to the existing Spring Boot application.

## Files

- `src/main/resources/static/index.html` — stadium booking page structure
- `src/main/resources/static/stadium.css` — responsive visual design
- `src/main/resources/static/stadium.js` — API loading, seat selection and Three.js scene

## Install

Copy the `README-MODULE-7.md` file and the contents of `src` into the root of the existing repository (the folder containing `pom.xml`). Merge the `src` folders. Do not replace `pom.xml` or other modules' files.

## Run

From the repository root:

```bash
mvn spring-boot:run
```

Open `http://localhost:8080/`. Spring Boot serves `index.html` from `src/main/resources/static`.

The page reads the existing `GET /api/demo/stadium` endpoint. Java remains responsible for domain data and prices; the browser uses Three.js only to draw the interactive stadium. Three.js is loaded from jsDelivr, so the browser needs internet access to render the 3D view. If it cannot load, the page still shows a seat list and selection summary.

Seat selection is a front-end preview. The current project does not yet expose the Module 5 booking/hold endpoint, so this page does not reserve seats or create bookings. The selected total uses the prices returned by the Java API.
