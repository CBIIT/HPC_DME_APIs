import { Component, OnInit } from '@angular/core';
import {VaultSummary} from "../../models/vault-summary.model";
import { VaultSummaryReportService} from "../../services/vault-summary-report.service";

@Component({
  selector: 'app-vault-summary-report',
  templateUrl: './vault-summary-report.component.html',
  styleUrls: ['./vault-summary-report.component.css']
})
export class VaultSummaryReportComponent implements OnInit {
  /*vaults: VaultSummary[] = [
    {
      id: 1,
      name: 'DSE-Vault1',
      description: 'HPC-DME DEV vault',
      used: 9147841410,
      capacity: 16578821846
    },
    {
      id: 2,
      name: 'DSE-Vault2',
      description: 'HPC-DME UAT vault',
      used: 247324723236,
      capacity: 8789798789787897
    }];*/

    vaults: VaultSummary[];

  constructor(private vaultSummaryService: VaultSummaryReportService) { }

  getVaultSummaryReport(): void {
    this.vaultSummaryService.getVaultSummaryReport().subscribe(vaults => this.vaults = vaults);
  }

  ngOnInit() {
    this.getVaultSummaryReport();
  }

}
