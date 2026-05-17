export interface UpdateProfileRequest {
  name: string;
  email: string;
  bio?: string | null;
  birthdate: string;
}
