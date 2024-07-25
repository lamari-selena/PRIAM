import { TestBed } from '@angular/core/testing';

import { PostSuppressionService } from './post-suppression.service';

describe('PostSuppressionService', () => {
  let service: PostSuppressionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PostSuppressionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
