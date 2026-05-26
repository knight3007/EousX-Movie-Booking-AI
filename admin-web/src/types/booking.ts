import type { Showtime } from "./showtime";

export type BookingStatus =
  | "PENDING"
  | "WAITING_PAYMENT"
  | "PAID"
  | "EXPIRED"
  | "CANCELLED"
  | "REFUNDED"
  | "CHECKED_IN";

export type PaymentStatus =
  | "CREATED"
  | "PENDING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED"
  | "EXPIRED";

export type TicketStatus = "VALID" | "USED" | "CANCELLED" | "EXPIRED";

export type BookingUser = {
  id: string;
  fullName?: string | null;
  email?: string | null;
  phone?: string | null;
};

export type BookingSeat = {
  id: string;
  bookingId?: string;
  seatId: string;
  price: number;
  seat?: {
    id: string;
    code?: string;
    row?: string;
    number?: number;
    type?: string;
  };
};

export type BookingPayment = {
  id: string;
  bookingId: string;
  provider: string;
  amount: number;
  transactionId?: string | null;
  status: PaymentStatus;
  paidAt?: string | null;
};

export type BookingTicket = {
  id: string;
  bookingId: string;
  qrCode?: string;
  status: TicketStatus;
  checkedInAt?: string | null;
};

export type Booking = {
  id: string;
  code?: string;
  userId: string;
  showtimeId: string;
  totalAmount: number;
  status: BookingStatus;
  createdAt: string;
  updatedAt?: string;
  user?: BookingUser;
  showtime?: Showtime;
  seats?: BookingSeat[];
  payment?: BookingPayment | null;
  ticket?: BookingTicket | null;
};