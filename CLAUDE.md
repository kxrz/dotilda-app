# Dotilda

Gentle daily journaling app for e-ink Android devices. Free, open source (Apache 2.0).
Primary target: Mudita Kompakt (MuditaOS K, AOSP 12, no Google services).
Secondary: Android 11+ e-readers. minSdk 30. Application id: `com.dotilda.app`.

Full product spec (French): `docs/spec.md`.

## Stack
Kotlin, Jetpack Compose, Mudita Mindful Design (`com.mudita:MMD`, https://github.com/mudita/MMD),
Room, DataStore, CameraX, AlarmManager. No network, ever.

Wrap every screen in `DotildaTheme` (which uses `ThemeMMD`). Prefer MMD components
(`TextMMD`, `ButtonMMD`, `OutlinedButtonMMD`, `TextFieldMMD`, `HorizontalDividerMMD`, ...)
over plain Material 3 ones.

## Non-negotiable rules
- No animations, no ripples, no scrolling lists: paginate everything.
  Exception: Dotilda may change pose (`ui/DotildaMark.kt`, `Pose`) in one discrete step,
  at most once per moment, never looping (e.g. glasses pushed up on the Done screen).
- Pure black on white. No gradients. Large type (MMD defaults).
- No Google Play Services, no Firebase, no analytics, no network calls.
- No background services. One exact daily alarm only.
- Never punish missed days: no streak counters, no red states.
- All user data stays local. Export/import must always work.
- The dot galaxy is drawn with Compose Canvas, redrawn once per entry, never animated.
- Photos are dithered (Floyd-Steinberg, 4 grays) before saving.
- One interaction per day; "Another question" at most 3 times a day, then only the
  inner weather is offered.

## Kompakt facts (MuditaOS K 1.6.1, API 31)
- Screen 480 × 800 px, 213 dpi (×1.33): the app gets **360 × 564 dp**. Design for that.
- Page buttons: `KEYCODE_VOLUME_UP` (24) = previous page, `KEYCODE_VOLUME_DOWN` (25) = next page,
  consumed only on paginated screens. The back key under the screen is `KEYCODE_BACK` (4).

## Content
Questions live in `app/src/main/assets/questions.json` (journeys will go in `journeys.json`).
Target: more than 200 questions so the user never feels they are going round in circles.
UI copy is English, in `res/values/strings.xml` so other languages can be added later.
Keep it short and warm. "Dotilda" (capital D) is both the app and the guide character who
signs each question: a kind Victorian governess, a little mischievous, never guilt-tripping.

## Build
`./gradlew assembleDebug`. CI (`.github/workflows/build.yml`) builds a debug APK on every
push and uploads it as the `dotilda-debug-apk` artifact.

## Definition of done per sprint
Installs via Mudita Center, runs on the test Kompakt, no crash over a full day of use,
acceptance criteria from the spec met.
