import { Component, OnInit } from '@angular/core';
import { GetAccessService } from '../../shared/services/api/rights/access/get-access/get-access.service';

@Component({
  selector: 'app-requests',
  templateUrl: './requests.component.html',
  styleUrls: ['./requests.component.css']
})

export class RequestsComponent implements OnInit {
  constructor(
    public getAccessService: GetAccessService,
  ) {}

  ngOnInit() {
  }
}
