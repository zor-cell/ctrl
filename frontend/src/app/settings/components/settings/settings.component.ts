import { Component, inject, signal } from '@angular/core';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from "primeng/tabs";
import { ProfileComponent } from "../../../user/components/profile/profile.component";
import { AlgorithmConfigComponent } from "../../../algorithm/components/config/config.component";
import { ActivatedRoute } from "@angular/router";
import { SessionConfigComponent } from "../../../session/components/config/config.component";
import { AnalyticsComponent } from '../../../session/components/analytics/analytics.component';

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

  private readonly STORAGE_KEY = 'settings-active-tab';

  protected activeTab = signal<string>(
    this.route.snapshot.queryParamMap.get('tab') ??
    localStorage.getItem(this.STORAGE_KEY) ??
    'account'
  );

  protected onTabChange(tab: string | number | undefined): void {
    if (tab == null) return;
    const value = String(tab);
    this.activeTab.set(value);
    localStorage.setItem(this.STORAGE_KEY, value);
  }
}
