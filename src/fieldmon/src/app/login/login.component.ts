import { Component, OnInit } from '@angular/core';
import {Router} from "@angular/router";
import {LoginService, ServerStatus} from "./login.service";
import {map, Observable, tap} from "rxjs";
import {AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.sass']
})
export class LoginComponent implements OnInit {

  loginForm = new FormGroup({
    username: new FormControl('',Validators.required),
    password: new FormControl('',Validators.required),
  });

  confirmationMatchesValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const password = control.get('password')!!;
    const passwordConfirmation = control.get('passwordConfirmation')!!;
    if(!passwordConfirmation.dirty) {
      return null
    } else if (password.value === passwordConfirmation.value) {
      return null
    } else {
      return { noMatch: true }
    }
  };

  installerForm = new FormGroup({
    username: new FormControl('admin', [Validators.required]),
    password: new FormControl('',[Validators.required]),
    passwordConfirmation: new FormControl('',[Validators.required])
  },{ validators: this.confirmationMatchesValidator });

  showSpinner: boolean = false
  connectionProblem: boolean;
  serverStatus: Observable<ServerStatus>

  constructor(private router: Router, private loginService: LoginService) {
  }

  ngOnInit() {
    //this.titleService.currentConfiguration.next({sectionTitle: 'Login'});
    this.showSpinner = false;
    this.serverStatus = this.loginService.serverStatus()

  }


  register() {
    const ctrls = this.installerForm.controls
    const username = ctrls.username.value || ""
    const password = ctrls.password.value || ""
    this.loginService.login(username, password).subscribe({
      next: () => console.log("Jo"),//this.router.navigate(['/stone-overview']),
      error: (e) => {
        console.log("Registration-error:", e)
        this.connectionProblem = true
        this.showSpinner = false
      }
    })
  }

  login() {
    this.showSpinner = true;
    const ctrls = this.loginForm.controls
    const username = ctrls.username.value || ""
    const password = ctrls.password.value || ""

    this.loginService.login(username, password). subscribe({
      next: () => console.log("Jo"),//this.router.navigate(['/stone-overview']),
      error: (e) => {
        console.log("Login-error:", e)
        this.connectionProblem = true
        this.showSpinner = false
      }
    })
  }

}
