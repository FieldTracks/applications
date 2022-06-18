import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { FlashtoolComponent } from './flashtool.component';

describe('FlashtoolComponent', () => {
  let component: FlashtoolComponent;
  let fixture: ComponentFixture<FlashtoolComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ FlashtoolComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FlashtoolComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
