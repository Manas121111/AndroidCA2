# 🌏 SmartTour360 Android App
<<<<<<< HEAD
### Swadeshi for Atmanirbhar Bharat | AICTE Problem Statement ID: 25137
> **Team:** Manas Maheshwari (12218829) · K. Venkata Ram (12219197)
> **Patent:** LPU Invention Disclosure 2026 — *"Ethical AI Decision Systems for Recommending Safe and Sustainable Tourism Options"*
> **Platform:** Android Studio · Kotlin · MVVM · Clean Architecture
=======

**Swadeshi for Atmanirbhar Bharat** 🇮🇳
**AICTE Problem Statement ID:** 25137

**Team:**
Manas Maheshwari (12218829)
K. Venkata Ram (12219197)

**Patent:**
LPU Invention Disclosure 2026 — *“Ethical AI Decision Systems for Recommending Safe and Sustainable Tourism Options”*

**Platform:**
Android Studio · Kotlin · MVVM · Clean Architecture
>>>>>>> b5c4e14414df6bd1468399d367bc647232311406

---

## 🧠 What Is This App

<<<<<<< HEAD
**SmartTour360** is the **native Android client** of the TicketKaru/SmartTour360 platform — an Ethical AI-powered travel companion that gives Indian travelers:

- 🚦 **Real-time Green / Yellow / Red safety flags** per destination — computed by XGBoost + SHAP on the backend
- 🌿 **Eco sustainability scores** with SDG-aligned rankings
- 🔗 **Blockchain-backed audit trail** — every safety evaluation is hashed immutably on Ethereum / Hyperledger Fabric
- 📡 **Live IoT sensor streams** via Apache Kafka (weather, AQI, crowd density from Zigbee/LoRaWAN sensors)
- 💬 **Explainable AI (XAI)** — deterministic plain-language reason for every flag decision
- 💸 **Dynamic pricing fairness + hidden cost detection**
- 📲 **Real-time push alerts** via WebSocket Gossip Mesh when a saved destination's flag changes
- 🗺️ **Trip planner + smart booking flow with user acknowledgement on blockchain**

This app connects to the FastAPI backend (Python 3.11 · XGBoost · SHAP · PostgreSQL · InfluxDB · Kafka · Ethereum) via HTTPS REST and WebSocket.
=======
SmartTour360 is the native Android client of the TicketKaru/SmartTour360 platform — an Ethical AI-powered travel companion for Indian travelers.

### Key Capabilities

* 🚦 Real-time Green / Yellow / Red safety flags per destination (XGBoost + SHAP)
* 🌿 Eco sustainability scores with SDG-aligned rankings
* 🔗 Blockchain-backed audit trail (Ethereum / Hyperledger Fabric)
* 📡 Live IoT sensor streams via Apache Kafka
* 💬 Explainable AI (XAI) with plain-language reasoning
* 💸 Dynamic pricing fairness and hidden cost detection
* 📲 Real-time alerts via WebSocket Gossip Mesh
* 🗺️ Trip planner with blockchain-based user acknowledgement

Backend: FastAPI (Python 3.11 · XGBoost · SHAP · PostgreSQL · InfluxDB · Kafka · Ethereum) via REST + WebSocket
>>>>>>> b5c4e14414df6bd1468399d367bc647232311406

---

## 🏗️ Full-Stack Architecture

```
<<<<<<< HEAD
┌──────────────────────────────────────────────────────────────────┐
│                   ANDROID APP (This Repo)                        │
│  Kotlin · MVVM · Retrofit · OkHttp WS · Room · Hilt · Jetpack   │
└───────────────────────────┬──────────────────────────────────────┘
                            │ HTTPS REST + wss:// WebSocket
┌───────────────────────────▼──────────────────────────────────────┐
│              FASTAPI BACKEND (Python 3.11)                       │
│  XGBoost + SHAP · Pydantic · Alembic · MLflow                   │
│  ┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ Risk Calculator  │  │ XAI Explainer│  │Ethical Guardrails│   │
│  │ R = w1·S_struct  │  │ SHAP + NL    │  │ 5 hard rules     │   │
│  │ + w2·S_sit       │  │ templates    │  │ (patent-defined) │   │
│  │ + w3·S_env       │  └──────────────┘  └──────────────────┘   │
│  └──────────────────┘                                            │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  Recommendation Engine · Eco Score · Pricing Fairness   │    │
│  │  Predictive Ecological Impact · WebSocket Gossip Mesh    │    │
│  └──────────────────────────────────────────────────────────┘    │
└────────┬─────────────────────┬────────────────┬──────────────────┘
         │                     │                │
┌────────▼──────┐  ┌───────────▼──────┐  ┌─────▼─────────────────┐
│  PostgreSQL   │  │  InfluxDB        │  │  Blockchain Ledger    │
│  Users,       │  │  IoT time-series │  │  Ethereum + Solidity  │
│  evaluations, │  │  sensor_readings │  │  IPFS for payloads    │
│  bookings,    │  │  30d retention   │  │  Hyperledger (prod)   │
│  incidents    │  └──────────────────┘  └───────────────────────┘
└───────────────┘
         │
┌────────▼──────────────────────────────────────────────────────┐
│                  REAL-TIME DATA SOURCES                        │
│  Kafka Topics (IoT ingestion):                                 │
│    smarttour.sensor.weather      ← OpenWeatherMap API         │
│    smarttour.sensor.environment  ← AQI / UV sensors           │
│    smarttour.sensor.crowd        ← Zigbee/LoRaWAN wearables   │
│    smarttour.alerts.safety       ← Processed flag events      │
│  External Free APIs:                                           │
│    OpenWeatherMap API            weather + alerts              │
│    US State Dept Travel Advisory government safety advisories  │
│    PredictHQ Events API          crowd / local event risk      │
│    FareTracker Dynamic Pricing   pricing fairness              │
│    GreenCert Eco Certification   eco scores                    │
│  IoT Hardware:                                                 │
│    Zigbee mesh     short-range indoor/venue sensors            │
│    LoRaWAN         long-range wilderness / off-grid            │
│    AWS IoT Greengrass v2         edge ML inference             │
└───────────────────────────────────────────────────────────────┘
=======
ANDROID APP (Kotlin · MVVM · Retrofit · OkHttp · Room · Hilt · Jetpack)
                      │
              HTTPS REST + WebSocket
                      │
FASTAPI BACKEND (Python 3.11)
  ├─ Risk Calculator (XGBoost)
  ├─ XAI Explainer (SHAP)
  ├─ Ethical Guardrails
  ├─ Recommendation Engine
  ├─ Pricing Fairness
                      │
        ┌─────────────┼─────────────┬─────────────┐
        │             │             │             │
   PostgreSQL     InfluxDB     Blockchain       Kafka
   (Users/Data)   (IoT Data)   (Ethereum)      (Streams)
>>>>>>> b5c4e14414df6bd1468399d367bc647232311406
```

---

## 📱 Feature → Backend Map

<<<<<<< HEAD
| Android Feature | Data Source | Backend Endpoint |
|---|---|---|
| Safety Flag (Green/Yellow/Red) | Kafka + Crime API + Weather API | `POST /api/v1/safety/evaluate` |
| XAI "Why this flag?" | SHAP + NL engine | Included in safety response |
| Eco Score Badge | GreenCert + Carbon model | `POST /api/v1/recommendations/rank` |
| Pricing Fairness + Hidden Costs | FareTracker API | `POST /api/v1/pricing/analyze` |
| Ranked Recommendations | Recommendation Engine | `POST /api/v1/recommendations/rank` |
| Blockchain Verified chip | Ethereum ledger | `GET /api/v1/safety/verify/{id}` |
| Real-Time Safety Alerts | WebSocket Gossip Mesh | `wss://api/ws/mesh/sync` |
| Hotel Listing / Booking | PostgreSQL | `GET /api/v1/booking/hotels` |
| User Acknowledgement | Blockchain UserAck contract | `POST /api/v1/booking/acknowledge` |
| GPS-based Safety Lookup | FusedLocationProvider | `POST /api/v1/safety/evaluate` (lat/lon) |
=======
| Android Feature      | Data Source  | Endpoint                          |
| -------------------- | ------------ | --------------------------------- |
| Safety Flag          | Kafka + APIs | POST /api/v1/safety/evaluate      |
| XAI Explanation      | SHAP Engine  | Included in response              |
| Eco Score            | GreenCert    | POST /api/v1/recommendations/rank |
| Pricing Fairness     | FareTracker  | POST /api/v1/pricing/analyze      |
| Recommendations      | AI Engine    | POST /api/v1/recommendations/rank |
| Blockchain Verify    | Ethereum     | GET /api/v1/safety/verify/{id}    |
| Live Alerts          | WebSocket    | wss://api/ws/mesh/sync            |
| Hotel Booking        | PostgreSQL   | GET /api/v1/booking/hotels        |
| User Acknowledgement | Blockchain   | POST /api/v1/booking/acknowledge  |
| GPS Safety           | Location API | POST /api/v1/safety/evaluate      |

---

## 🌐 Real-Time Data Sources

### Kafka Topics

* smarttour.sensor.weather
* smarttour.sensor.environment
* smarttour.sensor.crowd
* smarttour.alerts.safety

### External APIs

* OpenWeatherMap
* Travel Advisory APIs
* PredictHQ
* FareTracker
* GreenCert

### IoT Hardware

* Zigbee
* LoRaWAN
* AWS IoT Greengrass

---

## 🔗 Patent → Android Implementation

| Component               | Implementation              |
| ----------------------- | --------------------------- |
| Ethical AI Module       | Retrofit → /safety/evaluate |
| XAI Explainability      | BottomSheet explanation UI  |
| Blockchain Verification | Verified chip + hash        |
| IoT Streams             | AQI, weather, crowd UI      |
| Mesh Network            | WebSocket sync              |
| Pricing Fairness        | Cost transparency UI        |
| Sustainability          | Eco score badge             |
| GPS Integration         | Location-based API calls    |
| User Acknowledgement    | Risk confirmation dialog    |
| Ethical Rules           | Ethical score indicator     |
>>>>>>> b5c4e14414df6bd1468399d367bc647232311406

---

## 📁 Documentation Index

<<<<<<< HEAD
| File | Contents |
|---|---|
| `README.md` | ← You are here |
| `01_FUNCTIONAL_REQUIREMENTS.md` | What the app must do — Auth, Safety AI, IoT, Blockchain, Eco, Booking, Mesh Alerts |
| `02_NON_FUNCTIONAL_REQUIREMENTS.md` | Performance, Security, Ethical AI NFRs aligned to patent |
| `03_SDLC_METHODOLOGY.md` | Agile Scrum — 8 sprints, patent milestone phases |
| `04_SYSTEM_DESIGN.md` | MVVM layers, Room schema, full API contracts, risk formula, mesh WebSocket |
| `05_UI_UX_SPECIFICATION.md` | All screens, design tokens, XAI explanation card, blockchain chip |
| `06_TESTING_STRATEGY.md` | Unit/Integration/UI tests + 4-scenario patent evaluation (S1–S4) |

---

## 🔗 Patent → Android Implementation Map

| Patent Component (LPU IDF 2026) | Android Implementation |
|---|---|
| Ethical AI Analysis Module | Retrofit → `/safety/evaluate` → ViewModel parses XGBoost result |
| XAI Explainability (SHAP + NL) | BottomSheet: `explanation` field rendered as plain-language card |
| Blockchain Verification Layer | "Verified ✓" chip on cards; `blockchain_ref` hash displayed |
| IoT Sensor Data (Kafka streams) | Real-time AQI, weather, crowd bars on destination detail screen |
| Decentralized Mesh Network | `OkHttp WebSocket` → `/ws/mesh/sync` for live push alerts |
| Dynamic Pricing + Hidden Costs | Pricing fairness card on hotel detail; hidden fee warnings |
| Responsible Tourism Engine | Eco score badge + ranked list with sustainability index |
| GPS Integration | `FusedLocationProviderClient` → lat/lon in every safety call |
| User Acknowledgement on Blockchain | RED flag → "I acknowledge the risk" dialog → logged on-chain |
| IF-THEN Ethical Rule | Ethical Score chip: HIGH / MODERATE / LOW on destination card |
=======
| File                              | Description                 |
| --------------------------------- | --------------------------- |
| README.md                         | Overview                    |
| 01_FUNCTIONAL_REQUIREMENTS.md     | Functional requirements     |
| 02_NON_FUNCTIONAL_REQUIREMENTS.md | Non-functional requirements |
| 03_SDLC_METHODOLOGY.md            | Development process         |
| 04_SYSTEM_DESIGN.md               | Architecture & APIs         |
| 05_UI_UX_SPECIFICATION.md         | UI/UX design                |
| 06_TESTING_STRATEGY.md            | Testing                     |
>>>>>>> b5c4e14414df6bd1468399d367bc647232311406

---

## 🚀 Dev Setup

<<<<<<< HEAD
```bash
# Prerequisites
Android Studio Hedgehog (2023.1.1) or later
Kotlin 1.9+ | Min SDK: 26 | Target SDK: 34 | Gradle 8.x

# local.properties (NEVER commit this file)
=======
### Prerequisites

* Android Studio Hedgehog (2023.1.1+)
* Kotlin 1.9+
* Min SDK: 26
* Target SDK: 34
* Gradle 8.x

---

### Configuration

```
>>>>>>> b5c4e14414df6bd1468399d367bc647232311406
BASE_URL=http://10.0.2.2:8000/api/v1/
WS_URL=ws://10.0.2.2:8000/ws/mesh/sync
MAPS_API_KEY=your_google_maps_key
```

---

<<<<<<< HEAD
*AICTE SIH 2025 · MIC-Student Innovation · LPU Patent 2026*
=======
## 🧪 Testing

* Unit Testing
* Integration Testing
* UI Testing
* Scenario Testing (S1–S4)

---

## 🏆 Achievements

AICTE SIH 2025
MIC Student Innovation
LPU Patent 2026

---

## 📜 License

Academic and research use only.
>>>>>>> b5c4e14414df6bd1468399d367bc647232311406
