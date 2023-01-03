import {AfterContentInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Router} from "@angular/router";
import {LoginService} from "./login/login.service";
import {EventbusService} from "./eventbus.service";
import {MatSidenav} from "@angular/material/sidenav";
import {Subject, Subscription} from "rxjs";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.sass']
})
export class AppComponent implements AfterContentInit, OnDestroy  {
  title = 'app';

  private sideBarSubscription: Subscription
  @ViewChild("sidenav") sidenav: MatSidenav


  constructor(private router: Router, public loginService: LoginService, public eventbus: EventbusService) {
    if(loginService.isLoggedOut()) {
      router.navigate(['login'])
    }
  }


  ngAfterContentInit() {
    this.sideBarSubscription = this.eventbus.onSidenavToggle.subscribe(() => {
      this.sidenav.toggle()
    })
  }

  ngOnDestroy() {
    if (this.sideBarSubscription != null) {
      this.sideBarSubscription.unsubscribe()
    }
  }

  logout() {
    this.loginService.logout()
    this.sidenav.close()
  }

  // showToggle: boolean;
  //openSidenav = false;
  // private subscription: Subscription;
  // searchString: string;
  //
  // @ViewChild('searchInput') searchField: ElementRef;
  //
  // private menu: MatMenu;
  //
  // constructor(/*private headerBarService: HeaderBarService, */private loginService: LoginService, private router: Router) {
  //   if (loginService.isLoggedOut()) {
  //     this.router.navigate(['login']);
  //   }
  //
  // }
  //
  // public updateSearch(): void {
  //   setTimeout(() => {  // Timeout for better UX
  //     //this.headerBarService.searchEntered.emit(this.searchString);
  //   });
  // }
  //
  // ngOnInit() {
  //   // this.subscription = this.headerBarService.searchEntered.subscribe((searchString) => {
  //   //   this.searchString = searchString;
  //   // });
  // }
  //
  // ngOnDestroy() {
  //   this.subscription.unsubscribe();
  // }
  //
  // routerOutletActivated(component: any) {
  //   let config = {sectionTitle: ''}
  //   if (component.fmHeader) {
  //     config = component.fmHeader();
  //   }
  //   //this.headerBarService.updateConfiguration(config);
  //   let items = [];
  //   if (component.fmMenuItems) {
  //     items = component.fmMenuItems();
  //   }
  //  // this.menu = null;
  //   if (component.fmtMenu) {
  //     this.menu = component.fmtMenu();
  //   }
  //   //this.headerBarService.setMatMenu(this.menu);
  //
  //   //this.headerBarService.setMenu(items);
  //   this.showToggle = false;
  // }
  //
  // toggleSearch() {
  //   this.showToggle = !this.showToggle;
  //   if (this.showToggle) {
  //     setTimeout( () => // Hack (c.f. https://stackoverflow.com/questions/50006888/angular-5-set-focus-on-input-element)
  //         this.searchField.nativeElement.focus()
  //       , 0);
  //   }
  // }

}
