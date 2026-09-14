// Copiar este archivo a environment.ts y completar con los valores reales
// del App Registration creado en Microsoft Entra ID.
// Nunca versionar environment.ts si llega a contener valores reales de producción
// distintos a los de este laboratorio (clientId/tenantId de Entra no son secretos
// por sí solos, pero mantenemos el patrón de configuración separada del código).
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  auth: {
    clientId: 'REEMPLAZAR-CLIENT-ID',
    tenantId: 'REEMPLAZAR-TENANT-ID',
    apiScope: 'api://REEMPLAZAR-API-CLIENT-ID/reportes.write',
  },
};
