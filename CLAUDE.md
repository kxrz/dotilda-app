# Dotilda

Gentle daily journaling app for e-ink Android devices. Free, open source (Apache 2.0).
Primary target: Mudita Kompakt (MuditaOS K, AOSP 12, no Google services).
Secondary: Android 11+ e-readers. minSdk 30. Application id: `com.dotilda.app`.

Product spec: `docs/spec.md` (what the app is and how it behaves). The rules below are the ones
that matter for the code.

**Never touch the `website` branch** (the dotilda.com site, deployed by Vercel): no commit,
no merge, no rebase, no deletion. It is maintained separately.

## Stack
Kotlin, Jetpack Compose, Mudita Mindful Design (`com.mudita:MMD`, https://github.com/mudita/MMD),
Room, DataStore, AlarmManager; photos come from the system camera app (TakePicture). The only network access is the in-app update
(`update/Updater.kt`, same mechanism as the readme.club app): it reads the release manifest
and downloads the APK from GitHub Releases, sends nothing about the user, and can be turned off.

Wrap every screen in `DotildaTheme` (which uses `ThemeMMD`). Prefer MMD components
(`TextMMD`, `ButtonMMD`, `OutlinedButtonMMD`, `TextFieldMMD`, `HorizontalDividerMMD`, ...)
over plain Material 3 ones.

## Non-negotiable rules
- No animations, no ripples, no scrolling lists: paginate everything.
  Exception: Dotilda may change pose (`ui/DotildaMark.kt`, `Pose`) in one discrete step,
  at most once per moment, never looping (e.g. glasses pushed up on the Done screen).
- Pure black on white. No gradients. Large type (MMD defaults).
- No Google Play Services, no Firebase, no analytics, no network calls besides the updater.
- No background services. One exact daily alarm only, plus the Spira timer the user starts.
- Never punish missed days: no streak counters, no red states.
- All user data stays local. Export/import must always work.
- The dot galaxy is drawn with Compose Canvas, redrawn once per entry, never animated.
  Dots stay free: never link a full week or month with a line (no streak in disguise).
- Photos are dithered (Floyd-Steinberg, 4 grays) before saving.
- One interaction per day; "Another question" at most 3 times a day, then only the
  inner weather is offered.

## Kompakt facts (MuditaOS K 1.6.1, API 31)
- Screen 480 × 800 px, 213 dpi (×1.33): the app gets **360 × 564 dp**. Design for that.
- Page buttons: `KEYCODE_VOLUME_UP` (24) = previous page, `KEYCODE_VOLUME_DOWN` (25) = next page,
  consumed only on paginated screens. The back key under the screen is `KEYCODE_BACK` (4).

## Content
Questions live in `app/src/main/assets/questions.json`, journeys in `journeys.json` (their
questions are synced into the bank with their `journeyId` and never drawn outside the journey).
Journey state, constellations and opened letters live in DataStore: no schema change so far,
so users' databases need no migration.
Target: more than 200 questions so the user never feels they are going round in circles.
UI copy is English in `res/values/strings.xml`, Polish in `res/values-pl/strings.xml`.
A translation also sets `app_locale` (dates and the question bank follow it) and ships
`assets/questions-<code>.json` and `journeys-<code>.json` with the same ids, types and tags
(`TranslationsTest`). In Polish the user is "Ty" (capitalised) and nothing assumes their gender.
Keep it short and warm. "Dotilda" (capital D) is both the app and the guide character who
signs each question: a kind Victorian governess, a little mischievous, never guilt-tripping.

## Build and release
`./gradlew assembleDebug`. CI (`.github/workflows/build.yml`) runs the unit tests and builds a
debug APK (`com.dotilda.app.debug`, "Dotilda dev", signed with the committed `app/debug.keystore`)
on every push, uploaded as the `dotilda-debug-apk` artifact.

Releasing: bump `appVersion` in `app/build.gradle.kts`, add its section to `CHANGELOG.md`
(every user-visible change goes there, in the same commit), then push the tag `v<version>` or
run the workflow by hand on main. The release job signs the APK with the key from the repo
secrets and publishes `dotilda.apk` + `manifest.json`, which the app's updater reads.
A tag with a dash (`v0.4.0-rc1`) is a pre-release the updater ignores.

## Screenshots
`app/src/test/java/com/dotilda/app/screens/ScreensTest.kt` tours every screen at the Kompakt's
size (Robolectric + Roborazzi). The Screenshots workflow pushes the PNGs to `screenshots/<branch>`:
look at them before and after any visual change.

## Room schema
The schema lives in `app/schemas/` (version 2, as shipped since 0.3.0). The build writes it
again; CI fails if it changed without being committed. Any schema change needs a version bump
and a `Migration`, tested, because people's journals are in there.

## Definition of done per sprint
Installs via Mudita Center, runs on the test Kompakt, no crash over a full day of use,
acceptance criteria from the spec met.
