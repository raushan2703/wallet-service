#!/bin/sh
# Maven Wrapper script
# This script downloads and runs Maven if not already available

set -e

MAVEN_WRAPPER_PROPERTIES="$(dirname "$0")/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    MAVEN_URL=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

MAVEN_URL="${MAVEN_URL:-https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip}"
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-3.9.6"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven..."
    mkdir -p "$MAVEN_HOME"
    TMP_FILE=$(mktemp)
    curl -sL "$MAVEN_URL" -o "$TMP_FILE"
    unzip -q "$TMP_FILE" -d "$MAVEN_HOME"
    rm "$TMP_FILE"
fi

MAVEN_BIN=$(find "$MAVEN_HOME" -name 'mvn' -path '*/bin/mvn' | head -1)

if [ -z "$MAVEN_BIN" ]; then
    echo "Error: Maven binary not found after download"
    exit 1
fi

exec "$MAVEN_BIN" "$@"
