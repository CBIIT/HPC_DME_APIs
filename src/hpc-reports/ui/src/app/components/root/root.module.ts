import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms'

import { RootComponent } from './root.component';
import { VaultSummaryReportComponent } from '../vault-summary-report/vault-summary-report.component';
import { ReportsListComponent } from '../reports-list/reports-list.component';
import { MessagesComponent } from "../messages/messages.component";
import { HttpClientModule} from "@angular/common/http";

@NgModule({
  declarations: [
    RootComponent,
    VaultSummaryReportComponent,
    ReportsListComponent,
    MessagesComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule
  ],
  providers: [],
  bootstrap: [RootComponent]
})
export class RootModule { }
