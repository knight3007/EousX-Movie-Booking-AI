import { apiClient } from "../../shared/api/apiClient";
import type { Movie, MoviePayload } from "../../types/movie";

export async function getMovies() {
  const response = await apiClient.get<Movie[]>("/movies");
  return response.data;
}

export async function createMovie(payload: MoviePayload) {
  const response = await apiClient.post<Movie>("/admin/movies", payload);
  return response.data;
}

export async function updateMovie(id: string, payload: MoviePayload) {
  const response = await apiClient.put<Movie>(`/admin/movies/${id}`, payload);
  return response.data;
}

export async function deleteMovie(id: string) {
  const response = await apiClient.delete<Movie>(`/admin/movies/${id}`);
  return response.data;
}