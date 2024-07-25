import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
// DEFAULT
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CommonModule } from '@angular/common';
// ANGULAR MATERIALS
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
// ADDED MODULES
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
// ADDED COMPONENTS
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { AccessRequestComponent } from './pages/access-request/access-request.component';
import { RequestsComponent } from './pages/requests/requests.component';
import { ConsentComponent } from './pages/consent/consent.component';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ArSelectionComponent } from './pages/ar-selection/ar-selection.component';
import { RightsComponent } from './pages/rights/rights.component';
import { RectificationComponent } from './pages/rectification/rectification.component';
import { SuppressionComponent } from './pages/suppression/suppression.component';
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';
import { environment } from '../environment/environment';

export function kcFactory(kcService: KeycloakService) {
  return () => {
    kcService.init({
      config: {
        realm: 'priam-realm',
        clientId: 'Data-client',
        url: environment.keycloak,
      },
      initOptions: {
        onLoad: 'login-required',
        checkLoginIframe: true,
      },
    });
  };
}

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    AccessRequestComponent,
    RequestsComponent,
    ConsentComponent,
    LoginComponent,
    HomeComponent,
    FooterComponent,
    ArSelectionComponent,
    RightsComponent,
    RectificationComponent,
    SuppressionComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    FormsModule,
    KeycloakAngularModule,
    MatToolbarModule,
    MatGridListModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatMenuModule,
    MatTableModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatSnackBarModule,
    MatDialogModule,
    CommonModule,
  ],
  providers: [
    {
      provide: APP_INITIALIZER,
      deps: [KeycloakService],
      useFactory: kcFactory,
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
