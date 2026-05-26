import type { Movie } from "./movie";
import type { Room } from "./room";

export type ShowtimeStatus = "OPEN" | "CANCELLED";

export type Showtime = {
  id: string;
  movieId: string;
  roomId: string;
  startTime: string;
  endTime: string;
  basePrice: number;
  status: ShowtimeStatus;
  movie?: Movie;
  room?: Room;
};

export type ShowtimePayload = {
  movieId: string;
  roomId: string;
  startTime: string;
  endTime?: string;
  basePrice: number;
  status?: ShowtimeStatus;
};