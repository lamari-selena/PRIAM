import { TestBed } from '@angular/core/testing';

import { GetDashboardService } from './get-dashboard.service';

describe('GetDashboardService', () => {
  let service: GetDashboardService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GetDashboardService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
