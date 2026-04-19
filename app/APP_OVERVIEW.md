# SmartTour360 App Overview

## What The App Is

SmartTour360 is an Android travel application focused on:

- destination discovery
- transport booking handoff
- hotel exploration
- AI-style recommendations
- high-level multi-stop trip planning

The app is built as a single-activity Android app with fragment-based navigation.

## Current Product Shape

The app currently contains these major areas:

1. Home
2. Booking Portal
3. Recommendations
4. Trips
5. Profile
6. Cart and Bookings flows

Navigation is handled through:

- a top app bar
- a left drawer
- fragment transactions inside `MainActivity`

The old bottom navigation has been removed.

## Main Architecture

## Activity Shell

The app uses:

- `MainActivity`
- one fragment container
- drawer navigation
- top toolbar
- edge-to-edge layout

Key file:

- [MainActivity.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\MainActivity.kt)

Main responsibilities:

- hosts fragments
- controls drawer and toolbar
- switches between top-level tabs
- opens detail fragments
- applies system bar insets

## Navigation Model

Top-level tabs are represented by `MainTab`:

- `HOME`
- `SEARCH`
- `RECOMMENDATIONS`
- `TRIPS`
- `PROFILE`

Navigation interface:

- [AppNavigator.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\AppNavigator.kt)

## Shared App State

The app uses a centralized in-memory state store:

- [AppStateStore.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\state\AppStateStore.kt)

It currently manages:

- selected destination
- selected hotel
- trip planner entries
- cart entries
- bookings
- latest order confirmation
- user preferences

This store is the main glue between screens.

## Main Modules

## 1. Home

Key files:

- [HomeFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\home\HomeFragment.kt)
- [fragment_home.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\fragment_home.xml)

What it does:

- shows featured destinations
- shows featured hotels
- opens recommendations
- allows quick-add of destinations to trip planner
- allows quick-add of hotels as stays to trip planner
- opens destination detail and hotel detail

Current behavior:

- destination cards can open details
- destination cards can be added to planner directly
- hotel cards can open hotel detail
- hotel cards can be added to planner directly

## 2. Booking Portal

This replaces the old search-only area.

Key files:

- [SearchFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\search\SearchFragment.kt)
- [fragment_booking_portal.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\fragment_booking_portal.xml)

The Booking Portal contains 3 modes:

- Trains
- Flights
- Bus

Each mode is rendered inside a `ViewPager2` with a `TabLayout`.

### Train Booking

Key files:

- [TrainBookingFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\search\TrainBookingFragment.kt)
- [fragment_train_booking.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\fragment_train_booking.xml)
- [TrainDetailFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\train\TrainDetailFragment.kt)
- [TrainRepository.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\data\TrainRepository.kt)
- [TrainApi.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\data\remote\TrainApi.kt)
- [TrainAdapters.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\train\TrainAdapters.kt)

Implemented train features:

- station autocomplete
- typed station auto-resolution
- station swap
- date selection
- live train search
- train list scrolling
- schedule detail screen
- class picker
- IRCTC handoff flow
- booking summary copy/handoff behavior

Current train data source:

- `eRail`

Important note:

- search and station lookup are working off the free `eRail` flow
- schedule is fetched through free route-page parsing
- live running status is not fully integrated as a reliable free live source yet

### Flight Booking

Key files:

- [FlightBookingFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\search\FlightBookingFragment.kt)
- [fragment_flight_booking.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\fragment_flight_booking.xml)

Current state:

- dedicated flight UI exists
- search form exists
- results UI exists
- external booking handoff pattern exists

Current limitation:

- does not yet use a real live flight API

### Bus Booking

Key files:

- [BusBookingFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\search\BusBookingFragment.kt)
- [fragment_bus_booking.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\fragment_bus_booking.xml)

Current state:

- dedicated bus UI exists
- search form exists
- results UI exists
- external booking handoff pattern exists

Current limitation:

- does not yet use a real live bus API

## 3. Recommendations

Key files:

- [RecommendationsFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\recommendations\RecommendationsFragment.kt)
- [RecommendationAdapter.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\recommendations\RecommendationAdapter.kt)
- [fragment_recommendations.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\fragment_recommendations.xml)
- [TravelRepository.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\data\TravelRepository.kt)

What is implemented:

- interactive recommendations screen
- search field for destination filtering
- refresh button
- refreshed recommendation mix on each search or refresh
- recommendation cards with richer metadata
- India destination catalog list
- quick-add destinations into planner from the recommendations screen

Recommendation logic currently uses:

- destination catalog
- live weather-backed scoring where available
- ranking/rotation logic to create a fresh recommendation mix

Important note:

- this is AI-style recommendation logic
- it is not yet backed by a real remote LLM call

## 4. Trips

Key files:

- [TripPlannerFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\trip\TripPlannerFragment.kt)
- [TripEntryAdapter.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\trip\TripEntryAdapter.kt)
- [fragment_trip_planner.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\fragment_trip_planner.xml)
- [item_trip_entry.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\item_trip_entry.xml)

What is implemented:

- planner summary card
- route visualization as text path
- stop count and stay count
- duration estimate
- transport estimate
- stay estimate
- total estimate
- saved route item list
- add-places shortcut button
- clear planner button

Trip planner inputs currently come from:

- destinations added from home
- hotels added from home
- destinations added from detail flows
- destinations added from recommendations

Planner behavior:

- destinations become route stops
- hotels become stay/base entries
- planner computes a high-level trip summary

Current limitation:

- no drag-and-drop reorder
- no per-item remove action yet
- no day-by-day itinerary breakdown yet

## 5. Destination Detail

Key files:

- [DestinationDetailFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\destination\DestinationDetailFragment.kt)
- [DestinationDetailViewModel.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\destination\DestinationDetailViewModel.kt)

What is implemented:

- destination detail content
- weather/forecast section
- risk/eco style detail
- stay preview section
- add-to-trip action
- explanation and blockchain themed bottom sheets

## 6. Hotels

Key files:

- [HotelListFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\hotel\HotelListFragment.kt)
- [HotelDetailFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\hotel\HotelDetailFragment.kt)

What is implemented:

- hotel list
- hotel detail
- add hotel to cart
- add hotel to planner from listing flows

## 7. Cart / Bookings / Order Flow

Key files:

- [CartFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\booking\CartFragment.kt)
- [BookingsFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\booking\BookingsFragment.kt)
- [OrderConfirmationFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\booking\OrderConfirmationFragment.kt)

What is implemented:

- cart storage
- totals calculation
- order confirmation
- booking history records

## 8. Profile

Key file:

- [ProfileFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\profile\ProfileFragment.kt)

What is implemented:

- user preference storage through shared preferences
- profile-level travel preference state

## Data Layer

Main data files:

- [TravelRepository.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\data\TravelRepository.kt)
- [TrainRepository.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\data\TrainRepository.kt)
- [ApiClient.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\data\remote\ApiClient.kt)

## Current External Data Sources

### Destination/Weather

Used for:

- geocoding
- weather-backed destination scoring
- forecasts

### eRail

Used for:

- train station lookup
- train search
- schedule route parsing

### Ticketmaster

Used conditionally for:

- optional live event signals in recommendations

Important note:

- if the Ticketmaster key is missing, the app still works
- recommendation logic falls back gracefully

## UI Direction

Recent UI changes include:

- top app bar and drawer shell
- edge-to-edge layout
- Booking Portal replacing old search area
- reduced oversized portal header
- improved train screen spacing
- fixed train results scrolling
- upgraded recommendations from plain text to card-based layout
- upgraded trips from long text blocks to clearer summary blocks

## Current Known Limitations

1. Flights are not yet backed by a live flight provider
2. Bus booking is not yet backed by a live bus provider
3. Train live running status is not fully reliable with a free source
4. Recommendations are heuristic/AI-style, not true LLM-generated recommendations
5. Trip planner does not yet support reordering or removing individual entries
6. Trip planner is still high-level and not yet a detailed day planner

## Important Development Decisions Made So Far

1. Removed bottom navigation and moved to toolbar + drawer navigation
2. Converted Search into a Booking Portal with multi-mode booking
3. Reverted away from unstable keyed train API integration and returned to `eRail`
4. Added planner state that combines destinations and hotels
5. Improved recommendations to be refreshable and interactive

## Suggested Next Steps

Recommended next improvements:

1. Add remove/reorder controls inside Trip Planner
2. Add day-by-day itinerary generation
3. Add real live flight provider integration
4. Add real live bus provider integration
5. Improve train live running status with a stable source
6. Add persistent storage for trip planner entries
7. Add better detail handoff between recommendations and destination detail

## Build Status

As of the current state described in this file:

- the app builds successfully with `:app:assembleDebug`

## Quick File Map

Core app shell:

- [MainActivity.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\MainActivity.kt)
- [activity_main.xml](C:\Users\Intel\Downloads\android ca2\app\src\main\res\layout\activity_main.xml)

State:

- [AppStateStore.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\state\AppStateStore.kt)

Home:

- [HomeFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\home\HomeFragment.kt)

Booking Portal:

- [SearchFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\search\SearchFragment.kt)
- [TrainBookingFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\search\TrainBookingFragment.kt)
- [FlightBookingFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\search\FlightBookingFragment.kt)
- [BusBookingFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\search\BusBookingFragment.kt)

Recommendations:

- [RecommendationsFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\recommendations\RecommendationsFragment.kt)

Trips:

- [TripPlannerFragment.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\ui\trip\TripPlannerFragment.kt)

Data:

- [TravelRepository.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\data\TravelRepository.kt)
- [TrainRepository.kt](C:\Users\Intel\Downloads\android ca2\app\src\main\java\com\smarttour360\app\data\TrainRepository.kt)
