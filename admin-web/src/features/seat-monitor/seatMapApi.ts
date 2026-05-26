import { apiClient } from "../../shared/api/apiClient";
import type { SeatMapResponse, SeatMapSeat } from "../../types/seat";

export async function getAdminSeatMap(showtimeId: string) {
  const response = await apiClient.get<SeatMapResponse>(
    `/admin/showtimes/${showtimeId}/seats`,
  );

  if (Array.isArray(response.data)) {
    return response.data;
  }

  return response.data.seats ?? [];
}

export type { SeatMapSeat };