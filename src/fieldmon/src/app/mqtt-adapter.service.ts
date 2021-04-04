import * as mqtt from 'mqtt';

import {Injectable, OnDestroy} from '@angular/core';
import {environment} from './../environments/environment';
import {ConnectableObservable, Observable, PartialObserver, Subject, Subscription} from 'rxjs';
import {Router} from '@angular/router';
import {StoneConfiguration} from './model/StoneConfiguration';
import {map} from 'rxjs/operators';
import {FlashtoolStatus} from './model/flashtool/flashtool-status';
import {AggregatedStone} from './model/aggregated/aggregated-stone';
import {AggregatedGraph, AggregatedGraphLink, AggregatedGraphNode} from './model/aggregated/aggregated-graph';
import {StoneEvent} from './model/StoneEvent';
import {AggregatedName } from './model/aggregated/aggregated-name';
import {FieldmonConfig} from './model/configuration/fieldmon-config';
import {StoneStatus} from './model/stone-status';
import {LoginService} from './login.service';
import {MqttClient} from "mqtt";

@Injectable({
  providedIn: 'root',
})
export class MqttAdapterService implements OnDestroy {
  private readonly loginSubscript: Subscription;
  private client: MqttClient;
  private topicsForSubscriptions: Map<String,Subject<any>>;

  constructor(private router: Router, private loginService: LoginService) {
    this.topicsForSubscriptions = new Map<String,Subject<any>>()
    this.loginSubscript = loginService.token().subscribe( (v) => this.credential_update(v));
    this.connect();
  }

  credential_update(token: string) {
    this.connect()
  }

  private connect(): void {
    try {
      this.client.end()
    } catch (err) {
      // Ignore stop errors - the client is not running in this situations
    }

    this.client = mqtt.connect(environment.mqtt_url, {
      transformWsUrl: (url, options, client) => {
        client.options.username = localStorage.getItem('id_token')
        client.options.password = localStorage.getItem('id_token')
        return url;
      }
    })
    this.client.on('on', () => console.log('MQTT connected'))
    this.client.on('error', (err) => {console.log('Error in mqtt-adapter',err)})
    this.client.on('close', () => {console.log('close in mqtt-adapter')})
    this.client.on('offline', (err) => {console.log('offline in mqtt-adapter',err)})
    this.client.on('end', (err) => {console.log('end in mqtt-adapter',err)})
    this.client.on('message',  (topic, payload, packet) => {
      this.onMsgRecv(topic, payload, packet)
    })
 }

  private onMsgRecv(topic: string, payload: any, packet: any) {
    try {
      const data = JSON.parse(payload.toString());
      this.topicsForSubscriptions.get(topic).next(data)
    } catch (err) {
      console.log(`Error parsing / delivering to ${topic} ${err}`, [topic,payload,packet])
    }
  }

  ngOnDestroy(): void {
       if (this.loginSubscript) {
         this.loginSubscript.unsubscribe();
       }
    }


  public publishName(mac: String, name: String): void {
    this.client.publish('NameUpdate', JSON.stringify({
      'mac': mac,
      'name': name,
      'color': '#ff0000'}))
  }


  public sendInstallSoftware(sc: StoneConfiguration) {
    this.client.publish('flashtool/command', JSON.stringify({
      operation: 'full_flash',
      stone: sc}))
  }

  public sendInstallConfiguration(sc: StoneConfiguration) {
    this.client.publish('flashtool/command', JSON.stringify({
      operation: 'nvs',
      stone: sc}))
    const s = new Subject<string>()
    s.unsubscribe()

  }

  public aggregatedStonesSubject(): Observable<Map<string, AggregatedStone>> {
    return this.observableFor("Aggregated/Stones")
  }

  private observableFor(topic: string): Subject<any> {
    if(!this.topicsForSubscriptions.has(topic)) {
      const subject = new Subject<any>()
      this.topicsForSubscriptions.set(topic,subject)
      this.client.subscribe(topic)
    }
    return this.topicsForSubscriptions.get(topic)
  }

  public statusSubject(mac: string): Observable<StoneStatus> {
    return this.observableFor(`JellingStoneStatus/${mac}`)
  }

  public aggregatedNamesSubject(): Observable<Map<string, AggregatedName>> {
    return this.observableFor('Aggregated/Names').pipe(
      map( (jsonObj) => {
        const parsed = new Map<string, AggregatedName>();
        for (const mac in jsonObj) {
          if (mac) {
            parsed.set(mac, jsonObj[mac]);
          }
        }
        return parsed;
      })
    )
  }

  public aggregatedGraphSubject(): Observable<AggregatedGraph> {
    return this.aggregatedStonesSubject().pipe(map(stoneMap => {
      const nodeMap = new Map<string, AggregatedGraphNode>();
      const links: AggregatedGraphLink[] = [];

      // Build node database
      for (const mac in stoneMap) {
        if (mac) {
          nodeMap.set(mac, {id: mac, timestamp: new Date(stoneMap[mac].timestamp)});
          stoneMap[mac].contacts.forEach( (contact) => {
            nodeMap.set(contact.mac, {id: contact.mac});
          });
        }
      }

      // Build link database
      for (const mac in stoneMap) {
        if (mac) {
          const stone = stoneMap[mac];
          stone.contacts.forEach( (contact) => {
            links.push(
              {source: mac, target: contact.mac, rssi: contact.rssi_avg, timestamp: new Date(stone.timestamp)});
          });
        }
      }

      // Collect nodeMap and links
      return  {
        links: links,
        nodes: Array.from(nodeMap.values())
      };
    }));
  }


  public stoneEventSubject(): Observable<StoneEvent> {
    return this.observableFor('JellingStone/#')
  }

  public flashToolSubject(): Observable<FlashtoolStatus> {
    return this.observableFor('flashtool/status/#')
  }


  public fieldmonSubject(): Observable<FieldmonConfig> {
    return this.observableFor('fieldmon/config')
  }

  public publishFieldmonConfig(config: FieldmonConfig): void {
    this.client.publish('fieldmon/config', JSON.stringify(config), {qos: 1, retain: true})
  }

}
