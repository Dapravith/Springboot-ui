#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${ROOT_DIR}/logs/run-all"

SERVICES=(
  "customer-service"
  "account-service"
  "transfer-service"
  "notification-service"
  "api-gateway"
)
PORTS=(8081 8082 8083 8084 8080)

BUILD=true
START_INFRA=true
STOP_INFRA_ON_EXIT=false
INFRA_STARTED=false
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-120}"
PIDS=()

# BootUI developer diagnostics, passed to every service below.
#
# read-only=false unblocks the advisor scans and every other action-capable
# panel; BootUI otherwise refuses them with
#   BootUI is read-only via bootui.read-only=true
# mcp.enabled=ON serves each service's MCP endpoint for Claude Code.
#
# These are runtime overrides only. The committed application-dev.yml keeps the
# fail-safe defaults (read-only: true, mcp.enabled: OFF), so nothing here can
# leak into another entry point - see docs/bootui-development.md. Set either
# variable to restore the safe value for a run.
BOOTUI_MCP_ENABLED="${BOOTUI_MCP_ENABLED:-ON}"
BOOTUI_READ_ONLY="${BOOTUI_READ_ONLY:-false}"

usage() {
  cat <<'EOF'
Usage: ./run-all.sh [options]

Builds all modules, starts the local infrastructure, and runs every service.

Options:
  --skip-build          Reuse the JARs already present under */build/libs
  --skip-infra          Do not start PostgreSQL, Redis, and Kafka with Docker
  --stop-infra-on-exit  Stop the infrastructure containers when this script exits
  -h, --help            Show this help

Environment:
  STARTUP_TIMEOUT       Seconds to wait for each service (default: 120)
  BOOTUI_MCP_ENABLED    BootUI MCP endpoint: ON or OFF (default: ON)
  BOOTUI_READ_ONLY      Block BootUI actions and scans: true or false
                        (default: false, so the advisor scans can run)
EOF
}

while (($# > 0)); do
  case "$1" in
    --skip-build)
      BUILD=false
      ;;
    --skip-infra)
      START_INFRA=false
      ;;
    --stop-infra-on-exit)
      STOP_INFRA_ON_EXIT=true
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

compose() {
  docker compose \
    --project-directory "$ROOT_DIR" \
    -f "$ROOT_DIR/compose.yml" \
    "$@"
}

cleanup() {
  local pid

  trap - EXIT INT TERM

  if ((${#PIDS[@]} > 0)); then
    echo
    echo "Stopping application services..."
    kill "${PIDS[@]}" 2>/dev/null || true
    for pid in "${PIDS[@]}"; do
      wait "$pid" 2>/dev/null || true
    done
  fi

  if [[ "$INFRA_STARTED" == true && "$STOP_INFRA_ON_EXIT" == true ]]; then
    echo "Stopping PostgreSQL, Redis, and Kafka..."
    compose stop postgres redis kafka >/dev/null || true
  elif [[ "$INFRA_STARTED" == true ]]; then
    echo "Infrastructure is still running. Stop it with:"
    echo "  docker compose -f compose.yml stop"
  fi
}

on_signal() {
  exit 130
}

trap cleanup EXIT
trap on_signal INT TERM

require_command java
require_command curl

if [[ ! -x "$ROOT_DIR/gradlew" ]]; then
  echo "Gradle wrapper is missing or not executable: $ROOT_DIR/gradlew" >&2
  echo "Fix it with: chmod +x '$ROOT_DIR/gradlew'" >&2
  exit 1
fi

if ! [[ "$STARTUP_TIMEOUT" =~ ^[1-9][0-9]*$ ]]; then
  echo "STARTUP_TIMEOUT must be a positive integer." >&2
  exit 2
fi

if command -v lsof >/dev/null 2>&1; then
  for port in "${PORTS[@]}"; do
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN -t >/dev/null 2>&1; then
      echo "Port $port is already in use. Stop that process before running this script." >&2
      exit 1
    fi
  done
fi

cd "$ROOT_DIR"

if [[ "$START_INFRA" == true ]]; then
  require_command docker
  if ! docker info >/dev/null 2>&1; then
    echo "Docker is not running. Start Docker Desktop and try again." >&2
    exit 1
  fi

  echo "Starting PostgreSQL, Redis, and Kafka..."
  compose up -d --wait --wait-timeout "$STARTUP_TIMEOUT"
  INFRA_STARTED=true
fi

if [[ "$BUILD" == true ]]; then
  echo "Building and testing all Gradle modules in developer mode..."
  "$ROOT_DIR/gradlew" clean build -Pdev
fi

mkdir -p "$LOG_DIR"

find_service_jar() {
  local service="$1"
  local jar

  jar="$(find "$ROOT_DIR/$service/build/libs" -maxdepth 1 -type f \
    -name "$service-*.jar" ! -name '*-plain.jar' -print -quit 2>/dev/null || true)"

  if [[ -z "$jar" ]]; then
    echo "No runnable JAR found for $service. Run without --skip-build first." >&2
    exit 1
  fi

  printf '%s\n' "$jar"
}

echo "Starting application services..."
for index in "${!SERVICES[@]}"; do
  service="${SERVICES[$index]}"
  port="${PORTS[$index]}"
  jar="$(find_service_jar "$service")"
  log_file="$LOG_DIR/$service.log"

  : >"$log_file"
  java -Dspring.profiles.active=dev \
       -Dbootui.mcp.enabled="$BOOTUI_MCP_ENABLED" \
       -Dbootui.read-only="$BOOTUI_READ_ONLY" \
       -jar "$jar" >"$log_file" 2>&1 &
  PIDS+=("$!")
  echo "  $service (PID $!, port $port, log: $log_file)"
done

echo "Waiting for health checks..."
for index in "${!SERVICES[@]}"; do
  service="${SERVICES[$index]}"
  port="${PORTS[$index]}"
  pid="${PIDS[$index]}"
  log_file="$LOG_DIR/$service.log"
  deadline=$((SECONDS + STARTUP_TIMEOUT))

  until curl --silent --fail --max-time 2 \
    "http://127.0.0.1:$port/actuator/health" >/dev/null 2>&1; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "$service stopped during startup. Last log lines:" >&2
      tail -n 40 "$log_file" >&2 || true
      exit 1
    fi

    if ((SECONDS >= deadline)); then
      echo "$service did not become healthy within ${STARTUP_TIMEOUT}s." >&2
      echo "Last log lines:" >&2
      tail -n 40 "$log_file" >&2 || true
      exit 1
    fi

    sleep 2
  done

  echo "  READY  $service -> http://localhost:$port"
done

echo
echo "All services are running:"
echo "  API gateway:          http://localhost:8080"
echo "  Customer BootUI:      http://localhost:8081/bootui"
echo "  Account BootUI:       http://localhost:8082/bootui"
echo "  Transfer BootUI:      http://localhost:8083/bootui"
echo "  Notification BootUI:  http://localhost:8084/bootui"
echo "  Logs:                  $LOG_DIR"
echo
echo "Press Ctrl+C to stop all application services."

while true; do
  for index in "${!PIDS[@]}"; do
    pid="${PIDS[$index]}"
    if ! kill -0 "$pid" 2>/dev/null; then
      service="${SERVICES[$index]}"
      echo "$service stopped unexpectedly. See $LOG_DIR/$service.log" >&2
      exit 1
    fi
  done
  sleep 2
done
