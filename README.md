# SpaceState for Android

Shows whether a hackerspace is open – the Android version of [SpaceState](https://github.com/kerker00/SpaceState) for macOS and iOS.

Work in progress. The Apple implementation and shared architecture are described in [SpaceState's architecture documentation](https://github.com/kerker00/SpaceState/blob/dev/docs/PROJECT.md). The only maintained roadmap and open decisions for all projects live in the local `../Plan.md` when the repositories share the SpaceState workspace; that file is not tracked in this repository.

## Features

- Any space from the SpaceAPI directory, searchable by name and address; Mainframe Oldenburg is the default.
- Open, closed or unknown at a glance, with the time of the last change and the space's status message.
- Mainframe's rooms (Space, Radstelle, 3D Lab, Machining) with independent observation times and failures.
- State is read through [SpacePush](https://github.com/kerker00/SpacePush), with direct SpaceAPI requests as fallback; refreshed every 1 to 15 minutes while the app is visible, and by pulling down.
- Original observation, successful receipt and last attempt shown separately. Retained expired or failed states become unknown without losing names or last known details. Directory opening flags expire independently of name search.
- Detail view with access hints, country/timezone, coordinates/maps, contacts/keyholders, feeds, projects and further links. Linked spaces open with an isolated selection; Back returns to the saved space.
- English and German; Android 13+ also offers the language per app in the system settings.

Home screen widgets and push notifications are not implemented in this version.

English/German status labels, Mainframe room names, product names and observation
terms follow the [shared terminology](https://github.com/kerker00/SpaceState/blob/dev/docs/TERMINOLOGY.md).

The Compose interface uses shared card, spacing and status-color roles, with
paper/slate and amber fallback themes and Android wallpaper colors by default.
Appearance, accessibility choices and debug-only previews: [docs/DESIGN.md](docs/DESIGN.md).

## Shared code

The app uses [SpaceStateKit](https://github.com/kerker00/SpaceStateKit), the same Swift package as the Apple apps, compiled for Android with the Swift SDK for Android and bridged to Kotlin with swift-java (see SpaceStateKit's `Android/`).

## Build

Requirements: a Linux Android build host, Android NDK r30, Swift 6.4 with the Swift SDK for Android, and a JDK launcher. The committed Gradle daemon configuration requests JDK 25; app and library bytecode target Java 17.

Gradle builds `spacestatekit-android` from the existing sibling
`../SpaceStateKit/Android`. The coordinate in `gradle/libs.versions.toml` describes
version 0.3.2; **the composite build uses the actual sibling source and does not
pin or download an AAR by that version**.

This detail-view branch requires the matching `feat/android-space-details`
source from SpaceStateKit: [Kit getter fix](https://github.com/kerker00/SpaceStateKit/pull/10),
exact source commit `434a8648c4d1c0c9e062bf63316de26e0064181a`. Neither the Android observation facade nor these detail
exports are in the published 0.3.2 tag. Select the exact paired Kit/app commits
from their PR validation notes for the composite build; the coordinate alone does
not pin source. Keep the Linux workspace independent of Apple/CLI dependency
checkouts.

With the matching Kit sibling selected, build the debug app and test bundle
without running tests:

```sh
GIT_SSH_COMMAND=false GIT_ALLOW_PROTOCOL=https:file ./gradlew :app:assembleDebug :app:compileDebugUnitTestKotlin --no-daemon --console=plain
```

This build generates real Java/JNI bindings and packages both configured ABIs.
Host Swift CI cannot establish Kotlin/JNI compatibility. Validate changes with
the native app/test-bundle build on the paired sources before integration.
Once test execution is authorized, the standard task is
`./gradlew :app:testDebugUnitTest`. Tests use fake providers and virtual time,
without native client calls, production requests or app launches.

The first build compiles swift-syntax for swift-java and takes several minutes.

Release builds are signed with the key described in `~/.android/spacestate-release.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), or another file passed with `-PreleaseSigning=<path>`; without it they stay unsigned. `./gradlew :app:assembleRelease` writes one APK per ABI to `app/build/outputs/apk/release/`.

## License

MIT, see [LICENSE.MD](LICENSE.MD).
