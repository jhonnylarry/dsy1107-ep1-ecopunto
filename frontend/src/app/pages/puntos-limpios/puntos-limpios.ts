import { Component, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { environment } from '../../../environments/environment';

interface PuntoLimpio {
  id: number;
  nombre: string;
  direccion: string;
  comuna: string;
  materialesAceptados: string;
}

interface PuntoLimpioForm {
  nombre: string;
  direccion: string;
  comuna: string;
  materialesAceptados: string;
}

function formVacio(): PuntoLimpioForm {
  return { nombre: '', direccion: '', comuna: '', materialesAceptados: '' };
}

@Component({
  selector: 'app-puntos-limpios',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './puntos-limpios.html',
  styleUrl: './puntos-limpios.css',
})
export class PuntosLimpios implements OnInit {
  protected readonly puntos = signal<PuntoLimpio[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly esEncargado = signal(false);

  protected readonly mostrarFormCrear = signal(false);
  protected nuevoPunto: PuntoLimpioForm = formVacio();

  protected readonly idEditando = signal<number | null>(null);
  protected formEdicion: PuntoLimpioForm = formVacio();

  private readonly baseUrl = `${environment.apiBaseUrl}/api/puntos-limpios`;

  constructor(
    public readonly auth: AuthService,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.cargarPuntos();
    this.auth.getRoles().then((roles) => this.esEncargado.set(roles.includes('ENCARGADO')));
  }

  private cargarPuntos(): void {
    // El interceptor agrega el Bearer token automáticamente porque la URL
    // empieza con `${apiBaseUrl}/api/`.
    this.http.get<PuntoLimpio[]>(this.baseUrl).subscribe({
      next: (data) => this.puntos.set(data),
      error: (err) => this.error.set(`No fue posible cargar los puntos limpios: ${err.status ?? err.message}`),
    });
  }

  protected abrirFormCrear(): void {
    this.nuevoPunto = formVacio();
    this.mostrarFormCrear.set(true);
  }

  protected cancelarCrear(): void {
    this.mostrarFormCrear.set(false);
  }

  protected crearPunto(): void {
    this.http.post<PuntoLimpio>(this.baseUrl, this.nuevoPunto).subscribe({
      next: () => {
        this.mostrarFormCrear.set(false);
        this.cargarPuntos();
      },
      error: (err) => this.error.set(`No fue posible crear el punto limpio: ${err.status ?? err.message}`),
    });
  }

  protected empezarEdicion(punto: PuntoLimpio): void {
    this.idEditando.set(punto.id);
    this.formEdicion = {
      nombre: punto.nombre,
      direccion: punto.direccion,
      comuna: punto.comuna,
      materialesAceptados: punto.materialesAceptados,
    };
  }

  protected cancelarEdicion(): void {
    this.idEditando.set(null);
  }

  protected guardarEdicion(id: number): void {
    this.http.put<PuntoLimpio>(`${this.baseUrl}/${id}`, this.formEdicion).subscribe({
      next: () => {
        this.idEditando.set(null);
        this.cargarPuntos();
      },
      error: (err) => this.error.set(`No fue posible actualizar el punto limpio: ${err.status ?? err.message}`),
    });
  }

  protected eliminarPunto(punto: PuntoLimpio): void {
    if (!confirm(`¿Eliminar "${punto.nombre}"? Esta acción no se puede deshacer.`)) {
      return;
    }

    this.http.delete<void>(`${this.baseUrl}/${punto.id}`).subscribe({
      next: () => this.cargarPuntos(),
      error: (err) => this.error.set(`No fue posible eliminar el punto limpio: ${err.status ?? err.message}`),
    });
  }
}
