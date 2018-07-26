import { TestBed, inject } from '@angular/core/testing';

import { VaultSummaryReportService } from './vault-summary-report.service';

describe('VaultSummaryReportService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [VaultSummaryReportService]
    });
  });

  it('should be created', inject([VaultSummaryReportService], (service: VaultSummaryReportService) => {
    expect(service).toBeTruthy();
  }));
});
