import { apiClient } from "../../shared/api/apiClient";
import type { Room } from "../../types/room";

export async function getRooms() {
  const response = await apiClient.get<Room[]>("/admin/rooms");
  return response.data;
}