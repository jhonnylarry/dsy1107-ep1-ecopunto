import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  BindingInfo,
  ClusterInfo,
  ColaInfo,
  ContenedorMonitoreado,
  CrearTopic,
  EstadoReporte,
  Estadisticas,
  ExchangeInfo,
  GrupoDetalle,
  GrupoResumen,
  LecturaPublicada,
  PuntoLimpioResumen,
  Reporte,
  ResumenMonitoreo,
  Ticket,
  TopicDetalle,
  TopicResumen,
} from './modelos';

/**
 * Llamadas a la API. Todas pasan por el API Gateway (environment.apiBaseUrl), que enruta
 * /api/estadisticas, /api/tickets, /api/sensores, /api/monitoreo y /api/kafka-admin a sus
 * microservicios y el resto al backend.
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

  // ---- Sensores y monitoreo (Kafka; roles ENCARGADO o ADMIN) ----

  /** Solo para mostrar el nombre de cada punto limpio junto a sus contenedores. */
  puntosLimpios(): Observable<PuntoLimpioResumen[]> {
    return this.http.get<PuntoLimpioResumen[]>(`${this.api}/puntos-limpios`);
  }

  contenedoresMonitoreados(): Observable<ContenedorMonitoreado[]> {
    return this.http.get<ContenedorMonitoreado[]>(`${this.api}/monitoreo/contenedores`);
  }

  resumenMonitoreo(): Observable<ResumenMonitoreo> {
    return this.http.get<ResumenMonitoreo>(`${this.api}/monitoreo/resumen`);
  }

  registrarLectura(contenedorId: string, nivelLlenado: number): Observable<LecturaPublicada> {
    return this.http.post<LecturaPublicada>(`${this.api}/sensores/lecturas`, { contenedorId, nivelLlenado });
  }

  vaciarContenedor(contenedorId: string): Observable<LecturaPublicada> {
    return this.http.post<LecturaPublicada>(
      `${this.api}/sensores/contenedores/${encodeURIComponent(contenedorId)}/vaciar`, {});
  }

  // ---- Administración de Kafka (rol ADMIN) ----

  clusterKafka(): Observable<ClusterInfo> {
    return this.http.get<ClusterInfo>(`${this.api}/kafka-admin/cluster`);
  }

  topics(): Observable<TopicResumen[]> {
    return this.http.get<TopicResumen[]>(`${this.api}/kafka-admin/topics`);
  }

  topic(nombre: string): Observable<TopicDetalle> {
    return this.http.get<TopicDetalle>(`${this.api}/kafka-admin/topics/${encodeURIComponent(nombre)}`);
  }

  crearTopic(topic: CrearTopic): Observable<TopicDetalle> {
    return this.http.post<TopicDetalle>(`${this.api}/kafka-admin/topics`, topic);
  }

  modificarConfigTopic(nombre: string, fijar: Record<string, string>, eliminar: string[] = []): Observable<TopicDetalle> {
    return this.http.patch<TopicDetalle>(`${this.api}/kafka-admin/topics/${encodeURIComponent(nombre)}/config`,
      { fijar, eliminar });
  }

  aumentarParticiones(nombre: string, total: number): Observable<TopicDetalle> {
    return this.http.post<TopicDetalle>(`${this.api}/kafka-admin/topics/${encodeURIComponent(nombre)}/particiones`,
      { total });
  }

  eliminarTopic(nombre: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/kafka-admin/topics/${encodeURIComponent(nombre)}`);
  }

  gruposConsumidores(): Observable<GrupoResumen[]> {
    return this.http.get<GrupoResumen[]>(`${this.api}/kafka-admin/grupos`);
  }

  grupoConsumidores(grupo: string): Observable<GrupoDetalle> {
    return this.http.get<GrupoDetalle>(`${this.api}/kafka-admin/grupos/${encodeURIComponent(grupo)}`);
  }
}
