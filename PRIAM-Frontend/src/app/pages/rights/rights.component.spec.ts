import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyRightsComponent } from './my-rights.component';

describe('MyRightsComponent', () => {
  let component: MyRightsComponent;
  let fixture: ComponentFixture<MyRightsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MyRightsComponent]
    });
    fixture = TestBed.createComponent(MyRightsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
