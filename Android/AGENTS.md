# Wattagam Android agent instructions

## Scope and source of truth
- These rules apply to `Android/`. Read parent instructions and inspect the current Git diff before editing. Preserve user changes; do not reset or overwrite unrelated work.
- This repository contains a separate Spring Boot project in `backend/`. Android has its own Gradle wrapper and settings. Do not modify backend or root infrastructure for an Android-only task.
- Product: 왔다감, a native Android campus-tour app for Chung-Ang University. Use Kotlin and Jetpack Compose, a single Activity, and initially one `app` module.
- Current milestone: Figma-based common components and sample-data screens, then navigation, then real integrations. A showcase is development evidence, not a finished production feature.
- Design reference: https://www.figma.com/design/YPHFpqR55vNmkvEd4T9t5I?node-id=247-101 (`Compose · 공통 디자인`). Reuse its colors, Noto Sans KR, type hierarchy and shapes. Do not redesign without a product reason.
- Resolve conflicts in this order: explicit current user decisions, current agreed product scope, design reference, older PRD/screens. Flag unresolved behavior differences rather than silently adding features.

## Product invariants
- No proximity notifications, notification permission or background location monitoring.
- Exactly four tour places: 정문, 도서관, 청룡탕, 310관. Selection only opens information; it does not prove a visit.
- Kakao-only login. No password or guest-auth implementation. Nickname setup remains a UI proposal until its account contract is confirmed.
- Valid current GPS is required at writing start and final submission. The provisional visit radius is 30 m; freshness/accuracy thresholds require field validation. A previous visit never grants current write access.
- A successful server guestbook save grants at most one stamp per account/place, four total. Visiting or taking a photo alone grants none. Server results determine real eligibility and stamp state.
- All saved guestbooks are visible to signed-in app users. No private visibility selector. Record editing/deletion and notifications are outside the current MVP.
- AR is for photos with 푸앙이; unsupported devices can use ordinary capture. A saved AR photo must contain the rendered character. No image-recognition visit verification.
- Preserve drafts on failed submission, expired authentication or location departure. Recheck position before retry; do not automatically upload later from another location.

## Think before coding
- Read relevant code, Gradle configuration, instructions and design states first. State the intended outcome, assumptions and meaningful acceptance checks before a substantial change.
- Separate known requirements from proposals. Ask only for missing decisions that materially change scope; proceed with reversible work that is already authorized.
- Verify unfamiliar or version-sensitive Android APIs against official documentation. Check AGP, Gradle, Kotlin, JDK and SDK compatibility as a set.

## Surgical changes
- Touch only files needed for the requested outcome. Do not mix feature work with unrelated renames, formatting, dependency upgrades or architecture rewrites.
- Reuse existing components and tokens. Fix a defect at its owning layer; avoid scattered workarounds and silent exception swallowing.
- Do not edit generated outputs, local caches or tracked source to hide a failed check. Do not disable checks merely to make the build green.

## Simplicity first
- Prefer explicit Kotlin and standard AndroidX APIs. Start with one app module and feature packages; introduce modules or abstractions only for a demonstrated need.
- Do not add speculative use-case layers, generic base ViewModels, navigation frameworks, repositories or dependencies without a current consumer.
- Keep versions centralized in `gradle/libs.versions.toml`; use the Compose BOM for covered artifacts and pin other dependencies. No dynamic `+` versions.
- Keep network, map, login, camera and AR libraries out until their integration milestone. Use small, clearly named preview/sample fixtures in the meantime.

## Goal-driven execution
- Define done as observable behavior: for example, a disabled action cannot fire, errors preserve input, or Back restores the previous screen.
- Work in verifiable increments: theme → common component previews/showcase → screens → navigation → integrations.
- Complete authorized implementation and relevant checks. If blocked, report the exact blocker, completed work and next concrete step; do not claim success from code inspection alone.
- Report changed behavior, checks actually run, and remaining limits. Do not commit, publish or deploy unless requested.

## Compose and state
- Shared composables accept immutable state, event callbacks and a `Modifier`. Keep ViewModels and service calls at route boundaries.
- Hoist state to the nearest owner that needs it. Use ViewModel/StateFlow for screen business state and lifecycle-aware collection; use `rememberSaveable` for small restorable UI state.
- Model mutually exclusive states explicitly. Avoid independent loading/error/success booleans that can contradict each other.
- Never start I/O, permissions or navigation directly during composition. Use event handlers and correctly keyed effects. Avoid GlobalScope and do not swallow coroutine cancellation.
- Keep large images out of saved-state Bundles. Persist temporary images as internal files and pass URIs/IDs. ViewModel survival does not guarantee process-death recovery.

## Native behavior and accessibility
- Respect Activity lifecycle and release camera/location resources on exit. Request permissions when needed, and distinguish denied, unavailable and loading states.
- Use edge-to-edge insets and IME handling; do not hardcode system-bar or keyboard heights from Figma.
- Use dp for geometry, sp for text, flexible widths and scrolling for variable content. Check narrow screens, large fonts, keyboard visibility and system Back.
- Provide at least 48 dp interactive targets, meaningful semantics/content descriptions, visible disabled/error states and sufficient contrast. Do not rely on color alone.
- Store user-visible text in resources. Bundle only licensed fonts/assets, retain their notices and avoid production network dependencies for typography.
- Preserve the agreed light brand theme initially; do not introduce dynamic colors or claim dark-mode support without verifying it.

## Data and security
- Keep credentials, signing keys, tokens and machine-specific SDK paths out of Git. Do not log authentication headers or precise location.
- Sample data must remain visibly identifiable in the showcase. Never present fake login, GPS, upload or stamp behavior as real integration.
- Validate untrusted responses at data boundaries. Keep server authorization and idempotency authoritative; UI state is not proof of permission.

## Verification
- Use the Android project's wrapper, not the backend wrapper. Standard checks from `Android/`: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Add behavior-focused tests for meaningful logic and interaction changes. Avoid tests that only assert hardcoded constants or duplicate implementation.
- Give common components useful previews: default, disabled/loading/error where applicable, and narrow/large-font cases. Inspect rendered output when device or preview tooling is available.
- Run instrumented tests only on an explicitly selected compatible device. Preserve existing emulators and containers; stop task-owned resources after verification. Follow parent Docker cleanup rules if Docker is actually needed.
- Distinguish compilation, tests, lint, screenshot review and real-device verification in reports. State honestly which were not run.
