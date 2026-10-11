# SpaceState for Android

Shows whether a hackerspace is open – the Android version of [SpaceState](https://github.com/kerker00/SpaceState) for macOS and iOS.

Work in progress: the app currently only runs a smoke test of the shared library. The plan and decisions live in [SpaceState's PROJECT.md](https://github.com/kerker00/SpaceState/blob/master/docs/PROJECT.md).

## Shared code

The app uses [SpaceStateKit](https://github.com/kerker00/SpaceStateKit), the same Swift package as the Apple apps, compiled for Android with the Swift SDK for Android and bridged to Kotlin with swift-java (see SpaceStateKit's `Android/`).

## Build

Requirements: Android Studio (JDK 17+), Android NDK r30, Swift 6.4 with the Swift SDK for Android.

Until `spacestatekit-android` is published, it is built from a sibling checkout:

    git clone git@github.com:kerker00/SpaceStateKit.git ../SpaceStateKit
    git -C ../SpaceStateKit switch feature/android
    ./gradlew :app:assembleDebug

The first build compiles swift-syntax for swift-java and takes several minutes.

## License

MIT.
