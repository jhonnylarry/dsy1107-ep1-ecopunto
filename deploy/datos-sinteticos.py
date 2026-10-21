# Carga datos sintéticos de demostración en el stack desplegado (EC2 con docker compose).
# Inserta puntos limpios y reportes en la base del backend y publica en RabbitMQ los mismos
# eventos que publicaría el backend, para que los consumidores generen tickets,
# notificaciones y estadísticas por el flujo real.
#
# Uso, en la carpeta del repo en la instancia:  python3 deploy/datos-sinteticos.py
# Es idempotente: si los reportes sintéticos ya existen, no hace nada.
import base64, json, subprocess, sys, urllib.request, uuid
from datetime import datetime, timedelta, timezone

import os
ENV = dict(l.strip().split('=', 1) for l in open('.env') if '=' in l and not l.startswith('#')) if os.path.exists('.env') else {}
PG_USER = ENV.get('POSTGRES_USER', 'ecopunto')
RABBIT = (ENV.get('RABBITMQ_USER', 'ecopunto'), ENV.get('RABBITMQ_PASSWORD', 'ecopunto'))
ENCARGADO = 'encargado1@ecopunto.cl'

PUNTOS = [
    ('Punto Limpio Las Condes', 'Av. Apoquindo 6400', 'Las Condes', 'Papel, cartón, vidrio, latas, Tetra Pak', -33.4086, -70.5645),
    ('Punto Limpio Maipú', 'Av. Pajaritos 2500', 'Maipú', 'Plástico PET, vidrio, aceite', -33.5110, -70.7580),
    ('Punto Limpio La Florida', 'Av. Vicuña Mackenna 7200', 'La Florida', 'Papel, vidrio, pilas, electrónicos', -33.5225, -70.5985),
    ('Punto Limpio Santiago Centro', 'Av. Matta 650', 'Santiago', 'Papel, cartón, plástico, vidrio', -33.4610, -70.6490),
    ('Punto Limpio Vitacura', 'Av. Bicentenario 3800', 'Vitacura', 'Vidrio, latas, ropa, aceite', -33.3990, -70.5980),
]

# (punto, descripción, autor, estado final, días atrás)
REPORTES = [
    ('Punto Limpio Ñuñoa', 'El contenedor de vidrio está lleno y hay botellas en el suelo', 'camila.rojas@ejemplo.cl', 'RESUELTO', 9),
    ('Punto Limpio Ñuñoa', 'No reciben aceite usado, el bidón está retirado', 'diego.munoz@ejemplo.cl', 'EN_REVISION', 6),
    ('Punto Limpio Providencia', 'Contenedor de cartón rebalsado desde el fin de semana', 'valentina.soto@ejemplo.cl', 'RESUELTO', 8),
    ('Punto Limpio Providencia', 'Falta señalética para separar plásticos', 'matias.perez@ejemplo.cl', 'PENDIENTE', 1),
    ('Punto Limpio Las Condes', 'Tapa del contenedor de latas rota, peligro de corte', 'fernanda.diaz@ejemplo.cl', 'EN_REVISION', 4),
    ('Punto Limpio Las Condes', 'El contenedor de Tetra Pak no se ha vaciado en una semana', 'benjamin.silva@ejemplo.cl', 'RESUELTO', 7),
    ('Punto Limpio Maipú', 'Derrame de aceite junto al contenedor, el piso está resbaladizo', 'antonia.reyes@ejemplo.cl', 'EN_REVISION', 3),
    ('Punto Limpio Maipú', 'Contenedor de PET lleno', 'joaquin.vargas@ejemplo.cl', 'PENDIENTE', 0),
    ('Punto Limpio La Florida', 'Ya no hay contenedor para pilas', 'isidora.castro@ejemplo.cl', 'RESUELTO', 10),
    ('Punto Limpio La Florida', 'Dejaron un televisor afuera del punto limpio', 'tomas.fuentes@ejemplo.cl', 'EN_REVISION', 2),
    ('Punto Limpio Santiago Centro', 'Contenedor de vidrio quebrado, hay vidrios en la vereda', 'martina.lopez@ejemplo.cl', 'RESUELTO', 5),
    ('Punto Limpio Santiago Centro', 'El punto limpio estaba cerrado en horario de atención', 'lucas.morales@ejemplo.cl', 'PENDIENTE', 1),
    ('Punto Limpio Vitacura', 'Contenedor de ropa lleno, bolsas apiladas afuera', 'sofia.herrera@ejemplo.cl', 'EN_REVISION', 2),
    ('Punto Limpio Vitacura', 'Sin bolsas para separar latas', 'agustin.navarro@ejemplo.cl', 'PENDIENTE', 0),
]


def sql(q):
    r = subprocess.run(['docker', 'exec', '-i', 'ecopunto-db', 'psql', '-U', PG_USER, '-d', 'ecopunto', '-At', '-v', 'ON_ERROR_STOP=1'],
                       input=q, capture_output=True, text=True)
    if r.returncode:
        sys.exit('Error de SQL: ' + r.stderr)
    return [l.split('|') for l in r.stdout.strip().splitlines() if l]


def lit(v):
    return 'NULL' if v is None else "'" + str(v).replace("'", "''") + "'"


def publicar(exchange, routing_key, cuerpo, message_id):
    datos = json.dumps({'properties': {'delivery_mode': 2, 'content_type': 'application/json', 'message_id': message_id},
                        'routing_key': routing_key, 'payload': json.dumps(cuerpo, ensure_ascii=False), 'payload_encoding': 'string'}).encode()
    req = urllib.request.Request(f'http://localhost:15672/api/exchanges/%2F/{exchange}/publish', data=datos, method='POST',
                                 headers={'Content-Type': 'application/json',
                                          'Authorization': 'Basic ' + base64.b64encode(':'.join(RABBIT).encode()).decode()})
    if not json.load(urllib.request.urlopen(req)).get('routed'):
        sys.exit(f'El mensaje {routing_key} no llegó a ninguna cola')


def iso(t):
    return t.strftime('%Y-%m-%dT%H:%M:%S.%fZ')


if sql(f"SELECT count(*) FROM reportes WHERE autor_email = {lit(REPORTES[0][2])}")[0][0] != '0':
    print('Los datos sintéticos ya están cargados; no se hace nada.')
    sys.exit()

for nombre, direccion, comuna, materiales, lat, lon in PUNTOS:
    sql(f"INSERT INTO puntos_limpios (nombre, direccion, comuna, materiales_aceptados, latitud, longitud) "
        f"SELECT {lit(nombre)}, {lit(direccion)}, {lit(comuna)}, {lit(materiales)}, {lat}, {lon} "
        f"WHERE NOT EXISTS (SELECT 1 FROM puntos_limpios WHERE nombre = {lit(nombre)});")
puntos = {r[1]: {'id': int(r[0]), 'nombre': r[1], 'comuna': r[2], 'direccion': r[3]}
          for r in sql('SELECT id, nombre, comuna, direccion FROM puntos_limpios;')}

ahora = datetime.now(timezone.utc).replace(microsecond=0)
for i, (punto, descripcion, autor, estado, dias) in enumerate(REPORTES):
    p = puntos[punto]
    creado = ahora - timedelta(days=dias, hours=3 + i % 5, minutes=7 * i)
    rid = int(sql(f"INSERT INTO reportes (punto_limpio_id, descripcion, autor_email, estado, creado_en) VALUES "
                  f"({p['id']}, {lit(descripcion)}, {lit(autor)}, {lit(estado)}, {lit(creado.isoformat())}) RETURNING id;")[0][0])
    base = {'reporteId': rid, 'puntoLimpioId': p['id'], 'puntoNombre': p['nombre'], 'comuna': p['comuna']}

    eid = str(uuid.uuid4())
    publicar('ecopunto.events', 'reporte.creado',
             {'eventId': eid, 'ocurridoEn': iso(creado), **base, 'descripcion': descripcion, 'autorEmail': autor}, eid)
    pasos = {'PENDIENTE': [], 'EN_REVISION': ['EN_REVISION'], 'RESUELTO': ['EN_REVISION', 'RESUELTO']}[estado]
    anterior, t = 'PENDIENTE', creado
    for nuevo in pasos:
        t += timedelta(hours=5)
        eid = str(uuid.uuid4())
        publicar('ecopunto.events', 'reporte.' + nuevo.lower(),
                 {'eventId': eid, 'ocurridoEn': iso(t), **base, 'estadoAnterior': anterior, 'estadoNuevo': nuevo,
                  'autorEmail': autor, 'cambiadoPor': ENCARGADO}, eid)
        if nuevo == 'EN_REVISION':
            mid = str(uuid.uuid4())
            publicar('ecopunto.tareas', 'ticket.mantencion',
                     {'messageId': mid, 'ocurridoEn': iso(t), **base, 'direccion': p['direccion'],
                      'descripcion': descripcion, 'solicitadoPor': ENCARGADO}, mid)
        anterior = nuevo
    print(f'reporte {rid:>3} · {estado:<11} · {p["nombre"]}')

print(f'Listo: {len(PUNTOS)} puntos limpios y {len(REPORTES)} reportes sintéticos.')
