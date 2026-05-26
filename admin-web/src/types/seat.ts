export type SeatStatus = "AVAILABLE" | "LOCKED" | "SOLD" | "MAINTENANCE";

export type SeatType =
  | "STANDARD"
  | "VIP"
  | "COUPLE"
  | "DISABLED"
  | "MAINTENANCE";

export type SeatMapSeat = {
  id: string;
  seatId?: string;
  code?: string;
  seatCode?: string;
  row?: string;
  rowLabel?: string;
  number?: number;
  seatNumber?: number;
  type?: SeatType;
  seatType?: SeatType;
  status: SeatStatus;
  isActive?: boolean;

  lockedUntil?: string | null;

  user?: {
    id?: string;
    fullName?: string | null;
    email?: string | null;
  } | null;

  booking?: {
    id?: string;
    code?: string;
    status?: string;
  } | null;

  ticket?: {
    id?: string;
    qrCode?: string;
    status?: string;
  } | null;

  payment?: {
    id?: string;
    provider?: string;
    status?: string;
    amount?: number;
  } | null;
};

export type SeatMapResponse =
  | SeatMapSeat[]
  | {
      showtime?: unknown;
      seats: SeatMapSeat[];
    };