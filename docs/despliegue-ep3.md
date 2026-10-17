# Despliegue EP3/EP4 en AWS + Entra ID

## Despliegue actual

| Recurso | Valor |
|---|---|
| Frontend | https://ecopunto.larraguibel.dev → EC2 `ecopunto-frontend` (`t3.micro`), Elastic IP `34.207.44.149` |
| API Gateway (HTTP API `ecopunto-api`) | https://mld2c6mojh.execute-api.us-east-1.amazonaws.com |
| Backend stack | `ecopunto-api.larraguibel.dev` → EC2 `ecopunto-backend` (`t3.medium`, 30 GB), Elastic IP `34.203.5.9` |
| RabbitMQ Management UI | http://ecopunto-api.larraguibel.dev:15672 (nodo 1) · :15673 (nodo 2) · usuario `ecopunto`, contraseña en el `.env` de la instancia |
| Security Groups | `ecopunto-backend-sg`, `ecopunto-frontend-sg` |

Las dos instancias usan **Elastic IP**: la IP no cambia al reiniciar el laboratorio de AWS Academy, por lo que los registros `A` de Cloudflare se configuran una sola vez y el DNS dinámico (`deploy/ddns/`) queda como alternativa para quien prefiera IPs automáticas. Costo: USD 0,005/h por IP, también con el laboratorio apagado; liberarlas al terminar el semestre (`aws ec2 release-address`).

Cambios respecto al despliegue de EP2 (ver README): el backend ya no corre solo, sino como parte de un stack `docker-compose` con PostgreSQL, el clúster RabbitMQ de 2 nodos y cuatro microservicios.

## 1. Topología en la nube

```text
Navegador ─► https://<dominio-frontend>              EC2 frontend (nginx + Let's Encrypt)
          ─► https://<api-id>.execute-api.<region>.amazonaws.com   API Gateway (HTTP API, JWT authorizer)
                ├─ /public/*, /api/*            ─► http://<dominio-backend>:8080   ecopunto-backend
                ├─ /api/estadisticas            ─► http://<dominio-backend>:8082   ms-estadisticas
                ├─ /api/tickets[/*]             ─► http://<dominio-backend>:8083   ms-tickets
                └─ /api/admin/*                 ─► http://<dominio-backend>:8084   ms-rabbit-admin
EC2 backend: docker compose → postgres, rabbit1, rabbit2, backend, ms-notificaciones,
             ms-estadisticas, ms-tickets, ms-rabbit-admin
```

## 2. EC2 del backend

- **AMI:** Amazon Linux 2023. **Tipo:** `t3.medium` como mínimo (4 GB; el stack usa unos 2,5 GB). **Disco:** 30 GB.
- **Security Group (entrada):**

  | Puerto | Origen | Uso |
  |---|---|---|
  | 22 | 0.0.0.0/0 | SSH (solo con la llave `vockey`) |
  | 8080, 8082, 8083, 8084 | 0.0.0.0/0 | Integraciones del API Gateway |
  | 15672, 15673 | 0.0.0.0/0 | Management UI de cada nodo (demo; laboratorio, IP de origen variable por VPN) |

  PostgreSQL (5432) y AMQP (5672/5673) **no** se abren: solo se usan dentro de la red de Docker.

```bash
# Instalación (una vez)
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user          # cerrar y abrir la sesión SSH
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -sSL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
# buildx: el Docker de Amazon Linux no lo trae y `docker compose build` lo exige
V=$(curl -sSL https://api.github.com/repos/docker/buildx/releases/latest | grep -m1 '"tag_name"' | cut -d'"' -f4)
sudo curl -sSL "https://github.com/docker/buildx/releases/download/$V/buildx-$V.linux-amd64" \
  -o /usr/local/lib/docker/cli-plugins/docker-buildx
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx

# Código y configuración
git clone https://github.com/jhonnylarry/dsy1107-ep1-ecopunto.git && cd dsy1107-ep1-ecopunto
cp .env.example .env && nano .env         # contraseñas, cookie, tenant/clientId, dominio del frontend
chmod 600 .env

# Levantar (construcción secuencial para no agotar la memoria compilando 5 proyectos a la vez)
COMPOSE_PARALLEL_LIMIT=1 docker compose up -d --build
docker compose ps
docker exec ecopunto-rabbit1 rabbitmq-diagnostics cluster_status
```

Generar secretos en el propio servidor:

```bash
openssl rand -hex 24   # POSTGRES_PASSWORD y RABBITMQ_PASSWORD
openssl rand -hex 32   # RABBITMQ_COOKIE
```

## 3. API Gateway (HTTP API)

Rutas (las más específicas tienen prioridad sobre `/api/{proxy+}`). Todas las de `/api` usan el **JWT authorizer** de Entra ID:

| Ruta | Integración |
|---|---|
| `ANY /public/{proxy+}` (sin authorizer) | `http://<dominio-backend>:8080/public/{proxy}` |
| `GET /api/{proxy+}` | `http://<dominio-backend>:8080/api/{proxy}` |
| `POST /api/{proxy+}` | ídem |
| `PUT /api/{proxy+}` | ídem |
| `PATCH /api/{proxy+}` | ídem — **nuevo**, para `PATCH /api/reportes/{id}/estado` |
| `DELETE /api/{proxy+}` | ídem |
| `GET /api/estadisticas` | `http://<dominio-backend>:8082/api/estadisticas` |
| `GET /api/tickets` | `http://<dominio-backend>:8083/api/tickets` |
| `GET /api/tickets/{proxy+}` | `http://<dominio-backend>:8083/api/tickets/{proxy}` |
| `GET /api/admin/{proxy+}` | `http://<dominio-backend>:8084/api/admin/{proxy}` |
| `POST /api/admin/{proxy+}` | ídem |
| `DELETE /api/admin/{proxy+}` | ídem |

**CORS:** origen = dominio del frontend; headers `authorization, content-type`; métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

## 4. Microsoft Entra ID

En el App Registration de la API:

1. **App roles:** además de `ENCARGADO`, crear `ADMIN` (*Allowed member types: Users/Groups*, valor `ADMIN`).
2. **Enterprise applications → la app → Users and groups:** asignar `ADMIN` al usuario que administrará RabbitMQ y `ENCARGADO` al encargado.
3. Redirect URI del frontend: `https://<dominio-frontend>` (sin cambios si se mantiene el dominio).

Los valores de `.env` (`JWT_ISSUER`, `JWT_JWKS_URI`, `JWT_AUDIENCE`) son los mismos para todos los servicios.

## 5. Verificación rápida en producción

```bash
API=https://<api-id>.execute-api.<region>.amazonaws.com
curl -i $API/public/puntos-limpios                                  # 200
curl -i $API/api/estadisticas                                       # 401 (sin token, lo corta el Gateway)
curl -i -H "Authorization: Bearer $TOKEN" $API/api/estadisticas     # 200
curl -i -H "Authorization: Bearer $TOKEN_VECINO" $API/api/tickets   # 403 (falta ENCARGADO)
```

## 6. Frontend (EC2 `t3.micro`)

```bash
# swap de 2 GB: con 1 GB de RAM el build de Angular puede quedarse sin memoria
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile && echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

git clone https://github.com/jhonnylarry/dsy1107-ep1-ecopunto.git && cd dsy1107-ep1-ecopunto/frontend
# environment.ts (no versionado): copia de environment.example.ts con production: true
# y apiBaseUrl = URL del API Gateway
docker build -t ecopunto-frontend .

# certificado (con el registro A ya apuntando a la Elastic IP)
sudo docker run --rm -p 80:80 -v /etc/letsencrypt:/etc/letsencrypt certbot/certbot certonly \
  --standalone -d ecopunto.larraguibel.dev --non-interactive --agree-tos --register-unsafely-without-email
mkdir -p ~/certs && sudo cp -L /etc/letsencrypt/live/ecopunto.larraguibel.dev/{fullchain,privkey}.pem ~/certs/
sudo chown ec2-user ~/certs/*.pem

docker run -d --name ecopunto-frontend --restart unless-stopped -p 80:80 -p 443:443 \
  -v ~/certs:/etc/nginx/certs:ro ecopunto-frontend
```
