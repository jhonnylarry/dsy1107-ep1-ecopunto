import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { EstadoReporte, Estadisticas, Reporte, Ticket } from './modelos';

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
}
