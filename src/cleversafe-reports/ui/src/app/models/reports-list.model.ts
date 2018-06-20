export enum ReportType { NONE, VAULT_SUMMARY, OTHER}

export class Report {
  type: ReportType;
  name: string;
}
