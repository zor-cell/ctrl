import { CreateSourceRequest } from './CreateSourceRequest';

export interface CreatePostRequest {
  caption: string;
  mediaRef: string | null;
  isPublic: boolean;
  labelIds: string[];
  source: CreateSourceRequest | null;
}
