#!/usr/bin/env bash
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.local}"
GRADLE_TASK=":app:installDist"
DIST_DIR="$ROOT_DIR/app/build/install/app"
JAVA_CMD="${JAVA_CMD:-java}"
MAIN_CLASS="${MAIN_CLASS:-com.notegram.MainKt}"

log() {
  printf '[start-local] %s\n' "$*"
}

if [[ -f "$ENV_FILE" ]]; then
  log "Loading environment from $ENV_FILE"
  # shellcheck disable=SC1090
  set -a
  source "$ENV_FILE"
  set +a
else
  log "Environment file $ENV_FILE not found; relying on shell variables"
fi

: "${TELEGRAM_TOKEN:?TELEGRAM_TOKEN must be set}"
: "${ALLOWED_USERS:?ALLOWED_USERS must be set (comma-separated usernames)}"
: "${ASSEMBLYAI_TOKEN:?ASSEMBLYAI_TOKEN must be set}"
: "${GEMINI_TOKEN:?GEMINI_TOKEN must be set}"

ARGS=(
  "--telegram_token" "$TELEGRAM_TOKEN"
  "--allowed_users" "$ALLOWED_USERS"
  "--assemblyai_token" "$ASSEMBLYAI_TOKEN"
  "--gemini_token" "$GEMINI_TOKEN"
)

log "Building distribution via Gradle task $GRADLE_TASK"
"$ROOT_DIR/gradlew" "$GRADLE_TASK"

if [[ ! -d "$DIST_DIR/lib" ]]; then
  log "Distribution directory $DIST_DIR/lib not found after build"
  exit 1
fi

log "Launching bot with $JAVA_CMD (main=$MAIN_CLASS)"
"$JAVA_CMD" -cp "$DIST_DIR/lib/*" "$MAIN_CLASS" "${ARGS[@]}"
