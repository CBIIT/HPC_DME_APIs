import { Component, OnInit } from '@angular/core';
import {VaultSummary} from "./shared/vault-summary.model";

@Component({
  selector: 'app-vault-summary-report',
  templateUrl: './vault-summary-report.component.html',
  styleUrls: ['./vault-summary-report.component.css']
})
export class VaultSummaryReportComponent implements OnInit {
  vaults: VaultSummary[] = [
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
    }];

  constructor() { }

  ngOnInit() {
  }

}
