import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  protected readonly esEncargado = signal(false);
  protected readonly esAdmin = signal(false);

  constructor(public readonly auth: AuthService) {}

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.auth.getRoles().then((roles) => {
        this.esEncargado.set(roles.includes('ENCARGADO'));
        this.esAdmin.set(roles.includes('ADMIN'));
      });
    }
  }
}
