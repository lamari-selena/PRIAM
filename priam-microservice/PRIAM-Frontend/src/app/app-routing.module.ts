import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { AccessRequestComponent } from './pages/access-request/access-request.component';
import { RequestsComponent } from './pages/requests/requests.component';
import { RightsComponent } from './pages/rights/rights.component';
import { ConsentComponent } from './pages/consent/consent.component';
import { LoginComponent } from './pages/login/login.component';
import { ArSelectionComponent } from './pages/ar-selection/ar-selection.component';
import { RectificationComponent } from './pages/rectification/rectification.component';
import { SuppressionComponent } from './pages/suppression/suppression.component';

const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'access-request', component: AccessRequestComponent },
  { path: 'requests', component: RequestsComponent },
  { path: 'rights', component: RightsComponent },
  { path: 'consent', component: ConsentComponent },
  { path: 'login', component: LoginComponent },
  { path: 'ar-selection', component: ArSelectionComponent },
  { path: 'rectification', component: RectificationComponent },
  { path: 'suppression', component: SuppressionComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
