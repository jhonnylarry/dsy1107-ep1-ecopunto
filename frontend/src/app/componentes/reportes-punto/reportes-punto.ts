import { Component, OnInit, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EcopuntoApi } from '../../servicios/ecopunto-api';
import { EstadoReporte, Reporte } from '../../servicios/modelos';

const SIGUIENTE: Record<EstadoReporte, { estado: EstadoReporte; etiqueta: string } | null> = {
  PENDIENTE: { estado: 'EN_REVISION', etiqueta: 'Pasar a revisión' },
  EN_REVISION: { estado: 'RESUELTO', etiqueta: 'Marcar resuelto' },
  RESUELTO: null,
};

/**
 * Reportes de un punto limpio: listar, crear y (encargado) avanzar el estado.
 * Pasar a revisión genera un ticket de mantención de forma asíncrona (RabbitMQ).
 */
@Component({
  selector: 'app-reportes-punto',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './reportes-punto.html',
  styleUrl: './reportes-punto.css',
})
export class ReportesPunto implements OnInit {
  readonly puntoId = input.required<number>();
  readonly esEncargado = input(false);

  protected readonly reportes = signal<Reporte[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected descripcion = '';

  constructor(private readonly api: EcopuntoApi) {}

  ngOnInit(): void {
    this.cargar();
  }

  protected siguiente(reporte: Reporte) {
    return SIGUIENTE[reporte.estado];
  }

  protected etiquetaEstado(estado: EstadoReporte): string {
    return { PENDIENTE: 'Pendiente', EN_REVISION: 'En revisión', RESUELTO: 'Resuelto' }[estado];
  }

  private cargar(): void {
    this.api.reportesDePunto(this.puntoId()).subscribe({
      next: (data) => {
        this.reportes.set([...data].sort((a, b) => b.id - a.id));
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(`No fue posible cargar los reportes (${err.status ?? err.message}).`);
        this.cargando.set(false);
      },
    });
  }

  protected crear(): void {
    this.error.set(null);
    this.api.crearReporte(this.puntoId(), this.descripcion.trim()).subscribe({
      next: () => {
        this.descripcion = '';
        this.aviso.set('Reporte enviado. El encargado del punto será notificado.');
        this.cargar();
      },
      error: (err) => this.error.set(`No fue posible crear el reporte (${err.status ?? err.message}).`),
    });
  }

  protected avanzar(reporte: Reporte): void {
    const paso = SIGUIENTE[reporte.estado];
    if (!paso) {
      return;
    }
    this.error.set(null);
    this.api.cambiarEstado(reporte.id, paso.estado).subscribe({
      next: () => {
        this.aviso.set(paso.estado === 'EN_REVISION'
          ? `Reporte #${reporte.id} en revisión: se está generando su ticket de mantención.`
          : `Reporte #${reporte.id} resuelto: se notificará al vecino que lo reportó.`);
        this.cargar();
      },
      error: (err) => this.error.set(err.status === 409
        ? 'El reporte ya cambió de estado. Se actualizó la lista.'
        : `No fue posible cambiar el estado (${err.status ?? err.message}).`),
    });
  }
}
