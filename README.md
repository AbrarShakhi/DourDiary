# Dour Diary

A GPS running tracker for Android. Records distance, duration, pace and route, entirely on your
own phone.

There is no account, no server and no analytics. Runs are stored locally and never uploaded.

## Features

- Start, pause, resume and finish a run, with automatic pause when you stop moving
- Live distance, duration, current pace and average pace
- Live route on an OpenStreetMap map, with the full route saved for later
- Spoken and vibrating cues at each kilometre or mile
- Run summary, history with lifetime totals, and a home view of the current week
- Metric or imperial, light or dark, configurable cue interval

Recording works without a network connection; only the map needs one.

## Building

Requires a JDK and the Android SDK. `java` is not assumed to be on `PATH`:

```bash
JAVA_HOME=/path/to/jdk ./gradlew :app:installDebug
```

| Task | Command |
|---|---|
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Instrumented tests | `./gradlew :app:connectedDebugAndroidTest` |
| Lint | `./gradlew :app:lintDebug` |
| Release bundle | `./gradlew :app:bundleRelease` |

Release signing is read from an untracked `app/keystore.properties`; without it the release build
is simply unsigned. See [`docs/RELEASE.md`](docs/RELEASE.md).

## Architecture

Clean architecture in a single Gradle module, with `common/` for shared code and `features/` for
each screen. The domain layer is pure Kotlin — no Android, no DI framework — which is what lets a
whole run be replayed from a scripted list of GPS fixes in a unit test rather than by going
outside. A test reads the sources and fails the build if that boundary is crossed.

Presentation is MVI over Jetpack Compose and Navigation 3, with Koin for injection and Room for
storage. Recording is owned by a foreground service at application scope, so a run survives the
screen that started it, and is written progressively so a crash leaves a recoverable run rather
than nothing.

## Icon

The launcher icon is an adaptive icon authored as vector drawables in `app/src/main/res`. The
mark is two D's, the second dropped by exactly half the first one's height, ending on a map
position marker — the ring a navigator puts under you.

It is one continuous line, and the step is what allows that: half a D's height is where its bowl
is widest, so dropping the second D by exactly half puts the first D's right shoulder and the
second D's stem top on the same line, and the bridge between them is level. A connector anywhere
lower has to descend, and a descending stroke hanging off a D reads as an R's leg, which turned
"DD" into "RD" every time. Stacking them on a shared spine was tried earlier and abandoned for a
related reason: that shape is a B, and the gap needed to tell the letters apart leaves each bowl
too shallow to read as a D once the stroke is thick enough to survive at 48dp.

`tools/render_launcher_icon.py` is the single source of that geometry. It emits the `pathData`
for the vectors and renders the PNG copies for the two places a vector cannot go: the 512×512
Play listing icon and the legacy density buckets. Re-run it after any change so they cannot
drift.

## Privacy

Runs never leave the device. The only network request the app makes is for map tiles, which
discloses your IP address and map viewport to the tile provider, as any online map does.

Full policy: [`PRIVACY.md`](PRIVACY.md).

## Licence

Dour Diary is released under the [MIT License](LICENSE).

Map data © OpenStreetMap contributors, licensed under
[ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/); tiles by
[OpenFreeMap](https://openfreemap.org/). Attribution is shown on every map in the app and is a
licence condition.

Third-party components and their licences: [`NOTICE.md`](NOTICE.md).
