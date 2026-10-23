#!/usr/bin/env bash
# Pruebas de punta a punta contra el stack local (docker compose) con JWT de prueba.
J="$(cd "$(dirname "$0")" && pwd)"; M(){ python3 "$J/mint.py" "$@"; }
B=http://localhost:8080; E=http://localhost:8082; T=http://localhost:8083; A=http://localhost:8084
VEC=$(M vecino@ejemplo.cl); ENC=$(M encargado1@ecopunto.cl ENCARGADO); ADM=$(M admin@ecopunto.cl ADMIN)
SOLO_READ=$(M lector@ejemplo.cl "" "reportes.read")
EXP=$(TTL=-120 M vecino@ejemplo.cl)
OTRA=$(openssl genrsa 2048 2>/dev/null > "$J/otra.pem"; H=$(echo "$VEC" | cut -d. -f1-2); echo "$H.$(printf '%s' "$H" | openssl dgst -sha256 -sign "$J/otra.pem" | base64 | tr '+/' '-_' | tr -d '=\n')")
AUD=$(echo "$VEC" | cut -d. -f2 | tr '_-' '/+' | base64 -D 2>/dev/null)
PASS=0; FAIL=0; echo "| # | Prueba | Esperado | Obtenido | |"; echo "|---|---|---|---|---|"; N=0
t(){ local nombre="$1" esp="$2"; shift 2; N=$((N+1)); local c; c=$(curl -s -o /tmp/resp.json -w "%{http_code}" "$@"); local ok="✅"; [[ ",$esp," == *",$c,"* ]] || { ok="❌"; FAIL=$((FAIL+1)); } ; [ "$ok" = "✅" ] && PASS=$((PASS+1)); echo "| $N | $nombre | $esp | $c | $ok |"; }
j=(-H 'Content-Type: application/json')
echo "| | **Seguridad (EP1/EP2)** | | | |"
t "GET público sin token" 200 $B/public/puntos-limpios
t "GET /api sin token" 401 $B/api/puntos-limpios
t "GET /api con token basura" 401 $B/api/puntos-limpios -H "Authorization: Bearer abc.def.ghi"
t "GET /api con firma de otra clave" 401 $B/api/puntos-limpios -H "Authorization: Bearer $OTRA"
t "GET /api con token vencido" 401 $B/api/puntos-limpios -H "Authorization: Bearer $EXP"
t "GET /api con token válido (vecino)" 200 $B/api/puntos-limpios -H "Authorization: Bearer $VEC"
t "GET reportes con scope reportes.read" 200 $B/api/puntos-limpios/1/reportes -H "Authorization: Bearer $SOLO_READ"
t "POST reporte sin scope reportes.write" 403 -X POST $B/api/puntos-limpios/1/reportes -H "Authorization: Bearer $SOLO_READ" "${j[@]}" -d '{"descripcion":"Contenedor lleno"}'
echo "| | **CRUD de puntos limpios (EP2)** | | | |"
t "POST punto como vecino (sin rol)" 403 -X POST $B/api/puntos-limpios -H "Authorization: Bearer $VEC" "${j[@]}" -d '{"nombre":"Punto Prueba","direccion":"Calle 1","comuna":"Santiago","materialesAceptados":"Vidrio"}'
t "POST punto como encargado" 201 -X POST $B/api/puntos-limpios -H "Authorization: Bearer $ENC" "${j[@]}" -d '{"nombre":"Punto Prueba","direccion":"Calle 1","comuna":"Santiago","materialesAceptados":"Vidrio"}'
PID=$(python3 -c "import json;print(json.load(open('/tmp/resp.json'))['id'])" 2>/dev/null)
t "PUT punto como vecino" 403 -X PUT $B/api/puntos-limpios/$PID -H "Authorization: Bearer $VEC" "${j[@]}" -d '{"nombre":"Punto Prueba 2","direccion":"Calle 2","comuna":"Santiago","materialesAceptados":"Vidrio, latas"}'
t "PUT punto como encargado" 200 -X PUT $B/api/puntos-limpios/$PID -H "Authorization: Bearer $ENC" "${j[@]}" -d '{"nombre":"Punto Prueba 2","direccion":"Calle 2","comuna":"Santiago","materialesAceptados":"Vidrio, latas"}'
t "DELETE punto como encargado" 204 -X DELETE $B/api/puntos-limpios/$PID -H "Authorization: Bearer $ENC"
t "DELETE punto inexistente" 404 -X DELETE $B/api/puntos-limpios/99999 -H "Authorization: Bearer $ENC"
echo "| | **Reportes y mensajería (EP3)** | | | |"
t "POST reporte (vecino)" 201 -X POST $B/api/puntos-limpios/1/reportes -H "Authorization: Bearer $VEC" "${j[@]}" -d '{"descripcion":"El contenedor de vidrio esta rebalsado"}'
RID=$(python3 -c "import json;print(json.load(open('/tmp/resp.json'))['id'])" 2>/dev/null)
t "POST reporte con descripción vacía" 400 -X POST $B/api/puntos-limpios/1/reportes -H "Authorization: Bearer $VEC" "${j[@]}" -d '{"descripcion":""}'
t "PATCH estado como vecino" 403 -X PATCH $B/api/reportes/$RID/estado -H "Authorization: Bearer $VEC" "${j[@]}" -d '{"estado":"EN_REVISION"}'
t "PATCH a EN_REVISION (encargado)" 200 -X PATCH $B/api/reportes/$RID/estado -H "Authorization: Bearer $ENC" "${j[@]}" -d '{"estado":"EN_REVISION"}'
t "PATCH transición inválida" 409 -X PATCH $B/api/reportes/$RID/estado -H "Authorization: Bearer $ENC" "${j[@]}" -d '{"estado":"EN_REVISION"}'
t "PATCH a RESUELTO (encargado)" 200 -X PATCH $B/api/reportes/$RID/estado -H "Authorization: Bearer $ENC" "${j[@]}" -d '{"estado":"RESUELTO"}'
sleep 3
t "GET estadísticas (vecino)" 200 $E/api/estadisticas -H "Authorization: Bearer $VEC"
cp /tmp/resp.json /tmp/estadisticas.json
t "GET estadísticas sin token" 401 $E/api/estadisticas
t "GET tickets (encargado)" 200 $T/api/tickets -H "Authorization: Bearer $ENC"
cp /tmp/resp.json /tmp/tickets.json
t "GET tickets (vecino sin rol)" 403 $T/api/tickets -H "Authorization: Bearer $VEC"
echo "| | **Administrador de RabbitMQ (EP3)** | | | |"
t "GET colas como encargado (sin ADMIN)" 403 $A/api/admin/queues -H "Authorization: Bearer $ENC"
t "GET colas como admin" 200 $A/api/admin/queues -H "Authorization: Bearer $ADM"
t "POST cola con nombre amq.x" 400 -X POST $A/api/admin/queues -H "Authorization: Bearer $ADM" "${j[@]}" -d '{"nombre":"amq.x","tipo":"QUORUM","conDlq":true}'
t "POST cola con nombre vacío" 400 -X POST $A/api/admin/queues -H "Authorization: Bearer $ADM" "${j[@]}" -d '{"nombre":"","tipo":"QUORUM"}'
t "POST exchange ecopunto.auditoria" 201 -X POST $A/api/admin/exchanges -H "Authorization: Bearer $ADM" "${j[@]}" -d '{"nombre":"ecopunto.auditoria","tipo":"TOPIC"}'
t "POST cola auditoria.reportes con DLQ" 201 -X POST $A/api/admin/queues -H "Authorization: Bearer $ADM" "${j[@]}" -d '{"nombre":"auditoria.reportes","tipo":"QUORUM","conDlq":true}'
t "POST cola repetida" 409 -X POST $A/api/admin/queues -H "Authorization: Bearer $ADM" "${j[@]}" -d '{"nombre":"auditoria.reportes","tipo":"QUORUM","conDlq":true}'
t "POST binding reporte.#" 201 -X POST $A/api/admin/bindings -H "Authorization: Bearer $ADM" "${j[@]}" -d '{"exchange":"ecopunto.auditoria","cola":"auditoria.reportes","routingKey":"reporte.#"}'
t "POST binding a cola inexistente" 404 -X POST $A/api/admin/bindings -H "Authorization: Bearer $ADM" "${j[@]}" -d '{"exchange":"ecopunto.auditoria","cola":"no.existe","routingKey":"x"}'
t "DELETE cola con consumidor" 409 -X DELETE $A/api/admin/queues/tickets.mantencion -H "Authorization: Bearer $ADM"
t "DELETE binding" 204 -X DELETE $A/api/admin/bindings -H "Authorization: Bearer $ADM" "${j[@]}" -d '{"exchange":"ecopunto.auditoria","cola":"auditoria.reportes","routingKey":"reporte.#"}'
t "DELETE cola auditoria.reportes" 204 -X DELETE $A/api/admin/queues/auditoria.reportes -H "Authorization: Bearer $ADM"
t "DELETE cola auditoria.reportes.dlq" 204 -X DELETE $A/api/admin/queues/auditoria.reportes.dlq -H "Authorization: Bearer $ADM"
t "DELETE exchange ecopunto.auditoria" 204 -X DELETE $A/api/admin/exchanges/ecopunto.auditoria -H "Authorization: Bearer $ADM"
echo; echo "**Resultado: $PASS de $((PASS+FAIL)) pruebas OK, $FAIL fallidas.**"
echo "REPORTE_ID=$RID"
