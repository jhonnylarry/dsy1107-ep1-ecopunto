#!/bin/bash
# Instala el DNS dinamico en esta instancia. Ejecutar con sudo desde deploy/ddns/.
# Requiere que /etc/ecopunto-ddns.env exista (ver README, seccion "DNS dinamico").
set -euo pipefail

if [ ! -f /etc/ecopunto-ddns.env ]; then
  echo "Falta /etc/ecopunto-ddns.env (CF_API_TOKEN, CF_ZONE_ID, DDNS_RECORD)." >&2
  exit 1
fi

install -m 755 ecopunto-ddns.py /usr/local/bin/ecopunto-ddns
install -m 644 ecopunto-ddns.service ecopunto-ddns.timer /etc/systemd/system/
chmod 600 /etc/ecopunto-ddns.env

systemctl daemon-reload
systemctl enable --now ecopunto-ddns.timer
systemctl start ecopunto-ddns.service
journalctl -u ecopunto-ddns.service -n 5 --no-pager
