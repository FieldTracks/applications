import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { CommonModule } from '@angular/common';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';

import { LoginComponent } from './login/login.component';

import {MatLegacyProgressSpinnerModule as MatProgressSpinnerModule} from '@angular/material/legacy-progress-spinner';
import {MatLegacyFormFieldModule as MatFormFieldModule} from '@angular/material/legacy-form-field';
import {MatLegacyInputModule as MatInputModule} from "@angular/material/legacy-input";
import {MatLegacyButtonModule as MatButtonModule} from "@angular/material/legacy-button";
import {MatToolbarModule} from "@angular/material/toolbar";
import {MatLegacyCardModule as MatCardModule} from "@angular/material/legacy-card";
import { HeaderNavbarComponent } from './header-navbar/header-navbar.component';
import { GraphComponent } from './graph/graph.component';
import { D3ForceComponent } from './graph/d3-force/d3-force.component';
import { SettingsComponent } from './graph/settings/settings.component';
import {MatLegacyMenuModule as MatMenuModule} from "@angular/material/legacy-menu";
import {MatIconModule} from "@angular/material/icon";
@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HeaderNavbarComponent,
    GraphComponent,
    D3ForceComponent,
    SettingsComponent
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
    MatIconModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
