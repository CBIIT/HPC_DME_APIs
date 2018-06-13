import { Component, OnInit } from '@angular/core';
import {Report} from "./shared/reports-list.model";
import {ReportType} from "./shared/reports-list.model";
import {VaultSummary} from "../vault-summary-report/shared/vault-summary.model";

@Component({
  selector: 'app-reports-list',
  templateUrl: './reports-list.component.html',
  styleUrls: ['./reports-list.component.css']
})
export class ReportsListComponent implements OnInit {
  reports: Report[] = [
    {
      type: ReportType.VAULT_SUMMARY,
      name: 'Vault Summary'
    },
    {
      type: ReportType.OTHER,
      name: 'Other'
    }];

  selectedReport: Report;

  constructor() { }

  ngOnInit() {
  }

  onSelect(report: Report): void {
    this.selectedReport = report;
  }

}
