export type MovieStatus = "NOW_SHOWING" | "UPCOMING";

export type Movie = {
  id: string;
  title: string;
  originalTitle?: string | null;
  overview: string;
  posterUrl?: string | null;
  backdropUrl?: string | null;
  trailerKey?: string | null;
  runtime: number;
  genres: string[];
  rating: number;
  ageRating: string;
  status: MovieStatus;
  createdAt?: string;
  updatedAt?: string;
};

export type MoviePayload = {
  title: string;
  originalTitle?: string;
  overview: string;
  posterUrl?: string;
  backdropUrl?: string;
  trailerKey?: string;
  runtime: number;
  genres: string[];
  rating: number;
  ageRating: string;
  status: MovieStatus;
};