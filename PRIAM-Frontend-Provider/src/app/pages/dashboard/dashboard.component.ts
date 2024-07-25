import { Component, OnInit } from '@angular/core';
import { GetDashboardService } from '../../shared/services/api/dashboard/get-dashboard/get-dashboard.service';
import { SuccessErrorService } from '../../shared/services/success-error/success-error.service';
import { DataSubjectCategory } from '../../interfaces/data-subject-category';
import { RequestFilter } from '../../interfaces/request-filter';
import { Request } from '../../interfaces/request';
import { SelectedRequest } from '../../interfaces/selected-request';
import { Router } from '@angular/router';
import { MatSelectionListChange } from '@angular/material/list';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {
  dataSubjectCategories: DataSubjectCategory[] = [];
  selectedRequestResponses: string[] = ["In Progress"];
  selectedRequestTypes: string[] = [];
  selectedDataSubjectCategories: DataSubjectCategory[] = this.dataSubjectCategories;
  requests: Request[] = [];
  typeToRouteMap: Map<string, string> = new Map([
    ['ACCESS', '/access-request'],
    ['RECTIFICATION', '/rectification'],
    ['ERASURE', '/suppression'],
  ]);

  constructor(
    private getDashboardService: GetDashboardService,
    private successErrorService: SuccessErrorService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.getDataSubjectCategory();
    this.getFilteredRequests();
  }

  getDataSubjectCategory() {
    this.getDashboardService.getDataSubjectCategory().subscribe(
      response => {
        this.dataSubjectCategories = response;
        this.successErrorService.handleSuccess('getDataSubjectCategory', response);
      },
      error => {
        this.successErrorService.handleError('getDataSubjectCategory', error);
      }
    );
  }

  isDateThreeWeeksOlder(issuedAt: Date): boolean {
    const currentDate = new Date();

    const threeWeeksAgo = new Date();
    threeWeeksAgo.setDate(currentDate.getDate() - 21);

    return issuedAt < threeWeeksAgo;
  }

  onRequestTypeChange() {
    this.getFilteredRequests();
  }

  onRequestResponseChange() {
    this.getFilteredRequests();
  }

  onDataSubjectCategoryChange() {
    this.getFilteredRequests();
  }

  getFilteredRequests() {
    const requestFilter: RequestFilter = {
      requestTypes: this.selectedRequestTypes,
      requestResponses: this.selectedRequestResponses,
      dataSubjectCategories: this.selectedDataSubjectCategories,
    };

    this.getDashboardService.getFilteredRequests(requestFilter).subscribe(
      response => {
        this.requests = response;
        this.successErrorService.handleSuccess('getFilteredRequests', response);
      },
      error => {
        this.successErrorService.handleError('getFilteredRequests', error);
      }
    );
  }

  getSelectedRequest(requestId: number, requestType: string, response: boolean) {
    this.getDashboardService.selectedRequest = { dataRequestId: requestId, dataRequestType: requestType, response };
    const routePath = this.typeToRouteMap.get(requestType);
    this.router.navigate([routePath]);
  }
}
