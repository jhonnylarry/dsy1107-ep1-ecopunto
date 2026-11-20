#!/bin/bash
# Crea los topics de EcoPunto con la configuración declarada en docker-compose.kafka.yml.
# Es idempotente (--if-not-exists): si el topic ya existe no lo modifica;
# los cambios posteriores de configuración se hacen con ms-kafka-admin o deploy/kafka-admin.sh.
set -euo pipefail
K=/opt/kafka/bin

crear() {
  local nombre=$1 particiones=$2 rf=$3 config=$4
  local args=()
  IFS=',' read -ra pares <<< "$config"
  for par in "${pares[@]}"; do args+=(--config "$par"); done
  echo "→ $nombre (particiones=$particiones, RF=$rf, $config)"
  $K/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$nombre" --partitions "$particiones" --replication-factor "$rf" "${args[@]}"
}

crear "$TOPIC_LECTURAS" "$TOPIC_LECTURAS_PARTICIONES" "$TOPIC_LECTURAS_RF" "$TOPIC_LECTURAS_CONFIG"
crear "$TOPIC_ESTADO"   "$TOPIC_ESTADO_PARTICIONES"   "$TOPIC_ESTADO_RF"   "$TOPIC_ESTADO_CONFIG"
crear "$TOPIC_DLT"      "$TOPIC_DLT_PARTICIONES"      "$TOPIC_DLT_RF"      "$TOPIC_DLT_CONFIG"

echo
$K/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --describe --topic 'ecopunto\..*'
