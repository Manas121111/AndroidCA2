# 🔄 SDLC Methodology
## SmartTour360 Android App
**Document ID:** STS-SDLC-003 | **Version:** 2.0 | **Patent Aligned:** LPU IDF 2026
**Team:** Manas Maheshwari (12218829) · K. Venkata Ram (12219197)

---

## Methodology: Agile Scrum

**Why Agile Scrum over other models:**

| Methodology | Verdict | Reason |
|---|---|---|
| Waterfall | ❌ | Patent requirements evolved across v1→v2→v3 backend — sequential model can't absorb that |
| V-Model | ❌ | Too rigid for a 2-person team; testing formalism slows sprint delivery |
| Spiral | ⚠️ | Risk-driven but too process-heavy for a student team + hackathon timeline |
| RAD | ⚠️ | Fast prototyping but no structured iteration tracking |
| **Agile Scrum** | ✅ | Iterative, aligns to backend version releases (v1→v2→v3), flexible to patent additions |

The backend itself was developed in 3 major versions (v1: core safety → v2: pricing + eco → v3: XAI NL + ecological predictor + mesh). The Android app **mirrors this versioning** through sprints.

---

## Sprint Plan — 8 Sprints × 1 Week Each

```
Sprint 0  →  Setup, architecture, environment
Sprint 1  →  Auth + Splash + Home skeleton
Sprint 2  →  Safety Flag API + XAI Explainer UI
Sprint 3  →  IoT real-time data + Blockchain Verified chip
Sprint 4  →  Eco Score + Pricing Fairness + Recommendations
Sprint 5  →  Hotel Listing + Cart + Booking Flow
Sprint 6  →  WebSocket Mesh Alerts + Trip Planner
Sprint 7  →  Polish + Patent scenario testing + Demo prep
```

---

## Detailed Sprint Breakdown

### 🔧 Sprint 0 — Setup & Architecture (Week 0)

**Goal:** Skeleton app, all dependencies wired, backend reachable.

**Tasks:**
- Android Studio project: Kotlin, Min SDK 26, Target SDK 34
- Folder structure: `data/remote`, `data/local`, `domain/usecase`, `domain/model`, `ui/`
- Gradle dependencies: Retrofit, OkHttp, Room, Hilt, Coil, Navigation Component, Paging 3
- Firebase project: Auth, Firestore, FCM
- `local.properties` configured with `BASE_URL`, `WS_URL`, `MAPS_API_KEY`
- Verify backend health: `GET /api/v1/health` returns all services green (PostgreSQL, InfluxDB, Kafka, Blockchain)
- Git repo: `main` + `develop` + `feature/*` branch strategy

**Deliverable:** Shell app that hits `/health` endpoint and logs response

---

### 🔐 Sprint 1 — Auth + Splash + Home Skeleton (Week 1)

**Goal:** User can sign in and see the home screen.

**Tasks:**
- Splash screen with SmartTour360 logo + loading animation
- Firebase Auth: Email/OTP + Google Sign-In
- **Pseudonymization**: hash user ID via HMAC-SHA256 before any API call (matching backend `pseudonymize()`)
- Onboarding preferences: budget, trip type, eco-priority toggle
- Home screen: category chips, placeholder destination cards
- ViewModel + Repository wiring for auth state

**Deliverable:** Login → Home navigation working

---

### 🚦 Sprint 2 — Safety Flag API + XAI Explainer (Week 2)

**Goal:** Core patent feature — flags with explanation on screen.

**Tasks:**
- `DestinationRepository.evaluateSafety(locationId, lat, lon)` → calls `POST /api/v1/safety/evaluate`
- Parse response: `flag`, `risk_score`, `components` (structural / situational / environmental), `explanation`, `blockchain_ref`
- Destination card: render flag as colored chip (Green/Yellow/Red) + icon (accessibility)
- Component breakdown bar on destination detail (3 bars: crime / weather / AQI)
- **"Why this flag?" BottomSheet** — renders the `explanation` NL text from SHAP engine
- RED flag: show `alternatives` array as "Safer Options" cards
- GPS-based lookup: `FusedLocationProviderClient` → auto-evaluate current location on home

**Deliverable:** Full safety flag + XAI explanation working end-to-end

---

### 🔗 Sprint 3 — IoT Data Display + Blockchain (Week 3)

**Goal:** Live IoT metrics visible; blockchain verification chip working.

**Tasks:**
- Destination detail: IoT metrics row — temperature, AQI, crowd density, wind speed (from API response IoT fields)
- Data freshness indicator: "Updated N min ago" from `timestamp` field
- Stale data warning if `data_age_seconds > 900` (matching `DATA_STALENESS_01` guardrail)
- **"Verified ✓" chip** on destination cards — calls `GET /api/v1/safety/verify/{id}` on tap
- Blockchain detail sheet: shows `blockchain_ref` hash + `verified: true/false`
- **RED flag booking acknowledgement dialog** → calls `POST /api/v1/booking/acknowledge`
- Store `booking_id` + `flag_at_booking_time` in Room DB `acknowledgements` table

**Deliverable:** IoT live data + blockchain verification both functional

---

### 🌿 Sprint 4 — Eco Score + Pricing Fairness + Recommendations (Week 4)

**Goal:** Sustainability and pricing patent features live.

**Tasks:**
- Eco Score badge component (reusable, shows 0–100 with color tier)
- Carbon footprint display on destination detail (kg CO₂)
- 🌿 eco-certified badge on hotel cards (`eco_certified: true`)
- **Ethical Score chip** (HIGH / MODERATE / LOW) from backend IF-THEN rule result
- **Ecological risk tier badge** (`MINIMAL` / `MODERATE` / `CRITICAL`) from predictive ecological impact
- Pricing call: `POST /api/v1/pricing/analyze` on hotel detail load
- Price Fairness Score bar on hotel detail
- **"Hidden fees detected"** banner if backend flags hidden costs
- Price trend indicator: RISING / STABLE / FALLING
- Recommendations screen: ranked list with sustainability index

**Deliverable:** Eco + pricing patent features complete

---

### 🛒 Sprint 5 — Hotel Listing + Cart + Booking (Week 5)

**Goal:** End-to-end booking flow.

**Tasks:**
- Hotel list screen with image carousel, filters (price, stars, eco, cancellation)
- Hotel detail: full amenities, room types, pricing breakdown, eco detail
- Date picker: `MaterialDatePicker`
- Add to Cart → Room DB `cart_items` (with price snapshot)
- Cart screen: items, subtotal, taxes, total
- Place Order: `POST /api/v1/booking/orders` → confirmation screen with `booking_id` + `blockchain_ref`
- My Bookings in Profile screen

**Deliverable:** Complete booking flow from search to confirmation

---

### 📡 Sprint 6 — WebSocket Mesh Alerts + Trip Planner (Week 6)

**Goal:** Real-time alerts + itinerary builder.

**Tasks:**
- **OkHttp WebSocket client** connecting to `wss://{host}/ws/mesh/sync?node_id={deviceId}`
- Parse incoming `FLAG_CHANGE_ALERT` messages
- **HMAC-SHA256 signature verification** on every received mesh message
- Update destination card flag color in real-time on alert (no page refresh)
- FCM push notification for backgrounded flag changes
- Exponential backoff WebSocket reconnect on disconnect
- Trip Planner: create trip, add items, day-by-day itinerary view, drag-to-reorder
- Room DB sync + optional Firestore backup

**Deliverable:** WebSocket mesh alerts live + trip planner complete

---

### 🧪 Sprint 7 — Polish, Patent Testing & Demo (Week 7)

**Goal:** Demo-ready app validated against patent evaluation scenarios.

**Tasks:**
- Fix all lint errors; 0 lint errors on release build
- Unit tests for all ViewModels (JUnit 5 + MockK)
- UI tests: Login → Home, Safety Flag → XAI Explainer, Cart → Order, WebSocket alert → UI update
- **Run 4 patent evaluation scenarios** (S1–S4, see Testing doc)
- Dark mode QA, font scale QA, TalkBack QA
- ProGuard rules, release APK via Android App Bundle
- Record demo video

**Deliverable:** Signed demo APK + all docs complete

---

## Patent Milestone Alignment

| Patent Phase | Duration | Backend Milestone | Android Sprint |
|---|---|---|---|
| Phase 0 — Foundation | Month 1–2 | Repo + Docker + CI/CD | Sprint 0 |
| Phase 1 — Core Safety | Month 3–5 | Risk calculator + XAI + Blockchain | Sprint 1–3 |
| Phase 2 — Full Patent Coverage | Month 6–9 | Pricing + Eco + Mesh + Ecological Predictor | Sprint 4–6 |
| Phase 3 — Prototype Testing | Month 10–11 | Scenario S1–S4 evaluation + KPI validation | Sprint 7 |
| Phase 4 — AR Layer | Month 12–15 | AR overlay endpoint `/api/v1/ar/overlay` | Future scope (post-hackathon) |

---

## Team Workflow

```
Daily standup     10 min WhatsApp voice note
Sprint review     End of sprint — demo working feature on device
Sprint retro      What worked, what didn't
Backlog grooming  Every Sunday — add/refine next sprint tasks
```

## Branch Strategy

```
main        →  demo-ready, stable
develop     →  integration branch
feature/xxx →  one per feature (e.g. feature/websocket-mesh)
hotfix/xxx  →  urgent fix on main
```

## Full Toolchain

| Tool | Purpose |
|---|---|
| Android Studio | IDE |
| Kotlin 1.9+ | Language |
| Retrofit + OkHttp | REST API calls + WebSocket client |
| Hilt | Dependency injection |
| Room | Local DB + offline cache |
| Paging 3 | Paginated lists |
| Coil | Image loading |
| Jetpack Navigation | Screen navigation |
| Firebase Auth / Firestore / FCM | Auth + cloud sync + push |
| JUnit 5 + MockK | Unit testing |
| Espresso | UI testing |
| Postman | API contract testing against backend |
| Figma | UI wireframes |

---

*End of SDLC Methodology — STS-SDLC-003 v2.0*
