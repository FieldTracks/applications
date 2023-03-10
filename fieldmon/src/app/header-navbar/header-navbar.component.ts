import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {EventbusService} from "../eventbus.service";

@Component({
  selector: 'app-header-navbar',
  templateUrl: './header-navbar.component.html',
  styleUrls: ['./header-navbar.component.sass']
})
export class HeaderNavbarComponent implements OnInit {


  @Input()
  showMenu: boolean = false

  @Input("title")
  sectionTitle: string = ""

  constructor(private eventbus: EventbusService) { }

  ngOnInit(): void {
  }

  showSidenav(): void {
    this.eventbus.onSidenavToggle.next()
  }

}

