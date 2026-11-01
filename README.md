# Magikal Retrogrades

Standalone Android Studio project for the new Magikal Retrogrades app. It uses its own package ID (`com.magikalretrogrades.app`) and does not connect to or modify the Magikal Divinations website or its existing Play app.

## Included in this build

- Purple and black Compose interface with five bottom tabs: Retrogrades, Moon phases, Sabbats, Journal, and Natal chart.
- Persistent cursive “Magikal Divinations” branding with the supplied orbit-star motif and a triple moon goddess mark on every page.
- Individually illustrated planet cards, spiritual meanings, retrograde reflection prompts, rituals, intention spells, cautions, and crystal correspondences.
- A retrograde tracker using geocentric planetary vectors from NASA/JPL Horizons. It checks Mercury through Pluto, caches results for 12 hours, shows each current or upcoming station's local start/end time, and provides live countdowns for its start and end plus manual refresh.
- A guide to all eight moon phases, phase-specific prompts, current estimated illumination, and an approximate next phase time.
- A free natal chart calculated on the device from birth date, exact local time, and a selected birthplace. It shows tropical Sun-through-Pluto placements, Ascendant, Midheaven, Whole Sign houses, a chart wheel, and major aspects. Unknown birth time is handled explicitly by omitting time-dependent angles and houses.
- Interactive eight-sabbat Wheel of the Year with clear, large tap targets for each sabbat and Northern/Southern Hemisphere date sets.
- Journal entries tagged to a planet, moon phase, sabbat, or general topic. Entries are stored in app-private device storage; Android backup is disabled.

## Data and accuracy notes

Planet positions are geocentric and therefore the retrograde event is the same instant worldwide; the app converts the event time to the device's time zone and does not request precise location. Station times are estimates interpolated from two-day JPL samples. The moon phase and next phase time use the average synodic month and are approximate. The app requests planetary ephemeris data from NASA/JPL Horizons but does not send journal entries or location.

Natal chart planetary positions use the MIT-licensed Astronomy Engine library. Birth date and time are calculated locally; place search sends only the typed city/place query to Open-Meteo's geocoding API, which returns coordinates and an IANA time zone. Location data is based on GeoNames. Whole Sign houses are used. Chiron is not included in this first natal-chart version.

The app targets Android API 36. Earth is included in the illustrated planet guide, but is not listed as retrograde because it is the reference body for the geocentric tracker.

This project source has not been compiled into a release bundle. The data feed, visual layout, local journal, and Play Store disclosure details should be reviewed on-device before publishing.

## Open in Android Studio

Open this folder as a project in Android Studio with Android SDK Platform 36 installed. The project uses Android Gradle Plugin 8.10.1 and Kotlin 2.0.21. Android Studio will resolve the plugins and dependencies. The final Play Store package, signing, privacy details, and production readiness still need to be completed before release.

The project now includes the Gradle wrapper (`./gradlew`) and a GitHub Actions workflow at `.github/workflows/android-build.yml`. A push to `main` builds a debug APK for device installation and a release AAB. The AAB is Play-upload-ready only after adding a separate upload keystore to GitHub Actions secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Keep the keystore and passwords out of the repository. Without those secrets, the workflow's AAB is unsigned.

## Store artwork

`store-listing/feature-graphic-1024x500.png` is the Google Play feature graphic. It is an RGB PNG at the required 1024 × 500 dimensions. `store-listing/app-icon-512x512.png` is the square Play Store icon and is also wired into the Android launcher. Both use the same photographic triple-moon pendant artwork.

## Web preview

`preview/` contains the standalone responsive app preview for HTTPS hosting. Hosting it on HTTPS allows browser location permission prompts, subject to the visitor's browser and device location settings.

The web preview includes the same 512 × 512 icon as its favicon and installable web app icon (`preview/site.webmanifest`); the header also displays the icon. The Android launcher uses the same artwork through `@drawable/app_icon`.
