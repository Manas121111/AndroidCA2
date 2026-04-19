# 🌏 SmartTour360 Android App

**Swadeshi for Atmanirbhar Bharat** 🇮🇳
**AICTE Problem Statement ID:** 25137

**Team:**
Manas Maheshwari (12218829)
K. Venkata Ram (12219197)

**Patent:**
LPU Invention Disclosure 2026 — *“Ethical AI Decision Systems for Recommending Safe and Sustainable Tourism Options”*

**Platform:**
Android Studio · Kotlin · MVVM · Clean Architecture

---

## 🧠 What Is This App

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

---

## 🏗️ Full-Stack Architecture

```
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
```

---

## 📱 Feature → Backend Map

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

---

## 📁 Documentation Index

| File                              | Description                 |
| --------------------------------- | --------------------------- |
| README.md                         | Overview                    |
| 01_FUNCTIONAL_REQUIREMENTS.md     | Functional requirements     |
| 02_NON_FUNCTIONAL_REQUIREMENTS.md | Non-functional requirements |
| 03_SDLC_METHODOLOGY.md            | Development process         |
| 04_SYSTEM_DESIGN.md               | Architecture & APIs         |
| 05_UI_UX_SPECIFICATION.md         | UI/UX design                |
| 06_TESTING_STRATEGY.md            | Testing                     |

---

## 🚀 Dev Setup

### Prerequisites

* Android Studio Hedgehog (2023.1.1+)
* Kotlin 1.9+
* Min SDK: 26
* Target SDK: 34
* Gradle 8.x

---

### Configuration

```
BASE_URL=http://10.0.2.2:8000/api/v1/
WS_URL=ws://10.0.2.2:8000/ws/mesh/sync
MAPS_API_KEY=your_google_maps_key
```

---

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
