import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AlgorithmService } from '../../algorithm.service';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Slider } from 'primeng/slider';
import { Button } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';
import { AlgorithmConfig } from '../../dto/AlgorithmConfig';
import { TopicResponse } from '../../../post/dto/TopicResponse';
import { TopicService } from '../../topic.service';
import { MultiSelect, MultiSelectFilterEvent } from 'primeng/multiselect';

type SliderField = 'weightInteraction' | 'weightRecency' | 'weightExploration' |
  'weightSourceQuality' | 'weightPopularity' | 'weightTopic';

@Component({
  selector: 'algorithm-config',
  imports: [
    Slider,
    ReactiveFormsModule,
    Button,
    ProgressSpinner,
    TooltipModule,
    MultiSelect,
  ],
  templateUrl: './config.component.html',
  styleUrl: './config.component.scss',
})
export class AlgorithmConfigComponent implements OnInit {
  private readonly algorithmService = inject(AlgorithmService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly topicService = inject(TopicService);

  protected availableTopics = signal<TopicResponse[]>([]);
  protected isLoading = signal<boolean>(true);

  protected configForm = this.fb.group({
    weightInteraction:   [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightRecency:       [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightExploration:   [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightSourceQuality: [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightPopularity:      [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightTopic:         [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    topics: [[] as string[], [Validators.maxLength(5)]],
  });

  // Reactively tracks the current selection so sortedTopics() re-runs on every change.
  private readonly selectedTopics = toSignal(
    this.configForm.controls.topics.valueChanges,
    { initialValue: [] as string[] }
  );

  // Current text in the multiselect filter input.
  protected filterQuery = signal('');

  /**
   * Returns topics sorted by:
   *   1. Selected items always first (alphabetically among themselves)
   *   2. When a query is active: scored by match quality (exact > startsWith > contains)
   *   3. Unmatched items are excluded while a query is active
   *   4. Without a query: remaining items sorted alphabetically
   */
  protected sortedTopics = computed(() => {
    const all       = this.availableTopics();
    const selected  = new Set(this.selectedTopics());
    const query     = this.filterQuery().trim().toLowerCase();

    if (!query) {
      return [...all].sort((a, b) => {
        const diff = (selected.has(b.topicId) ? 1 : 0) - (selected.has(a.topicId) ? 1 : 0);
        return diff === 0 ? a.name.localeCompare(b.name) : diff;
      });
    }

    return all
      .map(t => ({ topic: t, score: this.scoreMatch(t.name, query) }))
      .filter(x => x.score > 0)
      .sort((a, b) => {
        // Selected items always rise to the top, then sort by match quality.
        const selDiff = (selected.has(b.topic.topicId) ? 1 : 0) - (selected.has(a.topic.topicId) ? 1 : 0);
        if (selDiff !== 0) return selDiff;
        return b.score - a.score;
      })
      .map(x => x.topic);
  });

  ngOnInit() {
    this.topicService.getTopics().subscribe(topics => {
      this.availableTopics.set(topics);
    });

    this.algorithmService.getConfig().subscribe({
      next: (config: AlgorithmConfig) => {
        this.configForm.patchValue(config);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  onTopicFilter(event: MultiSelectFilterEvent): void {
    this.filterQuery.set(event.filter ?? '');
  }

  getLabel(field: SliderField): string {
    const value = this.configForm.controls[field].value ?? 50;

    if (field === 'weightExploration') {
      if (value <= 33) return 'Familiar';
      if (value <= 66) return 'Mixed';
      return 'Exploratory';
    }

    if (value <= 33) return 'Irrelevant';
    if (value <= 66) return 'Relevant';
    return 'Important';
  }

  public saveConfig() {
    if (this.configForm.invalid) {
      this.configForm.markAllAsTouched();
      return;
    }

    const formValue = this.configForm.getRawValue() as AlgorithmConfig;

    this.algorithmService.updateConfig(formValue).subscribe({
      next: (updatedConfig) => {
        this.configForm.patchValue(updatedConfig);
      },
      error: (err) => {
        console.error('Failed to save config', err);
      },
    });
  }

  /**
   * Scores how well `name` matches `query` (the query must already be lower-cased).
   *   3 – exact match
   *   2 – starts with query
   *   1 – contains query (anywhere)
   *   0 – no match
   */
  private scoreMatch(name: string, query: string): number {
    const n = name.toLowerCase();
    if (n === query)          return 3;
    if (n.startsWith(query))  return 2;
    if (n.includes(query))    return 1;
    return 0;
  }
}
