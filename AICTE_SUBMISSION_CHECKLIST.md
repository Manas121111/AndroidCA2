# SmartTour360 Submission Checklist

This checklist groups the current app by delivery quality:

| Feature Area | Production ready | Demo ready | Gap / future scope |
|---|---|---|---|
| Home | Cached first paint, manual refresh, live destination feed, live location banner, dynamic safety spotlight, rotating recommendations | Some destination/image/API quality still depends on third-party providers | Stronger offline fallback and provider redundancy |
| Booking Portal - Train | Search flow works, chatbot can route users into train search, pending train search handoff works | Real train content quality depends on external provider responses and current integration depth | Full booking completion and provider-grade fare/seat accuracy |
| Booking Portal - Flight | Basic screen and portal navigation exist | Demoable as a tab/flow shell | Real flight inventory, live pricing, booking backend |
| Booking Portal - Bus | Basic screen and portal navigation exist | Demoable as a tab/flow shell | Real bus inventory, operator integration, live booking |
| Recommendations | Live ranking, safety-aware and eco-aware recommendation logic, chatbot can open recommendations | Recommendation quality is only as good as current live feed and scoring inputs | More personalization, A/B scoring, stronger destination diversity controls |
| Trip Planner | Add from destination/hotel, remove item, clear trip, share plan, AI day-plan generation | Costs and durations are planner estimates, not supplier-confirmed itineraries | Reordering, collaborative sharing, export to calendar/maps |
| Destination Detail | Safety explanation, sensor-aware refresh, forecast, image loading, AI entry point, XAI / verification style explanation | Some detail values still blend live signals with heuristic presentation | Richer live local data sources and deeper explainability UI |
| Hotels | Live nearby hotel/place sourcing instead of pure mock list, hotel detail, add to cart, AI entry point | Prices are estimated, not supplier-live hotel rates | Real hotel availability, booking partner integration, cancellation policies |
| Cart / Bookings | Cart, totals, order confirmation, booking records, booking history screens | Booking lifecycle is app-simulated rather than provider-backed | Real payment, booking confirmation from external providers, status sync |
| Chatbot - Text | Groq chat integration, domain guardrails, app-context grounding, action execution, location-aware replies, fast deterministic answers for exact app state | Final answer quality still depends on prompt/retrieval quality and external Groq/network availability | Better structured tool use, stronger multi-step transactional execution |
| Chatbot - Voice | Mic input, speech recognition, TTS output, mic-state bug fix | Device-specific voice quality can vary | More robust noise handling, language selection, streaming voice UX |
| Chatbot - RAG | Local knowledge assets, Room-backed knowledge base, Groq embeddings endpoint, retriever, per-user memory/profile learning | Retrieval is hybrid and still evolving, not yet a full production search stack | Better chunk curation, evaluation set, retrieval analytics |
| Chatbot - Brain Layer | Intent classification, context injection, prompt engineering, response parsing, episodic logging, action parsing/execution | Some actions still rely on current app context or lightweight parsing conventions | Broader action vocabulary with safer structured arguments |
| Sensors - GPS | Live location banner, city matching, chatbot location use | Accuracy depends on permission, device state, and location availability | Geofenced alerts, better background refresh policy |
| Sensors - Accelerometer | Shake-to-refresh style interaction exists | Best suited for demo interaction rather than essential core UX | Optionalization and more polished motion UX |
| Sensors - Barometer | Barometer affects destination detail safety presentation when available | Sensor availability varies by device | Broader sensor fusion and calibration strategy |
| Profile | User profile, preference storage, transport/budget/trip style grounding for chatbot and recommendations | Preferences are strong enough for demo and personalization | Deeper account sync, cloud backup, cross-device persistence |
| Onboarding | Onboarding and login flow exist and lead into app state | Authentication model is lightweight for demo scope | Production identity, recovery, secure backend auth |
| Images | Real destination images now load through improved Wikimedia pathing and filtering | A few ambiguous destinations can still produce imperfect matches from public sources | Curated destination image dataset or paid/stable image provider |
| Performance / caching | Home no longer refreshes constantly, cache TTLs are in place, image cache capped, chatbot latency reduced | Third-party API latency can still surface on slower networks | More incremental refresh, background warmup, offline-first catalogs |

## Summary

### Production ready
- Home core flow
- Recommendations core flow
- Trip Planner core flow
- Chatbot text/voice foundation
- Profile and onboarding core state flow
- Performance and caching baseline

### Demo ready
- Train portal experience
- Destination detail intelligence layer
- Hotels and bookings experience
- Sensor-driven enhancements
- Real-time image enrichment
- RAG and personalized chatbot memory

### Gap / future scope
- Full provider-backed bookings for train/flight/bus/hotels
- Real payment and booking confirmations
- Cloud sync / backend account system
- Hardening the RAG evaluation and structured tool/action stack
- Curated image and richer real-time travel intelligence providers
