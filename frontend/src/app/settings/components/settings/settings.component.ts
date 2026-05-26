import {Component, inject} from '@angular/core';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from "primeng/tabs";
import {ProfileComponent} from "../../../user/components/profile/profile.component";
import {AlgorithmConfigComponent} from "../../../algorithm/components/config/config.component";
import {toSignal} from "@angular/core/rxjs-interop";
import {map} from "rxjs";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-settings',
  imports: [
    TabList,
    Tab,
    Tabs,
    TabPanels,
    TabPanel,
    ProfileComponent,
    AlgorithmConfigComponent
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent {
  private route = inject(ActivatedRoute);

  protected activeTab = toSignal(
    this.route.queryParamMap.pipe(map(params => params.get('tab') ?? 'account')),
    { initialValue: 'account' }
  );
}
