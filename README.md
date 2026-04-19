🌏 SmartTour360 Android App
Swadeshi for Atmanirbhar Bharat 🇮🇳
AICTE Problem Statement ID: 25137

👨‍💻 Team

Manas Maheshwari (12218829)

K. Venkata Ram (12219197)

📜 Patent
Ethical AI Decision Systems for Recommending Safe and Sustainable Tourism Options

⚙️ Tech Stack
Android Studio · Kotlin · MVVM · Clean Architecture

📱 About the App
SmartTour360 is an Ethical AI-powered travel companion that helps users make safe, transparent, and sustainable travel decisions.

The app connects to a FastAPI backend using REST APIs and WebSockets.

✨ Features
🚦 Real-time Safety Flags (Green / Yellow / Red)

🌿 Eco Sustainability Score (SDG-aligned)

🔗 Blockchain-based Verification

📡 Live IoT Data (Weather, AQI, Crowd Density)

💬 Explainable AI (XAI reasoning)

💸 Pricing Fairness & Hidden Cost Detection

📲 Real-time Alerts via WebSockets

🗺️ Trip Planning & Booking

📸 Screenshots
Create a folder named screenshots:

screenshots/
│── screen1.png
│── screen2.png
App Preview
Screen 1	Screen 2
🏗️ Architecture Overview
ANDROID APP (Kotlin · MVVM · Retrofit · Room · Hilt)
            │
            │ REST API + WebSocket
            ▼
FASTAPI BACKEND (Python · XGBoost · SHAP)
            │
 ┌──────────┼──────────┬──────────────┐
 ▼          ▼          ▼              ▼
PostgreSQL  InfluxDB   Blockchain     Kafka
Users       IoT Data   Ethereum       Streams
Bookings    Sensors    Hyperledger    Real-time
📡 Real-Time Data Sources
OpenWeatherMap API

AQI / Environmental Sensors

PredictHQ Events API

FareTracker Pricing API

GreenCert Eco Data

IoT: Zigbee · LoRaWAN · AWS IoT Greengrass

🔗 Feature → Backend Mapping
Feature	Endpoint
Safety Evaluation	/api/v1/safety/evaluate
Eco Ranking	/api/v1/recommendations/rank
Pricing Analysis	/api/v1/pricing/analyze
Blockchain Verification	/api/v1/safety/verify/{id}
Real-time Alerts	wss://api/ws/mesh/sync
Booking	/api/v1/booking/hotels
🔬 Patent → Implementation
Component	Implementation
Ethical AI	Safety API integration
Explainable AI	UI explanation cards
Blockchain	Verified badge + hash
IoT	Real-time indicators
Mesh Network	WebSocket alerts
Pricing	Cost transparency UI
Sustainability	Eco scoring
GPS	Location-based safety
User Acknowledgement	Risk confirmation
📁 Documentation
01_FUNCTIONAL_REQUIREMENTS.md

02_NON_FUNCTIONAL_REQUIREMENTS.md

03_SDLC_METHODOLOGY.md

04_SYSTEM_DESIGN.md

05_UI_UX_SPECIFICATION.md

06_TESTING_STRATEGY.md

🚀 Setup
Requirements
Android Studio Hedgehog or later

Kotlin 1.9+

Min SDK: 26

Target SDK: 34

Configuration
Create local.properties:

BASE_URL=http://10.0.2.2:8000/api/v1/
WS_URL=ws://10.0.2.2:8000/ws/mesh/sync
MAPS_API_KEY=your_google_maps_key
🏁 Conclusion
SmartTour360 combines Ethical AI, IoT, and Blockchain to deliver a smart, safe, and sustainable tourism platform.

🏆 Credits
AICTE SIH 2025 · MIC Student Innovation · LPU Patent 2026

