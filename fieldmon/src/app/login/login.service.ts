import {Injectable} from '@angular/core';
import {BehaviorSubject, catchError, map, mergeMap, Observable, of, tap, timer} from "rxjs";
import * as moment from 'moment';

import {
  HttpClient,
  HttpErrorResponse,
  HttpEvent,
  HttpEventType,
  HttpHandler,
  HttpInterceptor,
  HttpRequest, HttpResponse
} from "@angular/common/http";
import jwt_decode from "jwt-decode";
import {EventbusService} from "../eventbus.service";

@Injectable({
  providedIn: 'root',
})
export class LoginService {

  tokenSubject = new BehaviorSubject<string>(localStorage.getItem('id_token') || 'token');
  constructor(private http: HttpClient, private eventbus: EventbusService) {
    if (this.isLoggedIn()) {
      eventbus.onLoginStatusChange.next(true)
    }
  }

  token(): BehaviorSubject<string> {
    return this.tokenSubject;
  }

  serverStatus(): Observable<ServerStatus> {

    return timer(0,5000).pipe(
      mergeMap(_ => {
        return this.http.get<ServerStatus>("/api/status").pipe(
          catchError(val => of({status: <ServerStatusType>`UNREACHABLE`, error: val}))
        )
      })
    )

  }

  login(user: string, password: string ):Observable<void> {
    return this.http.post<{ token: string }>('/api/login', {user: user, password: password}).pipe(
      map ( httpResult => {
        this.setSession(httpResult)
      })
    )
  }

  private setSession(token: { token: string }) {
    const authResult: any = jwt_decode(token['token'])
    console.log("Token received")
    const expires_epoch = authResult.exp;
    localStorage.setItem('id_token', token['token']);
    localStorage.setItem('expires_at', expires_epoch );
    this.tokenSubject.next(token['token']);
    this.eventbus.onLoginStatusChange.next(true)
  }


  logout() {
    localStorage.removeItem('id_token');
    localStorage.removeItem('expires_at');
    this.eventbus.onLoginStatusChange.next(false)
  }

  public isLoggedIn() {
    return this.getExpiration() > moment().unix();
  }

  isLoggedOut() {
    return !this.isLoggedIn();
  }

  getExpiration(): Number {
    try {
      const expiration = localStorage.getItem('expires_at');
      return JSON.parse(expiration!!);
    } catch (e) {
      return 1
    }
  }
}

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private loginService: LoginService) {
  }


  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    const idToken = localStorage.getItem('id_token');

    if (idToken) {
      const cloned = req.clone({
        headers: req.headers.set('Authorization', 'Bearer ' + idToken)
      });
      return next.handle(cloned).pipe(tap( (event) => {
        if (event instanceof HttpResponse) {
          if (event.status == 401 || event.status == 403 ) {
            this.loginService.logout()
          }
        }
      }));
    } else {
      return next.handle(req);
    }
  }
}

export interface ServerStatus {
  status: ServerStatusType
  error?: HttpErrorResponse
}

type ServerStatusType = "DISCONNECTED" | "RUNNING" | "INSTALLER" | "UNREACHABLE"
