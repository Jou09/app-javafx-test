#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="1.0.0"
APP_NAME="app-javafx-test"
INPUT_DIR="$PROJECT_DIR/target/jpackage-input"
OUTPUT_DIR="$PROJECT_DIR/target/dist/kali"
JAR_NAME="gestion-dossiers-desktop-${VERSION}.jar"
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
    printf 'Runtime Java introuvable dans JAVA_HOME=%s\n' "$JAVA_HOME" >&2
    exit 1
fi

cd "$PROJECT_DIR"
mvn clean package

rm -rf "$INPUT_DIR" "$OUTPUT_DIR"
mkdir -p "$INPUT_DIR" "$OUTPUT_DIR/image" "$OUTPUT_DIR/deb"
cp "$PROJECT_DIR/target/$JAR_NAME" "$INPUT_DIR/$JAR_NAME"

COMMON_OPTIONS=(
    --name "$APP_NAME"
    --app-version "$VERSION"
    --vendor "PRMP"
    --input "$INPUT_DIR"
    --main-jar "$JAR_NAME"
    --main-class com.ministere.dossiers.AppMain
    --icon "$PROJECT_DIR/icon/ic_1.png"
    --runtime-image "$JAVA_HOME"
)

jpackage \
    --type app-image \
    --dest "$OUTPUT_DIR/image" \
    "${COMMON_OPTIONS[@]}"

jpackage \
    --type deb \
    --dest "$OUTPUT_DIR/deb" \
    --linux-package-name "$APP_NAME" \
    --linux-app-category Office \
    --linux-menu-group "Office" \
    --linux-shortcut \
    "${COMMON_OPTIONS[@]}"

DEB_PACKAGE="$(find "$OUTPUT_DIR/deb" -maxdepth 1 -type f -name '*.deb' -print -quit)"
if [[ -z "$DEB_PACKAGE" ]]; then
    printf 'jpackage n’a produit aucun paquet Debian.\n' >&2
    exit 1
fi

printf '\nPaquet Debian : %s\n' "$DEB_PACKAGE"
printf 'Application autonome : %s\n' "$OUTPUT_DIR/image/$APP_NAME"
