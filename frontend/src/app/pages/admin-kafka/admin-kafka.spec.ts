import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminKafka } from './admin-kafka';
import { environment } from '../../../environments/environment';

const API = `${environment.apiBaseUrl}/api/kafka-admin`;

describe('AdminKafka', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminKafka],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => TestBed.resetTestingModule());

  function cargar() {
    const fixture = TestBed.createComponent(AdminKafka);
    fixture.detectChanges();
    http.expectOne(`${API}/cluster`).flush({
      clusterId: 'YA2N5lW0RnG1aMJJG1x2-Q', controllerId: 5,
      brokers: [4, 5, 6].map((id) => ({ id, host: `kafka-broker-${id - 3}`, puerto: 9092, controller: id === 5 })),
    });
    http.expectOne(`${API}/topics`).flush([
      { nombre: 'ecopunto.sensores.lecturas', particiones: 3, replicationFactor: 3, protegido: true },
      { nombre: 'ecopunto.pruebas', particiones: 1, replicationFactor: 1, protegido: false },
      { nombre: '__consumer_offsets', particiones: 50, replicationFactor: 3, protegido: false },
    ]);
    http.expectOne(`${API}/grupos`).flush([{ grupo: 'ecopunto-monitoreo', estado: 'STABLE', miembros: 3 }]);
    http.expectOne(`${API}/grupos/ecopunto-monitoreo`).flush({
      grupo: 'ecopunto-monitoreo', estado: 'STABLE', miembros: 3, lagTotal: 4,
      offsets: [{ topic: 'ecopunto.sensores.lecturas', particion: 0, offsetConfirmado: 10, offsetFinal: 14, lag: 4,
        consumidor: 'consumer-1 /172.18.0.9' }],
    });
    fixture.detectChanges();
    return fixture;
  }

  it('muestra los 3 brokers, oculta los topics internos y protege los del sistema', () => {
    const fixture = cargar();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelectorAll('.broker').length).toBe(3);
    expect(el.textContent).not.toContain('__consumer_offsets');
    const filas = [...el.querySelectorAll('tr')].filter((tr) => tr.textContent?.includes('ecopunto.'));
    const eliminarSistema = filas.find((f) => f.textContent?.includes('lecturas'))!.querySelector('button.danger') as HTMLButtonElement;
    const eliminarPrueba = filas.find((f) => f.textContent?.includes('pruebas'))!.querySelector('button.danger') as HTMLButtonElement;
    expect(eliminarSistema.disabled).toBe(true);
    expect(eliminarPrueba.disabled).toBe(false);
    expect(el.textContent).toContain('lag total 4');
    fixture.destroy();
  });

  it('el detalle marca una partición sub-replicada', () => {
    const fixture = cargar();
    const el = fixture.nativeElement as HTMLElement;
    ([...el.querySelectorAll('button')].find((b) => b.textContent?.includes('Detalle')) as HTMLButtonElement).click();
    http.expectOne(`${API}/topics/ecopunto.pruebas`).flush({
      nombre: 'ecopunto.pruebas', particiones: 2, replicationFactor: 3, protegido: false,
      detalleParticiones: [
        { particion: 0, lider: 4, replicas: [4, 5, 6], isr: [4, 5, 6] },
        { particion: 1, lider: 6, replicas: [5, 6, 4], isr: [6, 4] },
      ],
      configuracion: { 'cleanup.policy': 'delete', 'retention.ms': '86400000' },
    });
    fixture.detectChanges();

    expect(el.querySelectorAll('.badge.alta').length).toBe(1);
    expect(el.querySelector('section.detalle')?.textContent).toContain('retention.ms');
    fixture.destroy();
  });

  it('crear un topic envía particiones, réplicas y configuración', () => {
    const fixture = cargar();
    const cmp = fixture.componentInstance as unknown as { nuevoTopic: Record<string, unknown>; crearTopic(): void };
    cmp.nuevoTopic = { nombre: 'ecopunto.auditoria', particiones: 3, rf: 3, cleanupPolicy: 'compact', retencionHoras: 48, minIsr: 2 };
    cmp.crearTopic();

    const req = http.expectOne(`${API}/topics`);
    expect(req.request.body).toEqual({
      nombre: 'ecopunto.auditoria', particiones: 3, replicationFactor: 3,
      configuracion: { 'cleanup.policy': 'compact', 'retention.ms': '172800000', 'min.insync.replicas': '2' },
    });
    req.flush({}, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.alerta-error')?.textContent).toContain('409');
    fixture.destroy();
  });
});
