import { Component, computed, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TooltipModule } from 'primeng/tooltip';
import { Post } from '../../dto/Post';
import { AlgorithmService } from '../../../algorithm/algorithm.service';

interface ScoreRow {
  label: string;
  score: number;
  description: string;
  /** Width of the green bar as % of the full track (= actual score, capped at weight) */
  scoreFillPct: number;
  /** Width of the grey bar as % of the full track (= weight directly, 0–100) */
  maxFillPct: number;
}

@Component({
  selector: 'app-why-dialog',
  standalone: true,
  imports: [CommonModule, DialogModule, ButtonModule, TooltipModule],
  templateUrl: './why-dialog.component.html',
  styleUrl: './why-dialog.component.scss',
})
export class WhyDialogComponent {
  post    = input<Post | null>(null);
  visible = input<boolean>(false);

  private readonly algorithmService = inject(AlgorithmService);
  visibleChange = output<boolean>();

  scoreRows = computed<ScoreRow[]>(() => {
    const p = this.post();
    if (!p) return [];

    const cfg = this.algorithmService.config();

    const weights = {
      Interests:        cfg?.weightInteraction    ?? 100,
      Recency:          cfg?.weightRecency        ?? 100,
      'Source Quality': cfg?.weightSourceQuality  ?? 100,
      Popularity:       cfg?.weightPopularity     ?? 100,
      Exploration:      cfg?.weightExploration    ?? 100,
      Topics:           cfg?.weightTopic          ?? 100,
    };

    const explorationWeight = cfg?.weightExploration ?? 50;
    const explorationDescription =
      explorationWeight > 50
        ? "Accounts you don't follow are boosted."
        : explorationWeight < 50
          ? 'Accounts you follow are boosted.'
          : 'Familiar and new accounts are weighted equally. Adjust the slider to have an effect';

    const scale = 100 / 6;

    const raw = [
      { label: 'Interests',      score: p.explanation.scoreInteraction * scale, description: 'Similar content you have liked, commented on, or spent time watching is boosted.', weight: weights['Interests'] },
      { label: 'Recency',        score: p.explanation.scoreRecency     * scale, description: 'Newer posts get a boost.',                                                           weight: weights['Recency'] },
      { label: 'Source Quality', score: p.explanation.scoreSource      * scale, description: 'Reliable sources are boosted.',                                                      weight: weights['Source Quality'] },
      { label: 'Popularity',     score: p.explanation.scorePopularity  * scale, description: 'Posts with many upvotes and active discussions are boosted.',                        weight: weights['Popularity'] },
      { label: 'Exploration',    score: p.explanation.scoreExploration * scale, description: explorationDescription,                                                               weight: weights['Exploration'] },
      { label: 'Topics',         score: p.explanation.scoreTopic       * scale, description: 'Similar topics you have selected in your profile are boosted.',                      weight: weights['Topics'] },
    ];

    return raw.map(r => ({
      ...r,
      maxFillPct: r.weight,
      scoreFillPct: (Math.abs(r.score) / scale) * 100,
    }));
  });

  maxAchievableScore = computed<number>(() => {
    const cfg = this.algorithmService.config();
    if (!cfg) return 100;
    const weightSum =
      cfg.weightInteraction +
      cfg.weightTopic +
      cfg.weightRecency +
      cfg.weightSourceQuality +
      cfg.weightPopularity +
      cfg.weightExploration;
    return Math.round(weightSum / 6);
  });

  totalScore = computed<string>(() => {
    const p = this.post();
    if (!p) return '0';
    return ((p.explanation.totalScore / 6) * 100).toFixed(1);
  });

  close(): void {
    this.visibleChange.emit(false);
  }
}
