import { Component, computed, inject, input, output, signal } from '@angular/core';
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
  barWidth: number;
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
  visibleChange   = output<boolean>();

  scoreRows = computed<ScoreRow[]>(() => {
    const p = this.post();
    if (!p) return [];
    // Scores in explanation are already weighted by the backend.
    // Scale them to the same 0–max range as totalScore: multiply by (100/6).
    const scale = 100 / 6;

    const cfg = this.algorithmService.config();
    const explorationWeight = cfg?.weightExploration ?? 0.5;
    const explorationDescription =
      explorationWeight > 50
        ? "Accounts you don't follow are boosted."
        : explorationWeight < 50
          ? 'Accounts you follow are boosted.'
          : 'Familiar and new accounts are weighted equally. Ajust the slider to have an effect';

    const raw = [
      { label: 'Interests',      score: p.explanation.scoreInteraction * scale, description: 'Similar content you have liked, commented on, or spent time watching is boosted.' },
      { label: 'Recency',        score: p.explanation.scoreRecency     * scale, description: 'Newer posts get a boost.' },
      { label: 'Source Quality', score: p.explanation.scoreSource      * scale, description: 'Reliable sources are boosted.' },
      { label: 'Popularity',     score: p.explanation.scorePopularity  * scale, description: 'Posts with many upvotes and active discussions are boosted.' },
      { label: 'Exploration',    score: p.explanation.scoreExploration * scale, description: explorationDescription },
      { label: 'Topics',         score: p.explanation.scoreTopic       * scale, description: 'Similar topics you have selected in your profile are boosted.' },
    ];

    const maxAbs = Math.max(...raw.map(r => Math.abs(r.score)));

    return raw.map(r => ({
      ...r,
      barWidth: maxAbs > 0 ? (Math.abs(r.score) / maxAbs) * 100 : 0,
    }));
  });

  // Max achievable = sum(weight_i / 100) / 6 * 100 = sum(weight_i) / 6
  // Weights are 0–100, so divide weightSum by 6.
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

  topicsOpen = signal(true);

  close(): void {
    this.visibleChange.emit(false);
  }
}
