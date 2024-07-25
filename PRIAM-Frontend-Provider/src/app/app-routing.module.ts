import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AccessRequestComponent } from './pages/access-request/access-request.component';
import { RectificationComponent } from './pages/rectification/rectification.component';
import { SuppressionComponent } from './pages/suppression/suppression.component';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'access-request', component: AccessRequestComponent },
  { path: 'rectification', component: RectificationComponent },
  { path: 'suppression', component: SuppressionComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
