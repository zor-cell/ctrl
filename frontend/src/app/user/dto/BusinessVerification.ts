import {UserDetails} from './UserDetails';

export interface BusinessVerification {
  id: string;
  user: UserDetails;
}


export enum VerificationStatus {
  APPROVED = "APPROVED",
  REJECTED = "REJECTED",
  PENDING = "PENDING",
  NOT_VERIFIED = "NOT_VERIFIED"
}

export enum VerificationType {
  BUSINESS = "BUSINESS",
  PERSONAL = "PERSONAL"
}

export interface VerificationStatusResponse {
  type: VerificationType;
  status: VerificationStatus;
}
