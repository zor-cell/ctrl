export interface TopicOption {
  label: string;
  value: string;
}

export type PreviewKind = 'image' | 'video' | null;

export interface SourceFormValue {
  journal: string;
  author: string;
  title: string;
  url: string;
  date: string | Date | null;
}
