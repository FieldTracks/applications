export const environment = {
  production: true,

  mqtt_url: `ws://${window.location.hostname}:8883/`,
  uuid: 'fd:a5:06:93:a4:e2:4f:b1:af:cf:c6:eb:07:64:78:25',
  grafana_base: window.location.protocol + '//' +  window.location.hostname + '/grafana/d/Stones/stones?orgId=1&var-node='
};
