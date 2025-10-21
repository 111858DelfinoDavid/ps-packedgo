import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent {
  constructor(private router: Router) {}

  goToCustomerLogin(): void {
    this.router.navigate(['/customer/login']);
  }

  goToAdminLogin(): void {
    this.router.navigate(['/admin/login']);
  }

  goToCustomerRegister(): void {
    this.router.navigate(['/customer/register']);
  }
}
