#!/usr/bin/env bash
# Regenerates local-repo/org/fossify/commons/<version>/commons-<version>.aar from the upstream
# artifact, with ActivityKt.showModdedAppWarning() replaced by a no-op.
#
# That function shows a "you are using a fake version of the app" dialog whenever the running
# app's package name doesn't start with "org.fossify." (roughly every 50 launches, plus every
# 100th, from BaseSimpleActivity.onCreate) — it's not a tamper/signature check, just an anti-fork
# nag baked into the precompiled library with no application-level override hook. See
# StripModdedWarningVisitorFactory.kt in git history for a build-time (ASM instrumentation)
# alternative that was attempted first; it hit a classloader isolation issue between a custom
# Gradle plugin and this AGP version's Variant API, so this static patch is used instead.
#
# Run this after bumping the `commons` version in gradle/libs.versions.toml.
#
# Usage: tools/patch-fossify-commons/patch.sh [version]

set -euo pipefail

VERSION="${1:-6.1.6}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Locating commons $VERSION in the Gradle cache..."
ORIGINAL_AAR=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.fossify/commons/$VERSION" -name "commons-$VERSION.aar" | head -1)
ORIGINAL_POM=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.fossify/commons/$VERSION" -name "commons-$VERSION.pom" | head -1)

if [[ -z "$ORIGINAL_AAR" || -z "$ORIGINAL_POM" ]]; then
    echo "Could not find commons $VERSION in the Gradle cache." >&2
    echo "Run './gradlew :app:dependencies' once first so Gradle downloads it, then retry." >&2
    exit 1
fi

echo "Found AAR: $ORIGINAL_AAR"

ASM_JAR=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm" -name "asm-*.jar" | sort -V | tail -1)
if [[ -z "$ASM_JAR" ]]; then
    echo "org.ow2.asm:asm not found in the Gradle cache. Add it as a dependency somewhere temporarily," >&2
    echo "run a build to fetch it, then retry." >&2
    exit 1
fi
echo "Using ASM: $ASM_JAR"

echo "Compiling patcher..."
javac -cp "$ASM_JAR" -d "$WORK_DIR" "$SCRIPT_DIR/PatchModdedWarning.java"

echo "Extracting AAR..."
mkdir -p "$WORK_DIR/aar" "$WORK_DIR/classes"
(cd "$WORK_DIR/aar" && unzip -q "$ORIGINAL_AAR")
(cd "$WORK_DIR/classes" && unzip -q "$WORK_DIR/aar/classes.jar")

echo "Patching ActivityKt.class..."
TARGET_CLASS="$WORK_DIR/classes/org/fossify/commons/extensions/ActivityKt.class"
java -cp "$WORK_DIR:$ASM_JAR" PatchModdedWarning "$TARGET_CLASS" "$TARGET_CLASS"

echo "Repackaging..."
(cd "$WORK_DIR/classes" && zip -q -r -X "$WORK_DIR/aar/classes.jar" .)

OUT_DIR="$REPO_ROOT/local-repo/org/fossify/commons/$VERSION"
mkdir -p "$OUT_DIR"
(cd "$WORK_DIR/aar" && zip -q -r -X "$OUT_DIR/commons-$VERSION.aar" .)
cp "$ORIGINAL_POM" "$OUT_DIR/commons-$VERSION.pom"

echo "Done. Wrote $OUT_DIR/commons-$VERSION.aar"
echo "Verify with: javap -c -p -classpath $OUT_DIR/commons-$VERSION.aar org.fossify.commons.extensions.ActivityKt"
