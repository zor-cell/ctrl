import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminPanelComponent } from './admin-panel.component';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {afterEach} from 'vitest';

describe('AdminPanelComponent', () => {
  let component: AdminPanelComponent;
  let fixture: ComponentFixture<AdminPanelComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPanelComponent, HttpClientTestingModule],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminPanelComponent);
    component = fixture.componentInstance;

    fixture.detectChanges()

    const req = httpMock.expectOne(req => req.url.includes('/admin/verifications/pending'));
    req.flush(null)
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
