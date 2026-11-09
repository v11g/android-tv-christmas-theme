#!/usr/bin/env bash
# Compile and run the Android-free core tests with a plain JDK (17+).
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JAVA_HOME:-$HOME/.local/jdk17}
OUT=build/test-classes
rm -rf "$OUT" && mkdir -p "$OUT"
"$JDK/bin/javac" --release 11 -d "$OUT" src/io/github/snowvillage/core/*.java test/io/github/snowvillage/core/*.java
"$JDK/bin/java" -cp "$OUT" io.github.snowvillage.core.AppListTest
