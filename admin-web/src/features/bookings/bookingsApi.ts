import { apiClient } from "../../shared/api/apiClient";
import type { Booking } from "../../types/booking";

export async function getBookings() {
  const response = await apiClient.get<Booking[]>("/admin/bookings");
  return response.data;
}

export async function getBookingById(id: string) {
  const response = await apiClient.get<Booking>(`/admin/bookings/${id}`);
  return response.data;
}