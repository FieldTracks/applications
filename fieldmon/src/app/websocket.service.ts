import { Injectable } from '@angular/core';
import {BehaviorSubject, Observable, Subject, using} from "rxjs";
import {environment} from "../environments/environment";
import {LoginService} from "./login/login.service";

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {

  constructor(private loginService: LoginService) { }

  webSocketTopic<T>(endpoint: string): Observable<T> {
    const subject = ManagedSubjectWebsocket.subjectForChannel(endpoint, this.loginService.token())
    return using(() => {
        return {
          unsubscribe: () => {
            subject.refCount--
            if (subject.refCount <= 0) {
              ManagedSubjectWebsocket.removeSubject(endpoint)
            }
          }
        }
      },
      () => {
        return subject.subject
      });
  }

}
class ManagedSubjectWebsocket {
  private static SUBJECTS_BY_CHANNEL = new Map<String, ManagedSubjectWebsocket>()
  private ws: WebSocket

  constructor(public endpoint: string, public subject: Subject<any>, public refCount: number) {
  }

  static subjectForChannel(endpoint: string, tokenProvider: BehaviorSubject<string> ): ManagedSubjectWebsocket {
    let subject = ManagedSubjectWebsocket.SUBJECTS_BY_CHANNEL.get(endpoint)
    if (!subject || subject.refCount < 1) {
      subject = new ManagedSubjectWebsocket(endpoint, new Subject<any>(), 1)
      ManagedSubjectWebsocket.SUBJECTS_BY_CHANNEL.set(endpoint, subject)
      subject.assignWebsocket(tokenProvider)
    } else {
      subject.refCount++
    }
    return subject
  }

  assignWebsocket(tokenProvider: BehaviorSubject<string>) {
    console.log("Creating websocket for", this.endpoint)
    let ws = new WebSocket(environment.api_parameters.ws_base + "/" + this.endpoint)
    let that = this;
    ws.onmessage = (e) => {
      that.subject.next(JSON.parse(e.data));
    }
    ws.onerror = (e) => {
      console.log("Error in websocket", e)
    }
    ws.onclose = (e) => {
      console.log("Closing websocket",e)
      console.log("Re-initializing websocket in 3 seconds")
      setTimeout(function () {
        that.assignWebsocket(tokenProvider)
      }, 3000);
    }
    ws.onopen = () => {
      ws.send(tokenProvider.getValue())
    }
  }


  static removeSubject(channel: string) {
    ManagedSubjectWebsocket.SUBJECTS_BY_CHANNEL.delete(channel)
  }

  static subjectsMatchingTopic(topic: string): Set<Subject<any>> {
    const result = new Set<Subject<any>>()
    ManagedSubjectWebsocket.SUBJECTS_BY_CHANNEL.forEach((value, key, map) => {
      const mangledPrefix = key.replace('#', '')
      if (topic.startsWith(mangledPrefix)) {
        result.add(value.subject)
      }
    })
    return result
  }
}
