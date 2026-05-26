export interface JwtPayload {
  sub: string;
  email: string;
}

export interface AuthUser {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
}
