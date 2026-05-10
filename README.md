# Islamic Calendar

Android app that shows today’s **Hijri (Islamic) date** alongside the **moon phase**, with optional **±1 day** adjustment when local announcements differ from calculated dates.

Repository: [github.com/kaisarnajar/islamic-calendar](https://github.com/kaisarnajar/islamic-calendar)

## Features

- **Hijri date** using `java.time` [`HijrahDate`](https://developer.android.com/reference/java/time/chrono/HijrahDate) (Umm al-Qura–style rules as provided by the runtime).
- **Islamic month names** in English transliteration, with Arabic strings when the device locale uses Arabic (`values` / `values-ar`).
- **Community offset**: adjust the Hijri day (−1 / +1, repeatable) on the **Settings** screen; persisted with **DataStore** across restarts.
- **Location (optional)**: coarse location → reverse geocode → time zone when available (**API 34+** `Address` time zone via reflection); otherwise the device default time zone is used.
- **Moon phase**: lightweight synodic model with illuminated fraction and labels (new / waxing / full / waning); drawn with Jetpack Compose **Canvas**.
- **UI**: Material 3, dynamic color on Android 12+, dark fallback palette, safe-area insets, **Navigation Compose** (home + settings).

## Requirements

- **Android Studio** (recommended) or Android SDK + JDK **17**
- **Min SDK:** 26 · **Target / compile SDK:** 35
- **Google Play services** on the device/emulator if you use fused location (typical for Play-enabled images).

## Getting started

1. Clone the repository:

   ```bash
   git clone https://github.com/kaisarnajar/islamic-calendar.git
   cd islamic-calendar
   ```

2. Create **`local.properties`** in the project root (not committed) with your SDK path, for example:

   ```properties
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```

   On macOS/Linux you can use forward slashes, e.g. `sdk.dir=/Users/you/Library/Android/sdk`.

3. Open the folder in Android Studio and sync Gradle, or from a terminal:

   ```bash
   ./gradlew assembleDebug
   ```

4. Run the **`app`** configuration on a device or emulator.

## Tests

```bash
./gradlew testDebugUnitTest
```

## Architecture (overview)

- **UI:** Compose screens in `ui/` (`MainScreen.kt`, `SettingsScreen.kt`), theme under `ui/theme/`.
- **State:** `HijriViewModel` exposes `StateFlow` for Hijri display, moon phase, zone info, and offset.
- **Data:** `UserOffsetRepository` (DataStore), `LocationRepository` (Play Services Location + `Geocoder`).
- **Domain:** `HijriCalendar`, `MoonPhaseCalculator`.

## Notes

- **Calculated vs announced Hijri:** Many communities follow local moon-sighting; the **± day** control is there to align the on-screen date with your locality when it differs from the tabular/calculated calendar.
- **Moon phase** follows **civil “today”** at local noon for astronomy display; it does not shift when you only change the Hijri offset.

## License

No license file is bundled in this repository yet. Add one (e.g. MIT, Apache-2.0) if you intend to distribute or accept contributions under clear terms.
