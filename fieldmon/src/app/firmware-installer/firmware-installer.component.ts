import { Component } from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";

@Component({
  selector: 'app-firmware-installer',
  templateUrl: './firmware-installer.component.html',
  styleUrls: ['./firmware-installer.component.sass']
})
export class FirmwareInstallerComponent {

/*
  WLAN_SSID,data,string,fieldtracks
  WLAN_PSK,data,string,ThisIsSecret
  MQTT_URL,data,string,wss://dev.fieldtracks.org:8882
    MQTT_USER,data,string,esp32-dev-1
  MQTT_PWD,data,string,ThisIsSecret
  MQTT_CERT,file,string,./isrgrootx1.pem
  BLE_EDDY_ORG,data,hex2bin,c35ddb7c3e21921ce97b
  BLE_EDDY_INST,data,u32,42
*/

  installerForm = new FormGroup({
    bleEddyOrg: new FormControl('',Validators.required),
    bleEddyInst: new FormControl('',Validators.required),
    wlanSsid: new FormControl('',Validators.required),
    wlanPsk: new FormControl('',Validators.required),
    mqttUrl: new FormControl('',Validators.required),
    mqttUser: new FormControl('',Validators.required),
    mqttPwd: new FormControl('',Validators.required),
  });

  onSubmitInstallerForm() {

  }
}
