export interface AlgorithmConfigRequest {
  weightRecency: number;
  weightExploration: number;
  weightSourceQuality: number;
  weightTrending: number;
  weightTopic: number;
  topics: string[];
}