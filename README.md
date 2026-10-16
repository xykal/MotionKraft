![MotionKraft](branding/banner.png)

# MotionKraft

Motion graphics and video editor for Android. Built for short-form creators who need keyframe-based editing, beat-synced effects, and compositing control beyond what most mobile editors expose.

This repository contains the Android application source, signing configuration (keystore is never committed), and the release pipeline that produces signed APK and AAB artifacts via GitHub Actions.

---

## Status

This project is in active development. The current build ships a working foundation: multi-track timeline, keyframe animation across six transform channels, beat-aware jedag-jedug presets, color grading, text layers, speed ramps, and export via FFmpeg to H.264/AAC at resolutions up to 4K.

Minimum Android version: 7.0 (API 24)
Target SDK: 34
Architectures: arm64-v8a, armeabi-v7a

---

## Feature Set

### Timeline and Compositing
- Multi-track timeline with video, image, and text clips
- Pinch-zoom timeline ruler
- Playhead scrubbing and playback
- Per-clip trim, split, and speed control (0.1x to 4x)
- Layer ordering by track index
- Export frame accuracy via FFmpeg filter graph

### Animation
- Keyframe channels: position X/Y, scale X/Y, rotation, opacity
- Interpolation modes: linear, ease-in, ease-out, ease-in-out, hold, bounce, elastic
- Keyframe values evaluated at render time; presets always produce editable keyframes rather than burnt-in pixels

### Beat Effects (one-tap presets)
The presets write keyframes and filter instances so you can retime or fine-tune them after applying.

- Zoom Punch
- Heavy Shake
- Strobe Flash
- RGB Glitch
- Whip Pan transition
- Spin 360
- Pulse (BPM-synced scale)
- Earthquake (shake + punch composite)
- Zoom Blur

### Color and Visual Adjustment
- Brightness, contrast, saturation sliders
- Vintage, black-and-white, and teal-orange cinematic LUT filters
- Gaussian blur and outer glow
- Hue rotation (in development)

### Text
- Solid-fill text layers with outline stroke
- Adjustable size and color
- Font loading and per-text animation are scheduled for the next milestone

### Export
- Resolution: 720p / 1080p / 1440p / 2160p
- Frame rate: 24 / 30 / 60 fps
- Bitrate range: 4 to 50 Mbps
- Codec: H.264 video, AAC audio
- Output written to the device Movies directory and registered with MediaStore
- Foreground service with progress notification

---

## Project Structure

```
app/
├── src/main/
│   ├── java/xyz/xyverse/motionkraft/
│   │   ├── data/model/         # Timeline, Clip, Keyframe, Effect, Transform
│   │   ├── editor/             # FFmpeg command builder, export service
│   │   ├── effects/            # Jedag-jedug preset keyframe generators
│   │   ├── presentation/       # ViewModel and Jetpack Compose UI
│   │   └── ui/theme/           # Material 3 dark theme
│   └── res/                    # Drawables, values, manifest, adaptive icon
├── build.gradle.kts
└── proguard-rules.pro

.github/workflows/              # CI/CD build pipeline
branding/                       # Icon source and README banner
keystore/                       # GITIGNORED — release keystore lives in GitHub Secrets
```

---

## Building

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK with platform 34 and NDK (for ffmpeg-kit native libs)

### Debug Build
```
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (Local)
1. Place the release keystore at `keystore/motionkraft-release.jks`
2. Provide signing credentials via environment variables or `~/.gradle/gradle.properties`:
   ```
   SIGNING_STORE_PASSWORD=...
   SIGNING_KEY_PASSWORD=...
   SIGNING_KEY_ALIAS=motionkraft
   ```
3. Run:
   ```
   ./gradlew assembleRelease bundleRelease
   ```

App bundles are generated at `app/build/outputs/bundle/release/`.

---

## Continuous Integration and Release Pipeline

The GitHub Actions workflow at `.github/workflows/android-build.yml` runs on push, tag, pull request, and manual dispatch.

Two jobs exist:

1. **build-debug** — always runs, has no access to secrets, produces an unsigned debug APK. This is the job that runs on pull requests from forks.
2. **build-release** — requires the `production` GitHub Environment. The environment is configured with:
   - Required reviewer: only the repository owner (`xykal`)
   - Deployment branch restriction: `main` and tags matching `v*`
   - Secrets scoped exclusively to this environment

Release secrets configured in the `production` environment:

| Secret | Purpose |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | Base64-encoded PKCS12 release keystore |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_PASSWORD` | Key password (identical to store password for PKCS12) |
| `SIGNING_KEY_ALIAS` | Key alias, fixed as `motionkraft` |

The workflow decodes the keystore at job start, verifies it with `keytool`, builds the signed APK and AAB with R8 and resource shrinking enabled, computes SHA256 checksums, and uploads artifacts. When the build is triggered by a tag matching `v*`, it additionally creates a draft GitHub Release with signed artifacts attached.

Pull requests from forks never receive release secrets. Dependabot is enabled for weekly dependency updates. TruffleHog scans each push for accidentally committed credentials.

---

## Security Model

- The release keystore is never committed to source control. Only the base64-encoded keystore is stored as a GitHub environment secret.
- Release builds require manual approval from the owner before any job that touches signing material runs.
- Fork PRs run in a stripped-down job that cannot read repository secrets and cannot produce signed artifacts.
- `permissions:` block at the workflow level caps GITHUB_TOKEN access.
- `persist-credentials: false` on the release checkout to prevent token leaking to third-party steps.
- R8 obfuscation and repackaging are enabled for release builds. All verbose logging (`Log.v/d/i`) is stripped by ProGuard.
- Cleartext traffic is blocked via network security config.
- Code owners file enforces owner review on changes to workflow, build, and proguard files.

---

## APK Integrity

Release builds are signed with a 4096-bit RSA key valid for 27 years. The signing certificate SHA-256 fingerprint will be published alongside the first public release so that end users can verify APK provenance independently of the app store.

Each release job publishes a `sha256sums.txt` file next to the APK and AAB.

---

## Roadmap

Ordered roughly by priority:

1. Real-time OpenGL preview pipeline so effects and transforms render while scrubbing, not only at export
2. Bezier curve editor for keyframe interpolation
3. Masking and track matte layers
4. Chroma key
5. Beat detection from audio (automated BPM sync for jedag-jedug presets)
6. Motion blur and directional blur per transform
7. Audio waveform on timeline with ducking and voice-over
8. Custom font loading and per-text animation presets
9. Transition library between clips (slide, zoom, morph, glitch)
10. AI-assisted captions and shot detection
11. Project save/load as versioned JSON

---

## License

All rights reserved by XyVerse. No license is granted to the code in this repository at this time; it is published publicly for visibility and distribution only. A permissive or open-source license may be applied in a future release.

---

## Contact

Repository owner: xykal
