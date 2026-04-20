#!/usr/bin/env sh
set -eu

# Allow overrides but provide sane defaults for GitLab CI.
: "${DOCKER_HOST:=tcp://127.0.0.1:2375}"
: "${DOCKER_TLS_CERTDIR:=}"

export DOCKER_HOST
export DOCKER_TLS_CERTDIR

DOCKERD_LOG_FILE="/tmp/dockerd.log"

# Start the local Docker daemon for Testcontainers / Docker CLI usage.
dockerd --host=unix:///var/run/docker.sock --host=tcp://0.0.0.0:2375 >"${DOCKERD_LOG_FILE}" 2>&1 &
DOCKERD_PID=$!

cleanup() {
  if kill -0 "${DOCKERD_PID}" 2>/dev/null; then
    kill "${DOCKERD_PID}" || true
    wait "${DOCKERD_PID}" || true
  fi
}
trap cleanup EXIT INT TERM

attempt=0
until docker info >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "${attempt}" -ge 60 ]; then
    echo "Docker daemon did not become ready in time."
    tail -n 200 "${DOCKERD_LOG_FILE}" || true
    exit 1
  fi
  sleep 1
done

exec "$@"

