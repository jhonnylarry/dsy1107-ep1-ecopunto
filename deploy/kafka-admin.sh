#!/usr/bin/env bash
# EcoPunto · administración de Kafka por línea de comandos (EP5/EP6).
#
# Complementa a ms-kafka-admin (API REST) y a Kafka UI. Se ejecuta en la máquina donde corre el
# stack (EC2 o local) y usa las herramientas de Kafka que vienen dentro de los contenedores de los
# brokers, así que no hay que instalar nada.
#
#   ./deploy/kafka-admin.sh ayuda
#
# Variables opcionales:
#   KAFKA_PREFIJO   prefijo de los contenedores (por defecto ecopunto-kafka)

set -euo pipefail

PREFIJO="${KAFKA_PREFIJO:-ecopunto-kafka}"
BIN="/opt/kafka/bin"
TOPIC_LECTURAS="ecopunto.sensores.lecturas"
TOPIC_ESTADO="ecopunto.contenedores.estado"
TOPIC_DLT="ecopunto.sensores.lecturas.DLT"

azul() { printf '\033[1;34m%s\033[0m\n' "$*"; }
error() { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

# Primer broker que esté corriendo: así el script sigue funcionando con un broker caído.
broker() {
  local n
  for n in 1 2 3; do
    if [ "$(docker inspect -f '{{.State.Running}}' "$PREFIJO-broker-$n" 2>/dev/null)" = "true" ]; then
      echo "$PREFIJO-broker-$n"
      return
    fi
  done
  error "no hay ningún broker corriendo (contenedores $PREFIJO-broker-1..3)"
}

controller() {
  local n
  for n in 1 2 3; do
    if [ "$(docker inspect -f '{{.State.Running}}' "$PREFIJO-controller-$n" 2>/dev/null)" = "true" ]; then
      echo "$PREFIJO-controller-$n"
      return
    fi
  done
  error "no hay ningún controller corriendo (contenedores $PREFIJO-controller-1..3)"
}

# Solo los brokers que están corriendo, para no esperar ni advertir por uno caído.
bootstrap() {
  local n lista=""
  for n in 1 2 3; do
    if [ "$(docker inspect -f '{{.State.Running}}' "$PREFIJO-broker-$n" 2>/dev/null)" = "true" ]; then
      lista="${lista:+$lista,}kafka-broker-$n:9092"
    fi
  done
  echo "$lista"
}

kafka() {
  local herramienta="$1"
  shift
  docker exec "$(broker)" "$BIN/$herramienta" --bootstrap-server "$(bootstrap)" "$@"
}

kafka_stdin() {
  local herramienta="$1"
  shift
  docker exec -i "$(broker)" "$BIN/$herramienta" --bootstrap-server "$(bootstrap)" "$@"
}

numero_broker() {
  [[ "${1:-}" =~ ^[1-3]$ ]] || error "indica el número de broker: 1, 2 o 3"
}

ayuda() {
  cat <<'EOF'
Uso: ./deploy/kafka-admin.sh <comando> [argumentos]

Clúster
  estado                           Quórum KRaft (controller líder y votantes) y contenedores
  topics                           Particiones, líder, réplicas e ISR de los topics ecopunto.*

Topics y configuración
  config <topic>                   Configuración propia del topic
  config <topic> clave=valor,...   Modifica la configuración, ej. retention.ms=86400000
  config-default <topic> clave     Vuelve una clave al valor por defecto del clúster
  crear <topic> [particiones] [rf] Crea un topic (por defecto 3 particiones y 3 réplicas)
  particiones <topic> <total>      Aumenta las particiones (no se pueden reducir)
  eliminar <topic>                 Elimina un topic (no permite los ecopunto.* del sistema)

Mensajes
  leer <topic> [n]                 Últimos mensajes desde el principio, con partición, offset, key y headers
  invalida [json|rango|key]        Publica una lectura defectuosa en ecopunto.sensores.lecturas
                                   para mostrar validación, reintentos y DLT

Consumer groups
  grupos                           Lista los grupos con su estado
  lag [grupo]                      Offset confirmado, offset final y lag por partición (todos los grupos si se omite)
  reset <grupo> <topic> <modo> [--ejecutar]
                                   Reinicia offsets. modo: inicio | fin | -N (retroceder N) | fecha:2026-11-24T10:00:00
                                   Sin --ejecutar solo muestra el plan. El grupo debe estar detenido.

Alta disponibilidad (demo EP6)
  caida <1|2|3>                    Detiene un broker y muestra el nuevo líder y el ISR
  levantar <1|2|3>                 Vuelve a levantar el broker, espera que el ISR se complete y
                                   devuelve cada partición a su líder preferido
EOF
}

estado() {
  azul "Quórum de metadatos (KRaft)"
  docker exec "$(controller)" "$BIN/kafka-metadata-quorum.sh" --bootstrap-controller localhost:9093 describe --status
  echo
  azul "Réplicas del log de metadatos (controllers 1-3 votan, brokers 4-6 observan)"
  docker exec "$(controller)" "$BIN/kafka-metadata-quorum.sh" --bootstrap-controller localhost:9093 describe --replication
  echo
  azul "Contenedores"
  docker ps -a --filter "name=$PREFIJO" --format 'table {{.Names}}\t{{.Status}}'
}

topics() {
  kafka kafka-topics.sh --describe --topic 'ecopunto\..*'
}

config() {
  local topic="${1:-}" cambios="${2:-}"
  [ -n "$topic" ] || error "indica el topic"
  if [ -n "$cambios" ]; then
    kafka kafka-configs.sh --entity-type topics --entity-name "$topic" --alter --add-config "$cambios"
  fi
  kafka kafka-configs.sh --entity-type topics --entity-name "$topic" --describe
}

config_default() {
  local topic="${1:-}" clave="${2:-}"
  [ -n "$topic" ] && [ -n "$clave" ] || error "uso: config-default <topic> <clave>"
  kafka kafka-configs.sh --entity-type topics --entity-name "$topic" --alter --delete-config "$clave"
  kafka kafka-configs.sh --entity-type topics --entity-name "$topic" --describe
}

crear() {
  local topic="${1:-}" particiones="${2:-3}" rf="${3:-3}"
  [ -n "$topic" ] || error "indica el topic"
  kafka kafka-topics.sh --create --topic "$topic" --partitions "$particiones" --replication-factor "$rf" \
    --config min.insync.replicas=$(( rf > 1 ? 2 : 1 ))
  kafka kafka-topics.sh --describe --topic "$topic"
}

particiones() {
  local topic="${1:-}" total="${2:-}"
  [ -n "$topic" ] && [[ "$total" =~ ^[0-9]+$ ]] || error "uso: particiones <topic> <total>"
  kafka kafka-topics.sh --alter --topic "$topic" --partitions "$total"
  kafka kafka-topics.sh --describe --topic "$topic"
}

eliminar() {
  local topic="${1:-}"
  [ -n "$topic" ] || error "indica el topic"
  case "$topic" in
    "$TOPIC_LECTURAS" | "$TOPIC_ESTADO" | "$TOPIC_DLT") error "$topic es un topic del sistema; no se elimina" ;;
  esac
  read -r -p "¿Eliminar $topic y todos sus mensajes? Escribe el nombre para confirmar: " confirmacion
  [ "$confirmacion" = "$topic" ] || error "cancelado"
  kafka kafka-topics.sh --delete --topic "$topic"
  echo "Topic $topic eliminado."
}

leer() {
  local topic="${1:-}" n="${2:-10}"
  [ -n "$topic" ] || error "indica el topic"
  kafka kafka-console-consumer.sh --topic "$topic" --from-beginning --max-messages "$n" --timeout-ms 10000 \
    --property print.partition=true --property print.offset=true --property print.key=true \
    --property print.headers=true --property print.timestamp=true 2>/dev/null || true
}

invalida() {
  local tipo="${1:-rango}" ahora key valor
  ahora="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  key="PL-1-VIDRIO"
  case "$tipo" in
    json)
      valor='esto no es una lectura'
      ;;
    rango)
      valor="{\"lecturaId\":\"demo-$(date +%s)\",\"contenedorId\":\"$key\",\"puntoLimpioId\":1,\"material\":\"VIDRIO\",\"nivelLlenado\":140,\"medidoEn\":\"$ahora\"}"
      ;;
    key)
      valor="{\"lecturaId\":\"demo-$(date +%s)\",\"contenedorId\":\"$key\",\"puntoLimpioId\":1,\"material\":\"VIDRIO\",\"nivelLlenado\":50,\"medidoEn\":\"$ahora\"}"
      key="otra-key"
      ;;
    *) error "tipo desconocido: $tipo (json, rango o key)" ;;
  esac
  printf '%s|%s\n' "$key" "$valor" | kafka_stdin kafka-console-producer.sh --topic "$TOPIC_LECTURAS" \
    --property parse.key=true --property key.separator='|' --producer-property acks=all
  echo "Publicado en $TOPIC_LECTURAS (key=$key): $valor"
  echo
  echo "Qué mirar:"
  echo "  docker logs --tail 20 ecopunto-ms-monitoreo     → [DLT] con el motivo"
  echo "  docker logs --tail 20 ecopunto-ms-estadisticas  → [DESCARTADA] con el motivo"
  echo "  ./deploy/kafka-admin.sh leer $TOPIC_DLT 50      → el mensaje original con headers kafka_dlt-*"
}

grupos() {
  kafka kafka-consumer-groups.sh --list --state
}

lag() {
  local grupo="${1:-}"
  if [ -n "$grupo" ]; then
    kafka kafka-consumer-groups.sh --describe --group "$grupo"
  else
    kafka kafka-consumer-groups.sh --describe --all-groups
  fi
}

reset() {
  local grupo="${1:-}" topic="${2:-}" modo="${3:-}" ejecutar="${4:-}" opcion
  [ -n "$grupo" ] && [ -n "$topic" ] && [ -n "$modo" ] || error "uso: reset <grupo> <topic> <inicio|fin|-N|fecha:...> [--ejecutar]"
  case "$modo" in
    inicio) opcion=(--to-earliest) ;;
    fin) opcion=(--to-latest) ;;
    -[0-9]*) opcion=(--shift-by "$modo") ;;
    fecha:*) opcion=(--to-datetime "${modo#fecha:}.000") ;;
    *) error "modo desconocido: $modo" ;;
  esac
  if [ "$ejecutar" = "--ejecutar" ]; then
    kafka kafka-consumer-groups.sh --group "$grupo" --topic "$topic" --reset-offsets "${opcion[@]}" --execute
  else
    kafka kafka-consumer-groups.sh --group "$grupo" --topic "$topic" --reset-offsets "${opcion[@]}" --dry-run
    echo
    echo "Plan solamente. Para aplicarlo: detener el servicio del grupo (docker stop ...) y repetir con --ejecutar."
  fi
}

caida() {
  numero_broker "${1:-}"
  docker stop "$PREFIJO-broker-$1" >/dev/null
  echo "Broker $1 (node id $(( $1 + 3 ))) detenido. Esperando la elección de nuevos líderes…"
  sleep 8
  topics
  echo
  echo "Con min.insync.replicas=2 y 2 réplicas en el ISR, los productores con acks=all siguen escribiendo."
}

# Verdadero si cada partición ecopunto.* tiene como líder a la primera réplica de su lista.
lideres_preferidos() {
  ! topics 2>/dev/null | awk '/Partition:/ {
      for (i = 1; i <= NF; i++) { if ($i == "Leader:") lider = $(i + 1); if ($i == "Replicas:") { split($(i + 1), r, ","); pref = r[1] } }
      if (lider != pref) { fuera = 1 }
    } END { exit fuera ? 0 : 1 }'
}

levantar() {
  numero_broker "${1:-}"
  docker start "$PREFIJO-broker-$1" >/dev/null
  echo "Broker $1 iniciado. Esperando que vuelva al ISR…"
  local intento
  for intento in $(seq 1 30); do
    if ! kafka kafka-topics.sh --describe --under-replicated-partitions --topic 'ecopunto\..*' 2>/dev/null | grep -q Topic; then
      echo "Todas las particiones tienen sus 3 réplicas al día."
      # Tras la caída los líderes quedaron concentrados en los brokers que siguieron arriba;
      # la elección preferida devuelve cada partición a la primera réplica de su lista.
      # Recién vuelto al ISR el broker puede no aceptar aún el liderazgo: se reintenta.
      local ronda
      for ronda in 1 2 3 4 5; do
        kafka kafka-leader-election.sh --election-type preferred --all-topic-partitions >/dev/null 2>&1 || true
        lideres_preferidos && break
        sleep 2
      done
      topics
      return
    fi
    sleep 2
  done
  echo "Todavía hay particiones sub-replicadas:"
  kafka kafka-topics.sh --describe --under-replicated-partitions --topic 'ecopunto\..*'
}

comando="${1:-ayuda}"
[ $# -gt 0 ] && shift
case "$comando" in
  estado) estado ;;
  topics) topics ;;
  config) config "$@" ;;
  config-default) config_default "$@" ;;
  crear) crear "$@" ;;
  particiones) particiones "$@" ;;
  eliminar) eliminar "$@" ;;
  leer) leer "$@" ;;
  invalida) invalida "$@" ;;
  grupos) grupos ;;
  lag) lag "$@" ;;
  reset) reset "$@" ;;
  caida) caida "$@" ;;
  levantar) levantar "$@" ;;
  ayuda | -h | --help) ayuda ;;
  *) ayuda; exit 1 ;;
esac
