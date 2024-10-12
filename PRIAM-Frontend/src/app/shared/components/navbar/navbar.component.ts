import { Component } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import {SecurityService} from "../../services/security.service";

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  currentUrl!: string;

  constructor(private router: Router, public securityService: SecurityService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.currentUrl = event.url;
      }
    });
  }

  onLogout() {
    this.securityService.kcService.logout(window.location.origin)
  }
}
