import { CreateSourceRequest } from './CreateSourceRequest';

export interface CreatePostRequest {
  caption: string;
  mediaRef: string | null;
  isPublic: boolean;
  isAiGenerated: boolean;
  topicIds: string[];
  source: CreateSourceRequest | null;
}
