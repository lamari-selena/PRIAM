import { TestBed } from '@angular/core/testing';

import { GetRectificationService } from './get-rectification.service';

describe('GetRectificationService', () => {
  let service: GetRectificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GetRectificationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
