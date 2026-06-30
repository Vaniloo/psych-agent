#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="${OLLAMA_DOCKER_CONTAINER:-psych-agent-ollama-cpu}"
HOST_PORT="${OLLAMA_DOCKER_PORT:-11435}"

if ! docker info >/dev/null 2>&1; then
  printf 'Docker Desktop is not running.\n' >&2
  exit 1
fi

if docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  docker start "$CONTAINER_NAME" >/dev/null
else
  docker run -d \
    --name "$CONTAINER_NAME" \
    --restart unless-stopped \
    -p "${HOST_PORT}:11434" \
    -v "$HOME/.ollama:/root/.ollama" \
    ollama/ollama:latest >/dev/null
fi

for _ in {1..60}; do
  if curl --fail --silent --show-error "http://127.0.0.1:${HOST_PORT}/api/tags" >/dev/null; then
    printf 'Docker Ollama ready at http://127.0.0.1:%s\n' "$HOST_PORT"
    exit 0
  fi
  sleep 1
done

printf 'Docker Ollama started but did not become ready on port %s.\n' "$HOST_PORT" >&2
exit 1
