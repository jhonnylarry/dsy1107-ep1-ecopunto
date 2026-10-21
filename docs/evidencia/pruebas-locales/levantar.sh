#!/usr/bin/env bash
# Levanta el stack completo en local con un emisor de JWT de PRUEBA (no usa Entra ID).
# Uso: docs/evidencia/pruebas-locales/levantar.sh   y luego   docs/evidencia/pruebas-locales/pruebas.sh
set -e
J="$(cd "$(dirname "$0")" && pwd)"; RAIZ="$(cd "$J/../../.." && pwd)"
[ -f "$J/key.pem" ] || openssl genrsa -out "$J/key.pem" 2048 2>/dev/null
python3 "$J/mint.py" --jwks
cat > "$J/local.env" <<'ENV'
JWT_ISSUER=https://issuer.local.test/v2.0
JWT_JWKS_URI=http://host.docker.internal:9999/jwks.json
JWT_AUDIENCE=ecopunto-local
ALLOWED_ORIGINS=http://localhost:4200
ENV
# Las claves públicas (JWKS) se sirven en el puerto 9999 para que los contenedores las descarguen
lsof -iTCP:9999 -sTCP:LISTEN >/dev/null 2>&1 || (cd "$J" && nohup python3 -m http.server 9999 > /dev/null 2>&1 &)
cd "$RAIZ" && docker compose --env-file "$J/local.env" up -d --build
