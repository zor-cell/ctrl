import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VerificationCallbackComponent } from './verification-callback.component';

describe('VerificationCallbackComponent', () => {
  let component: VerificationCallbackComponent;
  let fixture: ComponentFixture<VerificationCallbackComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerificationCallbackComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(VerificationCallbackComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
