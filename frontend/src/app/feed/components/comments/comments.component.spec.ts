import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommentsComponent } from './comments.component';
import {expect} from 'vitest';

describe('CommentsComponent', () => {
  let fixture: ComponentFixture<CommentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommentsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CommentsComponent);
    fixture.detectChanges();
  });

  // Test cases will be added here
  it("test", () => {
    expect(true);
  })
});
