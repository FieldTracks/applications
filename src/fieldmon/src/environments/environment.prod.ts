import {IClientOptions} from "mqtt/types/lib/client-options";

const mqtt_parameters: IClientOptions = {
  host: window.location.host,
  hostname: window.location.host,
  port: (window.location.protocol == 'https') ? 443 : 80,
  protocol:  (window.location.protocol == 'https') ? "wss" : "ws",
}


export const environment = {
  production: true,
  mqtt_options: mqtt_parameters
};
