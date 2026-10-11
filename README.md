# SpaceState for Android

Shows whether a hackerspace is open – the Android version of [SpaceState](https://github.com/kerker00/SpaceState) for macOS and iOS.

Work in progress. The plan and decisions live in [SpaceState's PROJECT.md](https://github.com/kerker00/SpaceState/blob/master/docs/PROJECT.md).

## Features

- Any space from the SpaceAPI directory, searchable by name and address; Mainframe Oldenburg is the default.
- Open, closed or unknown at a glance, with the time of the last change and the space's status message.
- Mainframe's rooms (Space, Radstelle, 3D Lab, Machining) with their own states.
- State is read through [SpacePush](https://github.com/kerker00/SpacePush), with direct SpaceAPI requests as fallback; refreshed every 1 to 15 minutes while the app is visible, and by pulling down.
- English and German; Android 13+ also offers the language per app in the system settings.

Planned: home screen widgets and push notifications.

## Shared code

The app uses [SpaceStateKit](https://github.com/kerker00/SpaceStateKit), the same Swift package as the Apple apps, compiled for Android with the Swift SDK for Android and bridged to Kotlin with swift-java (see SpaceStateKit's `Android/`).

## Build

Requirements: Android Studio (JDK 17+), Android NDK r30, Swift 6.4 with the Swift SDK for Android.

Until `spacestatekit-android` is published, it is built from a sibling checkout of the release in `gradle/libs.versions.toml`:

    git clone git@github.com:kerker00/SpaceStateKit.git ../SpaceStateKit
    git -C ../SpaceStateKit switch --detach 0.3.0
    ./gradlew :app:assembleDebug
    ./gradlew :app:testDebugUnitTest

The first build compiles swift-syntax for swift-java and takes several minutes.

Release builds are signed with the key described in `~/.android/spacestate-release.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), or another file passed with `-PreleaseSigning=<path>`; without it they stay unsigned. `./gradlew :app:assembleRelease` writes one APK per ABI to `app/build/outputs/apk/release/`.

## License

MIT, see [LICENSE.MD](LICENSE.MD).
