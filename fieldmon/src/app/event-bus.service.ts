import { Injectable } from '@angular/core';
import {BehaviorSubject, Subject} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class EventbusService {

  constructor() { }

  public onSidenavToggle = new Subject<void>();

  public onLoginStatusChange = new BehaviorSubject<boolean>(false)

}
