// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

import {IClientOptions} from "mqtt/types/lib/client-options";

const mqtt_parameters: IClientOptions = {
  path: "/mqtt",
  hostname: "localhost",
  port: 8080,
  protocol: "ws",
}

const apiParameters = {
  http_base: "http://localhost:8080/api",
  ws_base: "ws://localhost:8080/api/ws"
}


export const environment = {
  production: false,
  mqtt_options: mqtt_parameters,
  api_parameters: apiParameters
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
