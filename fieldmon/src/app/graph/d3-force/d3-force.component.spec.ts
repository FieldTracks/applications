import { ComponentFixture, TestBed } from '@angular/core/testing';

import { D3ForceComponent } from './d3-force.component';

describe('D3ForceComponent', () => {
  let component: D3ForceComponent;
  let fixture: ComponentFixture<D3ForceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ D3ForceComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(D3ForceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
