🌏 SmartTour360 Android App
Swadeshi for Atmanirbhar Bharat 🇮🇳
AICTE Problem Statement ID: 25137

👨‍💻 Team

Manas Maheshwari (12218829)

K. Venkata Ram (12219197)

📜 Patent
Ethical AI Decision Systems for Recommending Safe and Sustainable Tourism Options

⚙️ Platform
Android Studio · Kotlin · MVVM · Clean Architecture

🧠 What Is This App
SmartTour360 is the native Android client of the TicketKaru/SmartTour360 platform — an Ethical AI-powered travel companion that provides:

🚦 Real-time Green / Yellow / Red safety flags (XGBoost + SHAP)

🌿 Eco sustainability scores (SDG-aligned)

🔗 Blockchain-backed audit trail (Ethereum / Hyperledger Fabric)

📡 Live IoT sensor streams (weather, AQI, crowd density via Zigbee/LoRaWAN)

💬 Explainable AI (XAI) with plain-language reasoning

💸 Dynamic pricing fairness + hidden cost detection

📲 Real-time alerts via WebSocket Gossip Mesh

🗺️ Trip planner + smart booking with blockchain acknowledgement

The app connects to a FastAPI backend (Python 3.11 · XGBoost · SHAP · PostgreSQL · InfluxDB · Kafka · Ethereum) via HTTPS REST and WebSocket.

📸 Screenshots
Create a folder named screenshots:

screenshots/
├── screen1.png
├── screen2.png
App Preview
Screen 1	Screen 2
🏗️ Full-Stack Architecture
ANDROID APP
(Kotlin · MVVM · Retrofit · OkHttp WS · Room · Hilt · Jetpack)
        │
        │ HTTPS REST + WebSocket
        ▼
FASTAPI BACKEND (Python 3.11)
(XGBoost · SHAP · Pydantic · Alembic · MLflow)
        │
 ┌──────────────┬──────────────┬──────────────┐
 ▼              ▼              ▼              ▼
PostgreSQL    InfluxDB     Blockchain       Kafka
Users         IoT Data     Ethereum         Topics
Bookings      Sensors      Hyperledger      Streams
Incidents     Time-series  IPFS             Real-time
📡 Real-Time Data Sources
Kafka Topics (IoT Ingestion)
smarttour.sensor.weather → OpenWeatherMap API

smarttour.sensor.environment → AQI / UV sensors

smarttour.sensor.crowd → Zigbee/LoRaWAN wearables

smarttour.alerts.safety → processed safety events

External APIs
OpenWeatherMap API

US State Dept Travel Advisory

PredictHQ Events API

FareTracker Dynamic Pricing

GreenCert Eco Certification

IoT Hardware
Zigbee mesh (short-range sensors)

LoRaWAN (long-range environments)

AWS IoT Greengrass v2 (edge inference)

📱 Feature → Backend Mapping
Android Feature	Data Source	Backend Endpoint
Safety Flag (Green/Yellow/Red)	Kafka + Crime API + Weather	POST /api/v1/safety/evaluate
XAI Explanation	SHAP + NLP	Included in response
Eco Score Badge	GreenCert + Carbon Model	POST /api/v1/recommendations/rank
Pricing Fairness	FareTracker API	POST /api/v1/pricing/analyze
Recommendations	ML Engine	POST /api/v1/recommendations/rank
Blockchain Verification	Ethereum	GET /api/v1/safety/verify/{id}
Real-time Alerts	WebSocket Mesh	wss://api/ws/mesh/sync
Hotel Booking	PostgreSQL	GET /api/v1/booking/hotels
User Acknowledgement	Blockchain Contract	POST /api/v1/booking/acknowledge
GPS Safety Lookup	Device Location	POST /api/v1/safety/evaluate
📁 Documentation Index
File	Description
README.md	Project overview
01_FUNCTIONAL_REQUIREMENTS.md	Features & system capabilities
02_NON_FUNCTIONAL_REQUIREMENTS.md	Performance & security
03_SDLC_METHODOLOGY.md	Agile Scrum process
04_SYSTEM_DESIGN.md	Architecture & API design
05_UI_UX_SPECIFICATION.md	UI components & screens
06_TESTING_STRATEGY.md	Testing methods & scenarios
🔗 Patent → Android Implementation
Patent Component	Android Implementation
Ethical AI Analysis	Retrofit → /safety/evaluate
XAI Explainability	BottomSheet explanation UI
Blockchain Verification	“Verified ✓” badge + hash
IoT Sensor Data	AQI, weather, crowd indicators
Mesh Network	WebSocket /ws/mesh/sync
Pricing Fairness	Cost analysis UI
Sustainability Engine	Eco score ranking
GPS Integration	FusedLocationProvider
User Acknowledgement	Blockchain logging
Ethical Rule Engine	Ethical score chip
🚀 Development Setup
Prerequisites
Android Studio Hedgehog (2023.1.1+)

Kotlin 1.9+

Min SDK: 26

Target SDK: 34

Gradle 8.x

Configuration
Create local.properties:

BASE_URL=http://10.0.2.2:8000/api/v1/
WS_URL=ws://10.0.2.2:8000/ws/mesh/sync
MAPS_API_KEY=your_google_maps_key
🧪 Testing
Unit Testing

Integration Testing

UI Testing

Scenario-based evaluation (S1–S4)

🏁 Conclusion
SmartTour360 integrates Ethical AI, IoT, and Blockchain into a unified Android application to deliver safe, transparent, and intelligent tourism experiences.

🏆 Credits
AICTE SIH 2025 · MIC Student Innovation · LPU Patent 2026

