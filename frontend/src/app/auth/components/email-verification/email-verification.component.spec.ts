import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
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

    fixture = TestBed.createComponent(EmailVerificationComponent);
    component = fixture.componentInstance;
  };

  beforeEach(async () => {
    routerMock = {
      currentNavigation: vi.fn().mockReturnValue(undefined),
    };

    await TestBed.configureTestingModule({
      imports: [EmailVerificationComponent],
      providers: [
        {
          provide: Router,
          useValue: routerMock,
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
  });

  it('should create with register request from router state', () => {
    expect(component).toBeTruthy();
    expect(routerMock.currentNavigation).toHaveBeenCalled();
    expect((component as unknown as { registerRequest: RegisterRequest | null }).registerRequest)
      .toEqual(expectedRegisterRequest);
  });
});
