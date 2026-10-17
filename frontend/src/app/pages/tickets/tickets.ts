import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EcopuntoApi } from '../../servicios/ecopunto-api';
import { Ticket } from '../../servicios/modelos';

/** Tickets de mantención generados por ms-tickets (rol ENCARGADO). */
@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './tickets.html',
})
export class Tickets implements OnInit {
  protected readonly tickets = signal<Ticket[] | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor(private readonly api: EcopuntoApi) {}

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.api.tickets().subscribe({
      next: (t) => this.tickets.set(t),
      error: (err) => this.error.set(err.status === 403
        ? 'Solo los encargados pueden ver los tickets de mantención.'
        : `No fue posible cargar los tickets (${err.status ?? err.message}).`),
    });
  }
}
