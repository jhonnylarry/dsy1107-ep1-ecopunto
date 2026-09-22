#!/usr/bin/env python3
"""Actualiza el registro A de Cloudflare con la IP publica de esta instancia EC2.

Variables de entorno (las carga systemd desde /etc/ecopunto-ddns.env):
  CF_API_TOKEN  token de Cloudflare con permiso Zone > DNS > Edit sobre la zona
  CF_ZONE_ID    id de la zona (Overview del dominio en Cloudflare)
  DDNS_RECORD   nombre completo del registro, p. ej. ecopunto.larraguibel.dev
  DDNS_TTL      opcional, segundos (60 por defecto)
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

API = "https://api.cloudflare.com/client/v4"
IMDS = "http://169.254.169.254/latest"

TOKEN = os.environ["CF_API_TOKEN"]
ZONE = os.environ["CF_ZONE_ID"]
RECORD = os.environ["DDNS_RECORD"]
TTL = int(os.environ.get("DDNS_TTL", "60"))


def request(method, url, headers=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    with urllib.request.urlopen(req, timeout=10) as resp:
        return resp.read().decode()


def public_ip():
    token = request("PUT", f"{IMDS}/api/token", {"X-aws-ec2-metadata-token-ttl-seconds": "60"})
    return request("GET", f"{IMDS}/meta-data/public-ipv4", {"X-aws-ec2-metadata-token": token}).strip()


def cloudflare(method, path, body=None):
    headers = {"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"}
    payload = json.loads(request(method, f"{API}{path}", headers, body))
    if not payload.get("success"):
        raise RuntimeError(f"Cloudflare respondio con error: {payload.get('errors')}")
    return payload["result"]


def sync():
    ip = public_ip()
    registro = {"type": "A", "name": RECORD, "content": ip, "ttl": TTL, "proxied": False}
    existentes = cloudflare("GET", f"/zones/{ZONE}/dns_records?type=A&name={RECORD}")

    if not existentes:
        cloudflare("POST", f"/zones/{ZONE}/dns_records", registro)
        print(f"{RECORD}: registro creado -> {ip}")
    elif existentes[0]["content"] != ip or existentes[0].get("proxied"):
        cloudflare("PUT", f"/zones/{ZONE}/dns_records/{existentes[0]['id']}", registro)
        print(f"{RECORD}: {existentes[0]['content']} -> {ip}")
    else:
        print(f"{RECORD}: sin cambios ({ip})")


def main():
    for intento in range(1, 7):
        try:
            sync()
            return 0
        except (urllib.error.URLError, RuntimeError, KeyError, ValueError) as error:
            print(f"intento {intento}/6 fallo: {error}", file=sys.stderr)
            time.sleep(10)
    return 1


if __name__ == "__main__":
    sys.exit(main())
