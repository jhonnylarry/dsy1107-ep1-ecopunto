import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, of } from 'rxjs';
import { EcopuntoApi } from '../../servicios/ecopunto-api';
import {
  ContenedorMonitoreado,
  EstadoLlenado,
  LecturaPublicada,
  PuntoLimpioResumen,
  ResumenMonitoreo,
} from '../../servicios/modelos';

interface GrupoPunto {
  puntoLimpioId: number;
  nombre: string;
  contenedores: ContenedorMonitoreado[];
}

const INTERVALO_MS = 5000;
const ORDEN_ESTADO: Record<EstadoLlenado, number> = { CRITICO: 0, ALERTA: 1, NORMAL: 2 };

/**
 * Monitoreo de contenedores en tiempo real (roles ENCARGADO y ADMIN).
 * El estado lo calcula ms-monitoreo consumiendo ecopunto.sensores.lecturas; esta página lo consulta
 * cada 5 segundos. También permite publicar una lectura manual en Kafka (vía ms-sensores) para ver
 * la reacción del sistema durante la demo.
 */
@Component({
  selector: 'app-monitoreo',
  standalone: true,
  imports: [RouterLink, FormsModule, DecimalPipe, DatePipe],
  templateUrl: './monitoreo.html',
  styleUrl: './monitoreo.css',
})
export class Monitoreo implements OnInit, OnDestroy {
  protected readonly contenedores = signal<ContenedorMonitoreado[]>([]);
  protected readonly resumen = signal<ResumenMonitoreo | null>(null);
  protected readonly puntos = signal<Map<number, string>>(new Map());
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly publicada = signal<LecturaPublicada | null>(null);
  protected readonly actualizadoEn = signal<Date | null>(null);
  protected readonly enVivo = signal(true);
  protected readonly filtro = signal<EstadoLlenado | 'TODOS'>('TODOS');

  protected lectura = { contenedorId: '', nivel: 95 };

  protected readonly grupos = computed<GrupoPunto[]>(() => {
    const filtro = this.filtro();
    const porPunto = new Map<number, ContenedorMonitoreado[]>();
    for (const c of this.contenedores()) {
      if (filtro !== 'TODOS' && c.estado !== filtro) {
        continue;
      }
      porPunto.set(c.puntoLimpioId, [...(porPunto.get(c.puntoLimpioId) ?? []), c]);
    }
    return [...porPunto.entries()]
      .map(([id, lista]) => ({
        puntoLimpioId: id,
        nombre: this.puntos().get(id) ?? `Punto limpio #${id}`,
        contenedores: lista.sort((a, b) => a.material.localeCompare(b.material)),
      }))
      .sort((a, b) => this.peorEstado(a) - this.peorEstado(b) || a.puntoLimpioId - b.puntoLimpioId);
  });

  protected readonly ordenados = computed(() =>
    [...this.contenedores()].sort((a, b) => a.contenedorId.localeCompare(b.contenedorId)));

  private temporizador?: ReturnType<typeof setInterval>;

  constructor(private readonly api: EcopuntoApi) {}

  ngOnInit(): void {
    // Los nombres de los puntos son opcionales: si fallan se muestra el id.
    this.api.puntosLimpios().pipe(catchError(() => of([] as PuntoLimpioResumen[]))).subscribe((lista) =>
      this.puntos.set(new Map(lista.map((p) => [p.id, p.nombre]))));
    this.cargar();
    this.programar();
  }

  ngOnDestroy(): void {
    clearInterval(this.temporizador);
  }

  protected alternarEnVivo(): void {
    this.enVivo.update((v) => !v);
    this.programar();
  }

  protected cargar(): void {
    forkJoin({ lista: this.api.contenedoresMonitoreados(), resumen: this.api.resumenMonitoreo() }).subscribe({
      next: ({ lista, resumen }) => {
        this.contenedores.set(lista);
        this.resumen.set(resumen);
        this.actualizadoEn.set(new Date());
        this.cargando.set(false);
        this.error.set(null);
        if (!this.lectura.contenedorId && lista.length) {
          this.lectura.contenedorId = lista[0].contenedorId;
        }
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(this.mensaje(err, 'cargar el monitoreo'));
      },
    });
  }

  protected enviarLectura(): void {
    this.publicar(this.api.registrarLectura(this.lectura.contenedorId, Number(this.lectura.nivel)));
  }

  protected vaciar(c: ContenedorMonitoreado): void {
    this.publicar(this.api.vaciarContenedor(c.contenedorId));
  }

  protected cantidad(estado: EstadoLlenado): number {
    return this.resumen()?.porEstado?.[estado] ?? 0;
  }

  protected claseEstado(estado: EstadoLlenado): string {
    return estado.toLowerCase();
  }

  protected ancho(nivel: number): string {
    return `${Math.max(0, Math.min(100, nivel))}%`;
  }

  private publicar(peticion: Observable<LecturaPublicada>): void {
    this.publicada.set(null);
    this.error.set(null);
    peticion.subscribe({
      next: (p) => {
        this.publicada.set(p);
        // ms-monitoreo la procesa en milisegundos; se vuelve a consultar al tiro.
        setTimeout(() => this.cargar(), 800);
      },
      error: (err: HttpErrorResponse) => this.error.set(this.mensaje(err, 'publicar la lectura')),
    });
  }

  private programar(): void {
    clearInterval(this.temporizador);
    if (this.enVivo()) {
      this.temporizador = setInterval(() => this.cargar(), INTERVALO_MS);
    }
  }

  private peorEstado(g: GrupoPunto): number {
    return Math.min(...g.contenedores.map((c) => ORDEN_ESTADO[c.estado]));
  }

  private mensaje(err: HttpErrorResponse, accion: string): string {
    if (err.status === 403) {
      return 'Necesitas el rol ENCARGADO o ADMIN para ver el monitoreo.';
    }
    const campos = err.error?.campos ? Object.values(err.error.campos).join('; ') : null;
    const detalle = campos ?? err.error?.error ?? err.error?.detail ?? err.message;
    return `No fue posible ${accion}: ${detalle} (HTTP ${err.status})`;
  }
}
