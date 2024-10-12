import { TestBed } from '@angular/core/testing';

import { AccessRequestService } from './access-request.service';

describe('AccessRequestService', () => {
  let service: AccessRequestService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AccessRequestService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
