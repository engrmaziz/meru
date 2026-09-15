# Annex C — Database DDL Notes, Indexes, Room Mirror

---

## C.1 Index priorities
- `trips(user_id, start_at DESC)`
- `trip_locations(trip_id, ts)`
- `leaderboard_entries(board_type, geo_id, period, season_id, rank)`
- `leaderboard_entries(user_id, board_type, period)`
- `vehicles(owner_user_id)`
- `service_records(vehicle_id, date DESC)`
- `workshops(city_id)`, GiST on `location` geography
- `workshop_brands(workshop_id, make)`
- `slots(workshop_id, start_at)` WHERE status open
- `bookings(customer_id, start_at DESC)`
- `history_shares(workshop_id, vehicle_id, expires_at)`

---

## C.2 PostGIS usage
- Workshop pin: `geography(Point,4326)`
- City bounds optional polygons for exploration %
- Road segments: linestrings; discovery = intersection of trip lines with unvisited segments (simplify for v1: geohash cells)

### Exploration v1 (pragmatic)
Use geohash precision 7–8 cells as `explored_segments` key per user/city. Upgrade to real road graph later (OSM extract).

---

## C.3 Entitlements
`user_entitlements(user_id, max_vehicles, premium_until, source, updated_at)`  
Default `max_vehicles=1`. Play verify increments.

---

## C.4 Room entities (Android)
`LocalUser`, `LocalVehicle`, `LocalTrip`, `LocalTripPoint`, `LocalTripEvent`, `PendingSync`, `LocalBooking`, `LocalChallenge`, `LocalFeatureFlags`, `CalibrationProfile`.

`PendingSync`: `{id, type, payload_json, attempts, next_attempt_at, status}`.

---

## C.5 Retention
- Raw high-frequency points: retain full on device limited window; server downsample after processing.
- Invoices/docs: retain per legal policy.
- Deleted users: anonymize public ranks after grace period.
