import { TestBed } from '@angular/core/testing';

import { PostRectificationService } from './post-rectification.service';

describe('PostRectificationService', () => {
  let service: PostRectificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PostRectificationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
