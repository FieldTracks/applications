import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LoginComponent} from "./login/login.component";
import {GraphComponent} from "./graph/graph.component";
import {FirmwareInstallerComponent} from "./firmware-installer/firmware-installer.component";

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'graph', component: GraphComponent },
  { path: 'firmware-installer', component: FirmwareInstallerComponent },

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
