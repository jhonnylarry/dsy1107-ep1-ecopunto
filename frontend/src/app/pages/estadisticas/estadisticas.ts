import { Component, OnInit, computed, signal } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EcopuntoApi } from '../../servicios/ecopunto-api';
import { Estadisticas as Resumen } from '../../servicios/modelos';

/** Estadísticas que ms-estadisticas arma consumiendo los eventos reporte.* de RabbitMQ. */
@Component({
  selector: 'app-estadisticas',
  standalone: true,
  imports: [RouterLink, KeyValuePipe],
  templateUrl: './estadisticas.html',
  styleUrl: './estadisticas.css',
})
export class Estadisticas implements OnInit {
  protected readonly datos = signal<Resumen | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly maxComuna = computed(() => Math.max(1, ...Object.values(this.datos()?.reportesPorComuna ?? {})));

  constructor(private readonly api: EcopuntoApi) {}

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.api.estadisticas().subscribe({
      next: (d) => this.datos.set(d),
      error: (err) => this.error.set(`No fue posible cargar las estadísticas (${err.status ?? err.message}).`),
    });
  }

  protected ancho(valor: number): string {
    return `${Math.round((valor / this.maxComuna()) * 100)}%`;
  }

  protected porValorDesc = (a: { value: number }, b: { value: number }) => b.value - a.value;
}
