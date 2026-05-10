import {UserDetails} from './UserDetails';

export interface BusinessVerification {
  id: string;
  user: UserDetails;
}

export type VerificationStatus = 'APPROVED' | 'DENIED' | 'PENDING';
