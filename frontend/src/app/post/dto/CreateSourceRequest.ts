import { CreateSingleSourceRequest } from './CreateSingleSourceRequest';

export interface CreateSourceRequest {
  upvotes: number;
  downvotes: number;
  singleSources: CreateSingleSourceRequest[];
}
