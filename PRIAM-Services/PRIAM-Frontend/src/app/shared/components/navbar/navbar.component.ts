import { Component } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import {SecurityService} from "../../services/security.service";
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  currentUrl!: string;
  // Visible on every page (not just Home) - a data subject navigating PRIAM
  // otherwise has no way back to the target app except the browser's own
  // back button. See Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4ter.
  targetAppUrl = environment.targetAppUrl;

  constructor(private router: Router, public securityService: SecurityService) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.currentUrl = event.url;
      }
    });
  }

  onLogout() {
    this.securityService.logout();
  }
}
