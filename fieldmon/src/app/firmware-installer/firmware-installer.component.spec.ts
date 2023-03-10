import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FirmwareInstallerComponent } from './firmware-installer.component';

describe('FirmwareInstallerComponent', () => {
  let component: FirmwareInstallerComponent;
  let fixture: ComponentFixture<FirmwareInstallerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FirmwareInstallerComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FirmwareInstallerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
