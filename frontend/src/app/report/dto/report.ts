export interface Report {
  reason: string;
  description: string;
  contentType: ContentTypeEnum;
  contentId: string | undefined;
  reportingUser: string;
}

export enum ContentTypeEnum {
  POST = "POST",
  COMMENT = "COMMENT",
  ACCOUNT = "ACCOUNT",
  SOURCE = "SOURCE"
}

export enum ReportStatus {
  CREATED = 'CREATED',
  VALIDATED = 'VALIDATED',
  DECLINED = 'DECLINED',
}

export enum ReportAction {
  DELETE_ITEM = 'DELETE_ITEM',
  DELETE_ACCOUNT = 'DELETE_ACCOUNT',
  NONE = 'NONE'
}
