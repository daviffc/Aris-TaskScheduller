
#!/usr/bin/env bash

set -euo pipefail

URL="http://127.0.0.1:8081/workflows"

for attempt in {1..30}; do
    if curl --fail --silent --show-error \
        --connect-timeout 2 \
        --max-time 5 \
        "$URL" > /dev/null 2>&1; then

        echo "Smoke test aprovado"
        echo "Endpoint: $URL"
        exit 0
    fi

    sleep 2
done

echo "Smoke test falhou"
echo "A API de homologacao nao respondeu corretamente"

exit 1
