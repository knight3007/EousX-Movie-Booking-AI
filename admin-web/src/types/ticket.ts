import type { Booking } from "./booking";

export type VerifyTicketStatus =
  | "VALID"
  | "ALREADY_USED"
  | "INVALID"
  | "EXPIRED"
  | "CANCELLED"
  | "USED";

export type TicketDetail = {
  id: string;
  bookingId: string;
  qrCode?: string;
  status: "VALID" | "USED" | "CANCELLED" | "EXPIRED";
  checkedInAt?: string | null;
  booking?: Booking;
};

export type VerifyTicketResponse = {
  status: VerifyTicketStatus;
  message: string;
  ticket: TicketDetail | null;
};

export type CheckInTicketResponse = {
  message: string;
  note?: string;
  ticket: TicketDetail;
};