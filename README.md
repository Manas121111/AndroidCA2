# 🌏 SmartTour360 Android App  

Swadeshi for Atmanirbhar Bharat | AICTE Problem Statement ID: 25137  

## 👨‍💻 Team  
Manas Maheshwari (12218829)  
K. Venkata Ram (12219197)  

## 📜 Patent  
LPU Invention Disclosure 2026 — "Ethical AI Decision Systems for Recommending Safe and Sustainable Tourism Options"  

## ⚙️ Platform  
Android Studio · Kotlin · MVVM · Clean Architecture  

---

## 🎯 Project Vision  

SmartTour360 aims to build a **trust-first tourism ecosystem** by combining:

- Ethical AI decision-making  
- Real-time environmental awareness  
- Transparent blockchain verification  
- Sustainable travel recommendations  

The goal is to ensure **safe, responsible, and informed travel experiences**.

---

## 🧠 What Is This App  

SmartTour360 is the native Android client of the TicketKaru/SmartTour360 platform — an Ethical AI-powered travel companion that gives Indian travelers:

- 🚦 Real-time Green / Yellow / Red safety flags per destination — computed by XGBoost + SHAP on the backend  
- 🌿 Eco sustainability scores with SDG-aligned rankings  
- 🔗 Blockchain-backed audit trail — every safety evaluation is hashed immutably on Ethereum / Hyperledger Fabric  
- 📡 Live IoT sensor streams via Apache Kafka (weather, AQI, crowd density from Zigbee/LoRaWAN sensors)  
- 💬 Explainable AI (XAI) — deterministic plain-language reason for every flag decision  
- 💸 Dynamic pricing fairness + hidden cost detection  
- 📲 Real-time push alerts via WebSocket Gossip Mesh when a saved destination's flag changes  
- 🗺️ Trip planner + smart booking flow with user acknowledgement on blockchain  

This app connects to the FastAPI backend (Python 3.11 · XGBoost · SHAP · PostgreSQL · InfluxDB · Kafka · Ethereum) via HTTPS REST and WebSocket.

---

## 🚀 Key Highlights  

- Ethical AI-based decision system (Patent-backed)  
- Integration of AI + IoT + Blockchain in one platform  
- Real-time safety intelligence using streaming data  
- Transparent and explainable recommendations  
- Scalable microservice-based backend architecture  

---

## 📸 Screenshots  

screenshots/
├── screen1.png  
├── screen2.png  

| Screen 1 | Screen 2 |
|----------|----------|
| ![Screen1](screenshots/screen1.png) | ![Screen2](screenshots/screen2.png) |

---

##  📱 ANDROID APP
Kotlin · MVVM · Clean Architecture · Retrofit · OkHttp · Room · Hilt · Jetpack
        │
        │  HTTPS (REST) + WebSocket (Real-time updates)
        ▼
⚡ FASTAPI BACKEND
FastAPI · Pydantic · XGBoost · SHAP · MLflow · Alembic
        │
        ├──────────────────────┬──────────────────────┬──────────────────────┬──────────────────────┐
        │                      │                      │                      │
        ▼                      ▼                      ▼                      ▼
🗄️ POSTGRESQL            ⏱️ INFLUXDB          ⛓️ BLOCKCHAIN          📡 APACHE KAFKA
Users & Bookings        IoT Time-Series      Ethereum / Fabric      Real-time Streams
Transactions            Weather / AQI        Smart Contracts        Event Topics
Incidents               Sensor Data          Audit Logs             Alerts Pipeline
        │                      │                      │                      │
        └──────────────────────┴──────────────────────┴──────────────────────┴──────────────────────┘
                                   │
                                   ▼
                    🌐 EXTERNAL APIS + IOT SOURCES
              Weather · AQI · Events · Pricing · Sensors
                     
---
## 📡 Real-Time Data Sources  

### Kafka Topics (IoT ingestion)
- smarttour.sensor.weather → OpenWeatherMap API  
- smarttour.sensor.environment → AQI / UV sensors  
- smarttour.sensor.crowd → Zigbee/LoRaWAN wearables  
- smarttour.alerts.safety → processed flag events  

### External APIs
- OpenWeatherMap API  
- US State Dept Travel Advisory  
- PredictHQ Events API  
- FareTracker Dynamic Pricing  
- GreenCert Eco Certification  

### IoT Hardware
- Zigbee mesh (short-range indoor sensors)  
- LoRaWAN (long-range outdoor coverage)  
- AWS IoT Greengrass v2 (edge ML inference)  

---

## 📱 Feature → Backend Map  

| Android Feature | Data Source | Backend Endpoint |
|----------------|-----------|------------------|
| Safety Flag (Green/Yellow/Red) | Kafka + Crime API + Weather API | POST /api/v1/safety/evaluate |
| XAI "Why this flag?" | SHAP + NL engine | Included in safety response |
| Eco Score Badge | GreenCert + Carbon model | POST /api/v1/recommendations/rank |
| Pricing Fairness + Hidden Costs | FareTracker API | POST /api/v1/pricing/analyze |
| Ranked Recommendations | Recommendation Engine | POST /api/v1/recommendations/rank |
| Blockchain Verified chip | Ethereum ledger | GET /api/v1/safety/verify/{id} |
| Real-Time Safety Alerts | WebSocket Gossip Mesh | wss://api/ws/mesh/sync |
| Hotel Listing / Booking | PostgreSQL | GET /api/v1/booking/hotels |
| User Acknowledgement | Blockchain UserAck contract | POST /api/v1/booking/acknowledge |
| GPS-based Safety Lookup | FusedLocationProvider | POST /api/v1/safety/evaluate |

---

## 📁 Documentation Index  

| File | Contents |
|------|---------|
| README.md | You are here |
| 01_FUNCTIONAL_REQUIREMENTS.md | System functionality |
| 02_NON_FUNCTIONAL_REQUIREMENTS.md | Performance & security |
| 03_SDLC_METHODOLOGY.md | Agile Scrum |
| 04_SYSTEM_DESIGN.md | Architecture & API |
| 05_UI_UX_SPECIFICATION.md | UI design |
| 06_TESTING_STRATEGY.md | Testing methods |

---

## 🔗 Patent → Android Implementation Map  

| Patent Component | Android Implementation |
|------------------|----------------------|
| Ethical AI Analysis Module | Retrofit → /safety/evaluate |
| XAI Explainability | BottomSheet explanation UI |
| Blockchain Verification | "Verified ✓" badge |
| IoT Sensor Data | Real-time indicators |
| Mesh Network | WebSocket alerts |
| Pricing Fairness | Cost transparency UI |
| Sustainability Engine | Eco score ranking |
| GPS Integration | Location-based safety |
| User Acknowledgement | Blockchain logging |
| Ethical Rule Engine | Ethical score chip |

---

## 🛠️ Technology Stack  

- **Frontend:** Android (Kotlin)  
- **Architecture:** MVVM + Clean Architecture  
- **Networking:** Retrofit, OkHttp  
- **Database:** Room  
- **Backend:** FastAPI  
- **AI/ML:** XGBoost, SHAP  
- **Streaming:** Apache Kafka  
- **Blockchain:** Ethereum, Hyperledger  

---

## 🚀 Dev Setup  

### Prerequisites  
Android Studio Hedgehog (2023.1.1) or later  
Kotlin 1.9+  
Min SDK: 26  
Target SDK: 34  
Gradle 8.x  

---

### local.properties (NEVER commit this file)
BASE_URL=http://10.0.2.2:8000/api/v1/
WS_URL=ws://10.0.2.2:8000/ws/mesh/sync
MAPS_API_KEY=your_google_maps_key


---

## 🧪 Testing  

- Unit Testing  
- Integration Testing  
- UI Testing  
- Scenario-based evaluation (S1–S4)  

---

## 🔮 Future Scope  

- Offline AI inference support  
- Advanced route optimization  
- Integration with more global APIs  
- Enhanced real-time analytics dashboard  

---

## 🏁 Conclusion  

SmartTour360 integrates Ethical AI, IoT, and Blockchain into a unified Android platform to deliver safe, transparent, and intelligent tourism experiences.

---

## 🏆 Credits  

AICTE SIH 2025 · MIC-Student Innovation · LPU Patent 2026  
