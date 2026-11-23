import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { KeyValuePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { EcopuntoApi } from '../../servicios/ecopunto-api';
import { ClusterInfo, GrupoDetalle, GrupoResumen, ParticionInfo, TopicDetalle, TopicResumen } from '../../servicios/modelos';

/** Mismas claves que acepta ms-kafka-admin (ecopunto.kafka-admin.configuraciones-permitidas). */
const CONFIGURACIONES = [
  'cleanup.policy',
  'retention.ms',
  'retention.bytes',
  'min.insync.replicas',
  'segment.ms',
  'delete.retention.ms',
  'min.cleanable.dirty.ratio',
  'max.message.bytes',
];

const INTERVALO_MS = 5000;

/**
 * Administración de Kafka (rol ADMIN). Toda la lógica vive en ms-kafka-admin (AdminClient);
 * esta página consume su API REST: clúster, topics con su configuración y particiones,
 * y consumer groups con offsets y lag por partición.
 */
@Component({
  selector: 'app-admin-kafka',
  standalone: true,
  imports: [RouterLink, FormsModule, KeyValuePipe],
  templateUrl: './admin-kafka.html',
  styleUrl: './admin-kafka.css',
})
export class AdminKafka implements OnInit, OnDestroy {
  protected readonly configuraciones = CONFIGURACIONES;

  protected readonly cluster = signal<ClusterInfo | null>(null);
  protected readonly topics = signal<TopicResumen[]>([]);
  protected readonly seleccionado = signal<TopicDetalle | null>(null);
  protected readonly grupos = signal<GrupoDetalle[]>([]);
  protected readonly grupoAbierto = signal<string | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly erroresCampo = signal<Record<string, string>>({});
  protected readonly aviso = signal<string | null>(null);
  protected readonly lagEnVivo = signal(true);

  protected readonly topicsPropios = computed(() => this.topics().filter((t) => !t.nombre.startsWith('__')));
  protected readonly lagTotal = computed(() => this.grupos().reduce((suma, g) => suma + g.lagTotal, 0));

  protected nuevoTopic = this.topicVacio();
  protected cambioConfig = { clave: 'retention.ms', valor: '' };
  protected totalParticiones = 4;

  private temporizador?: ReturnType<typeof setInterval>;

  constructor(private readonly api: EcopuntoApi) {}

  ngOnInit(): void {
    this.cargar();
    this.programarLag();
  }

  ngOnDestroy(): void {
    clearInterval(this.temporizador);
  }

  protected cargar(): void {
    forkJoin({ cluster: this.api.clusterKafka(), topics: this.api.topics(), grupos: this.cargarGrupos() }).subscribe({
      next: ({ cluster, topics, grupos }) => {
        this.cluster.set(cluster);
        this.topics.set([...topics].sort((a, b) => a.nombre.localeCompare(b.nombre)));
        this.grupos.set(grupos);
        this.cargando.set(false);
        const actual = this.seleccionado();
        if (actual && topics.some((t) => t.nombre === actual.nombre)) {
          this.verTopic(actual.nombre);
        } else {
          this.seleccionado.set(null);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.mostrarError(err, 'cargar el clúster');
      },
    });
  }

  protected alternarLag(): void {
    this.lagEnVivo.update((v) => !v);
    this.programarLag();
  }

  protected verTopic(nombre: string): void {
    this.api.topic(nombre).subscribe({
      next: (t) => {
        this.seleccionado.set(t);
        this.totalParticiones = t.particiones + 1;
      },
      error: (err: HttpErrorResponse) => this.mostrarError(err, `leer el topic ${nombre}`),
    });
  }

  protected crearTopic(): void {
    const t = this.nuevoTopic;
    const configuracion: Record<string, string> = { 'cleanup.policy': t.cleanupPolicy };
    if (t.retencionHoras) {
      configuracion['retention.ms'] = String(Math.round(t.retencionHoras * 3_600_000));
    }
    if (t.minIsr) {
      configuracion['min.insync.replicas'] = String(t.minIsr);
    }
    this.ejecutar(
      this.api.crearTopic({ nombre: t.nombre.trim(), particiones: t.particiones, replicationFactor: t.rf, configuracion }),
      `Topic "${t.nombre}" creado con ${t.particiones} particiones y factor de replicación ${t.rf}.`,
      (creado) => {
        this.nuevoTopic = this.topicVacio();
        this.seleccionado.set(creado as TopicDetalle);
      });
  }

  protected aplicarConfig(): void {
    const t = this.seleccionado();
    if (!t) {
      return;
    }
    const { clave, valor } = this.cambioConfig;
    this.ejecutar(this.api.modificarConfigTopic(t.nombre, { [clave]: valor.trim() }),
      `${clave} = ${valor} aplicado a "${t.nombre}".`, () => (this.cambioConfig.valor = ''));
  }

  protected restaurarConfig(clave: string): void {
    const t = this.seleccionado();
    if (!t || !confirm(`¿Volver "${clave}" al valor por defecto del clúster en "${t.nombre}"?`)) {
      return;
    }
    this.ejecutar(this.api.modificarConfigTopic(t.nombre, {}, [clave]), `${clave} vuelve al valor por defecto.`);
  }

  protected editarConfig(clave: string, valor: string): void {
    this.cambioConfig = { clave, valor };
  }

  protected aumentarParticiones(): void {
    const t = this.seleccionado();
    if (!t) {
      return;
    }
    if (!confirm(`Las particiones no se pueden reducir y cambia la partición de cada key. ¿Llevar "${t.nombre}" a ${this.totalParticiones}?`)) {
      return;
    }
    this.ejecutar(this.api.aumentarParticiones(t.nombre, this.totalParticiones),
      `"${t.nombre}" ahora tiene ${this.totalParticiones} particiones.`);
  }

  protected eliminarTopic(t: TopicResumen): void {
    if (!confirm(`¿Eliminar el topic "${t.nombre}" y todos sus mensajes? No se puede deshacer.`)) {
      return;
    }
    this.ejecutar(this.api.eliminarTopic(t.nombre), `Topic "${t.nombre}" eliminado.`);
  }

  protected alternarGrupo(grupo: string): void {
    this.grupoAbierto.update((g) => (g === grupo ? null : grupo));
  }

  protected subReplicada(p: ParticionInfo): boolean {
    return p.isr.length < p.replicas.length;
  }

  protected claseGrupo(estado: string): string {
    return estado === 'STABLE' ? 'resuelto' : estado === 'EMPTY' ? 'normal' : 'pendiente';
  }

  private cargarGrupos(): Observable<GrupoDetalle[]> {
    return this.api.gruposConsumidores().pipe(
      switchMap((lista: GrupoResumen[]) => lista.length
        ? forkJoin(lista.map((g) => this.api.grupoConsumidores(g.grupo).pipe(
            catchError(() => of({ ...g, lagTotal: 0, offsets: [] } as GrupoDetalle)))))
        : of([] as GrupoDetalle[])),
      map((grupos) => [...grupos].sort((a, b) => a.grupo.localeCompare(b.grupo))),
    );
  }

  private programarLag(): void {
    clearInterval(this.temporizador);
    if (this.lagEnVivo()) {
      this.temporizador = setInterval(() => this.cargarGrupos().subscribe({
        next: (g) => this.grupos.set(g),
        error: () => undefined,
      }), INTERVALO_MS);
    }
  }

  private topicVacio() {
    return { nombre: '', particiones: 3, rf: 3, cleanupPolicy: 'delete', retencionHoras: 24 as number | null, minIsr: 2 as number | null };
  }

  private ejecutar(peticion: Observable<unknown>, exito: string, despues?: (respuesta: unknown) => void): void {
    this.error.set(null);
    this.erroresCampo.set({});
    this.aviso.set(null);
    peticion.subscribe({
      next: (respuesta) => {
        this.aviso.set(exito);
        despues?.(respuesta);
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.mostrarError(err),
    });
  }

  private mostrarError(err: HttpErrorResponse, accion?: string): void {
    if (err.status === 403) {
      this.error.set('Necesitas el rol ADMIN para administrar Kafka.');
      return;
    }
    const detalle = err.error?.detail ?? err.message;
    this.error.set(`${accion ? `No fue posible ${accion}: ` : ''}${detalle} (HTTP ${err.status})`);
    this.erroresCampo.set(err.error?.errores ?? {});
  }
}
