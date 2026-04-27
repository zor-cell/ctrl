export interface UserDetails {
  id: string;
  username: string;
  profilePicture: string | null;
  email: string;
  name: string;
  bio: string | null;
  birthdate: string;
  createdAt: string;
  isVerified: boolean;
}
