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
