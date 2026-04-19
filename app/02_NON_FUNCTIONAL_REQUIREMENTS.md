# ⚙️ Non-Functional Requirements
## SmartTour360 Android App
**Document ID:** STS-NFR-002 | **Version:** 2.0 | **Patent Aligned:** LPU IDF 2026
**Team:** Manas Maheshwari (12218829) · K. Venkata Ram (12219197)

---

## NFR-01 · Performance (Patent KPI: Response Time < 500ms)

| ID | Requirement | Target |
|---|---|---|
| NFR-01.1 | App cold start (splash to home) | < 3 seconds |
| NFR-01.2 | Safety flag API call → flag rendered on screen | < 500ms (patent north star KPI) |
| NFR-01.3 | Search results appear after user stops typing | < 500ms (300ms debounce + 200ms render) |
| NFR-01.4 | WebSocket mesh alert → UI flag color updated | < 200ms after message received |
| NFR-01.5 | RecyclerView / LazyColumn scroll on destination list | 60 fps minimum |
| NFR-01.6 | Memory usage during active browsing | ≤ 150 MB RAM |
| NFR-01.7 | App shall not ANR. All network calls on IO coroutine dispatcher | 0 ANR events |

**Implementation:**
- Kotlin Coroutines + Flow for all async — never block main thread
- Paging 3 for paginated destination/hotel lists
- Coil for image loading with memory + disk cache
- OkHttp connection pooling for persistent backend connections

---

## NFR-02 · Reliability & Fault Tolerance

| ID | Requirement |
|---|---|
| NFR-02.1 | API failures shall use exponential backoff retry (max 3 retries, 1s/2s/4s intervals) |
| NFR-02.2 | On API failure, Room DB cached data shall be shown with a staleness warning |
| NFR-02.3 | WebSocket disconnects shall trigger automatic reconnect with backoff — not a silent failure |
| NFR-02.4 | App shall handle process death and restore UI state via `ViewModel` + `SavedStateHandle` |
| NFR-02.5 | Cart items and trip data shall survive app kill — persisted in Room DB |
| NFR-02.6 | If blockchain verification call fails, the UI shall show "Verification Pending" — not a crash |

---

## NFR-03 · Security (Patent: Blockchain + Privacy-First)

| ID | Requirement |
|---|---|
| NFR-03.1 | All network calls shall use HTTPS with OkHttp `CertificatePinner` (SSL pinning) |
| NFR-03.2 | Auth tokens stored in `EncryptedSharedPreferences` — never in plain SharedPreferences |
| NFR-03.3 | No PII (name, phone, email) shall be included in safety evaluation API calls — only pseudonymized `user_id` (HMAC-SHA256, matching backend `pseudonymize()` function) |
| NFR-03.4 | API keys (OpenWeatherMap, Maps) stored in `local.properties` → injected via `BuildConfig` — never hardcoded in source |
| NFR-03.5 | WebSocket mesh messages shall be HMAC-SHA256 signature-verified before the app processes them (matching backend `MESH_SECRET_KEY`) |
| NFR-03.6 | ProGuard / R8 rules shall obfuscate all class names in release builds |
| NFR-03.7 | Firebase Security Rules shall restrict Firestore reads/writes to authenticated users only |
| NFR-03.8 | No raw GPS coordinates shall be logged or persisted — only `region_code` derived from coordinates (matching backend privacy design) |

---

## NFR-04 · Ethical AI Transparency (Patent Core Requirement)

> These NFRs directly implement the patent's "Ethical AI Transparency" parameter — target: HIGH column.

| ID | Requirement |
|---|---|
| NFR-04.1 | Every safety flag displayed shall have an accessible explanation — the XAI text shall never be hidden behind more than one tap |
| NFR-04.2 | The dominant risk factor driving a flag shall always be named explicitly (e.g., structural / situational / environmental) |
| NFR-04.3 | The app shall never show a safety flag without a corresponding `blockchain_ref` — if missing, show "Verification Pending" |
| NFR-04.4 | Recommendations shall be ranked by the backend's ethical scoring (safety + eco + fair pricing) — the app shall not re-rank by revenue or any other commercial criteria |
| NFR-04.5 | The app shall display the Ethical Score (HIGH / MODERATE / LOW) derived from the patent IF-THEN rule on every destination |
| NFR-04.6 | A "How is this scored?" info button shall be accessible on every scored item — linking to a transparent explanation dialog |
| NFR-04.7 | Backend guardrail violations (e.g., `PRICE_DISCRIMINATION`, `SAFETY_SUPPRESSION_ATTEMPT`) surfaced in the API response shall be shown to the user as plain-language warnings |

---

## NFR-05 · Usability & Accessibility

| ID | Requirement |
|---|---|
| NFR-05.1 | All interactive elements: minimum 48dp × 48dp touch target |
| NFR-05.2 | TalkBack screen reader support — all images, icons, and flag chips have `contentDescription` |
| NFR-05.3 | Safety flag colors (green/yellow/red) shall also use icons + text labels — color alone shall not be the only signal (accessibility for color-blind users) |
| NFR-05.4 | Material 3 dynamic theming — full light and dark mode support |
| NFR-05.5 | All text meets WCAG AA contrast ratio (≥ 4.5:1 for normal text) |
| NFR-05.6 | Font sizes respect system font scale settings |
| NFR-05.7 | Error, Loading, Empty, and Offline states each have a distinct UI treatment |

---

## NFR-06 · Maintainability

| ID | Requirement |
|---|---|
| NFR-06.1 | Strict MVVM — zero business logic in Fragment or Activity |
| NFR-06.2 | All ViewModels covered by unit tests using JUnit 5 + MockK |
| NFR-06.3 | Repository interfaces used throughout — enables Hilt injection and testability |
| NFR-06.4 | Kotlin lint clean — 0 lint errors on release builds |
| NFR-06.5 | Feature modules: `:feature:home`, `:feature:search`, `:feature:safety`, `:feature:booking`, `:feature:trip` |

---

## NFR-07 · Compatibility

| ID | Requirement |
|---|---|
| NFR-07.1 | Min Android: 8.0 (API 26) — file-based encryption + biometric API available |
| NFR-07.2 | Target SDK: 34 (Android 14) |
| NFR-07.3 | Screen sizes: 5" phones to 12" tablets |
| NFR-07.4 | Portrait and landscape both supported without data loss |
| NFR-07.5 | APK/AAB size ≤ 30MB |

---

## NFR-08 · Patent Evaluation KPIs (System Characterization — Table 1)

> Source: LPU Invention Disclosure (2026) — Table 1: Parameters Considered for Evaluation
> The Android app must reach the **"High"** column for each parameter.

| Patent Parameter | Low (unacceptable) | Moderate | High (target) | Android Implementation |
|---|---|---|---|---|
| Ethical AI Transparency | No explanation shown | Some explanation | Every flag has deterministic XAI + dominant factor named | XAI BottomSheet on every flag; `explanation` field always rendered |
| Data Privacy & Security | No encryption | Basic HTTPS | SSL pinning + EncryptedPrefs + pseudonymized IDs + on-chain audit | NFR-03 full set |
| Sustainability Scoring | No eco data shown | Basic eco badge | Full eco score + carbon footprint + SDG alignment + ethical score chip | FR-07 full set |
| Safety Prediction (Real-Time) | Static data only | API-based checks | Live IoT data + flag updates via WebSocket mesh < 200ms | FR-06, FR-10 |
| Connectivity (Decentralized) | App offline = broken | Shows cached data | Room DB offline + WebSocket reconnect + mesh alerts | NFR-02 + FR-13 |

---

*End of Non-Functional Requirements — STS-NFR-002 v2.0*
