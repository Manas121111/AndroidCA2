# 🧪 Testing Strategy
## SmartTour360 Android App
**Document ID:** STS-TST-006 | **Version:** 2.0 | **Patent Aligned:** LPU IDF 2026

---

## Testing Pyramid

```
               ┌──────────────┐
               │ Patent S1–S4  │  ← 10% — 4 scenario-based UAT runs
               │ Scenario UAT  │
               ├──────────────┤
               │  E2E/Manual  │  ← 15% — device UAT checklist
               ├──────────────┤
               │   UI Tests   │  ← 20% — Espresso: critical user flows
               ├──────────────┤
               │ Integration  │  ← 25% — Room DAO + MockWebServer + WS
               ├──────────────┤
               │  Unit Tests  │  ← 30% — ViewModels + UseCases + Utils
               └──────────────┘
```

---

## Layer 1 — Unit Tests (JUnit 5 + MockK)

**Location:** `app/src/test/`

### SafetyViewModel Tests

```kotlin
// SafetyViewModelTest.kt
@Test
fun `evaluateSafety emits loading then success with flag`() = runTest {
    val fakeRepo = mockk<SafetyRepository>()
    coEvery { fakeRepo.evaluate("IND-GOA-01", any(), any()) } returns safetyResponse

    val vm = SafetyViewModel(EvaluateSafetyUseCase(fakeRepo))
    vm.uiState.test {
        assertEquals(UiState.Loading, awaitItem())
        val success = awaitItem() as UiState.Success
        assertEquals("YELLOW", success.data.flag)
        assertNotNull(success.data.explanation)     // XAI text must be present
        assertNotNull(success.data.blockchain_ref)  // Blockchain ref must be present
    }
}

@Test
fun `evaluateSafety emits error when network fails`() = runTest {
    coEvery { fakeRepo.evaluate(any(), any(), any()) } throws IOException()
    val vm = SafetyViewModel(EvaluateSafetyUseCase(fakeRepo))
    vm.uiState.test {
        awaitItem() // Loading
        assertTrue(awaitItem() is UiState.Error)
    }
}
```

### Pseudonymizer Tests

```kotlin
// PseudonymizerTest.kt
@Test
fun `same userId produces same pseudonym (deterministic)`() {
    val p1 = Pseudonymizer.pseudonymize("user-123", "test-key")
    val p2 = Pseudonymizer.pseudonymize("user-123", "test-key")
    assertEquals(p1, p2)
}

@Test
fun `different userIds produce different pseudonyms`() {
    val p1 = Pseudonymizer.pseudonymize("user-123", "test-key")
    val p2 = Pseudonymizer.pseudonymize("user-456", "test-key")
    assertNotEquals(p1, p2)
}

@Test
fun `pseudonym is 32 chars (matching backend truncation)`() {
    val p = Pseudonymizer.pseudonymize("user-123", "test-key")
    assertEquals(32, p.length)
}
```

### HMAC Mesh Signature Verifier Tests

```kotlin
// HmacVerifierTest.kt
@Test
fun `valid message passes signature check`() {
    val msg = buildMeshMessage(secretKey = "test-mesh-key")
    assertTrue(HmacVerifier("test-mesh-key").verify(msg))
}

@Test
fun `tampered message fails signature check`() {
    val msg = buildMeshMessage(secretKey = "test-mesh-key")
    val tampered = msg.copy(new_flag = "GREEN")  // Flip flag — should fail
    assertFalse(HmacVerifier("test-mesh-key").verify(tampered))
}

@Test
fun `wrong key fails signature check`() {
    val msg = buildMeshMessage(secretKey = "real-key")
    assertFalse(HmacVerifier("wrong-key").verify(msg))
}
```

### Cart ViewModel Tests

```kotlin
@Test
fun `addItem increases cart item count`() = runTest {
    val vm = CartViewModel(fakeCartRepo)
    vm.addItem(fakeHotelBooking)
    assertEquals(1, vm.cartState.value.items.size)
}

@Test
fun `removeItem decreases item count`() = runTest {
    val vm = CartViewModel(fakeCartRepo)
    vm.addItem(fakeHotelBooking)
    vm.removeItem(fakeHotelBooking.id)
    assertEquals(0, vm.cartState.value.items.size)
}
```

**Coverage Target:** ≥ 80% for ViewModel + UseCase + Pseudonymizer + HmacVerifier

---

## Layer 2 — Integration Tests

### Room DAO Tests (In-Memory DB)

```kotlin
// DestinationDaoTest.kt
@RunWith(AndroidJUnit4::class)
class DestinationDaoTest {

    @Test
    fun `insert and retrieve destination with flag and blockchain_ref`() = runTest {
        dao.insert(fakeDestinationEntity)
        val result = dao.getAll().first()
        assertEquals("YELLOW", result[0].flag_color)
        assertNotNull(result[0].blockchain_ref)
        assertNotNull(result[0].explanation)
    }

    @Test
    fun `insert acknowledgement stores flag_at_booking_time`() = runTest {
        dao.insertAcknowledgement(fakeAck)
        val result = dao.getAcknowledgement("booking-001")
        assertEquals("RED", result.flag_at_booking_time)
    }
}
```

### Safety API Tests (MockWebServer)

```kotlin
// SafetyApiTest.kt
@Test
fun `evaluate returns parsed flag and explanation on 200`() = runTest {
    mockServer.enqueue(MockResponse()
        .setBody(readJson("safety_evaluate_response.json"))
        .setResponseCode(200))
    val result = api.evaluateSafety(SafetyEvaluateRequest("IND-GOA-01", 15.29, 74.12))
    assertEquals("YELLOW", result.flag)
    assertTrue(result.explanation.isNotBlank())
    assertTrue(result.blockchain_ref.startsWith("0x"))
}

@Test
fun `evaluate returns non-null alternatives on RED flag`() = runTest {
    mockServer.enqueue(MockResponse()
        .setBody(readJson("safety_evaluate_red.json"))
        .setResponseCode(200))
    val result = api.evaluateSafety(SafetyEvaluateRequest("TEST-RED", 0.0, 0.0))
    assertEquals("RED", result.flag)
    assertNotNull(result.alternatives)
    assertTrue(result.alternatives!!.isNotEmpty())
}
```

### WebSocket Mesh Integration Test

```kotlin
// MeshWebSocketClientTest.kt
@Test
fun `FLAG_CHANGE_ALERT with valid signature emits to alerts flow`() = runTest {
    val client = MeshWebSocketClient(okHttpClient, HmacVerifier(TEST_KEY), testWsUrl)
    client.connect("TEST-NODE-01")

    val received = mutableListOf<MeshAlert>()
    val job = launch { client.alerts.collect { received.add(it) } }

    // Simulate backend sending a valid alert
    mockWsServer.sendMessage(buildSignedAlert("IND-GOA-01", "RED", TEST_KEY))

    delay(200)
    assertEquals(1, received.size)
    assertEquals("RED", received[0].newFlag)
    job.cancel()
}

@Test
fun `FLAG_CHANGE_ALERT with invalid signature is ignored`() = runTest {
    val client = MeshWebSocketClient(okHttpClient, HmacVerifier(TEST_KEY), testWsUrl)
    client.connect("TEST-NODE-01")

    val received = mutableListOf<MeshAlert>()
    val job = launch { client.alerts.collect { received.add(it) } }

    // Tampered alert with wrong signature
    mockWsServer.sendMessage(buildSignedAlert("IND-GOA-01", "RED", "wrong-key"))

    delay(200)
    assertEquals(0, received.size)   // Must be ignored
    job.cancel()
}
```

---

## Layer 3 — UI Tests (Espresso)

### Flow 1: Login → Home → Safety Flag Visible

```kotlin
@Test
fun loginNavigatesToHomeWithSafetyFlags() {
    onView(withId(R.id.email_field)).perform(typeText("test@email.com"))
    onView(withId(R.id.password_field)).perform(typeText("Test@1234"))
    onView(withId(R.id.sign_in_btn)).perform(click())

    onView(withId(R.id.home_screen_root)).check(matches(isDisplayed()))
    onView(withId(R.id.safety_flag_chip)).check(matches(isDisplayed()))
}
```

### Flow 2: Destination Detail → XAI Explainer Opens

```kotlin
@Test
fun destinationDetailShowsXAIExplanation() {
    navigateToDestinationDetail("Manali")

    onView(withId(R.id.why_this_flag_btn)).perform(click())

    onView(withId(R.id.xai_explanation_text)).check(matches(isDisplayed()))
    onView(withId(R.id.blockchain_ref_text)).check(matches(isDisplayed()))
    onView(withId(R.id.component_bar_structural)).check(matches(isDisplayed()))
}
```

### Flow 3: RED Flag → Acknowledge Dialog → Blockchain Logged

```kotlin
@Test
fun redFlagShowsAcknowledgeDialogOnBooking() {
    navigateToRedFlagDestination()

    onView(withId(R.id.book_now_btn)).perform(click())

    onView(withText(R.string.acknowledge_dialog_title)).check(matches(isDisplayed()))
    onView(withId(R.id.acknowledge_proceed_btn)).perform(click())

    onView(withId(R.id.order_confirmed_title)).check(matches(isDisplayed()))
    onView(withId(R.id.blockchain_ref_order)).check(matches(isDisplayed()))
}
```

### Flow 4: WebSocket Alert → UI Flag Updated

```kotlin
@Test
fun meshAlertUpdatesDestinationFlagInRealTime() {
    // Initial state: Manali is GREEN
    onView(withId(R.id.safety_flag_chip)).check(matches(withText("GREEN")))

    // Simulate backend sending YELLOW alert via mock WS
    mockMeshServer.sendAlert("IND-MAN-01", "YELLOW")
    Thread.sleep(300)

    onView(withId(R.id.safety_flag_chip)).check(matches(withText("YELLOW")))
    onView(withId(R.id.mesh_alert_banner)).check(matches(isDisplayed()))
}
```

---

## Layer 4 — Patent Scenario-Based UAT (S1–S4)

> Source: TicketKaru Backend SDLC v3.0 — 4 patent evaluation scenarios. The Android app must pass all 4 to satisfy patent validation criteria.

### Scenario S1 — Ideal Destination (Safe, Eco, Fair Price)

**Backend:** Tokyo / Manali preset — GREEN flag, risk < 0.35, high eco score

**Android Test Steps:**
1. Home screen → tap on Manali destination card
2. ✅ SafetyFlagChip shows GREEN
3. ✅ "Verified ✓" blockchain chip visible
4. Tap "Why this flag?" → ✅ Explanation names dominant safe factor
5. EcoScoreCard → ✅ Ethical Score: HIGH
6. Add to cart → ✅ No acknowledgement dialog (safe destination)
7. Place order → ✅ Confirmation shows GREEN flag + blockchain ref

---

### Scenario S2 — HIGH RISK Destination (RED flag)

**Backend:** RED flag destination — risk ≥ 0.70, alternatives populated

**Android Test Steps:**
1. Navigate to RED-flagged destination
2. ✅ SafetyFlagChip shows RED with 🔴 icon
3. ✅ "Safer Alternatives" section appears with alternative cards
4. Tap "Why this flag?" → ✅ Explanation names critical factor (e.g., Level 4 Advisory / Crime)
5. Tap Book → ✅ AcknowledgeRiskDialog appears
6. Confirm acknowledge → ✅ `POST /booking/acknowledge` called → blockchain logged
7. My Bookings → ✅ Shows "⚠️ RED flag at booking time — Acknowledged"

---

### Scenario S3 — Safe but Unsustainable (Dubai-type)

**Backend:** GREEN safety flag but low eco score, high carbon

**Android Test Steps:**
1. Navigate to destination with GREEN flag + low eco score (e.g., Eco: 28)
2. ✅ SafetyFlagChip shows GREEN
3. ✅ EcoScoreCard shows low score + high carbon
4. ✅ Ethical Score chip: MODERATE or LOW (eco drag)
5. Recommendations screen → ✅ This destination ranks LOWER than a train-route alternative
6. ✅ Ecological Risk badge shows CRITICAL or MODERATE

---

### Scenario S4 — High Crime + Weather + Surge Pricing

**Backend:** YELLOW/RED flag + pricing guardrail triggered + hidden costs

**Android Test Steps:**
1. Navigate to high-risk destination with YELLOW flag
2. ✅ Component breakdown: Structural bar high
3. Open hotel detail → ✅ PricingFairnessCard loads
4. ✅ "⚠️ Hidden fees detected" banner visible
5. ✅ Price trend: RISING shown
6. ✅ "This price may be above fair market rate" warning visible (PRICE_DISCRIMINATION guardrail surfaced)
7. ✅ Eco score still shown accurately (decoupled from pricing)

---

## Manual UAT Checklist (Pre-Demo)

| # | Test | Expected | Pass? |
|---|---|---|---|
| 1 | Cold launch fresh install | Splash → Onboarding | ☐ |
| 2 | Google Sign-In | Navigates to Home | ☐ |
| 3 | Safety flag shown on home card | Green/Yellow/Red chip visible | ☐ |
| 4 | "Verified ✓" chip on card | Taps → Blockchain sheet opens | ☐ |
| 5 | "Why this flag?" button | XAI BottomSheet with explanation + components | ☐ |
| 6 | IoT metrics row | Temperature, AQI, Crowd visible + freshness | ☐ |
| 7 | Eco score + ethical score chip | HIGH/MODERATE/LOW on destination | ☐ |
| 8 | Hotel pricing fairness | Fairness bar + hidden fee banner if applicable | ☐ |
| 9 | RED flag → Acknowledge | Dialog appears, confirm logs to blockchain | ☐ |
| 10 | Add hotels to cart | Cart badge updates | ☐ |
| 11 | Place order | Confirmation + blockchain_ref shown | ☐ |
| 12 | Airplane mode → open app | Cached data + "Offline" banner | ☐ |
| 13 | WebSocket flag change | Card flag color updates without refresh | ☐ |
| 14 | FCM notification (bg) | Alert notification received | ☐ |
| 15 | Dark mode | All screens render correctly | ☐ |
| 16 | TalkBack | All flags readable as "GREEN Safe" / "RED Unsafe" | ☐ |
| 17 | Device rotate on destination detail | Data preserved, no crash | ☐ |
| 18 | Kill app mid-cart | Cart persisted on reopen | ☐ |

---

## Patent KPI Validation Table

| Patent KPI | Target | Android Verification |
|---|---|---|
| Response Time | < 500ms | Measure Retrofit call → flag render latency |
| Transparency Accuracy | XAI text always present | Assert `explanation.isNotBlank()` in every safety response |
| Blockchain Audit Coverage | `blockchain_ref` on every evaluation | Assert `blockchain_ref.startsWith("0x")` |
| Mesh Broadcast Latency | < 200ms UI update | Measure WS receipt → StateFlow update time |
| Ethical Rule Enforcement | PRICE_DISCRIMINATION surfaced to user | Espresso: hidden fee banner visible on flagged hotel |
| HMAC Signature Security | Tampered alerts rejected | Unit test: wrong key → `verify() == false` → alert ignored |
| No PII in API calls | Pseudonym only | Assert no email/phone in any Retrofit request log |

---

## Bug Severity

| Severity | Definition | Fix By |
|---|---|---|
| P0 | Crash, data loss, broken auth, blockchain_ref missing from confirmed order | Before next demo |
| P1 | Feature broken (XAI not showing, flag not updating, WebSocket not connecting) | Same sprint |
| P2 | UI glitch, wrong value displayed, slow render | Next sprint |
| P3 | Cosmetic, spelling, minor layout issue | Backlog |

---

*End of Testing Strategy — STS-TST-006 v2.0*
