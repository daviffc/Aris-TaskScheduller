
#!/usr/bin/env bash

set -euo pipefail

COMPOSE=(
    docker compose
    --env-file .env.staging
    -f compose.staging.yml
)

echo "Iniciando deploy de homologacao"

"${COMPOSE[@]}" config --quiet

echo "Baixando imagem publicada no GHCR"

"${COMPOSE[@]}" pull app

echo "Iniciando containers"

"${COMPOSE[@]}" up -d --wait

echo "Executando smoke tests"

bash scripts/smoke-staging.sh

echo "Containers de homologacao"

"${COMPOSE[@]}" ps

echo "Deploy de homologacao concluido"
