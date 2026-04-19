# 🌏 SmartTour360 Android App

**Swadeshi for Atmanirbhar Bharat** 🇮🇳
**AICTE Problem Statement ID:** 25137

**Team:**

* Manas Maheshwari (12218829)
* K. Venkata Ram (12219197)

**Patent:**
LPU Invention Disclosure 2026 — *“Ethical AI Decision Systems for Recommending Safe and Sustainable Tourism Options”*

**Platform:**
Android Studio · Kotlin · MVVM · Clean Architecture

---

# 🧠 What Is This App

SmartTour360 is the **native Android client** of the TicketKaru/SmartTour360 platform — an **Ethical AI-powered travel companion** that provides:

* 🚦 Real-time **Green / Yellow / Red safety flags** per destination (XGBoost + SHAP)
* 🌿 **Eco sustainability scores** with SDG-aligned rankings
* 🔗 **Blockchain-backed audit trail** (Ethereum / Hyperledger Fabric)
* 📡 **Live IoT sensor streams** (Weather, AQI, Crowd density via Kafka)
* 💬 **Explainable AI (XAI)** — human-readable decision explanations
* 💸 **Dynamic pricing fairness** + hidden cost detection
* 📲 **Real-time push alerts** via WebSocket Gossip Mesh
* 🗺️ **Trip planner + smart booking flow** with blockchain acknowledgement

📡 Connects to FastAPI backend via **REST APIs + WebSockets**

---

# 🏗️ Full-Stack Architecture

```
ANDROID APP (Kotlin · MVVM · Retrofit · Room · Hilt)
                │
        HTTPS + WebSocket
                │
FASTAPI BACKEND (Python 3.11)
  ├─ XGBoost + SHAP (AI Engine)
  ├─ Ethical Guardrails (Patent Rules)
  ├─ Recommendation Engine
  ├─ Pricing Fairness Module
                │
 ┌──────────────┼───────────────┬──────────────┐
 │              │               │              │
PostgreSQL   InfluxDB     Blockchain      Kafka
(User Data)  (IoT Data)   (Ethereum)     (Streams)
```

---

# 📱 Feature → Backend Mapping

| Feature              | Data Source   | API Endpoint                   |
| -------------------- | ------------- | ------------------------------ |
| Safety Flags         | Kafka + APIs  | `/api/v1/safety/evaluate`      |
| XAI Explanation      | SHAP Engine   | Included in response           |
| Eco Score            | GreenCert API | `/api/v1/recommendations/rank` |
| Pricing Fairness     | FareTracker   | `/api/v1/pricing/analyze`      |
| Recommendations      | AI Engine     | `/api/v1/recommendations/rank` |
| Blockchain Verify    | Ethereum      | `/api/v1/safety/verify/{id}`   |
| Live Alerts          | WebSocket     | `ws://.../mesh/sync`           |
| Hotel Booking        | PostgreSQL    | `/api/v1/booking/hotels`       |
| User Acknowledgement | Blockchain    | `/api/v1/booking/acknowledge`  |

---

# 🌐 Real-Time Data Sources

### Kafka Topics

* `smarttour.sensor.weather` → Weather API
* `smarttour.sensor.environment` → AQI / UV
* `smarttour.sensor.crowd` → IoT crowd sensors
* `smarttour.alerts.safety` → Processed alerts

### External APIs

* OpenWeatherMap → Weather
* Travel Advisory APIs → Safety
* PredictHQ → Events & crowd risk
* FareTracker → Pricing
* GreenCert → Sustainability

### IoT Hardware

* Zigbee → Indoor sensors
* LoRaWAN → Long-range sensors
* AWS IoT Greengrass → Edge ML

---

# 🔗 Patent → Android Implementation

| Patent Component           | Android Implementation        |
| -------------------------- | ----------------------------- |
| Ethical AI Module          | Retrofit → `/safety/evaluate` |
| XAI Explainability         | BottomSheet explanation card  |
| Blockchain Verification    | “Verified ✓” chip             |
| IoT Data Streams           | Live AQI, weather, crowd UI   |
| Mesh Network               | WebSocket (`/ws/mesh/sync`)   |
| Pricing Fairness           | Cost transparency UI          |
| Sustainability Engine      | Eco score badge               |
| GPS Integration            | FusedLocationProvider         |
| Blockchain Acknowledgement | Risk confirmation dialog      |
| Ethical Rule Engine        | Ethical score chip            |

---

# 📁 Documentation Index

| File                              | Description             |
| --------------------------------- | ----------------------- |
| README.md                         | Project overview        |
| 01_FUNCTIONAL_REQUIREMENTS.md     | Features & requirements |
| 02_NON_FUNCTIONAL_REQUIREMENTS.md | Performance & security  |
| 03_SDLC_METHODOLOGY.md            | Agile Scrum process     |
| 04_SYSTEM_DESIGN.md               | Architecture & APIs     |
| 05_UI_UX_SPECIFICATION.md         | UI/UX design            |
| 06_TESTING_STRATEGY.md            | Testing approach        |

---

# 🚀 Development Setup

### Prerequisites

* Android Studio Hedgehog (2023.1.1+)
* Kotlin 1.9+
* Min SDK: 26
* Target SDK: 34
* Gradle 8.x

---

### 🔧 Configuration

Create `local.properties` 

```
BASE_URL=http://10.0.2.2:8000/api/v1/
WS_URL=ws://10.0.2.2:8000/ws/mesh/sync
MAPS_API_KEY=your_google_maps_key
```

---

# 🧪 Testing Strategy

* Unit Testing
* Integration Testing
* UI Testing
* Patent Scenario Testing (S1–S4)

---

# 🏆 Achievements

* 🎯 AICTE SIH 2025 Submission
* 💡 MIC Student Innovation Project
* 📜 LPU Patent Filing (2026)

---

# 📜 License

This project is for **academic and research purposes only**.
