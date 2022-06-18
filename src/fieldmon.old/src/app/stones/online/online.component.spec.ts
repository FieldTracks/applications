import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { OnlineComponent } from './online.component';

describe('OnlineComponent', () => {
  let component: OnlineComponent;
  let fixture: ComponentFixture<OnlineComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ OnlineComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OnlineComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
