export interface AlgorithmConfig {
  weightInteraction: number;
  weightRecency: number;
  weightExploration: number;
  weightSourceQuality: number;
  weightPopularity: number;
  weightTopic: number;
  topics: string[];
}