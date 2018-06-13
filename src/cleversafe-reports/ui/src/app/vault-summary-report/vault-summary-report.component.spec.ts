import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { VaultSummaryReportComponent } from './vault-summary-report.component';

describe('VaultSummaryReportComponent', () => {
  let component: VaultSummaryReportComponent;
  let fixture: ComponentFixture<VaultSummaryReportComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ VaultSummaryReportComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VaultSummaryReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
