# 🏗️ System Design
## SmartTour360 Android App
**Document ID:** STS-SYS-004 | **Version:** 2.0 | **Patent Aligned:** LPU IDF 2026

---

## Android MVVM — Clean Architecture

```
┌──────────────────────────────────────────────────────┐
│                    UI LAYER                          │
│  Fragment / Composable  →  observes StateFlow        │
└───────────────────────┬──────────────────────────────┘
                        │ calls
┌───────────────────────▼──────────────────────────────┐
│                  VIEWMODEL LAYER                     │
│  StateFlow · LiveData · SavedStateHandle             │
│  Calls Use Cases — zero Android framework imports   │
└───────────────────────┬──────────────────────────────┘
                        │ calls
┌───────────────────────▼──────────────────────────────┐
│               DOMAIN LAYER (Use Cases)               │
│  EvaluateSafetyUseCase   GetDestinationsUseCase      │
│  VerifyBlockchainUseCase AcknowledgeRiskUseCase      │
│  RankRecommendationsUseCase  AnalyzePricingUseCase   │
└──────────┬────────────────────────┬──────────────────┘
           │                        │
┌──────────▼──────────┐  ┌──────────▼─────────────────┐
│  REMOTE DATA SOURCE │  │  LOCAL DATA SOURCE          │
│  Retrofit (REST)    │  │  Room DB (offline cache)    │
│  OkHttp WS (mesh)   │  │  DataStore (prefs + tokens) │
└─────────────────────┘  └────────────────────────────┘
```

---

## Module / Folder Structure

```
app/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── SafetyApi.kt         → POST /safety/evaluate, GET /safety/verify/{id}
│   │   │   ├── BookingApi.kt        → GET /booking/hotels, POST /booking/orders
│   │   │   ├── PricingApi.kt        → POST /pricing/analyze
│   │   │   ├── RecommendationApi.kt → POST /recommendations/rank
│   │   │   └── AcknowledgeApi.kt   → POST /booking/acknowledge
│   │   ├── dto/                     → API response data classes (SafetyResponseDto, etc.)
│   │   ├── websocket/
│   │   │   ├── MeshWebSocketClient.kt  → OkHttp WS + HMAC-SHA256 verify
│   │   │   └── MeshMessage.kt          → FLAG_CHANGE_ALERT data class
│   │   └── datasource/              → RemoteSafetyDataSource, RemoteHotelDataSource
│   ├── local/
│   │   ├── db/
│   │   │   └── AppDatabase.kt       → Room DB (version 1)
│   │   ├── dao/
│   │   │   ├── DestinationDao.kt
│   │   │   ├── HotelDao.kt
│   │   │   ├── TripDao.kt
│   │   │   ├── CartDao.kt
│   │   │   └── AcknowledgementDao.kt
│   │   └── entity/                  → Room entity data classes
│   └── repository/
│       ├── SafetyRepositoryImpl.kt
│       ├── HotelRepositoryImpl.kt
│       ├── RecommendationRepositoryImpl.kt
│       └── BookingRepositoryImpl.kt
│
├── domain/
│   ├── model/                       → Destination, Hotel, SafetyFlag, EcoScore, Trip, Order
│   ├── repository/                  → interfaces (SafetyRepository, HotelRepository…)
│   └── usecase/
│       ├── EvaluateSafetyUseCase.kt
│       ├── VerifyBlockchainUseCase.kt
│       ├── AcknowledgeRiskUseCase.kt
│       ├── RankRecommendationsUseCase.kt
│       ├── AnalyzePricingUseCase.kt
│       └── GetDestinationsUseCase.kt
│
├── ui/
│   ├── home/
│   ├── search/
│   ├── destination/       → DestinationDetailFragment + SafetyFlagCard + XAIBottomSheet
│   ├── blockchain/        → BlockchainVerifySheet + AcknowledgeDialog
│   ├── hotel/
│   ├── pricing/           → PricingFairnessCard + HiddenCostBanner
│   ├── eco/               → EcoScoreCard + EthicalScoreChip + EcologicalRiskBadge
│   ├── cart/
│   ├── booking/
│   ├── trip/
│   ├── auth/
│   ├── mesh/              → MeshAlertViewModel (processes WS FLAG_CHANGE_ALERT)
│   └── profile/
│
├── di/                    → Hilt: NetworkModule, DatabaseModule, WebSocketModule
└── utils/
    ├── Pseudonymizer.kt   → HMAC-SHA256 user ID pseudonymization
    ├── HmacVerifier.kt    → Verify mesh message HMAC-SHA256 signatures
    └── FlagColorMapper.kt → risk_score → FlagColor + color res
```

---

## Room Database Schema

### `destinations` table
```sql
CREATE TABLE destinations (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    state           TEXT NOT NULL,
    image_url       TEXT,
    flag_color      TEXT NOT NULL CHECK (flag_color IN ('GREEN','YELLOW','RED')),
    risk_score      REAL NOT NULL,        -- 0.0–1.0
    eco_score       INTEGER NOT NULL,     -- 0–100
    ethical_score   TEXT,                 -- HIGH | MODERATE | LOW
    eco_risk_tier   TEXT,                 -- MINIMAL | MODERATE | CRITICAL
    carbon_kg       REAL,
    latitude        REAL,
    longitude       REAL,
    blockchain_ref  TEXT,                 -- tx hash from last evaluation
    explanation     TEXT,                 -- XAI NL explanation
    cached_at       INTEGER NOT NULL      -- Unix timestamp
);
```

### `hotels` table
```sql
CREATE TABLE hotels (
    id               TEXT PRIMARY KEY,
    destination_id   TEXT NOT NULL,
    name             TEXT NOT NULL,
    star_rating      INTEGER,
    price_per_night  REAL NOT NULL,
    eco_certified    INTEGER NOT NULL,    -- 0 | 1
    eco_score        INTEGER,
    price_fairness   REAL,               -- 0.0–1.0 from pricing API
    hidden_costs     TEXT,               -- JSON array of flagged fees
    price_trend      TEXT,               -- RISING | STABLE | FALLING
    image_urls       TEXT,               -- JSON array
    FOREIGN KEY (destination_id) REFERENCES destinations(id)
);
```

### `acknowledgements` table
```sql
CREATE TABLE acknowledgements (
    id                    TEXT PRIMARY KEY,
    booking_id            TEXT NOT NULL,
    flag_at_booking_time  TEXT NOT NULL,  -- GREEN | YELLOW | RED
    blockchain_ref        TEXT,
    acknowledged_at       INTEGER NOT NULL
);
```

### `cart_items` table
```sql
CREATE TABLE cart_items (
    id              TEXT PRIMARY KEY,
    hotel_id        TEXT NOT NULL,
    check_in_date   INTEGER NOT NULL,
    check_out_date  INTEGER NOT NULL,
    num_guests      INTEGER NOT NULL,
    price_snapshot  REAL NOT NULL
);
```

### `trips` + `trip_items` tables
```sql
CREATE TABLE trips (
    id         TEXT PRIMARY KEY,
    name       TEXT NOT NULL,
    start_date INTEGER NOT NULL,
    end_date   INTEGER NOT NULL
);

CREATE TABLE trip_items (
    id          TEXT PRIMARY KEY,
    trip_id     TEXT NOT NULL,
    item_type   TEXT NOT NULL,     -- DESTINATION | HOTEL
    item_id     TEXT NOT NULL,
    day_number  INTEGER NOT NULL,
    notes       TEXT,
    FOREIGN KEY (trip_id) REFERENCES trips(id)
);
```

---

## Full API Contracts

### POST /api/v1/safety/evaluate
```kotlin
// Request
data class SafetyEvaluateRequest(
    val location_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy_m: Double = 50.0
)

// Response
data class SafetyEvaluateResponse(
    val location_id: String,
    val flag: String,               // "GREEN" | "YELLOW" | "RED"
    val risk_score: Double,         // 0.0–1.0
    val explanation: String,        // XAI NL text (SHAP-derived)
    val components: Components,     // { structural, situational, environmental }
    val blockchain_ref: String,     // Ethereum tx hash
    val timestamp: Double,
    val alternatives: List<String>? // Populated on RED flag
)

data class Components(
    val structural: Double,    // w=0.45: crime/incident history
    val situational: Double,   // w=0.35: weather/events
    val environmental: Double  // w=0.20: AQI/UV
)
```

**Sample Response:**
```json
{
  "location_id": "IND-GOA-01",
  "flag": "YELLOW",
  "risk_score": 0.4821,
  "explanation": "Caution advised — elevated weather/event conditions: weather severity index at 58.7%. Structural risk is within range. Environmental hazard nominal.",
  "components": { "structural": 0.18, "situational": 0.587, "environmental": 0.12 },
  "blockchain_ref": "0x3f4a9b2c1d8e7f0a5c3b2d1e4f8a9b0c",
  "timestamp": 1720800000.0
}
```

---

### GET /api/v1/safety/verify/{location_id}
```json
{
  "location_id": "IND-GOA-01",
  "verified": true,
  "blockchain_ref": "0x3f4a9b2c1d8e7f0a5c3b2d1e4f8a9b0c",
  "flag_on_chain": "YELLOW",
  "timestamp_on_chain": 1720800000
}
```

---

### POST /api/v1/booking/acknowledge
```kotlin
// Request
data class AcknowledgeRequest(
    val booking_id: String,
    val flag_shown: String,         // Flag the user was shown
    val user_pseudonym: String,     // HMAC-SHA256 of user_id
    val acknowledged: Boolean
)

// Response
data class AcknowledgeResponse(
    val booking_id: String,
    val blockchain_ref: String,     // On-chain log of acknowledgement
    val status: String              // "LOGGED"
)
```

---

### POST /api/v1/pricing/analyze
```json
// Response
{
  "location_id": "IND-GOA-01",
  "price_fairness_score": 0.73,
  "price_trend": "RISING",
  "hidden_costs": [
    { "label": "Resort fee", "amount": 850.0, "currency": "INR" }
  ],
  "guardrail_triggered": "PRICE_DISCRIMINATION",
  "base_price": 3200.0,
  "offered_price": 3900.0
}
```

---

### POST /api/v1/recommendations/rank
```json
// Request
{ "location_ids": ["IND-GOA-01", "IND-MAN-01", "IND-VAR-01"] }

// Response — ranked by ethical scoring
[
  {
    "location_id": "IND-MAN-01",
    "rank": 1,
    "eco_score": 78,
    "carbon_kg": 32.1,
    "sustainability_index_pct": 24,
    "ethical_score": "HIGH",
    "ecological_risk_tier": "MINIMAL"
  }
]
```

---

## WebSocket Mesh Client

```kotlin
// di/WebSocketModule.kt
@Module @InstallIn(SingletonComponent::class)
object WebSocketModule {
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
}

// data/remote/websocket/MeshWebSocketClient.kt
class MeshWebSocketClient @Inject constructor(
    private val client: OkHttpClient,
    private val hmacVerifier: HmacVerifier,
    @WsUrl private val wsUrl: String
) {
    private val _alerts = MutableSharedFlow<MeshAlert>()
    val alerts: SharedFlow<MeshAlert> = _alerts.asSharedFlow()

    fun connect(nodeId: String) {
        val request = Request.Builder()
            .url("$wsUrl?node_id=$nodeId")
            .build()
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(ws: WebSocket, text: String) {
                val msg = Json.decodeFromString<MeshMessage>(text)
                if (msg.type == "FLAG_CHANGE_ALERT") {
                    if (hmacVerifier.verify(msg)) {   // HMAC-SHA256 signature check
                        _alerts.tryEmit(MeshAlert(
                            locationId = msg.location_id,
                            newFlag    = msg.new_flag,
                            urgent     = msg.urgent
                        ))
                    }
                }
            }
            override fun onFailure(ws: WebSocket, t: Throwable, r: Response?) {
                reconnectWithBackoff(nodeId)   // Exponential backoff
            }
        })
    }
}

// utils/HmacVerifier.kt
class HmacVerifier @Inject constructor(
    @MeshSecretKey private val secretKey: String
) {
    fun verify(msg: MeshMessage): Boolean {
        val payload = msg.toSortedJson(excludeSignature = true)
        val expected = Mac.getInstance("HmacSHA256")
            .apply { init(SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")) }
            .doFinal(payload.toByteArray())
            .toHex()
        return MessageDigest.isEqual(expected.toByteArray(), msg.signature.toByteArray())
    }
}
```

---

## Pseudonymization (Privacy-First)

```kotlin
// utils/Pseudonymizer.kt
object Pseudonymizer {
    fun pseudonymize(userId: String, secretKey: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKey.toByteArray(), "HmacSHA256"))
        return mac.doFinal(userId.toByteArray())
            .toHex()
            .take(32)   // 32 hex chars — matches backend pseudonymize()
    }
}
// Called before EVERY API request that includes a user identifier.
// Raw user ID (email/UID) is NEVER sent to the backend.
```

---

## Navigation Graph

```
SplashScreen
└─→ OnboardingScreen (first launch)
└─→ LoginScreen
        └─→ HomeScreen  ← NavHost Root
                ├─→ SearchScreen
                │       └─→ DestinationDetailScreen
                │               ├─→ XAIBottomSheet (modal)
                │               ├─→ BlockchainVerifySheet (modal)
                │               ├─→ AcknowledgeRiskDialog (modal)
                │               └─→ HotelListScreen
                │                       └─→ HotelDetailScreen
                │                               └─→ CartScreen
                │                                       └─→ OrderConfirmScreen
                ├─→ RecommendationsScreen
                ├─→ TripPlannerScreen
                └─→ ProfileScreen
                        └─→ MyBookingsScreen
```

---

*End of System Design — STS-SYS-004 v2.0*
