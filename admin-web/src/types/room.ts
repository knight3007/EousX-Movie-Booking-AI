export type Room = {
  id: string;
  name: string;
  type: string;
  totalSeats?: number;
  status?: string;
  cinemaId?: string;
  cinema?: {
    id: string;
    name: string;
  };
  _count?: {
    seats?: number;
    showtimes?: number;
  };
};