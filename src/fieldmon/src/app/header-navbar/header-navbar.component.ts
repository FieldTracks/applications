import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {MatMenuTrigger} from "@angular/material/menu";

@Component({
  selector: 'app-header-navbar',
  templateUrl: './header-navbar.component.html',
  styleUrls: ['./header-navbar.component.sass']
})
export class HeaderNavbarComponent implements OnInit {

  @ViewChild(MatMenuTrigger) matMenu: MatMenuTrigger;

  @Input()
  showMenu: boolean = false



  constructor() { }

  ngOnInit(): void {
  }

}
