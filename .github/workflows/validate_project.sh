#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
fail=0
check() { if "$@"; then :; else echo "FAIL: $*"; fail=1; fi; }
check test -f settings.gradle.kts
check test -f app/build.gradle.kts
check test -f app/src/main/AndroidManifest.xml
check test -x gradlew
check test -f gradle/wrapper/gradle-wrapper.jar
check test -f gradle/wrapper/gradle-wrapper.properties
check test -f .github/workflows/build-apk.yml
check grep -q 'compileSdk = 35' app/build.gradle.kts
check grep -q 'minSdk = 26' app/build.gradle.kts
check grep -q 'targetSdk = 35' app/build.gradle.kts
check grep -q 'MediaStore.Video.Media.IS_PENDING' app/src/main/java/com/movierecap/ai/media/AndroidMediaAdapters.kt
check grep -q 'Movies/Movie Recap AI' app/src/main/java/com/movierecap/ai/media/AndroidMediaAdapters.kt
check grep -q 'Route.EXPORT' app/src/main/java/com/movierecap/ai/ui/MovieRecapApp.kt
check grep -q 'Generate Voice-over' app/src/main/java/com/movierecap/ai/ui/MovieRecapApp.kt
check grep -q 'enum class VoiceProfile' app/src/main/java/com/movierecap/ai/domain/Models.kt
check grep -q 'Recap voice' app/src/main/java/com/movierecap/ai/ui/MovieRecapApp.kt
check grep -q 'voiceProfile' app/src/main/java/com/movierecap/ai/data/ProjectStore.kt
check grep -q 'ExportSettings' app/src/main/java/com/movierecap/ai/domain/ExportModels.kt
check grep -q 'interface VideoRenderer' app/src/main/java/com/movierecap/ai/domain/ExportServices.kt
check grep -q 'interface ExportManager' app/src/main/java/com/movierecap/ai/domain/ExportServices.kt
check grep -q 'assembleDebug' .github/workflows/build-apk.yml
check grep -q 'platforms;android-${ANDROID_PLATFORM}' .github/workflows/build-apk.yml
if grep -RInE '<<<<<<<|=======|>>>>>>>' . --exclude='*.zip' --exclude='validate_project.sh'; then echo 'FAIL: conflict marker found'; fail=1; fi
if grep -q 'uses-permission' app/src/main/AndroidManifest.xml; then echo 'FAIL: unexpected manifest permission; review before release'; fail=1; fi
if grep -RInE 'sk-[A-Za-z0-9]|AIza[0-9A-Za-z_-]{20,}|OPENAI_API_KEY=|GEMINI_API_KEY=' . --exclude='*.zip' --exclude='validate_project.sh'; then echo 'FAIL: possible hard-coded secret found'; fail=1; fi
if (( fail != 0 )); then exit 1; fi
echo 'Movie Recap AI structural validation: PASS'
