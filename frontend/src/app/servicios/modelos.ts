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
  /** Lecturas de sensores (Kafka, grupo ecopunto-estadisticas-sensores). Claves: id del punto o material. */
  sensores?: EstadisticasSensores;
}

export interface EstadisticasSensores {
  lecturasTotales: number;
  lecturasPorPunto: Record<string, number>;
  lecturasPorMaterial: Record<string, number>;
  nivelMaximoHoyPorPunto: Record<string, number>;
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

// ---- Sensores y monitoreo de contenedores (Kafka) ----
export interface PuntoLimpioResumen {
  id: number;
  nombre: string;
  comuna: string;
}

export type EstadoLlenado = 'NORMAL' | 'ALERTA' | 'CRITICO';

/** Estado calculado por ms-monitoreo a partir de las lecturas. */
export interface ContenedorMonitoreado {
  contenedorId: string;
  puntoLimpioId: number;
  material: string;
  nivelActual: number;
  nivelPromedio: number;
  estado: EstadoLlenado;
  lecturas: number;
  ultimaMedicion: string | null;
  cambioEstadoEn: string | null;
}

export interface ResumenMonitoreo {
  contenedores: number;
  porEstado: Partial<Record<EstadoLlenado, number>>;
  nivelPromedioGeneral: number;
  lecturasProcesadas: number;
  ultimaLectura: string | null;
}

/** Dónde quedó una lectura publicada por ms-sensores. */
export interface LecturaPublicada {
  lecturaId: string;
  contenedorId: string;
  nivelLlenado: number;
  topic: string;
  particion: number;
  offset: number;
}

// ---- Administración de Kafka (ms-kafka-admin) ----
export interface BrokerInfo {
  id: number;
  host: string;
  puerto: number;
  controller: boolean;
}

export interface ClusterInfo {
  clusterId: string;
  controllerId: number;
  brokers: BrokerInfo[];
}

export interface TopicResumen {
  nombre: string;
  particiones: number;
  replicationFactor: number;
  protegido: boolean;
}

export interface ParticionInfo {
  particion: number;
  lider: number | null;
  replicas: number[];
  isr: number[];
}

export interface TopicDetalle extends TopicResumen {
  detalleParticiones: ParticionInfo[];
  configuracion: Record<string, string>;
}

export interface CrearTopic {
  nombre: string;
  particiones: number;
  replicationFactor: number;
  configuracion: Record<string, string>;
}

export interface GrupoResumen {
  grupo: string;
  estado: string;
  miembros: number;
}

export interface OffsetParticion {
  topic: string;
  particion: number;
  offsetConfirmado: number | null;
  offsetFinal: number;
  lag: number | null;
  consumidor: string | null;
}

export interface GrupoDetalle extends GrupoResumen {
  lagTotal: number;
  offsets: OffsetParticion[];
}
