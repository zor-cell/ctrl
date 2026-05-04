export interface PublicUserProfile {
  id: string;
  username: string;
  name: string | null;
  bio: string | null;
  profilePicture: string | null;
  isVerified: boolean;
}
