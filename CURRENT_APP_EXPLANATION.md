# SmartTour360 Current App Explanation

## What The App Is

SmartTour360 is a single-activity Android travel app built in Kotlin. Right now it works as a travel discovery, planning, booking-handoff, and assistant app centered on Indian destinations.

The current app is not a full production travel platform with live integrations for every feature. Some areas are powered by real APIs, some are heuristic, and some are polished UI flows waiting for stronger backend providers.

## Tech Stack

- Android Views with fragments
- Single `MainActivity` shell
- Kotlin + coroutines
- Retrofit + OkHttp for APIs
- Room for local user profile storage
- Coil for image loading and caching
- Material Components UI
- In-memory app state via `AppStateStore`

Build configuration in `app/build.gradle.kts` sets:

- `minSdk = 26`
- `targetSdk = 34`
- `compileSdk = 34`
- Java/Kotlin target `17`

## App Entry And Navigation

The app starts in `MainActivity`, which:

- enables edge-to-edge layout
- initializes `AppStateStore`
- hosts a single fragment container
- controls the top toolbar and left drawer
- routes between top-level sections and child screens

Top-level sections are:

1. Home
2. Booking Portal
3. Recommendations
4. Trips
5. Profile

Additional child screens are opened from those sections:

- destination detail
- hotel list and hotel detail
- cart
- bookings
- order confirmation
- chatbot
- train detail

The toolbar chat action opens the travel assistant from anywhere the shell is visible.

## Permissions And Device Features

The manifest currently requests:

- `INTERNET`
- `RECORD_AUDIO`
- `POST_NOTIFICATIONS`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`

These power the app's current live/device features:

- location-aware safety banner on Home
- voice input and text-to-speech in the chatbot
- shake-to-refresh on Home and Destination Detail
- barometer-based environmental signal on Destination Detail
- local notifications for safety spotlight checks

## Current App Flow

### 1. Splash, Login, And Onboarding

The startup flow is simple:

- `SplashFragment` shows a short splash screen
- `LoginFragment` accepts any identity string and starts a user session
- `OnboardingFragment` collects profile data and travel preferences

The login flow is currently lightweight. There is no real authentication backend. The entered identity is used to load or create a local user profile.

Onboarding stores:

- name
- email
- mobile
- country
- home city
- budget preference
- travel styles
- preferred transport
- eco priority toggle

These preferences directly affect recommendation ranking and the profile screen.

### 2. Home

`HomeFragment` is the app's live dashboard.

It currently shows:

- greeting based on the saved user profile
- trip snapshot if the user already added stops or stays
- current-location safety banner
- top destinations
- eco picks
- safety spotlight list
- featured hotels

Important Home behavior:

- pulls live destination data from the repository
- caches and displays featured destinations
- auto-refreshes every 90 seconds while visible
- refreshes when the user shakes the device
- updates app-wide live destination state
- prefetches destination images
- checks safety notifications on resume

Home is the main place where live destination state is refreshed and distributed to the rest of the app.

### 3. Booking Portal

`SearchFragment` is now the Booking Portal. It contains a `ViewPager2` + `TabLayout` setup with:

- Train booking
- Flight booking
- Bus booking

#### Train Booking

This is the strongest real transport integration in the app today.

`TrainBookingFragment` and `TrainRepository` currently support:

- station autocomplete
- resolving typed station names/codes
- swapping origin and destination
- picking travel date
- fetching trains between stations from eRail
- opening a train detail screen
- viewing parsed route/schedule data
- IRCTC handoff-style booking flow

Current train limitations:

- live running status is not truly integrated
- status text clearly falls back to a placeholder
- route parsing depends on HTML parsing from the free eRail flow

#### Flight Booking

`FlightBookingFragment` exists and the user flow is present, but it is still mostly a structured UI and handoff experience. It does not yet use a real live flight provider.

#### Bus Booking

`BusBookingFragment` is in a similar state to flights. The UI and flow exist, but a real live bus provider is not integrated yet.

### 4. Recommendations

`RecommendationsFragment` is the app's ranking and exploration screen.

It currently:

- accepts a search query
- refreshes a recommendation mix
- shows personalized recommendation cards
- shows a broader Indian destination catalog
- allows adding destinations directly to the trip planner
- stores the latest recommendation reasoning in app state for chatbot grounding

The recommendation engine in `TravelRepository` combines:

- sample destination seeds
- Open-Meteo geocoding + forecast data
- optional Ticketmaster event signals
- user preferences from onboarding/profile
- ranking heuristics for eco score, safety flag, rating, budget fit, style fit, and transport fit

This produces AI-style personalized recommendations, but it is not yet a full recommender system backed by a proprietary ML service.

### 5. Destination Detail

`DestinationDetailFragment` and `DestinationDetailViewModel` turn the currently selected destination into a richer screen.

The screen currently includes:

- destination hero image
- safety flag and score
- eco score
- carbon estimate
- short weather summary
- 3-day forecast
- eco stay suggestions
- XAI-style explanation sheet
- blockchain verification sheet
- ask-AI shortcut
- add-to-trip action

Special device-enhanced behavior:

- reads barometer data through `BarometerReader`
- adjusts environmental contribution in the displayed safety logic
- refreshes safety data on shake

Important implementation note:

The blockchain and XAI parts are currently presentation-level, grounded in app state and static/generated values. They communicate the intended product model, but they are not connected to a real blockchain verification backend in this repo.

### 6. Hotels

The hotel module includes:

- `HotelListFragment`
- `HotelDetailFragment`

Current hotel behavior:

- display curated/sample hotel content
- open hotel detail
- add selected hotel to trip planner
- add selected hotel to cart

The cart flow is the currently implemented checkout-like experience for stays.

### 7. Trips

`TripPlannerFragment` is the current itinerary workspace.

Trip entries come from:

- destinations added from Home
- destinations added from Recommendations
- the currently selected destination
- hotels added from Home/hotel flows

The planner currently provides:

- saved route/stay list
- per-entry remove action
- computed summary card
- route line
- trip duration estimate
- transport budget estimate
- stay budget estimate
- total estimate
- share trip action
- generate day plan action

The "Generate Day Plan" button calls the chatbot repository in itinerary mode and shows the result in a bottom sheet.

Trip planning is still simplified. It is not a calendar-grade itinerary engine.

### 8. Cart, Orders, And Bookings

Checkout state is managed centrally by `AppStateStore`.

Current booking flow:

- hotel added to cart
- cart totals computed with tax
- place order converts cart entries into booking records
- order confirmation screen shows a generated booking summary
- bookings screen shows the stored booking history

Generated booking records currently include:

- booking ID
- hotel name
- stay info
- total cost
- safety flag at booking
- synthetic blockchain reference
- eco impact summary
- status
- acknowledgement flag

This is a local app flow, not a real payment gateway or remote booking system.

### 9. Chatbot / Travel Assistant

The travel assistant is implemented through:

- `ChatbotFragment`
- `ChatbotViewModel`
- `ContextBuilder`
- `ChatbotRepository`
- `VoiceManager`

The assistant is grounded using current app state, including:

- selected destination
- current safety flag
- explanation text
- eco score
- selected hotel
- saved trip stops
- trip summary
- user profile
- live safety snapshot
- latest recommendation rankings

Behavior today:

- chat UI with user/bot messages
- quick replies
- voice input through microphone permission
- text-to-speech for bot replies
- optional LLM call to Groq when `GROQ_API_KEY` is present
- deterministic fallback replies when the key is absent
- itinerary-specific mode for day-by-day trip output

This means the assistant works even without an API key, but the fallback mode is rule-based rather than truly conversational AI.

### 10. Profile

`ProfileFragment` reads the current user preferences from `AppStateStore` and displays:

- avatar initials
- name
- email
- mobile
- country
- summarized preferences

It also links to the bookings screen.

## State Management

The central glue in the app is `AppStateStore`.

It currently stores:

- selected destination
- selected hotel
- trip entries
- cart entries
- bookings
- latest order confirmation
- user preferences
- active user key
- live destination snapshots
- assistant recommendation snapshots
- chat history
- barometer-derived environmental values

This store is in-memory for most runtime data. It is what lets one screen affect another without a heavier architecture layer.

### What Persists

Local persistence is limited but real.

Room database:

- `SmartTourDatabase`
- `UserProfileEntity`
- `UserProfileDao`

Persisted now:

- user profile/preferences keyed by normalized identity

Not persisted yet:

- trip planner entries
- cart
- bookings
- chat history
- cached live destination state

## Data Layer And APIs

### `TravelRepository`

This is the main destination/recommendation data source.

It currently handles:

- featured destination loading
- destination search
- live Indian destination catalog refresh
- forecast loading
- optional live events lookup
- personalized recommendation generation
- Wikimedia/Wikipedia image lookup and caching

### `TrainRepository`

This repository handles:

- station catalog loading from eRail script data
- station search and resolution
- trains-between-stations lookup
- route/schedule scraping/parsing
- placeholder live-status response

### `ApiClient`

Retrofit clients exist for:

- Open-Meteo geocoding
- Open-Meteo forecast
- Ticketmaster
- Wikimedia Commons
- Wikipedia
- Groq
- eRail

Configured optional keys:

- `TICKETMASTER_API_KEY`
- `GROQ_API_KEY`

If these keys are absent, the app still runs with graceful fallback behavior.

## Image Loading And Performance Choices

`SmartTourApp` configures a custom Coil image loader with:

- memory cache
- disk cache
- crossfade
- disabled cache-header respect

The app also prefetches destination images after live catalog/recommendation refreshes to make the UI feel more responsive.

## What Is Real Vs Simulated Right Now

### Real Or Semi-Real

- Android navigation and UI flows
- Room-based user profile persistence
- Open-Meteo geocoding and forecast calls
- Wikimedia/Wikipedia image fetching
- eRail train search and schedule parsing
- location-based city matching
- microphone, TTS, shake, and barometer usage

### Simulated / Heuristic / Partial

- authentication
- hotel inventory and hotel booking backend
- flight provider integration
- bus provider integration
- train live running status
- blockchain verification backend
- production-grade XAI engine
- payment flow
- long-term persistence for trip/cart/booking/chat

## Current Strengths Of The App

- Clear single-activity app structure
- Strong cross-screen state sharing
- Several polished user flows already connected together
- Live destination refresh with meaningful fallback logic
- A usable train-search feature with real external data
- Chatbot that degrades gracefully without an API key
- Good demonstration of the intended SmartTour360 product vision

## Main Current Gaps

1. No real authentication backend
2. Flights and buses are not live-provider backed
3. Hotel flow is local/demo-oriented
4. Trip planner is still lightweight and heuristic
5. Bookings are local records, not remote transactions
6. Several "AI / blockchain / explainability" parts are modeled in the UI but not fully backed by production services in this repo

## Practical Summary

As of now, SmartTour360 is best understood as a well-connected Android prototype / demo app with a few real data integrations and a broad product surface area. The app already demonstrates discovery, recommendation, trip planning, train booking flow, hotel/cart flow, profile persistence, and a voice-enabled travel assistant. The next step toward a production app would be replacing the remaining heuristic/demo modules with real backend services and persisting more user data beyond the profile.
