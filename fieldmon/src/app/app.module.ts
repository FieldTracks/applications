import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { CommonModule } from '@angular/common';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';

import { LoginComponent } from './login/login.component';

import {MatToolbarModule} from "@angular/material/toolbar";
import { HeaderNavbarComponent } from './header-navbar/header-navbar.component';
import { GraphComponent } from './graph/graph.component';
import { D3ForceComponent } from './graph/d3-force/d3-force.component';
import { SettingsComponent } from './graph/settings/settings.component';
import {MatIconModule} from "@angular/material/icon";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {MatMenuModule} from "@angular/material/menu";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatListModule} from "@angular/material/list";
import { FirmwareInstallerComponent } from './firmware-installer/firmware-installer.component';
import {MatGridListModule} from "@angular/material/grid-list";
@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HeaderNavbarComponent,
    GraphComponent,
    D3ForceComponent,
    SettingsComponent,
    FirmwareInstallerComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    FormsModule,
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    MatToolbarModule,
    MatCardModule,
    MatMenuModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
    MatGridListModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
