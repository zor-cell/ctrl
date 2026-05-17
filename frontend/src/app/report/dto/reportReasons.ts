import {ContentTypeEnum} from './report';

export interface ReportReason {
  label: string;
  value: string;
}

export const REPORT_REASONS_POST: ReportReason[] = [
  { label: 'Bullying or Harassment', value: 'bullying' },
  { label: 'Self harm', value: 'self_harm' },
  { label: 'Violence', value: 'violence' },
  { label: 'Illegal or Restricted items', value: 'illegal' },
  { label: 'Nudity or sexual content', value: 'nudity' },
  { label: 'Scam or Fraud', value: 'copyright' },
  { label: 'Misinformation', value: 'misinformation' },
  { label: 'Child abuse, exploitation or endangerment', value: 'child_exploitation' },
  { label: 'Non-consensual intimate images', value: 'non_consensual' },
  { label: 'Hate speech or discrimination', value: 'hate_speech' },
  { label: 'Terrorism or violent extremism', value: 'terrorism' },
  { label: 'Other', value: 'other' }
];

export const REPORT_REASONS_SOURCE: ReportReason[] = [
  { label: 'Bullying or Harassment', value: 'bullying' },
  { label: 'Self harm', value: 'self_harm' },
  { label: 'Violence', value: 'violence' },
  { label: 'Illegal or Restricted items', value: 'illegal' },
  { label: 'Nudity or sexual content', value: 'nudity' },
  { label: 'Scam or Fraud', value: 'copyright' },
  { label: 'Misinformation', value: 'misinformation' },
  { label: 'Child abuse, exploitation or endangerment', value: 'child_exploitation' },
  { label: 'Source does not fit to post', value: 'false_source' },
  { label: 'Hate speech or discrimination', value: 'hate_speech' },
  { label: 'Terrorism or violent extremism', value: 'terrorism' },
  { label: 'Other', value: 'other' }
];

export const REPORT_REASON_BY_TYPE: Record<ContentTypeEnum, ReportReason[]> = {
  [ContentTypeEnum.POST]: REPORT_REASONS_POST,
  [ContentTypeEnum.SOURCE]: REPORT_REASONS_SOURCE,
  [ContentTypeEnum.ACCOUNT]: REPORT_REASONS_POST,
  [ContentTypeEnum.COMMENT]: REPORT_REASONS_POST
}

export function getReportReason(contentType: ContentTypeEnum) {
  return REPORT_REASON_BY_TYPE[contentType] || []
}
