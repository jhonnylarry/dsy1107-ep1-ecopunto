import { Component, OnInit, computed, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { KeyValuePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';
import { EcopuntoApi } from '../../servicios/ecopunto-api';
import { BindingInfo, ColaInfo, ExchangeInfo } from '../../servicios/modelos';

const SISTEMA = /^(amq\.|$)/;

/**
 * Administración de RabbitMQ (rol ADMIN). Toda la lógica vive en ms-rabbit-admin;
 * esta página solo consume su API REST y muestra los errores de validación que devuelve.
 */
@Component({
  selector: 'app-admin-rabbitmq',
  standalone: true,
  imports: [RouterLink, FormsModule, KeyValuePipe],
  templateUrl: './admin-rabbitmq.html',
  styleUrl: './admin-rabbitmq.css',
})
export class AdminRabbitmq implements OnInit {
  protected readonly colas = signal<ColaInfo[]>([]);
  protected readonly exchanges = signal<ExchangeInfo[]>([]);
  protected readonly bindings = signal<BindingInfo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly erroresCampo = signal<Record<string, string>>({});
  protected readonly aviso = signal<string | null>(null);

  protected readonly exchangesPropios = computed(() => this.exchanges().filter((e) => !SISTEMA.test(e.nombre)));
  protected readonly bindingsPropios = computed(() => this.bindings().filter((b) => !SISTEMA.test(b.exchange)));

  protected nuevaCola = { nombre: '', tipo: 'QUORUM', conDlq: true };
  protected nuevoExchange = { nombre: '', tipo: 'TOPIC' };
  protected nuevoBinding: BindingInfo = { exchange: '', cola: '', routingKey: '' };

  constructor(private readonly api: EcopuntoApi) {}

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    forkJoin({ colas: this.api.colas(), exchanges: this.api.exchanges(), bindings: this.api.bindings() }).subscribe({
      next: ({ colas, exchanges, bindings }) => {
        this.colas.set([...colas].sort((a, b) => a.nombre.localeCompare(b.nombre)));
        this.exchanges.set([...exchanges].sort((a, b) => a.nombre.localeCompare(b.nombre)));
        this.bindings.set(bindings);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.mostrarError(err, 'cargar la topología');
      },
    });
  }

  protected tieneDlq(cola: ColaInfo): string | null {
    return (cola.argumentos?.['x-dead-letter-routing-key'] as string) ?? null;
  }

  protected crearCola(): void {
    const c = this.nuevaCola;
    this.ejecutar(this.api.crearCola(c.nombre.trim(), c.tipo, c.conDlq),
      `Cola "${c.nombre}" creada${c.conDlq ? ' junto con su DLQ' : ''}.`,
      () => (this.nuevaCola = { nombre: '', tipo: 'QUORUM', conDlq: true }));
  }

  protected crearExchange(): void {
    const e = this.nuevoExchange;
    this.ejecutar(this.api.crearExchange(e.nombre.trim(), e.tipo), `Exchange "${e.nombre}" (${e.tipo}) creado.`,
      () => (this.nuevoExchange = { nombre: '', tipo: 'TOPIC' }));
  }

  protected crearBinding(): void {
    const b = { ...this.nuevoBinding, routingKey: this.nuevoBinding.routingKey.trim() };
    this.ejecutar(this.api.crearBinding(b), `Binding ${b.exchange} --${b.routingKey}--> ${b.cola} creado.`,
      () => (this.nuevoBinding = { exchange: '', cola: '', routingKey: '' }));
  }

  protected eliminarCola(cola: ColaInfo): void {
    if (!confirm(`¿Eliminar la cola "${cola.nombre}"?`)) {
      return;
    }
    this.ejecutar(this.api.eliminarCola(cola.nombre), `Cola "${cola.nombre}" eliminada.`);
  }

  protected eliminarExchange(exchange: ExchangeInfo): void {
    if (!confirm(`¿Eliminar el exchange "${exchange.nombre}"?`)) {
      return;
    }
    this.ejecutar(this.api.eliminarExchange(exchange.nombre), `Exchange "${exchange.nombre}" eliminado.`);
  }

  protected eliminarBinding(b: BindingInfo): void {
    if (!confirm(`¿Eliminar el binding ${b.exchange} --${b.routingKey}--> ${b.cola}?`)) {
      return;
    }
    this.ejecutar(this.api.eliminarBinding(b), 'Binding eliminado.');
  }

  private ejecutar(peticion: Observable<unknown>, exito: string, despues?: () => void): void {
    this.error.set(null);
    this.erroresCampo.set({});
    this.aviso.set(null);
    peticion.subscribe({
      next: () => {
        this.aviso.set(exito);
        despues?.();
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.mostrarError(err),
    });
  }

  private mostrarError(err: HttpErrorResponse, accion?: string): void {
    if (err.status === 403) {
      this.error.set('Necesitas el rol ADMIN para administrar RabbitMQ.');
      return;
    }
    const detalle = err.error?.detail ?? err.message;
    this.error.set(`${accion ? `No fue posible ${accion}: ` : ''}${detalle} (HTTP ${err.status})`);
    this.erroresCampo.set(err.error?.errores ?? {});
  }
}
