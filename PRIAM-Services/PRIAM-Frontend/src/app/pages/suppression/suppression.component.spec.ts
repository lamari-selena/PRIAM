import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuppressionComponent } from './suppression.component';

describe('SuppressionComponent', () => {
  let component: SuppressionComponent;
  let fixture: ComponentFixture<SuppressionComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SuppressionComponent]
    });
    fixture = TestBed.createComponent(SuppressionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
