import { Injectable } from '@angular/core';
import { connect } from "mqtt"
import {Observable, Subject, Subscription, using} from "rxjs";
import {MqttClient} from "mqtt";
import {Router} from "@angular/router";
import {LoginService} from "./login/login.service";
import {environment} from "../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class MqttService {

  private readonly loginSubscript: Subscription;
  private client: MqttClient;

  constructor(private router: Router, private loginService: LoginService) {
    this.loginSubscript = loginService.token().subscribe((v) => this.credential_update(v));
    this.connect();
  }

  private credential_update(token: string) {
    //this.connect()
  }

  private connect(): void {
    try {
      this.client.end()
    } catch (err) {
      // Ignore stop errors - the client is not running in this situations
    }

    this.client = connect(environment.mqtt_options)

    // this.client = connect(environment.mqtt_url, {
    //   // transformWsUrl: (url, options, client) => {
    //   //   client.options.username = localStorage.getItem('id_token') || ""
    //   //   client.options.password = localStorage.getItem('id_token') || ""
    //   //   return url;
    //   // }
    // })
    this.client.on('on', () => console.log('MQTT connected'))
    this.client.on('error', (err) => {
      console.log('Error in mqtt-adapter', err)
    })
    this.client.on('close', () => {
      console.log('close in mqtt-adapter')
    })
    this.client.on('offline', () => {
      console.log('offline in mqtt-adapter')
    })
    this.client.on('end', () => {
      console.log('end in mqtt-adapter')
    })
    this.client.on('message', (topic, payload, packet) => {
      this.onMsgRecv(topic, payload, packet)
    })
  }


  ngOnDestroy(): void {
    if (this.loginSubscript) {
      this.loginSubscript.unsubscribe();
    }
    try {
      this.client.end()
    } catch (err) {
      // Ignore errors - just exit
    }
  }

  mqttTopic<T>(topic: string): Observable<T> {
    const subject = ManagedSubjectMQTT.subjectForChannel(topic)
    if (subject.refCount == 1) {
      this.client.subscribe(topic)
    }
    return using(() => {
        return {
          unsubscribe: () => {
            subject.refCount--
            if (subject.refCount <= 0) {
              this.client.unsubscribe(topic)
              ManagedSubjectMQTT.removeSubject(topic)
            }
          }
        }
      },
      () => {
        return subject.subject
      });
  }

  private onMsgRecv(topic: string, payload: Buffer, packet: any) {
    try {
      const data = JSON.parse(payload.toString())
      ManagedSubjectMQTT.subjectsMatchingTopic(topic).forEach((subj) => {
        subj.next(data)
      })

    } catch (err) {
      console.log(`Error parsing / delivering to ${topic} ${err}`, [topic, payload, packet])
    }
  }
}

class ManagedSubjectMQTT {
  private static SUBJECTS_BY_CHANNEL = new Map<String, ManagedSubjectMQTT>()

  constructor(public subject: Subject<any>, public refCount: number) {
  }

  static subjectForChannel(channel: string): ManagedSubjectMQTT {
    let subject = ManagedSubjectMQTT.SUBJECTS_BY_CHANNEL.get(channel)
    if (!subject || subject.refCount < 1) {
      subject = new ManagedSubjectMQTT(new Subject<any>(), 1)
      ManagedSubjectMQTT.SUBJECTS_BY_CHANNEL.set(channel, subject)
    } else {
      subject.refCount++
    }
    return subject
  }

  static removeSubject(channel: string) {
    ManagedSubjectMQTT.SUBJECTS_BY_CHANNEL.delete(channel)
  }

  static subjectsMatchingTopic(topic: string): Set<Subject<any>> {
    const result = new Set<Subject<any>>()
    ManagedSubjectMQTT.SUBJECTS_BY_CHANNEL.forEach((value, key, map) => {
      const mangledPrefix = key.replace('#', '')
      if (topic.startsWith(mangledPrefix)) {
        result.add(value.subject)
      }
    })
    return result
  }
}


