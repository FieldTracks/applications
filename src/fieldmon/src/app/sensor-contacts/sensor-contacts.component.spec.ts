import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { SensorContactsComponent } from './sensor-contacts.component';

describe('SensorContactsComponent', () => {
  let component: SensorContactsComponent;
  let fixture: ComponentFixture<SensorContactsComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ SensorContactsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SensorContactsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
