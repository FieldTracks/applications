import { Component, OnInit } from '@angular/core';
import {Router} from "@angular/router";
import {LoginService, ServerStatus} from "./login.service";
import {map, Observable, tap} from "rxjs";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  FormGroupDirective, NgForm,
  ValidationErrors,
  ValidatorFn,
  Validators
} from "@angular/forms";
import {ErrorStateMatcher} from "@angular/material/core";

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

  readonly isNotConfirmed: ErrorStateMatcher = {
    isErrorState(control: AbstractControl | null, form: FormGroupDirective | NgForm | null): boolean {
      return control != null && control.dirty && form!!.hasError("noMatch")
    }
  }

  private confirmationMatchesValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
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
    this.showSpinner = false;
    this.serverStatus = this.loginService.serverStatus()

  }

  onRegisterformSubmit() {
    this.doLogin(this.installerForm.controls)
  }

  onLoginformSubmit() {
    this.doLogin(this.loginForm.controls)
  }

  private doLogin(ctrls: { password: FormControl<string | null>, username: FormControl<string | null> }) {
    const username = ctrls.username.value || ""
    const password = ctrls.password.value || ""
    this.loginService.login(username, password).subscribe({
      next: () => console.log("Jo"),//this.router.navigate(['/stone-overview']),
      error: (e) => {
        console.log("Login-Error:", e)
        this.connectionProblem = true
        this.showSpinner = false
      }
    })
  }


}
