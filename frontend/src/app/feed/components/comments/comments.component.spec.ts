import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommentsComponent } from './comments.component';
import {expect} from 'vitest';
import {MessageService} from "primeng/api";

describe('CommentsComponent', () => {
  let fixture: ComponentFixture<CommentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommentsComponent],
      providers: [
        { provide: MessageService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CommentsComponent);
    fixture.detectChanges();
  });

  // Test cases will be added here
  it("test", () => {
    expect(fixture.componentInstance).toBeTruthy();
  })
});
