import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { NamesComponent } from './names.component';

describe('NamesComponent', () => {
  let component: NamesComponent;
  let fixture: ComponentFixture<NamesComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ NamesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NamesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
