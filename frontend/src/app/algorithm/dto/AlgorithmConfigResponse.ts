import {TopicResponse} from "../../post/dto/TopicResponse";

export interface AlgorithmConfigResponse {
  weightRecency: number;
  weightExploration: number;
  weightSourceQuality: number;
  weightTrending: number;
  weightTopic: number;
  topics: TopicResponse[];
}