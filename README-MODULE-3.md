# Module 3 — Match Management

Add these files to the root of your existing `smart-stadium` repository, preserving the paths.

## What this module adds
- Match creation and scheduling
- Stadium assignment (using the current Module 1 stadium model)
- Match listing and lookup by ID
- Controlled status transitions: Scheduled → On Sale → Sold Out → Completed
- Cancellation for matches that have not completed
- Input validation and consistent API error responses

## API endpoints
- `GET /api/matches`
- `GET /api/matches/{id}`
- `POST /api/matches`
- `POST /api/matches/{id}/on-sale`
- `POST /api/matches/{id}/sold-out`
- `POST /api/matches/{id}/complete`
- `POST /api/matches/{id}/cancel`

Example create request:
```json
{
  "homeTeam": "Dubai Falcons",
  "awayTeam": "Abu Dhabi United",
  "kickoff": "2027-02-18T19:30:00",
  "stadiumId": "stadium-001"
}
```

The `kickoff` value is local date-time without a timezone. The API currently uses the server's local-time convention. A later deployment module should standardize the stadium timezone.

## Install
Copy the contents of this ZIP into your existing repository, allowing the included `Match.java` and `MatchStatus.java` to replace the Module 1 versions. Do not delete the rest of your project.

Then run:
```bash
mvn test
mvn spring-boot:run
```

Note: Matches are currently stored in memory and reset when the application restarts. Database persistence is a later module.
