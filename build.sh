#!/usr/bin/env bash
# Build a signed APK without Gradle: aapt2 -> javac -> d8 -> zip -> zipalign -> apksigner.
# Needs JDK 17+ and the Android SDK (build-tools 34.0.0, platforms;android-34).
# Release: SNOW_KEYSTORE_PASS=... ./build.sh signs with ./release.keystore (created on first use). Keep both private:
# updates must be signed with the same key.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JAVA_HOME:-$HOME/.local/jdk17}
SDK=${ANDROID_HOME:-$HOME/Android/Sdk}
BT=$SDK/build-tools/34.0.0
JAR=$SDK/platforms/android-34/android.jar
B=build
rm -rf "$B/res" "$B/classes" "$B/dex" && mkdir -p "$B/res" "$B/classes" "$B/dex"

"$BT/aapt2" compile --dir res -o "$B/res/res.zip"
"$BT/aapt2" link -o "$B/base.apk" -I "$JAR" --manifest AndroidManifest.xml -A assets \
  --min-sdk-version 24 --target-sdk-version 34 "$B/res/res.zip"
"$JDK/bin/javac" --release 11 -nowarn -cp "$JAR" -d "$B/classes" $(find src -name '*.java')
"$BT/d8" --min-api 24 --lib "$JAR" --output "$B/dex" $(find "$B/classes" -name '*.class')

cp "$B/base.apk" "$B/unsigned.apk"
(cd "$B/dex" && zip -q -j ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 "$B/unsigned.apk" "$B/aligned.apk"

# Release builds: set SNOW_KEYSTORE_PASS (and optionally SNOW_KEYSTORE). Without it you get a throwaway debug key,
# fine for trying things out but never for published APKs.
if [ -n "${SNOW_KEYSTORE_PASS:-}" ]; then KS=${SNOW_KEYSTORE:-release.keystore}; PASS=$SNOW_KEYSTORE_PASS
else KS=debug.keystore; PASS=android; echo "No SNOW_KEYSTORE_PASS: signing with debug.keystore (not for release)"; fi
[ -f "$KS" ] || "$JDK/bin/keytool" -genkeypair -keystore "$KS" -storepass "$PASS" -keypass "$PASS" \
  -alias snow -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Snow Village" >/dev/null 2>&1
JAVA_HOME=$JDK "$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --out "$B/snow-village.apk" "$B/aligned.apk"
ls -la "$B/snow-village.apk"
