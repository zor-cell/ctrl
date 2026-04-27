import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../auth.service';
import { RegisterRequest } from '../../dto/RegisterRequest';
import { NotificationService } from '../../../core/services/notification.service';
import { EmailVerificationComponent } from './email-verification.component';

describe('EmailVerificationComponent', () => {
  let component: EmailVerificationComponent;
  let fixture: ComponentFixture<EmailVerificationComponent>;
  let routerMock: { currentNavigation: ReturnType<typeof vi.fn> };
  let activatedRouteMock: { snapshot: { paramMap: { has: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn> } } };
  const expectedRegisterRequest: RegisterRequest = {
    username: 'test-user',
    email: 'test@example.com',
    password: 'secret123',
    name: 'test-name',
    birthdate: '21-11-2003',
  };

  const createComponentWithRouterState = (registerRequest: RegisterRequest) => {
    routerMock.currentNavigation.mockReturnValue({
      extras: {
        state: {
          registerRequest,
        },
      },
    });

    // Mock ActivatedRoute snapshot params
    activatedRouteMock.snapshot.paramMap.has.mockImplementation((key: string) => {
      return key === 'email' || key === 'username';
    });
    activatedRouteMock.snapshot.paramMap.get.mockImplementation((key: string) => {
      if (key === 'email') return registerRequest.email;
      if (key === 'username') return registerRequest.username;
      return null;
    });

    fixture = TestBed.createComponent(EmailVerificationComponent);
    component = fixture.componentInstance;
  };

  beforeEach(async () => {
    routerMock = {
      currentNavigation: vi.fn().mockReturnValue(undefined),
    };

    activatedRouteMock = {
      snapshot: {
        paramMap: {
          has: vi.fn(),
          get: vi.fn(),
        },
      },
    };

    await TestBed.configureTestingModule({
      imports: [EmailVerificationComponent],
      providers: [
        {
          provide: Router,
          useValue: routerMock,
        },
        {
          provide: ActivatedRoute,
          useValue: activatedRouteMock,
        },
        {
          provide: AuthService,
          useValue: {
            verify_email: () => of(true),
            resendVerificationEmail: () => of(void 0),
          },
        },
        {
          provide: NotificationService,
          useValue: {
            showWarning: () => undefined,
          },
        },
      ],
    }).compileComponents();

    history.replaceState({}, '');
    createComponentWithRouterState(expectedRegisterRequest);
    fixture.detectChanges();
  });

  it('should create with username and email data', () => {
    expect(component).toBeTruthy();
    expect(component.emailData).toEqual({
      username: expectedRegisterRequest.username,
      email: expectedRegisterRequest.email,
    });
  });

  it('should display button with lable "Verify"', () => {
    const buttonElement: HTMLButtonElement = fixture.nativeElement.querySelector('p-button');
    expect(buttonElement).toBeTruthy();
    expect(buttonElement.textContent).toContain('Verify');
    expect(component.showResendButton).toEqual(false);
  });

  it('should start resend cooldown with "send email" initial state and countdown', () => {
    const timers = vi.useFakeTimers();

    component['startResendCooldown']();

    const buttonVerifyElement: HTMLButtonElement = fixture.nativeElement.querySelector('p-button');
    expect(buttonVerifyElement).toBeTruthy();
    expect(buttonVerifyElement.textContent).toContain('Verify');

    expect(component.showResendButton).toEqual(false);
    expect(component.resendCountdownSeconds).toEqual(60);

    timers.advanceTimersByTime(60000);

    expect(component.showResendButton).toEqual(true);
    expect(component.resendCountdownSeconds).toEqual(0);

    const buttonElement: HTMLButtonElement = fixture.nativeElement.querySelector('p-button');
    expect(buttonElement).toBeTruthy();
    expect(buttonElement.textContent).toContain('Resend email');
    timers.useRealTimers();
  });

  it('should not submit verification code with 5 numbers only', () => {
    const authServiceMock = TestBed.inject(AuthService);
    const verifySpy = vi.spyOn(authServiceMock, 'verify_email');

    component.verificationForm.controls.code.setValue('12345');
    component.submitCode();

    expect(component.verificationForm.invalid).toEqual(true);
    expect(component.verificationForm.touched).toEqual(true);
    expect(component.verificationForm.dirty).toEqual(true);
    expect(verifySpy).not.toHaveBeenCalled();
  });

  it('should display enter valid code prompt with 5 numbers only', () => {
    component.verificationForm.controls.code.setValue('12345');
    component.verificationForm.controls.code.markAllAsTouched();
    fixture.detectChanges();

    const errorMessageElement = fixture.nativeElement.querySelector('p-message[severity="error"]');
    expect(errorMessageElement).toBeTruthy();
    expect(errorMessageElement.textContent).toContain('Please enter a valid 6-digit code.');
  });
});
