import {Component, inject, OnInit} from '@angular/core';
import {AdminService} from "../../../admin.service";

@Component({
  selector: 'app-admin-panel',
  imports: [],
  templateUrl: './admin-panel.component.html',
  styleUrl: './admin-panel.component.scss',
})
export class AdminPanelComponent implements OnInit {
  private adminService = inject(AdminService);

  ngOnInit() {
    this.adminService.getPendingVerifications().subscribe();
  }
}
