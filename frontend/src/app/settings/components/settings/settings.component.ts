import {Component, effect, inject, signal} from '@angular/core';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from "primeng/tabs";
import { ProfileComponent } from "../../../user/components/profile/profile.component";
import { AlgorithmConfigComponent } from "../../../algorithm/components/config/config.component";
import {ActivatedRoute, Router} from "@angular/router";
import { SessionConfigComponent } from "../../../session/components/config/config.component";
import { AnalyticsComponent } from '../../../session/components/analytics/analytics.component';
import {toSignal} from "@angular/core/rxjs-interop";

@Component({
  selector: 'app-settings',
  imports: [
    TabList,
    Tab,
    Tabs,
    TabPanels,
    TabPanel,
    ProfileComponent,
    AlgorithmConfigComponent,
    SessionConfigComponent,
    AnalyticsComponent
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private readonly STORAGE_KEY = 'settings-active-tab';

  private queryParams = toSignal(this.route.queryParamMap);
  protected activeTab = signal<string>('account');

  constructor() {
    effect(() => {
      const currentParams = this.queryParams();
      const tabFromUrl = currentParams?.get('tab');

      if (tabFromUrl) {
        this.activeTab.set(tabFromUrl);
        localStorage.setItem(this.STORAGE_KEY, tabFromUrl);
      } else {
        const storedTab = localStorage.getItem(this.STORAGE_KEY) ?? 'account';
        this.activeTab.set(storedTab);
      }
    });
  }

  protected onTabChange(tab: string | number | undefined): void {
    if (tab == null) return;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: String(tab) },
      queryParamsHandling: 'merge'
    });
  }
}
