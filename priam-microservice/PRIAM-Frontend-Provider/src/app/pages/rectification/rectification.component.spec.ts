import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RectificationComponent } from './rectification.component';

describe('RectificationComponent', () => {
  let component: RectificationComponent;
  let fixture: ComponentFixture<RectificationComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RectificationComponent]
    });
    fixture = TestBed.createComponent(RectificationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
