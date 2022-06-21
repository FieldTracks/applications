import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, tap} from "rxjs";
import * as moment from 'moment';

import {
  HttpClient,
  HttpEvent,
  HttpHandler,
  HttpHeaders,
  HttpInterceptor,
  HttpRequest,
  HttpUrlEncodingCodec
} from "@angular/common/http";
import jwt_decode from "jwt-decode";

@Injectable({
  providedIn: 'root',
})
export class LoginService {

  tokenSubject = new BehaviorSubject<string>(localStorage.getItem('id_token') || 'token');
  constructor(private http: HttpClient) {  }

  token(): BehaviorSubject<string> {
    return this.tokenSubject;
  }

  login(email: string, password: string ) {
    const encoder = new HttpUrlEncodingCodec();
    const options = {
      headers: new HttpHeaders().set('Content-Type', 'application/x-www-form-urlencoded')
    };
    return this.http.post('/api/login', 'user=' + encoder.encodeValue(email) + '&password=' + encoder.encodeValue(password) , options).pipe(
      tap((res) => {
        this.setSession(res); }
      )
    );
  }

  private setSession(token: any) {
    const authResult: any = jwt_decode(token['token'])
    const expires_epoch = authResult.exp;
    localStorage.setItem('id_token', token['token']);
    localStorage.setItem('expires_at', expires_epoch );
    this.tokenSubject.next(token['token']);
  }


  logout() {
    localStorage.removeItem('id_token');
    localStorage.removeItem('expires_at');
  }

  public isLoggedIn() {
    return this.getExpiration() > moment().unix();
  }

  isLoggedOut() {
    return !this.isLoggedIn();
  }

  getExpiration(): Number {
    const expiration = localStorage.getItem('expires_at');
    if(expiration != null) {
      return JSON.parse(expiration);
    } else {
      return 1; // expired at 1970-01-01
    }

  }
}

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    const idToken = localStorage.getItem('id_token');

    if (idToken) {
      const cloned = req.clone({
        headers: req.headers.set('Authorization', 'Bearer ' + idToken)
      });
      return next.handle(cloned);
    } else {
      return next.handle(req);
    }
  }
}

