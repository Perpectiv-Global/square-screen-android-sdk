#!/usr/bin/env bash
# publish.sh — stage all modules and upload the bundle to Maven Central Portal.
#
# The vanniktech plugin's built-in HTTP upload (BuildService.close hook) does not
# fire on Gradle 9.0.x. This script replicates what the plugin should do:
#   1. Run the Gradle staging tasks (produces a signed ZIP under build/publish/staging/)
#   2. POST that ZIP to the Central Portal upload API
#
# Required: mavenCentralUsername and mavenCentralPassword in ~/.gradle/gradle.properties
# Optional: set PUBLISHING_TYPE=USER_MANAGED to skip auto-release (default: AUTOMATIC)
set -euo pipefail

GRADLE_PROPS="$HOME/.gradle/gradle.properties"
STAGING_DIR="build/publish/staging"
PUBLISHING_TYPE="${PUBLISHING_TYPE:-AUTOMATIC}"

# ── Credentials ─────────────────────────────────────────────────────────────
MAVEN_USER=$(grep "^mavenCentralUsername=" "$GRADLE_PROPS" | cut -d= -f2)
MAVEN_PASS=$(grep "^mavenCentralPassword=" "$GRADLE_PROPS" | cut -d= -f2)
if [[ -z "$MAVEN_USER" || -z "$MAVEN_PASS" ]]; then
    echo "ERROR: mavenCentralUsername / mavenCentralPassword not found in $GRADLE_PROPS"
    exit 1
fi
TOKEN=$(printf '%s:%s' "$MAVEN_USER" "$MAVEN_PASS" | base64)

# ── Stage ────────────────────────────────────────────────────────────────────
echo "Cleaning staging directory..."
rm -rf "$STAGING_DIR"

echo "Staging release artifacts..."
./gradlew publishAllPublicationsToMavenCentralRepository --no-configuration-cache

# ── Find the ZIP ─────────────────────────────────────────────────────────────
ZIP_FILE=$(ls "$STAGING_DIR"/*.zip 2>/dev/null | head -1)
if [[ -z "$ZIP_FILE" ]]; then
    echo "ERROR: No ZIP found in $STAGING_DIR after staging"
    exit 1
fi
VERSION=$(unzip -p "$ZIP_FILE" "*/maven-metadata.xml" 2>/dev/null | grep -m1 "<version>" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
echo "Uploading: $ZIP_FILE  (version: ${VERSION:-unknown})"

# ── Upload ───────────────────────────────────────────────────────────────────
HTTP_RESPONSE=$(curl -s \
    -o /tmp/central_upload_response.json \
    -w "%{http_code}" \
    -X POST \
    "https://central.sonatype.com/api/v1/publisher/upload?publishingType=${PUBLISHING_TYPE}" \
    -H "Authorization: Bearer $TOKEN" \
    -F "bundle=@${ZIP_FILE}")

BODY=$(cat /tmp/central_upload_response.json)

if [[ "$HTTP_RESPONSE" == "201" ]]; then
    echo ""
    echo "Published successfully."
    echo "Deployment ID: $BODY"
    echo "Track at: https://central.sonatype.com/publishing/deployments"
else
    echo ""
    echo "ERROR: Upload failed (HTTP $HTTP_RESPONSE)"
    echo "Response: $BODY"
    exit 1
fi
