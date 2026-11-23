import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Monitoreo } from './monitoreo';
import { environment } from '../../../environments/environment';

const API = `${environment.apiBaseUrl}/api`;

describe('Monitoreo', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Monitoreo],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  function cargar() {
    const fixture = TestBed.createComponent(Monitoreo);
    fixture.detectChanges();
    http.expectOne(`${API}/puntos-limpios`).flush([{ id: 1, nombre: 'Punto Limpio Ñuñoa', comuna: 'Ñuñoa' }]);
    http.expectOne(`${API}/monitoreo/contenedores`).flush([
      { contenedorId: 'PL-1-VIDRIO', puntoLimpioId: 1, material: 'VIDRIO', nivelActual: 96.4, nivelPromedio: 91,
        estado: 'CRITICO', lecturas: 12, ultimaMedicion: '2026-11-24T13:00:00Z', cambioEstadoEn: null },
      { contenedorId: 'PL-1-PAPEL', puntoLimpioId: 1, material: 'PAPEL', nivelActual: 30, nivelPromedio: 28,
        estado: 'NORMAL', lecturas: 12, ultimaMedicion: '2026-11-24T13:00:00Z', cambioEstadoEn: null },
      { contenedorId: 'PL-2-LATAS', puntoLimpioId: 2, material: 'LATAS', nivelActual: 80, nivelPromedio: 77,
        estado: 'ALERTA', lecturas: 3, ultimaMedicion: null, cambioEstadoEn: null },
    ]);
    http.expectOne(`${API}/monitoreo/resumen`).flush({
      contenedores: 3, porEstado: { CRITICO: 1, NORMAL: 1, ALERTA: 1 }, nivelPromedioGeneral: 68.8,
      lecturasProcesadas: 27, ultimaLectura: '2026-11-24T13:00:00Z',
    });
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('agrupa por punto limpio, con los críticos primero y el nombre del punto', () => {
    const fixture = cargar();
    const el = fixture.nativeElement as HTMLElement;
    const titulos = [...el.querySelectorAll('section.panel h2')].map((h) => h.textContent?.trim());

    expect(titulos).toContain('Simular una lectura');
    expect(titulos.findIndex((t) => t?.startsWith('Punto Limpio Ñuñoa')))
      .toBeLessThan(titulos.findIndex((t) => t?.startsWith('Punto limpio #2')));
    expect(el.querySelector('.kpi.critico .valor')?.textContent?.trim()).toBe('1');
    expect(el.querySelector('.badge.critico')?.textContent?.trim()).toBe('CRITICO');
    expect(el.textContent).toContain('96 %');
    fixture.destroy();
  });

  it('el filtro muestra solo los contenedores del estado elegido', () => {
    const fixture = cargar();
    const el = fixture.nativeElement as HTMLElement;
    const critico = [...el.querySelectorAll('.filtros button')].find((b) => b.textContent?.includes('Critico')) as HTMLButtonElement;

    critico.click();
    fixture.detectChanges();

    const filas = el.querySelectorAll('tbody tr');
    expect(filas.length).toBe(1);
    expect(filas[0].textContent).toContain('PL-1-VIDRIO');
    fixture.destroy();
  });

  it('al vaciar muestra la partición y el offset donde quedó la lectura', () => {
    const fixture = cargar();
    const el = fixture.nativeElement as HTMLElement;
    const fila = [...el.querySelectorAll('tbody tr')].find((tr) => tr.textContent?.includes('PL-1-VIDRIO'))!;
    (fila.querySelector('td.acciones button') as HTMLButtonElement).click();

    const req = http.expectOne(`${API}/sensores/contenedores/PL-1-VIDRIO/vaciar`);
    expect(req.request.method).toBe('POST');
    req.flush({ lecturaId: 'x', contenedorId: 'PL-1-VIDRIO', nivelLlenado: 0, topic: 'ecopunto.sensores.lecturas',
      particion: 2, offset: 1534 });
    fixture.detectChanges();

    expect(el.querySelector('.alerta-info')?.textContent).toContain('partición 2');
    expect(el.querySelector('.alerta-info')?.textContent).toContain('offset 1534');
    fixture.destroy();
  });

  it('sin rol muestra un mensaje claro', () => {
    const fixture = TestBed.createComponent(Monitoreo);
    fixture.detectChanges();
    http.expectOne(`${API}/puntos-limpios`).flush([]);
    http.expectOne(`${API}/monitoreo/contenedores`).flush(null, { status: 403, statusText: 'Forbidden' });
    http.match(`${API}/monitoreo/resumen`);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.alerta-error')?.textContent)
      .toContain('rol ENCARGADO o ADMIN');
    fixture.destroy();
  });
});
