import { TestBed } from '@angular/core/testing';

import { PostAccessService } from './post-access.service';

describe('PostAccessService', () => {
  let service: PostAccessService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PostAccessService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
