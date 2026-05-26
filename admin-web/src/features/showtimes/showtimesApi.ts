import { apiClient } from "../../shared/api/apiClient";
import type { Showtime, ShowtimePayload } from "../../types/showtime";

export async function getShowtimes(date?: string) {
  const response = await apiClient.get<Showtime[]>("/admin/showtimes", {
    params: date ? { date } : undefined,
  });

  return response.data;
}

export async function createShowtime(payload: ShowtimePayload) {
  const response = await apiClient.post<Showtime>("/admin/showtimes", payload);
  return response.data;
}

export async function updateShowtime(id: string, payload: ShowtimePayload) {
  const response = await apiClient.put<Showtime>(
    `/admin/showtimes/${id}`,
    payload,
  );

  return response.data;
}

export async function cancelShowtime(id: string) {
  const response = await apiClient.delete<Showtime>(`/admin/showtimes/${id}`);
  return response.data;
}