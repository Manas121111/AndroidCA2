# 🎨 UI/UX Specification
## SmartTour360 Android App
**Document ID:** STS-UX-005 | **Version:** 2.0 | **Patent Aligned:** LPU IDF 2026

---

## Design System

### Color Palette

| Token | Hex | Usage |
|---|---|---|
| `primary` | `#006D5B` | Buttons, FABs, selected tabs, main actions |
| `primary_variant` | `#004D40` | Toolbar, status bar |
| `secondary` | `#FF8F00` | Accents, saffron badges |
| `background` | `#F5F5F5` | Screen backgrounds |
| `surface` | `#FFFFFF` | Cards, sheets |
| `flag_green` | `#2E7D32` | GREEN safety flag |
| `flag_yellow` | `#F57F17` | YELLOW safety flag |
| `flag_red` | `#C62828` | RED safety flag |
| `eco_badge` | `#388E3C` | Eco-certified badge |
| `blockchain_accent` | `#1565C0` | Blockchain verified chip |
| `ethical_high` | `#2E7D32` | Ethical Score HIGH |
| `ethical_moderate` | `#F57F17` | Ethical Score MODERATE |
| `ethical_low` | `#C62828` | Ethical Score LOW |
| `eco_risk_minimal` | `#388E3C` | Ecological risk: MINIMAL |
| `eco_risk_moderate` | `#F57F17` | Ecological risk: MODERATE |
| `eco_risk_critical` | `#C62828` | Ecological risk: CRITICAL |
| `iot_live` | `#00C853` | Live data pulse indicator |

> **Accessibility rule:** Safety flags must NEVER rely on color alone. Each uses color + icon + text label.

### Typography

| Style | Size | Weight | Use |
|---|---|---|---|
| Screen title | 20sp | Bold | Toolbar |
| Card title | 16sp | SemiBold | Destination/hotel name |
| Score value | 22sp | Bold | Flag score, eco score numbers |
| Body | 14sp | Regular | Descriptions, explanations |
| Caption | 12sp | Regular | Metadata, timestamps |
| Badge label | 10sp | Medium | Chips and badges |

### Spacing: `4dp · 8dp · 16dp · 24dp · 32dp`

---

## Reusable Components

### SafetyFlagChip
```
┌─────────────────────────────┐
│  🟢  GREEN  Safe            │  ← color + icon + text (accessibility)
│  🟡  YELLOW  Caution        │
│  🔴  RED  Unsafe            │
└─────────────────────────────┘
Tap action → opens XAIBottomSheet
```

### BlockchainVerifiedChip
```
┌──────────────────────────────┐
│  🔗  Verified ✓  [hash...] │  ← blue accent color
└──────────────────────────────┘
Tap action → opens BlockchainVerifySheet
Unverified state: "Verification Pending ⏳"
```

### EcoScoreCard
```
┌───────────────────────────────┐
│  🌿  Eco Score   78 / 100    │
│  🌱  Carbon: ~32kg CO₂       │
│  Ethical Score: [HIGH]        │  ← HIGH/MODERATE/LOW chip
│  Eco Risk: [MINIMAL]          │  ← MINIMAL/MODERATE/CRITICAL
└───────────────────────────────┘
```

### IoTMetricsRow
```
┌───────────────────────────────────────────────────┐
│  🌡️ 18°C  |  💨 AQI 45  |  👥 Low  |  💨 12km/h │
│  ● LIVE — Updated 3 min ago                       │
└───────────────────────────────────────────────────┘
Stale state: ⚠️ "Live data unavailable — cached values"
```

### PricingFairnessCard
```
┌──────────────────────────────────────────────────┐
│  Price Fairness: ██████░░░░ 73/100               │
│  Trend: 📈 RISING                                 │
│  ⚠️ Hidden fees detected:                        │
│     Resort fee: ₹850  |  Cleaning fee: ₹300      │
└──────────────────────────────────────────────────┘
```

---

## Screen Specifications

---

### 01 · Splash Screen
- Full screen gradient: `#006D5B` → `#004D40`
- SmartTour360 logo centered
- Tagline: *"Travel Smart. Travel Safe. Travel Green."*
- Bottom progress bar
- Duration: 2s or until Firebase auth check resolves

---

### 02 · Onboarding (3 slides + Preferences)

**Slides:**
1. "Real-time Safety Flags" — flag illustration
2. "Blockchain-Verified Travel" — chain + lock illustration
3. "Eco-Smart Recommendations" — leaf/earth

**Preferences (Slide 4):**
- Budget slider: Budget / Mid-range / Luxury
- Trip type chips: Adventure · Beach · Heritage · Hill Station · Wildlife
- Eco-priority toggle: "Prefer eco-certified stays & low-carbon routes"
- CTA: "Get Started"

---

### 03 · Home Screen

```
┌─────────────────────────────────────────┐
│  🔍 Search destinations...              │
├─────────────────────────────────────────┤
│  [Destinations] [Hotels] [Packages] ... │  ← horizontal chips
├─────────────────────────────────────────┤
│  🟡 3 saved destinations changed flag  │  ← mesh alert banner
│     [View Updates]                      │
├─────────────────────────────────────────┤
│  Top Rated — Safety Verified            │
│  [DestCard] [DestCard] [DestCard] →    │  ← horizontal scroll
├─────────────────────────────────────────┤
│  Recommended For You                    │
│  [DestCard] [DestCard]                 │
├─────────────────────────────────────────┤
│  Featured Hotels                        │
│  [HotelCard]                            │
│  [HotelCard]                            │
└─────────────────────────────────────────┘
```

**Mesh Alert Banner** (shown when WebSocket delivers FLAG_CHANGE_ALERT):
- Amber background banner: "⚠️ Manali flag changed to YELLOW — Tap to view"
- Dismissible; auto-fades after 8 seconds

---

### 04 · Destination Card

```
┌────────────────────────────────────────────────────┐
│  [Banner Image]                       🟢 GREEN     │  ← flag chip top-right
│                             🔗 Verified ✓          │  ← blockchain chip
├────────────────────────────────────────────────────┤
│  Manali, Himachal Pradesh                           │
│  ⭐ 4.7  |  🌿 Eco: 78  |  Ethical: [HIGH]        │
│  Risk: 0.21  |  Carbon: ~32kg CO₂                  │
└────────────────────────────────────────────────────┘
```

---

### 05 · Destination Detail Screen

**Sections (top to bottom):**

1. **Hero image** — back arrow + share icon
2. **Name + State + SafetyFlagChip + BlockchainVerifiedChip** (same row)
3. **Component Breakdown** — 3 horizontal bars:
   - Structural (crime/incidents): `████░░ 45%`
   - Situational (weather/events): `██████ 59%`
   - Environmental (AQI/UV): `███░░░ 32%`
4. **"Why this flag?" button** → opens XAIBottomSheet
5. **IoTMetricsRow** — live temperature, AQI, crowd, wind
6. **EcoScoreCard** — eco score, carbon, ethical score, ecological risk
7. **PricingFairnessCard** (if available)
8. **RED flag only:** "Safer Alternatives" section with alternative destination cards
9. **Nearby Hotels** — horizontal scroll
10. **Reviews Section**
11. **"Add to Trip" FAB** (bottom right)

---

### 06 · XAI Explanation BottomSheet

```
┌─────────────────────────────────────────────────────┐
│  🟡 Why is this YELLOW?                    [✕]     │
├─────────────────────────────────────────────────────┤
│  Risk Score: 0.482                                   │
│                                                      │
│  "Caution advised — elevated weather/event           │
│  conditions: weather severity index at 58.7%.        │
│  Structural risk is within normal range.             │
│  Environmental hazard nominal."                      │
│                                                      │
│  Dominant Factor: Situational (Weather/Events)       │
│  ──────────────────────────────────────────         │
│  Component Scores:                                   │
│  Structural (Crime)  ██░░░░  18%                    │
│  Situational (Wx)    █████░  59%   ← dominant       │
│  Environmental (AQI) ███░░░  32%                    │
│  ──────────────────────────────────────────         │
│  🔗 Blockchain Ref: 0x3f4a9b2c...  [Verify]        │
│  ℹ️  How is this scored?                             │
└─────────────────────────────────────────────────────┘
```

---

### 07 · Blockchain Verify BottomSheet

```
┌───────────────────────────────────────────────────┐
│  🔗 Blockchain Verification              [✕]     │
├───────────────────────────────────────────────────┤
│  Status:  ✅ Verified                              │
│  On-chain flag:  YELLOW                           │
│  Transaction: 0x3f4a9b2c1d8e7f0a...               │
│  Timestamped: 2026-03-20 14:32:01 UTC             │
│                                                    │
│  "This safety evaluation is immutably recorded     │
│   on the blockchain — it cannot be altered."       │
└───────────────────────────────────────────────────┘
```

---

### 08 · RED Flag Acknowledge Dialog

```
┌───────────────────────────────────────────────────┐
│  ⚠️  High Risk Destination                        │
├───────────────────────────────────────────────────┤
│  This destination is currently flagged 🔴 RED.    │
│                                                    │
│  Risk Score: 0.81                                 │
│  Primary risk: Crime/incident history at 78%      │
│                                                    │
│  Your acknowledgement will be recorded on the     │
│  blockchain for transparency.                      │
│                                                    │
│  [ Cancel ]        [ I Understand — Proceed ]     │
└───────────────────────────────────────────────────┘
```

---

### 09 · Hotel Card

```
┌────────────────────────────────────────────────────┐
│  [Image Carousel ← →]                🌿 ECO ✓     │
├────────────────────────────────────────────────────┤
│  The Himalayan Resort               ⭐⭐⭐⭐        │
│  ₹3,500 / night  |  Free Cancellation              │
│  🌿 Eco Score: 80  |  📈 Price: RISING             │
│  ⚠️ Hidden fees detected                          │
│              [View Details]  [Add to Cart]         │
└────────────────────────────────────────────────────┘
```

---

### 10 · Cart Screen

```
┌──────────────────────────────────────────────┐
│  My Cart (2 items)                           │
├──────────────────────────────────────────────┤
│  The Himalayan Resort                        │
│  Apr 10–13 · 2 guests · ₹10,500  [✕]       │
│  🔴 RED flag at time of booking — Acknowledged│
├──────────────────────────────────────────────┤
│  Shimla Heritage Inn                         │
│  Apr 14–15 · 2 guests · ₹4,200   [✕]       │
│  🟢 GREEN                                    │
├──────────────────────────────────────────────┤
│  Subtotal: ₹14,700                           │
│  Taxes:      ₹1,764                          │
│  Total:     ₹16,464                          │
│                                              │
│  [  PLACE ORDER  ]                           │
└──────────────────────────────────────────────┘
```

---

### 11 · Order Confirmation Screen

- Lottie checkmark animation
- "Booking Confirmed!"
- Booking ID: `BKG-25137-A01`
- 🔗 Blockchain Ref: `0xabc123...`
- Safety flag at booking: 🟢 GREEN
- Eco impact summary: *"~64kg CO₂ for this trip"*
- CTAs: "View My Bookings" · "Go Home"

---

## Loading / Error / Empty / Offline States

| State | Treatment |
|---|---|
| Loading | Shimmer skeleton matching card shape |
| IoT loading | Pulsing dot + "Fetching live data..." |
| Empty | Illustrated empty state + contextual CTA |
| API Error | Snackbar: error message + Retry action |
| Offline | Top banner: "Offline — Showing cached data" |
| Stale IoT | ⚠️ banner: "Live data unavailable — last updated N min ago" |
| Blockchain pending | "Verification Pending ⏳" chip (grey) |

---

*End of UI/UX Specification — STS-UX-005 v2.0*
