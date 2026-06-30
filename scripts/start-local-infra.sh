#!/usr/bin/env bash
set -euo pipefail

REDIS_PORT="${REDIS_PORT:-6379}"
CHROMA_PORT="${CHROMA_PORT:-8000}"
REDIS_CONTAINER="${REDIS_CONTAINER:-redis}"
CHROMA_CONTAINER="${CHROMA_CONTAINER:-chroma}"
OLLAMA_CONTAINER="${OLLAMA_DOCKER_CONTAINER:-psych-agent-ollama-cpu}"
OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://localhost:11434}"

port_open() {
  nc -z 127.0.0.1 "$1" >/dev/null 2>&1
}

start_container_for_port() {
  local label="$1"
  local port="$2"
  local container="$3"

  if port_open "$port"; then
    printf '%s ready on port %s\n' "$label" "$port"
    return
  fi
  if ! docker info >/dev/null 2>&1; then
    printf 'Docker Desktop is not running; cannot start %s.\n' "$label" >&2
    exit 1
  fi
  if ! docker inspect "$container" >/dev/null 2>&1; then
    printf '%s is unavailable and Docker container "%s" does not exist.\n' "$label" "$container" >&2
    exit 1
  fi

  docker start "$container" >/dev/null
  for _ in {1..20}; do
    if port_open "$port"; then
      printf '%s container started on port %s\n' "$label" "$port"
      return
    fi
    sleep 1
  done
  printf '%s container started but port %s is not ready.\n' "$label" "$port" >&2
  exit 1
}

if ! port_open 3306; then
  printf 'MySQL is not listening on port 3306. Start MySQL before the backend.\n' >&2
  exit 1
fi

start_container_for_port "Redis" "$REDIS_PORT" "$REDIS_CONTAINER"
start_container_for_port "Chroma" "$CHROMA_PORT" "$CHROMA_CONTAINER"

if ! curl --fail --silent --show-error "http://127.0.0.1:${CHROMA_PORT}/api/v2/heartbeat" >/dev/null; then
  printf 'Chroma is listening but its heartbeat check failed.\n' >&2
  exit 1
fi

OLLAMA_PORT="${OLLAMA_BASE_URL##*:}"
if [[ "$OLLAMA_PORT" =~ ^[0-9]+$ ]] && ! port_open "$OLLAMA_PORT"; then
  if docker info >/dev/null 2>&1 && docker inspect "$OLLAMA_CONTAINER" >/dev/null 2>&1; then
    docker start "$OLLAMA_CONTAINER" >/dev/null
    for _ in {1..30}; do
      if port_open "$OLLAMA_PORT"; then
        printf 'Ollama container started on port %s\n' "$OLLAMA_PORT"
        break
      fi
      sleep 1
    done
    if ! port_open "$OLLAMA_PORT"; then
      printf 'Warning: Ollama container did not become ready on port %s.\n' "$OLLAMA_PORT" >&2
    fi
  else
    printf 'Warning: Ollama is not listening at %s; AI chat will be unavailable.\n' "$OLLAMA_BASE_URL" >&2
  fi
fi
