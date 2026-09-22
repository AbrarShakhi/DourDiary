# Releasing Dour Diary

Everything needed to get a build onto Play, and the decisions that must not be
reversed by accident.

## Signing

The release signing key is **not** in this repository and never should be. The build reads it
from `app/keystore.properties`, which is git-ignored. When that file is absent the release
build is simply unsigned, so the project still builds for anyone without the key.

Create the key once, and back it up somewhere you will still have in five years — losing it
means you can never update the app again under the same listing:

```bash
keytool -genkeypair -v \
  -keystore ~/keys/dour-diary-release.jks \
  -alias dour-diary -keyalg RSA -keysize 4096 -validity 10000
```

Then write `app/keystore.properties`:

```properties
storeFile=/absolute/path/to/dour-diary-release.jks
storePassword=...
keyAlias=dour-diary
keyPassword=...
```

Enrolling in **Play App Signing** is strongly recommended: Google holds the app signing key and
you keep only an upload key, so a lost laptop is recoverable.

**Ship the bundle, not the APK.** MapLibre carries native renderers for every ABI, which takes a
universal APK from about 3 MB to about 50 MB. Play splits an `.aab` per device, so the actual
download is a fraction of that. Do not judge app size from `assembleRelease`.

Build the artefact Play wants:

```bash
JAVA_HOME=/path/to/jdk ./gradlew :app:bundleRelease
```

## Before the first upload

- [ ] **Decide the `applicationId`.** It is `com.abrarshakhi.dourdiary` and becomes permanent
      the moment the listing goes live. This is the last chance to change it.
- [ ] Play Console developer account: **$25 one-time**, plus identity verification.
- [ ] New personal accounts must run **closed testing with 12+ testers for 14 days** before
      production access is granted. Start this early; it is the longest pole.
- [ ] Set `versionCode`/`versionName` in `app/build.gradle.kts`.
- [ ] Upload `docs/play-store-icon-512.png` as the store icon. It is generated from the same
      geometry as the launcher icon by `tools/render_launcher_icon.py`; re-run that script after
      any change to `ic_launcher_foreground.xml` so the two cannot drift.
- [ ] Capture store screenshots. Phone screenshots are required; the feature graphic is not, but
      a listing without one looks unfinished.
- [ ] **Confirm the copyright holder** named in `LICENSE`. It currently reads
      "Abrar Shakhi"; it should be the name you want on a public licence.
- [ ] **Fill in the contact address** in `PRIVACY.md`. It is deliberately left as a placeholder,
      because that document is published and the address in it should be a decision.
- [ ] **Host `PRIVACY.md`** at a public URL. Mandatory because the app collects location; the URL
      goes in the store listing and is required by Health Connect later.
- [ ] Put that URL in the `privacy_policy_url` string resource. While it is empty the in-app
      Settings row is hidden, so an unset URL is invisible rather than broken.
- [ ] Check the in-app **Open source licences** screen still matches `NOTICE.md` after any
      dependency change. Apache 2.0 and BSD both require their notices to accompany the binary,
      so that screen is compliance, not decoration.

## What leaves the device

**Recorded runs never leave the device.** There is no backend, no account and no analytics.

**Map tiles do involve the network.** While a map is on screen the app fetches vector tiles from
`tiles.openfreemap.org`, which necessarily reveals this device's IP address and the map viewport
to that server. That is true of every online map, Google's included, and it is the reason the
`INTERNET` permission is declared. It is not run data: no route, time or pace is ever sent.

Consequences that must be kept true:

- The privacy policy has to **name the tile provider** and say what a tile request discloses.
- **OpenStreetMap attribution must stay visible.** It is a licence condition. MapLibre's
  attribution control is enabled and its margins are lifted clear of the controls that sit over
  the map; do not hide it to tidy up a screen.
- **Recording must keep working with no network.** GPS needs no connection. Tiles fail to a blank
  background while the route line still draws, and list thumbnails never touch the network at all.
- Offline map download, on the roadmap, would remove the disclosure entirely.

## Data Safety declaration

The app collects one sensitive data type. Answer the form as follows, and re-check it whenever
a feature is added:

| Question | Answer |
|---|---|
| Data types collected | **Location → Precise location** |
| Collected or shared? | Collected, **not shared** |
| Transmitted off device? | **No.** No run data is transmitted; there is no backend and no analytics. Map tile requests disclose IP and viewport to the tile provider, which is disclosed in the privacy policy. |
| Processed ephemerally? | No — runs are stored so history works |
| Required or optional? | Required for the app's core function |
| Purpose | App functionality |
| User can request deletion? | Yes — every run can be deleted in the app |
| Encrypted in transit? | Not applicable; nothing is transmitted |

The "not shared, never transmitted" answers are only true while there is no backend. Adding
sync, analytics or a crash reporter changes them.

## Things that must stay true

These are deliberate and are enforced or documented elsewhere in the repo:

- **No `ACCESS_BACKGROUND_LOCATION`.** A foreground service started while the app is visible
  covers a whole run. Requesting background location triggers a much heavier Play review.
- **`android:allowBackup="false"`** plus the exclusions in `res/xml/data_extraction_rules.xml`.
  Recorded GPS traces must not reach cloud backup or device-to-device transfer.
- **No coordinates in release logs.**
- **Never declare `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.** The app reads
  `PowerManager.isIgnoringBatteryOptimizations`, which needs no permission, and sends the
  runner to the system list with `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`. The one-tap
  prompt would need that restricted permission and buys a Play policy review — the same trade
  already declined for background location. The seam is
  `common/domain/power/BackgroundRestrictionChecker`.
- R8 is enabled for release. Keep rules live in `app/src/main/keepRules/`; the serialization
  rules are load-bearing, because without them the navigation back stack silently fails to
  restore after process death **in release builds only**.

## Verifying a release build locally

R8 breakage does not show up in unit tests. After changing dependencies or keep rules:

1. `./gradlew :app:assembleRelease`
2. Install it and confirm the app launches, the tabs navigate, and the back stack still
   restores after `adb shell am kill com.abrarshakhi.dourdiary` followed by a relaunch. That
   last step is what exercises kotlinx.serialization through obfuscation.
3. **Open the Record tab and a run summary.** MapLibre resolves style layers reflectively
   through JNI; its own consumer rules cover this, and `mapping.txt` should show the
   `org.maplibre.*` classes kept unrenamed, but only a running map proves it.

## Known gaps

- **List thumbnails are drawn, not mapped.** `CanvasRouteThumbnailRenderer` draws the route on a
  plain background so a feed scrolls at any length and works offline. Replacing it with cached
  MapLibre snapshots is the remaining piece of the map work.
- **ColorOS and other aggressive OEMs** kill background processes. The app detects the
  battery-optimisation state, offers advice about it on the record screen and keeps it
  reachable under Settings → Battery. That only covers the part of the picture Android
  exposes: a manufacturer's own process killer sits above the API and reports nothing, so long
  runs on those devices still need testing on the device itself.
- **No baseline profile.** Worth adding a macrobenchmark module before launch; it measurably
  improves cold start and scroll jank on first run.
