// Matriz de autorización 200/401/403 contra el API Gateway desplegado.
//
// Uso: iniciar sesión en la app (https://<dominio-frontend>), abrir la consola del
// navegador (F12 → Console), pegar este script y editar GW si cambió la URL del Gateway.
// Lee el access token de la sesión actual (sessionStorage), no contiene ni guarda
// credenciales, y si el usuario tiene el rol ENCARGADO crea un punto de prueba y lo elimina.

(async () => {
  const GW = 'https://gmm4008ljj.execute-api.us-east-1.amazonaws.com';

  const key = Object.keys(sessionStorage).find((k) => k.toLowerCase().includes('accesstoken'));
  if (!key) {
    console.error('No hay sesión: inicia sesión en la app antes de correr el script.');
    return;
  }

  const token = JSON.parse(sessionStorage.getItem(key)).secret;
  const claims = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
  const auth = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
  const sinToken = { 'Content-Type': 'application/json' };
  const esEncargado = (claims.roles ?? []).includes('ENCARGADO');

  const punto = JSON.stringify({
    nombre: 'Punto de prueba',
    direccion: 'Av. Prueba 123',
    comuna: 'Santiago',
    materialesAceptados: 'Papel, vidrio',
    latitud: -33.45,
    longitud: -70.66,
  });

  const filas = [];
  const probar = async (escenario, esperado, ruta, opciones) => {
    const r = await fetch(GW + ruta, opciones);
    filas.push({ escenario, esperado, obtenido: r.status, ok: r.status === esperado ? 'OK' : 'FALLA' });
    return r;
  };

  console.log('Usuario:', claims.preferred_username, '| roles:', claims.roles ?? '(ninguno)', '| scp:', claims.scp);
  console.log('iss:', claims.iss, '| aud:', claims.aud, '| ver:', claims.ver);

  await probar('GET público sin token', 200, '/public/puntos-limpios');
  await probar('GET protegido sin token', 401, '/api/puntos-limpios');
  await probar('PUT sin token', 401, '/api/puntos-limpios/1', { method: 'PUT', headers: sinToken, body: punto });
  await probar('GET protegido con token', 200, '/api/puntos-limpios', { headers: auth });
  await probar('GET reportes (scope reportes.read)', 200, '/api/puntos-limpios/1/reportes', { headers: auth });

  if (esEncargado) {
    const creado = await probar('POST crear (ENCARGADO)', 201, '/api/puntos-limpios', { method: 'POST', headers: auth, body: punto });
    const { id } = await creado.json();
    await probar('PUT editar (ENCARGADO)', 200, `/api/puntos-limpios/${id}`, { method: 'PUT', headers: auth, body: punto });
    await probar('DELETE eliminar (ENCARGADO)', 204, `/api/puntos-limpios/${id}`, { method: 'DELETE', headers: auth });
    await probar('DELETE id inexistente (ENCARGADO)', 404, '/api/puntos-limpios/999999', { method: 'DELETE', headers: auth });
  } else {
    await probar('POST crear sin rol', 403, '/api/puntos-limpios', { method: 'POST', headers: auth, body: punto });
    await probar('PUT editar sin rol', 403, '/api/puntos-limpios/1', { method: 'PUT', headers: auth, body: punto });
    await probar('DELETE eliminar sin rol', 403, '/api/puntos-limpios/1', { method: 'DELETE', headers: auth });
  }

  console.table(filas);
})();
