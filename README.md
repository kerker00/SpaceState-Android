# SpaceState for Android

Shows whether a hackerspace is open – the Android version of [SpaceState](https://github.com/kerker00/SpaceState) for macOS and iOS.

Work in progress. The Apple implementation and shared architecture are described in [SpaceState's architecture documentation](https://github.com/kerker00/SpaceState/blob/dev/docs/PROJECT.md). The only maintained roadmap and open decisions for all projects live in the local `../Plan.md` when the repositories share the SpaceState workspace; that file is not tracked in this repository.

## Features

- Any space from the SpaceAPI directory, searchable by name and address; Mainframe Oldenburg is the default.
- Open, closed or unknown at a glance, with the time of the last change and the space's status message.
- Mainframe's rooms (Space, Radstelle, 3D Lab, Machining) with independent observation times and failures.
- State is read through [SpacePush](https://github.com/kerker00/SpacePush), with direct SpaceAPI requests as fallback; refreshed every 1 to 15 minutes while the app is visible, and by pulling down.
- Original observation, successful receipt and last attempt shown separately. Retained expired or failed states become unknown without losing names or last known details. Directory opening flags expire independently of name search.
- English and German; Android 13+ also offers the language per app in the system settings.

Home screen widgets and push notifications are not implemented in this version.

## Shared code

The app uses [SpaceStateKit](https://github.com/kerker00/SpaceStateKit), the same Swift package as the Apple apps, compiled for Android with the Swift SDK for Android and bridged to Kotlin with swift-java (see SpaceStateKit's `Android/`).

## Build

Requirements: a Linux Android build host, Android NDK r30, Swift 6.4 with the Swift SDK for Android, and a JDK launcher. The committed Gradle daemon configuration requests JDK 25; app and library bytecode target Java 17.

Gradle builds `spacestatekit-android` from the existing sibling
`../SpaceStateKit/Android`. The coordinate in `gradle/libs.versions.toml` describes
version 0.3.2; **the composite build uses the actual sibling source and does not
pin or download an AAR by that version**.

The C-01 app changes in this branch require the additive Android observation
facade from [Kit PR #8](https://github.com/kerker00/SpaceStateKit/pull/8), currently
commit `1a2e7b04cadf0ecdb0727f1fe9647078cfa2afee`. These APIs are not in the
published 0.3.2 tag. Use the paired source for branch validation; a subsequent
release/adoption is a separate step. Keep the Linux build workspace independent
of the Kit checkout used for Apple/CLI work.

From the Android checkout with an existing Kit sibling, select the paired source
over HTTPS and build the debug app and test bundle without running tests:

```sh
git -C ../SpaceStateKit -c core.sshCommand=false fetch https://github.com/kerker00/SpaceStateKit.git feat/android-observation-freshness
git -C ../SpaceStateKit switch --detach 1a2e7b04cadf0ecdb0727f1fe9647078cfa2afee
./gradlew :app:assembleDebug :app:compileDebugUnitTestKotlin --no-daemon
```

This build generates real Java/JNI bindings and packages both configured ABIs.
Host Swift CI cannot establish Kotlin/JNI compatibility. Until the native build
is verified, this app change remains a draft; its JVM regressions are prepared,
not yet executed. Once their execution is authorized, the standard task is
`./gradlew :app:testDebugUnitTest`. Tests use fake providers and virtual time,
without native client calls, production requests or app launches.

The first build compiles swift-syntax for swift-java and takes several minutes.

Release builds are signed with the key described in `~/.android/spacestate-release.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), or another file passed with `-PreleaseSigning=<path>`; without it they stay unsigned. `./gradlew :app:assembleRelease` writes one APK per ABI to `app/build/outputs/apk/release/`.

## License

MIT, see [LICENSE.MD](LICENSE.MD).
