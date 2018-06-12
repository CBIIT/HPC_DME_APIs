import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms'

import { AppComponent } from './app.component';
import { VaultSummaryReportComponent } from './vault-summary-report/vault-summary-report.component';
import { ReportsListComponent } from './reports-list/reports-list.component';

@NgModule({
  declarations: [
    AppComponent,
    VaultSummaryReportComponent,
    ReportsListComponent
  ],
  imports: [
    BrowserModule,
    FormsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
