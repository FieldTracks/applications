import { Injectable } from '@angular/core';
import {BehaviorSubject} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GraphConfigService {

  readonly currentConfig = new BehaviorSubject<GraphConfig>({});

  constructor() { }


}


