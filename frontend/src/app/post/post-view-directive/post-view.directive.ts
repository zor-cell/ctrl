import {
  Directive, ElementRef, Input, Output,
  EventEmitter, OnInit, OnDestroy, inject
} from '@angular/core';

export interface ViewEvent {
  postId: string;
  durationMs: number;
}

@Directive({
  selector: '[appPostView]',
  standalone: true,
})
export class PostViewDirective implements OnInit, OnDestroy {
  @Input({ required: true }) appPostView!: string;
  @Output() viewed = new EventEmitter<ViewEvent>();

  private readonly el = inject(ElementRef);
  private observer!: IntersectionObserver;
  private enteredAt: number | null = null;

  private readonly VISIBILITY_THRESHOLD = 0.5;
  private readonly MIN_VIEW_MS = 1000;

  /** Starts the IntersectionObserver on the host element. */
  ngOnInit(): void {
    this.observer = new IntersectionObserver(
      (entries) => this.onIntersection(entries),
      { threshold: this.VISIBILITY_THRESHOLD }
    );
    this.observer.observe(this.el.nativeElement);
  }

  /**
   * Responds to the element entering and leaving the viewport.
   * When the element enters the viewport, the timestamp is saved;
   * when it leaves, the duration is calculated and a `viewed` event is emitted if applicable.
   */
  private onIntersection(entries: IntersectionObserverEntry[]): void {
    const entry = entries[0];
    if (entry.isIntersecting) {
      this.enteredAt = Date.now();
    } else if (this.enteredAt !== null) {
      const durationMs = Date.now() - this.enteredAt;
      this.enteredAt = null;
      if (durationMs >= this.MIN_VIEW_MS) {
        this.viewed.emit({ postId: this.appPostView, durationMs });
      }
    }
  }

  /**
   * Closes a view that's still running if the element was still visible
   * when it was destroyed, and detaches the observer from the DOM.
   */
  ngOnDestroy(): void {
    if (this.enteredAt !== null) {
      const durationMs = Date.now() - this.enteredAt;
      if (durationMs >= this.MIN_VIEW_MS) {
        this.viewed.emit({ postId: this.appPostView, durationMs });
      }
    }
    this.observer.disconnect();
  }
}
