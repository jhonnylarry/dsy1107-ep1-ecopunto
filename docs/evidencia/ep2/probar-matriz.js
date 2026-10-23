// Matriz de autorización 200/401/403 contra el API Gateway desplegado.
//
// Uso: iniciar sesión en la app, abrir la consola del navegador (F12 → Console), pegar
// este script y presionar Enter (editar GW si cambió la URL del Gateway). Lee el access
// token de la sesión actual (sessionStorage) y no guarda credenciales.
//
// - Con un usuario con rol ENCARGADO: crea un punto de prueba, lo edita y lo elimina.
// - Con un usuario sin rol: comprueba que crear, editar y eliminar devuelven 403.
// La columna "responde" distingue los rechazos del API Gateway ({"message":...}) de las
// respuestas del backend.

(async () => {
  const GW = 'https://gmm4008ljj.execute-api.us-east-1.amazonaws.com';

  const key = Object.keys(sessionStorage).find((k) => k.toLowerCase().includes('accesstoken'));
  if (!key) {
    console.error('No hay sesión: inicia sesión en la app antes de correr el script.');
    return;
  }

  const token = JSON.parse(sessionStorage.getItem(key)).secret;
  const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  const claims = JSON.parse(atob(payload));
  const tipo = { 'Content-Type': 'application/json' };
  const auth = { ...tipo, Authorization: 'Bearer ' + token };
  const falso = { ...tipo, Authorization: 'Bearer ' + ['aaa', 'bbb', 'ccc'].join('.') };
  const sinToken = { ...tipo };
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
    const cuerpo = await r.clone().text();
    const responde = cuerpo.startsWith('{"message"') ? 'API Gateway' : 'Backend';
    const ok = r.status === esperado ? 'OK' : 'FALLA';
    filas.push({ escenario, esperado, obtenido: r.status, responde, ok });
    return r;
  };

  console.log('Usuario:', claims.preferred_username);
  console.log('Roles:', claims.roles ?? '(ninguno)', '| Scopes:', claims.scp);

  const lista = '/api/puntos-limpios';
  await probar('GET público sin token', 200, '/public/puntos-limpios');
  await probar('GET protegido sin token', 401, lista);
  await probar('GET protegido con token falso', 401, lista, { headers: falso });
  await probar('PUT sin token', 401, lista + '/1', { method: 'PUT', headers: sinToken, body: punto });
  await probar('GET protegido con token válido', 200, lista, { headers: auth });
  await probar('GET reportes (scope read)', 200, lista + '/1/reportes', { headers: auth });

  if (esEncargado) {
    const r = await probar('POST crear (ENCARGADO)', 201, lista, { method: 'POST', headers: auth, body: punto });
    const { id } = await r.json();
    await probar('PUT editar (ENCARGADO)', 200, lista + '/' + id, { method: 'PUT', headers: auth, body: punto });
    await probar('DELETE eliminar (ENCARGADO)', 204, lista + '/' + id, { method: 'DELETE', headers: auth });
    await probar('DELETE id inexistente', 404, lista + '/999999', { method: 'DELETE', headers: auth });
  } else {
    await probar('POST crear sin rol', 403, lista, { method: 'POST', headers: auth, body: punto });
    await probar('PUT editar sin rol', 403, lista + '/1', { method: 'PUT', headers: auth, body: punto });
    await probar('DELETE eliminar sin rol', 403, lista + '/1', { method: 'DELETE', headers: auth });
  }

  console.table(filas);
})();
