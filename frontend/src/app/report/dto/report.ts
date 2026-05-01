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
