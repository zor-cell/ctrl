export interface AlgorithmConfig {
  weightInteraction: number;
  weightRecency: number;
  weightExploration: number;
  weightSourceQuality: number;
  weightTrending: number;
  weightTopic: number;
  topics: string[];
}