# Movie Recap AI — Android Step 5

Movie Recap AI is a modular Android foundation for creating recap videos from content supplied by the user. Steps 1–4 remain intact. Step 5 adds release validation, a Gradle 8.9 wrapper, and a GitHub Actions workflow that builds a debug APK in the cloud without requiring a local computer.

## Step 4 workflow

```text
Home
→ Create New Recap
→ Select Video
→ Analyze Video
→ Generate Recap Script
→ Edit Script
→ Generate Voice-over
→ Create Voice Timestamps
→ Match Scenes
→ Generate Subtitles
→ Synchronize Audio/Video
→ Final Preview
→ Export Settings
→ Render MP4
→ Save to Gallery
→ Share
```

## Implemented

- Final export screen with:
  - Video preview
  - Selected aspect ratio
  - Subtitle status
  - Estimated output duration
  - Audio controls
  - Export quality controls
  - Resolution controls
- Aspect ratios:
  - 9:16 vertical
  - 16:9 landscape
  - 1:1 square
- Quality choices:
  - Standard
  - High
  - Maximum
- Resolution choices:
  - Auto
  - 720p
  - 1080p
- Social presets:
  - Vertical Short Video
  - Landscape Video
  - Square Video
- Render progress stages:
  - Preparing video
  - Processing scenes
  - Mixing audio
  - Rendering subtitles
  - Encoding video
  - Finalizing file
  - Saving video
- Safe cancellation and retry state without deleting the project.
- Export history model with filename, URI, duration, resolution, aspect ratio, quality, date, and gallery-saved state.
- Persistent export history via `ExportStore`.
- Modular Step 4 interfaces:
  - `VideoRenderer`
  - `AudioMixer`
  - `SubtitleRenderer`
  - `ExportManager`
  - `GallerySaver`
  - `ShareManager`
  - `ExportOpener`
- `ModularExportManager` orchestration boundary that consumes the synchronized `RenderPlan`, reports stages, asks the renderer to encode, and passes the result to `MediaStoreGallerySaver`.
- Existing `MediaStoreGallerySaver` writes to `Movies/Movie Recap AI` using scoped MediaStore storage.
- Existing Android share adapter uses the standard chooser with `video/mp4` and URI read permission.

## Renderer integration status

The project deliberately keeps the rendering engine replaceable. The sandbox used for this implementation does not include an Android SDK/Gradle toolchain, and a full production renderer requires a concrete Android media engine plus real generated narration/music assets. `ModularExportManager` is therefore the safe orchestration layer; it reports a clear renderer-configuration error rather than pretending to produce a valid MP4.

To complete production MP4 encoding, inject a `VideoRenderer` implementation backed by one of:

- AndroidX Media3 Transformer for Android-native composition/transcoding.
- FFmpegKit or another appropriately licensed Android FFmpeg integration for advanced filter graphs, audio ducking, subtitle burn-in, crop/fit, and mixed tracks.

The renderer must consume `RenderPlan` absolute timestamps, avoid cumulative duration arithmetic, select crop/fit behavior from `AspectRatio`, avoid upscaling below source resolution, and clean temporary files on success/failure/cancellation.

## Gallery and sharing

`MediaStoreGallerySaver` uses:

- `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`
- MIME type `video/mp4`
- Relative path `Movies/Movie Recap AI`
- `IS_PENDING` while the file is being written

This avoids broad storage access on modern Android versions. The standard Android chooser is used for sharing. The app does not claim that every social platform supports direct upload; compatible installed apps receive the MP4 URI through Android sharing.

## API keys and costs

No API key is required for the project UI, timeline editing, export settings, progress UI, or persistence.

For production AI generation/TTS, configure credentials only on a secure backend or approved developer secret store:

- `OPENAI_API_KEY` — script generation backend where applicable.
- `GEMINI_API_KEY` or server-side Google credentials — Gemini/Google Cloud TTS backend where applicable.

Never embed provider keys in the APK or commit them to Git. External AI/TTS providers may incur usage costs.

## Architecture

```text
app/src/main/java/com/movierecap/ai/
├── data/
│   ├── AppRepository.kt        # project/settings/export state boundary
│   ├── AppViewModel.kt         # recap + render progress and actions
│   ├── ProjectStore.kt         # Step 1–3 project persistence
│   └── ExportStore.kt          # Step 4 export history persistence
├── domain/
│   ├── Models.kt               # project/export/settings records
│   ├── RecapModels.kt          # script and draft state
│   ├── SyncModels.kt           # narration, subtitles, mix, timeline
│   ├── ExportModels.kt         # presets, quality, resolution, progress
│   ├── ExportServices.kt       # renderer/mixer/subtitle/export boundaries
│   ├── Step3Services.kt        # TTS, timestamps, sync, preview boundaries
│   └── ProcessingModules.kt    # legacy-compatible media contracts
├── media/
│   └── AndroidMediaAdapters.kt # MediaStore gallery + Android share adapter
├── ui/
│   ├── Theme.kt
│   └── MovieRecapApp.kt         # Compose screens and workflows
└── MainActivity.kt
```

## Validation

The source tree was checked for:

- Step 1–3 references preserved
- Export model/service references present
- MediaStore and share adapters retained
- No merge conflict markers
- No broad storage permission added
- Export settings and history types connected to repository/UI state

Android compilation must be run in Android Studio Ladybug or newer with Android SDK Platform 35 and build tools installed.

## Copyright and protected content

The app processes only local videos supplied by the user. Use content the user owns or has permission to use. There is no DRM bypass, protected-content downloader, or protected-stream scraping logic.

## Step 5 release and validation checklist

### Open and prepare the project

1. Open `/home/ubuntu/MovieRecapAI` in Android Studio **Ladybug or newer**.
2. In **SDK Manager**, install:
   - Android SDK Platform 35
   - Android SDK Build-Tools for API 35
   - Android SDK Platform-Tools
3. Accept any Gradle/Android plugin sync prompts.
4. Click **File → Sync Project with Gradle Files**.
5. Confirm `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, Kotlin 2.0.21, AGP 8.7.3, and Java 17.

### Configure API keys

No API key is required for the local UI, project management, timeline editing, export settings, or structural validation. For production AI/TTS, configure `OPENAI_API_KEY` or `GEMINI_API_KEY` only on a secure backend/developer secret store. Never place these values in `local.properties`, source files, resources, Git, or the APK.

### Run on a phone

1. Enable Developer Options and USB debugging on an Android API 26+ phone, or create an API 35 emulator.
2. Select the device in Android Studio.
3. Choose the `app` run configuration.
4. Click **Run**.
5. Test: video picker → preview → analysis → script → voice/timeline → subtitles/audio controls → export settings → render progress → gallery/share adapters.

### Build Debug APK

In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**. The expected output is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK

1. Select **Build → Generate Signed App Bundle / APK**.
2. Choose **APK**.
3. Create or select a keystore. Keep it outside Git and back it up securely.
4. Choose the `release` variant.
5. The expected output is:

```text
app/release/app-release.apk
```

### Build Release AAB

1. Select **Build → Generate Signed App Bundle / APK**.
2. Choose **Android App Bundle**.
3. Select the `release` variant and a protected keystore.
4. The expected output is:

```text
app/release/app-release.aab
```

The sandbox used for this task has no Android SDK, Gradle executable, or Kotlin compiler, so no APK/AAB build is claimed here. Structural checks were performed instead.

### Step 5 validation notes

- AndroidManifest declares no unnecessary broad storage permission.
- `OpenDocument` is used for user-selected videos and persisted URI access is attempted safely.
- MediaStore uses `IS_PENDING`, the Movies collection, and cleans failed inserts.
- Android sharing uses the standard chooser and grants URI read permission.
- Empty video/script states are guarded with clear messages.
- Missing provider configuration remains non-fatal.
- Render cancellation preserves the project and clears/retries progress safely.
- Export failures are represented as retryable state rather than deleting project data.
- The renderer boundary remains explicit: connect Media3 Transformer or a properly licensed FFmpeg implementation before claiming production MP4 output.

## Cloud Android APK build from an Android phone

This repository includes `.github/workflows/build-apk.yml`. It builds in GitHub's cloud and does not require Android Studio, a local Android SDK, or a computer.

### 1. Upload the project to GitHub from a phone

1. Open the GitHub mobile app or `github.com` in a mobile browser and sign in.
2. Create a new repository, for example `MovieRecapAI`.
3. Download and extract `MovieRecapAI-Step5.zip` on the phone using a file manager that supports ZIP files.
4. Open the new GitHub repository and choose **Add file → Upload files**.
5. Upload the **contents** of the extracted `MovieRecapAI` folder, including:
   - `.github/workflows/build-apk.yml`
   - `gradlew`
   - `gradle/wrapper/gradle-wrapper.jar`
   - `gradle/wrapper/gradle-wrapper.properties`
   - `settings.gradle.kts`
   - `build.gradle.kts`
   - `app/`
6. If the mobile uploader hides dotfiles, use the GitHub web upload flow in the browser and confirm that `.github` is present. The workflow file must be located exactly at `.github/workflows/build-apk.yml`.
7. Commit the files to the `main` branch.

The project Gradle wrapper is pinned to **Gradle 8.9**, compatible with the existing Android Gradle Plugin 8.7.3. The workflow installs JDK 17, Android SDK Platform 35, and Build Tools 35.0.0. No API keys or signing secrets are included.

### 2. Start the cloud build

1. Open the repository's **Actions** tab.
2. Select **Build Movie Recap AI APK**.
3. Tap **Run workflow**.
4. Select the `main` branch and confirm **Run workflow**.
5. The `build-debug-apk` job runs validation and creates the debug APK.

The workflow also runs automatically when code is pushed to `main` or `master`, and for pull requests targeting those branches.

### 3. Find and download the generated APK

1. Wait for the workflow run to show a green checkmark.
2. Open the completed workflow run.
3. Scroll to the **Artifacts** section.
4. Tap **movie-recap-ai-debug-apk**.
5. GitHub downloads a ZIP file containing `app-debug.apk`.
6. Extract the ZIP on the Android phone.

The artifact is retained by GitHub for 14 days by this workflow.

### 4. Install the APK on the Android phone

1. Open the extracted `app-debug.apk` in the phone's Files app.
2. If Android asks, enable **Install unknown apps** for the Files app or browser that opened the APK.
3. Return to the APK and tap **Install**.
4. Launch **Movie Recap AI** after installation.

A debug APK may show an Android warning because it is not distributed through Google Play. Only install APKs from a repository and workflow you trust.

### 5. Release APK behavior

Release signing is intentionally **not configured**. The workflow does not create fake signing credentials or commit keystores. The optional `build-release-apk` job runs only when the repository variable `RELEASE_SIGNING_CONFIGURED` is set to `true` and a real signing configuration/secrets have been added by the project owner. Debug APK building remains available without signing setup.

To add release signing later, use GitHub Actions secrets for the keystore and passwords, configure the release signing block without committing secret values, then set the repository variable `RELEASE_SIGNING_CONFIGURED=true`. Never upload a keystore or password to the repository.

### Cloud build files

```text
.github/workflows/build-apk.yml  # GitHub Actions workflow
gradlew                           # Gradle wrapper entry point
gradle/wrapper/                    # Gradle 8.9 wrapper JAR and properties
```

## Recap voice selection

Recap projects now support selectable narration voice profiles. The selected profile is available in **Create New Recap → Recap voice** and in **Settings → Recap voice**.

Available profiles:

- Storyteller Male — warm and cinematic
- Storyteller Female — warm and cinematic
- Deep Male — low and dramatic
- Calm Female — soft and clear
- Energetic Male — fast and exciting
- Bright Female — friendly and lively

Changing the voice after a voice-over was generated clears the generated voice state safely and asks the user to generate the voice-over again. The selected profile is saved with the project and restored when the project is reopened. New projects use the default profile from Settings.

The TTS boundary already accepts a provider voice ID through `TtsService.synthesize(..., voice = ...)`. Current Step 5 uses the offline workflow placeholder; a production TTS backend should map `VoiceProfile.providerVoiceId` to the selected provider's supported voice for the selected language. Voice names and availability can vary by provider, so the backend must validate the combination and return a clear error when unsupported.

## GitHub Mobile ဖြင့် APK ထုတ်ရန် မြန်မာလို အဆင့်များ

ဒီ project ကို phone တစ်လုံးတည်းဖြင့် APK ထုတ်နိုင်ပါသည်။ Android Studio သို့မဟုတ် computer မလိုပါ။ GitHub Mobile app တွင် workflow run ခလုတ်မပေါ်ပါက Chrome/Safari mobile browser ဖြင့် `github.com` ကို ဖွင့်ပြီး Desktop site မလိုဘဲ ဆက်လုပ်နိုင်ပါသည်။

### GitHub သို့ project တင်ခြင်း

1. GitHub Mobile သို့မဟုတ် mobile browser ဖြင့် GitHub account ဝင်ပါ။
2. Repository အသစ်တစ်ခု ဖန်တီးပါ။ ဥပမာ `MovieRecapAI`။
3. `MovieRecapAI-Step5-cloud-build.zip` ကို phone ထဲ download လုပ်ပြီး extract လုပ်ပါ။
4. Repository ထဲတွင် **Add file → Upload files** ကိုနှိပ်ပါ။
5. Extract လုပ်ထားသော `MovieRecapAI` folder အတွင်းရှိ file/folder အားလုံးကို upload လုပ်ပါ။ အထူးသဖြင့် `.github/workflows/build-apk.yml` ပါရမည်။
6. Commit message ထည့်ပြီး **Commit changes** ကိုနှိပ်ပါ။ Default branch ကို `main` ထားပါ။

### Cloud build စတင်ခြင်း

1. Repository ထဲရှိ **Actions** tab ကိုဖွင့်ပါ။
2. **Build Movie Recap AI APK** workflow ကိုရွေးပါ။
3. **Run workflow** ကိုနှိပ်ပါ။
4. Branch အဖြစ် `main` ကိုရွေးပြီး **Run workflow** ကို ထပ်နှိပ်ပါ။
5. GitHub cloud runner က JDK 17၊ Android SDK Platform 35၊ Build Tools 35.0.0 နှင့် Gradle wrapper ကို အလိုအလျောက်ပြင်ဆင်ပြီး debug APK ထုတ်ပါမည်။

### APK download လုပ်ခြင်း

Build အောင်မြင်ပြီး အစိမ်းရောင် check ပြပါက workflow run ကိုဖွင့်ပါ။ အောက်ဘက်ရှိ **Artifacts** မှ `movie-recap-ai-debug-apk` ကိုနှိပ်ပြီး ZIP ကို download လုပ်ပါ။ ZIP ကို extract လုပ်ပြီး `app-debug.apk` ကို ရပါမည်။

### Phone တွင် install လုပ်ခြင်း

`app-debug.apk` ကို Files app ဖြင့်ဖွင့်ပါ။ Android က ခွင့်ပြုချက်တောင်းပါက Files app သို့မဟုတ် browser အတွက် **Install unknown apps** ကို ခွင့်ပြုပါ။ ထို့နောက် APK ကို ပြန်ဖွင့်ပြီး **Install** ကိုနှိပ်ပါ။

### မအောင်မြင်ပါက စစ်ရန်

Workflow file သည် repository root အောက်ရှိ `.github/workflows/build-apk.yml` တွင် တိတိကျကျရှိရမည်။ `.github` folder ကို mobile uploader က မမြင်ပါက GitHub website ရှိ **Add file → Upload files** ကို browser ဖြင့်သုံးပါ။ Actions run log ထဲတွင် error အကြောင်းရင်းကိုကြည့်ပြီး **Re-run failed jobs** ကိုနှိပ်နိုင်ပါသည်။ API key သို့မဟုတ် signing key မလိုဘဲ debug APK ထုတ်နိုင်ပါသည်။
