import { Component, OnInit } from '@angular/core';
import {GraphConfig} from "./graph.model";

@Component({
  selector: 'app-graph',
  templateUrl: './graph.component.html',
  styleUrls: ['./graph.component.sass']
})
export class GraphComponent implements OnInit {

  graphConfig: GraphConfig = {}

  constructor() { }

  ngOnInit(): void {
  }

  openDialog() {

  }

  openSettingsDialog() {

  }

  showUnnamed(b: boolean) {
    this.graphConfig.showUnnamed = b
  }

  showLastContact(b: boolean) {
    this.graphConfig.showLastContacts = b

  }
}
