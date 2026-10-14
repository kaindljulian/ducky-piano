![Ducky Piano](android/play/feature-graphic.png)

Ducky Piano is a tile tapping game that generates its levels from whatever music (or audio in general) is playing on the
phone. It's built
with [libGDX](https://libgdx.com/) and consists of three Gradle projects:

- `core`, the shared code. Game mechanics and music analysis.
- `lwjgl3`, the desktop build, used for development and generatign assets.
- `android`, the android specific APIs and build scripts. Utilizing the
  [AudioPlaybackCapture API](https://developer.android.com/media/platform/av-capture#capture_audio_playback).

# Development setup

You need JDK 21. For the Android build you also need the Android SDK with platform 36. Point Gradle at the SDK in
`local.properties`.

```properties
sdk.dir=~/Android/Sdk
```

Run the unit tests with:

```bash
./gradlew core:test
```

## Desktop

The desktop build can't capture other apps, so it plays a WAV file and makes tiles from that. Pass the file as an
argument, relative to the project root.

```bash
./gradlew lwjgl3:run --args="core/src/test/resources/piano-loop-120-bpm.wav"
```

Without a file the game runs, but the board stays on "Listening..." state.

To build a runnable jar, which lands in `lwjgl3/build/libs/`:

```bash
./gradlew lwjgl3:jar
```

## Android

Connect a phone with USB debugging turned on, or start an emulator. Then install and launch the debug build:

```bash
./gradlew android:installDebug
./gradlew android:run
```

Audio capture needs Android 10 (API 29) or later. Start a song in another app and
the tiles should generate. Some apps block capture, so if nothing happens, check if it blocks the AudioPlaybackCapture
API.

# Release steps

1. **Set up signing once.** Release builds are signed with the upload key defined in
   `~/.gradle/gradle.properties`:

   ```properties
   duckyPianoUploadStoreFile=~/.android/ducky-piano-upload.jks
   duckyPianoUploadStorePassword=...
   duckyPianoUploadKeyAlias=upload
   duckyPianoUploadKeyPassword=...
   ```

2. **Raise the version.** In `android/build.gradle.kts`, increase `versionCode` by one and update `versionName`. Google
   Play refuses a version code it has already seen.

3. **Build and check the bundle.**

   ```bash
   ./gradlew core:test android:lint android:bundleRelease
   jarsigner -verify android/build/outputs/bundle/release/android-release.aab
   ```

   The bundle is `android/build/outputs/bundle/release/android-release.aab`

4. **Update the store listing if anything changed.** Everything for the listing is in `android/play/`:

    - `./gradlew lwjgl3:exportIcons`
    - `./gradlew lwjgl3:screenshots`
    - `text/` holds the short and full description.

5. **Upload** the `.aab` in the Play Console as a new release, then add release notes.
