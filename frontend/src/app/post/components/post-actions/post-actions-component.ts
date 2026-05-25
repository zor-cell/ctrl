import { Component, computed, inject, input, model, output, signal } from "@angular/core";
import { PostService, VoteType } from '../../../post/post.service';
import { DetailedPostResponse } from "../../dto/DetailedPostResponse";
import { NotificationService } from "../../../core/services/notification.service";
import { ReportComponent } from "../../../report/components/report/report.component";
import { ContentTypeEnum } from "../../../report/dto/report";

@Component({
  selector: 'app-post-actions',
  standalone: true,
  imports: [
    ReportComponent
  ],
  templateUrl: './post-actions-component.html',
  styleUrl: './post-actions-component.scss',
})
export class PostActions {
  private readonly postService = inject(PostService);
  private readonly notificationService = inject(NotificationService);
  protected ContentTypeEnum = ContentTypeEnum;

  public votingInProgress = signal<boolean>(false);
  public userVote = model.required<VoteType | null>();
  public upvotes = computed(() => this.userVote() === 'up' ? this.post().upvotes + 1 : this.post().upvotes);
  public downvotes = computed(() => this.userVote() === 'down' ? this.post().downvotes + 1 : this.post().downvotes);
  public reportDialogVisible = signal<boolean>(false);

  post = input.required<DetailedPostResponse>();
  openComments = output<void>();

  async votePost(voteType: VoteType): Promise<void> {
    this.votingInProgress.set(true);

    const previousType = this.userVote();
    this.userVote.update(current => current === voteType ? null : voteType);

    try {
      const response = await this.postService.votePost(this.post().postId, voteType);
      this.userVote.set(response.userVote);
    } catch {
      this.notificationService.showError('Voting failed, please try again shortly!');
      this.userVote.set(previousType);
    } finally {
      this.votingInProgress.set(false);
    }
  }
}
