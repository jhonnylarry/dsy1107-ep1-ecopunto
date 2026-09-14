import { Component, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../auth/auth.service';
import { environment } from '../../../environments/environment';

interface PuntoLimpio {
  id: number;
  nombre: string;
  direccion: string;
  materialesAceptados: string[];
}

@Component({
  selector: 'app-puntos-limpios',
  standalone: true,
  templateUrl: './puntos-limpios.html',
  styleUrl: './puntos-limpios.css',
})
export class PuntosLimpios implements OnInit {
  protected readonly puntos = signal<PuntoLimpio[]>([]);
  protected readonly error = signal<string | null>(null);

  constructor(
    public readonly auth: AuthService,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    // El interceptor agrega el Bearer token automáticamente porque la URL
    // empieza con `${apiBaseUrl}/api/`.
    this.http.get<PuntoLimpio[]>(`${environment.apiBaseUrl}/api/puntos-limpios`).subscribe({
      next: (data) => this.puntos.set(data),
      error: (err) => this.error.set(`No fue posible cargar los puntos limpios: ${err.status ?? err.message}`),
    });
  }
}
