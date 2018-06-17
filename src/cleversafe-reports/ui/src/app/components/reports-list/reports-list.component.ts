import { Component, OnInit } from '@angular/core';
import {Report} from "../../models/reports-list.model";
import {ReportType} from "../../models/reports-list.model";

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

  selectedReport: ReportType;
  vaultSummaryReport: ReportType = ReportType.VAULT_SUMMARY;

  constructor() { }

  ngOnInit() {
  }

  onSelect(report: Report): void {
    this.selectedReport = report.type;
  }

}
