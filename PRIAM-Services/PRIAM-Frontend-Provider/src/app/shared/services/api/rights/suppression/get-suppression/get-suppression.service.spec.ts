import { TestBed } from '@angular/core/testing';

import { GetSuppressionService } from './get-suppression.service';

describe('GetSuppressionService', () => {
  let service: GetSuppressionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GetSuppressionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
