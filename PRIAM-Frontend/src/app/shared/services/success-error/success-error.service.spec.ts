import { TestBed } from '@angular/core/testing';

import { SuccessErrorService } from './success-error.service';

describe('SuccessErrorService', () => {
  let service: SuccessErrorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SuccessErrorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
