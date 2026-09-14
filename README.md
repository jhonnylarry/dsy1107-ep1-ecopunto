# EcoPunto · DSY1107 · Evaluación Parcial 1

**Asignatura:** DSY1107 · Desarrollo Cloud Native I · Sección 002D
**Estudiante:** Jonathan Larraguibel (trabajo individual, autorizado por el docente)
**Evaluación:** EP1 (16%) — base de este mismo proyecto continúa en EP2 (24%)

Sistema para consultar puntos limpios de reciclaje y reportar si un material ya no se puede depositar en un punto específico.

## Arquitectura

```mermaid
flowchart LR
    U[Usuario] --> FE["Angular + MSAL<br/>frontend/"]
    FE -->|"Authorization Code + PKCE"| ENTRA["Microsoft Entra ID"]
    ENTRA -->|"Access Token JWT"| FE
    FE -->|"Bearer token"| GW["AWS API Gateway"]
    GW -->|"Integración HTTP"| BE["Spring Boot · EC2<br/>backend/"]
    BE -->|"valida token"| ENTRA
    BE --> DB[("Base de datos")]
```

## Estado

| Componente | Estado |
|---|---|
| `frontend/` — Angular + MSAL (login, guard, interceptor) | 🟡 En desarrollo |
| `backend/` — Spring Boot Resource Server | 🟡 En desarrollo (compila y corre local con H2, falta Entra real) |
| Tenant / App Registration en Entra ID | ⏳ Pendiente |
| Despliegue en EC2 + API Gateway | ⏳ Pendiente (EP2) |

## Entidades del dominio

```text
PuntoLimpio ── Reporte
```

```text
GET  /public/puntos-limpios            → público
GET  /api/puntos-limpios               → protegido, autenticado
GET  /api/puntos-limpios/{id}/reportes → protegido, scope reportes.read
POST /api/puntos-limpios/{id}/reportes → protegido, scope reportes.write
PUT  /api/puntos-limpios/{id}          → protegido, ROLE_ENCARGADO
```

## Estructura

```text
dsy1107-ep1-ecopunto/
├── README.md
├── frontend/    ← Angular 22 + @azure/msal-angular
└── backend/     ← Spring Boot 4 · Java 21 · Resource Server OAuth2/JWT
```

## Cómo ejecutar el frontend

```bash
cd frontend
npm install
cp src/environments/environment.example.ts src/environments/environment.ts
# completar clientId, tenantId y apiScope con los valores reales del App Registration
npm start
```

## Cómo ejecutar el backend

```bash
cd backend
./mvnw spring-boot:run
```

Levanta en `http://localhost:8080` con base de datos H2 en memoria (se recrea y se
siembra con datos de prueba en cada arranque). Mientras no exista el App
Registration real en Entra ID, `issuer`/`audience`/`jwks-uri` quedan con valores
por defecto que permiten que el backend arranque, pero ningún token real va a
pasar la validación — solo sirve para probar las rutas públicas y el 401 en las
protegidas. Una vez creado el tenant, sobrescribir con variables de entorno:

```bash
export JWT_ISSUER=https://login.microsoftonline.com/<tenantId>/v2.0
export JWT_JWKS_URI=https://login.microsoftonline.com/<tenantId>/discovery/v2.0/keys
export JWT_AUDIENCE=api://<clientId>
```
