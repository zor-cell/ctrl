import { Injectable, inject } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { SearchService } from '../services/search.service';
import { PostPreview } from '../dto/PostPreview';
import { firstValueFrom } from 'rxjs';

export interface PostsSearchResolvedData {
  query: string;
  topics: string[];
  results: PostPreview[] | null;
  fromCache: boolean;
  scrollY: number;
}

@Injectable({
  providedIn: 'root',
})
export class PostsSearchResolver implements Resolve<PostsSearchResolvedData> {
  private searchService = inject(SearchService);

  async resolve(route: ActivatedRouteSnapshot): Promise<PostsSearchResolvedData> {
    // Read URL parameters
    const query = route.queryParams['q'] || '';
    const topicsParam = route.queryParams['topics'] || '';
    const topics = topicsParam ? topicsParam.split(',') : [];

    if ((query === '' || query.length < 3) && topics.length == 0) {
      return {
        query,
        topics,
        results: null,
        fromCache: false,
        scrollY: 0
      }
    }

    const request = {
      query: query.trim(),
      topicIds: topics,
      page: 0,
      postsPerPage: 21,
    };

    const searchResult = await firstValueFrom(this.searchService.searchPosts(request));

    return {
      query,
      topics,
      results: searchResult,
      fromCache: true,
      scrollY: this.searchService.getSavedScrollPosition(),
    };
  }
}
