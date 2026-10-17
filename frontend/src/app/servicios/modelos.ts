export type EstadoReporte = 'PENDIENTE' | 'EN_REVISION' | 'RESUELTO';

export interface Reporte {
  id: number;
  descripcion: string;
  autorEmail: string;
  estado: EstadoReporte;
  creadoEn: string;
}

export interface Ticket {
  id: number;
  codigo: string;
  reporteId: number;
  puntoNombre: string;
  direccion: string;
  comuna: string;
  descripcion: string;
  prioridad: 'NORMAL' | 'ALTA';
  estado: 'ABIERTO' | 'CERRADO';
  solicitadoPor: string;
  creadoEn: string;
}

export interface Estadisticas {
  reportesCreados: number;
  pasadosARevision: number;
  resueltos: number;
  reportesPorComuna: Record<string, number>;
  reportesPorPunto: Record<string, number>;
}

// ---- Administración de RabbitMQ (ms-rabbit-admin) ----
export interface ColaInfo {
  nombre: string;
  tipo: string;
  durable: boolean;
  mensajes: number;
  consumidores: number;
  argumentos: Record<string, unknown>;
}

export interface ExchangeInfo {
  nombre: string;
  tipo: string;
  durable: boolean;
}

export interface BindingInfo {
  exchange: string;
  cola: string;
  routingKey: string;
}
