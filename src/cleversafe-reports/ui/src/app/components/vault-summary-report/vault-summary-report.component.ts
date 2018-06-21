import { Component, OnInit } from '@angular/core';
import {VaultSummary} from "../../models/vault-summary.model";
import { VaultSummaryReportService} from "../../services/vault-summary-report.service";

@Component({
  selector: 'app-vault-summary-report',
  templateUrl: './vault-summary-report.component.html',
  styleUrls: ['./vault-summary-report.component.css']
})
export class VaultSummaryReportComponent implements OnInit {
  vaults: VaultSummary[];

  constructor(private vaultSummaryService: VaultSummaryReportService) { }

  getVaultSummaryReport(): void {
    this.vaultSummaryService.getVaultSummaryReport().subscribe(vaults => this.vaults = vaults);
  }

  ngOnInit() {
    this.getVaultSummaryReport();
  }

}
