export interface Report {
  reason: string;
  detail: string;
  contentType: ContentTypeEnum;
  contentId: string | undefined;
}

export enum ContentTypeEnum {
  POST = "POST",
  COMMENT = "COMMENT",
  ACCOUNT = "ACCOUNT",
  SOURCE = "SOURCE"
}
