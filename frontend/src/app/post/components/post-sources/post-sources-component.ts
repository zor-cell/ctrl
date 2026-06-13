import { Component, input, signal } from "@angular/core";
import { ButtonModule } from "primeng/button";
import { SingleSourceResponse, SourceResponse } from "../../dto/PostResponse";
import { ContentTypeEnum } from "../../../report/dto/report";
import { ReportComponent } from "../../../report/components/report/report.component";
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';

@Component({
  selector: 'app-post-sources',
  standalone: true,
  imports: [ButtonModule, ReportComponent, Tag, Tooltip],
  templateUrl: './post-sources-component.html',
  styleUrl: './post-sources-component.scss',
})
export class PostSources {
  protected ContentTypeEnum = ContentTypeEnum;

  username = input.required<string>();
  profilePicture = input.required<string>();
  source = input.required<SourceResponse>();
  sourcesExpanded = signal<boolean>(false);
  reportDialogVisible = signal<boolean>(false);

  sourcesButtonLabel(): string {
    const sourceCount = this.source().singleSources.length;
    const sourceLabel = sourceCount === 1 ? 'source' : 'sources';
    return this.sourcesExpanded() ? 'Hide sources' : `Sources (${sourceCount} ${sourceLabel})`;
  }

  formattedSourceDate(source: SingleSourceResponse): string {
    if (!source.date) {
      return '';
    }
    return new Intl.DateTimeFormat('de-AT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(new Date(source.date));
  }

  displaySourceUrl(url: string): string {
    const maxLength = 120;
    return url.length <= maxLength ? url : `${url.slice(0, maxLength - 1)}...`;
  }
}

