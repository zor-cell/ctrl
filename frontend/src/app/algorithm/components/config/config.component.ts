import {Component, inject, OnInit, signal} from '@angular/core';
import {AlgorithmService} from "../../algorithm.service";
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {AlgorithmConfig} from "../../dto/AlgorithmConfig";
import {finalize} from "rxjs";
import {Slider} from "primeng/slider";
import {Button} from "primeng/button";
import {ProgressSpinner} from "primeng/progressspinner";

@Component({
  selector: 'app-config',
  imports: [
    Slider,
    ReactiveFormsModule,
    Button,
    ProgressSpinner
  ],
  templateUrl: './config.component.html',
  styleUrl: './config.component.scss',
})
export class AlgorithmConfigComponent implements OnInit {
  private algorithmService = inject(AlgorithmService);
  private fb = inject(NonNullableFormBuilder);

  protected isLoading = signal<boolean>(true);

  protected configForm = this.fb.group({
    weightRecency: [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightExploration: [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightSourceQuality: [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightTrending: [50, [Validators.required, Validators.min(0), Validators.max(100)]],
    weightTopic: [50, [Validators.required, Validators.min(0), Validators.max(100)]]
  });

  ngOnInit() {
    this.algorithmService.getConfig().subscribe({
      next: (config: AlgorithmConfig) => {
        this.configForm.patchValue(config);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
      }
    });
  }

  public saveConfig() {
    if (this.configForm.invalid) {
      this.configForm.markAllAsTouched();
      return;
    }

    const formValue = this.configForm.getRawValue() as AlgorithmConfig;

    this.algorithmService.updateConfig(formValue)
      .subscribe({
        next: (updatedConfig) => {
          this.configForm.patchValue(updatedConfig);
        },
        error: (err) => {
          console.error('Failed to save config', err);
        }
      });
  }
}
