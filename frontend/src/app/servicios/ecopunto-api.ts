import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BindingInfo, ColaInfo, EstadoReporte, Estadisticas, ExchangeInfo, Reporte, Ticket } from './modelos';

/**
 * Llamadas a la API. Todas pasan por el API Gateway (environment.apiBaseUrl), que enruta
 * /api/estadisticas y /api/tickets a sus microservicios y el resto al backend.
 */
@Injectable({ providedIn: 'root' })
export class EcopuntoApi {
  private readonly api = `${environment.apiBaseUrl}/api`;

  constructor(private readonly http: HttpClient) {}

  reportesDePunto(puntoId: number): Observable<Reporte[]> {
    return this.http.get<Reporte[]>(`${this.api}/puntos-limpios/${puntoId}/reportes`);
  }

  crearReporte(puntoId: number, descripcion: string): Observable<Reporte> {
    return this.http.post<Reporte>(`${this.api}/puntos-limpios/${puntoId}/reportes`, { descripcion });
  }

  cambiarEstado(reporteId: number, estado: EstadoReporte): Observable<Reporte> {
    return this.http.patch<Reporte>(`${this.api}/reportes/${reporteId}/estado`, { estado });
  }

  estadisticas(): Observable<Estadisticas> {
    return this.http.get<Estadisticas>(`${this.api}/estadisticas`);
  }

  tickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.api}/tickets`);
  }

  // ---- Administración de RabbitMQ (rol ADMIN) ----

  colas(): Observable<ColaInfo[]> {
    return this.http.get<ColaInfo[]>(`${this.api}/admin/queues`);
  }

  crearCola(nombre: string, tipo: string, conDlq: boolean): Observable<ColaInfo> {
    return this.http.post<ColaInfo>(`${this.api}/admin/queues`, { nombre, tipo, conDlq });
  }

  eliminarCola(nombre: string, forzar = false): Observable<void> {
    return this.http.delete<void>(`${this.api}/admin/queues/${encodeURIComponent(nombre)}`, { params: { forzar } });
  }

  exchanges(): Observable<ExchangeInfo[]> {
    return this.http.get<ExchangeInfo[]>(`${this.api}/admin/exchanges`);
  }

  crearExchange(nombre: string, tipo: string): Observable<ExchangeInfo> {
    return this.http.post<ExchangeInfo>(`${this.api}/admin/exchanges`, { nombre, tipo });
  }

  eliminarExchange(nombre: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/admin/exchanges/${encodeURIComponent(nombre)}`);
  }

  bindings(): Observable<BindingInfo[]> {
    return this.http.get<BindingInfo[]>(`${this.api}/admin/bindings`);
  }

  crearBinding(binding: BindingInfo): Observable<BindingInfo> {
    return this.http.post<BindingInfo>(`${this.api}/admin/bindings`, binding);
  }

  eliminarBinding(binding: BindingInfo): Observable<void> {
    return this.http.delete<void>(`${this.api}/admin/bindings`, { body: binding });
  }
}
