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

    //exploration metric is the only metric that work twofold:
    // when slider is 0 -> 100% boost for friends, when slider is 1 -> 100% for new accounts
    const explorationWeight = cfg?.weightExploration ?? 50;
    const explorationDescription = explorationWeight > 50
      ? "Accounts you don't follow are boosted."
      : explorationWeight < 50
        ? 'Accounts you follow are boosted.'
        : 'Familiar and new accounts are weighted equally. Adjust the slider to have an effect';

    let explorationEffectiveWeight = 0;
    if (explorationWeight < 50) {
      explorationEffectiveWeight = (50 - explorationWeight) * 2;
    } else if (explorationWeight > 50) {
      explorationEffectiveWeight = (explorationWeight - 50) * 2;
    }

    const weights = {
      Interests:        cfg?.weightInteraction    ?? 100,
      Recency:          cfg?.weightRecency        ?? 100,
      'Source Quality': cfg?.weightSourceQuality  ?? 100,
      Popularity:       cfg?.weightPopularity     ?? 100,
      Exploration:      explorationEffectiveWeight,
      Topics:           cfg?.weightTopic          ?? 100,
    };

    let weightSum =
      weights['Interests'] + weights['Recency'] + weights['Source Quality'] +
      weights['Popularity'] + weights['Exploration'] + weights['Topics'];
    weightSum = weightSum > 0 ? weightSum : 1;

    const raw = [
      { label: 'Interaction',      score: p.explanation.scoreInteraction, description: 'Similar content you have liked, commented on, or spent time watching is boosted.', weight: weights['Interests'] },
      { label: 'Recency',        score: p.explanation.scoreRecency, description: 'Newer posts get a boost.',                                                           weight: weights['Recency'] },
      { label: 'Exploration',    score: p.explanation.scoreExploration , description: explorationDescription,                                                               weight: weights['Exploration'] },
      { label: 'Source Quality', score: p.explanation.scoreSource, description: 'Reliable sources are boosted.',                                                      weight: weights['Source Quality'] },
      { label: 'Popularity',     score: p.explanation.scorePopularity, description: 'Posts with many upvotes and active discussions are boosted.',                        weight: weights['Popularity'] },
      { label: 'Topics',         score: p.explanation.scoreTopic, description: 'Similar topics you have selected in your profile are boosted.',                      weight: weights['Topics'] },
    ];

    return raw.map(r => ({
      ...r,
      score: r.score * (r.weight / weightSum) * 100,
      maxFillPct: r.weight,
      scoreFillPct: Math.abs(r.score) * r.weight,
    }));
  });

  maxAchievableScore = computed<number>(() => 100);

  totalScore = computed<string>(() => {
    const rows = this.scoreRows();
    if (!rows || rows.length === 0) return '0.0';

    const sum = rows.reduce((acc, row) => acc + row.score, 0);
    return sum.toFixed(1);
  });

  close(): void {
    this.visibleChange.emit(false);
  }
}
