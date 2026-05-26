import axios from "axios";
import dayjs from "dayjs";
import {
  CalendarDays,
  CreditCard,
  Eye,
  Film,
  RefreshCw,
  Search,
  Ticket,
  UserRound,
  X,
} from "lucide-react";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getBookingById, getBookings } from "./bookingsApi";
import type { Booking, BookingStatus } from "../../types/booking";

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;

    if (Array.isArray(message)) {
      return message.join(", ");
    }

    if (typeof message === "string") {
      return message;
    }

    return error.message;
  }

  return "Something went wrong";
}

function formatCurrency(value?: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

function getSeatCodes(booking: Booking) {
  const seats = booking.seats ?? [];

  if (seats.length === 0) return "No seats";

  return seats
    .map((bookingSeat) => {
      const seat = bookingSeat.seat;
      return seat?.code ?? `${seat?.row ?? ""}${seat?.number ?? ""}`.trim();
    })
    .filter(Boolean)
    .join(", ");
}

function getCustomerName(booking: Booking) {
  return (
    booking.user?.fullName ||
    booking.user?.email ||
    booking.user?.phone ||
    booking.userId
  );
}

function getBookingCode(booking: Booking) {
  return booking.code || booking.id;
}

function getStatusLabel(status?: string | null) {
  if (!status) return "N/A";

  return status
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function getBookingStatusClass(status: BookingStatus) {
  switch (status) {
    case "PAID":
      return "border-[rgba(50,213,131,0.35)] bg-[rgba(50,213,131,0.12)] text-[var(--color-success)]";
    case "CHECKED_IN":
      return "border-[rgba(245,196,0,0.45)] bg-[rgba(245,196,0,0.14)] text-[var(--color-primary)]";
    case "WAITING_PAYMENT":
    case "PENDING":
      return "border-[rgba(56,189,248,0.35)] bg-[rgba(56,189,248,0.12)] text-sky-300";
    case "CANCELLED":
    case "EXPIRED":
    case "REFUNDED":
      return "border-[rgba(255,59,59,0.35)] bg-[rgba(255,59,59,0.12)] text-[var(--color-danger)]";
    default:
      return "border-[var(--color-border)] bg-[var(--color-card)] text-muted";
  }
}

function getPaymentStatusClass(status?: string | null) {
  switch (status) {
    case "SUCCESS":
      return "text-[var(--color-success)]";
    case "PENDING":
    case "CREATED":
      return "text-[var(--color-primary)]";
    case "FAILED":
    case "CANCELLED":
    case "EXPIRED":
      return "text-[var(--color-danger)]";
    default:
      return "text-muted";
  }
}

function getTicketStatusClass(status?: string | null) {
  switch (status) {
    case "VALID":
      return "text-[var(--color-success)]";
    case "USED":
      return "text-[var(--color-primary)]";
    case "CANCELLED":
    case "EXPIRED":
      return "text-[var(--color-danger)]";
    default:
      return "text-muted";
  }
}

function DetailRow({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-[var(--color-border)] py-3 last:border-b-0">
      <span className="text-sm text-muted">{label}</span>
      <span className="max-w-[65%] text-right text-sm font-semibold text-white">
        {value}
      </span>
    </div>
  );
}

function BookingDetailModal({
  booking,
  onClose,
}: {
  booking: Booking;
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-5 backdrop-blur">
      <div className="eous-card max-h-[90vh] w-full max-w-4xl overflow-hidden">
        <div className="flex items-start justify-between border-b border-[var(--color-border)] p-6">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
              Booking Detail
            </p>
            <h2 className="card-title mt-2 text-3xl text-white">
              {getBookingCode(booking)}
            </h2>
            <p className="mt-1 text-sm text-muted">
              Created at {dayjs(booking.createdAt).format("HH:mm DD/MM/YYYY")}
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-[var(--color-border)] p-2 text-muted transition hover:text-white"
          >
            <X size={20} />
          </button>
        </div>

        <div className="max-h-[calc(90vh-120px)] overflow-y-auto p-6">
          <div className="grid gap-6 lg:grid-cols-2">
            <section className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.45)] p-5">
              <h3 className="card-title text-xl text-white">Customer</h3>

              <div className="mt-4">
                <DetailRow label="Name" value={getCustomerName(booking)} />
                <DetailRow label="Email" value={booking.user?.email || "N/A"} />
                <DetailRow label="Phone" value={booking.user?.phone || "N/A"} />
                <DetailRow label="User ID" value={booking.userId} />
              </div>
            </section>

            <section className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.45)] p-5">
              <h3 className="card-title text-xl text-white">Movie & Showtime</h3>

              <div className="mt-4">
                <DetailRow
                  label="Movie"
                  value={booking.showtime?.movie?.title || "Unknown Movie"}
                />
                <DetailRow
                  label="Room"
                  value={`${booking.showtime?.room?.name ?? "Unknown Room"} • ${
                    booking.showtime?.room?.type ?? "Room"
                  }`}
                />
                <DetailRow
                  label="Date"
                  value={
                    booking.showtime?.startTime
                      ? dayjs(booking.showtime.startTime).format("DD/MM/YYYY")
                      : "N/A"
                  }
                />
                <DetailRow
                  label="Time"
                  value={
                    booking.showtime?.startTime && booking.showtime?.endTime
                      ? `${dayjs(booking.showtime.startTime).format(
                          "HH:mm",
                        )} - ${dayjs(booking.showtime.endTime).format("HH:mm")}`
                      : "N/A"
                  }
                />
              </div>
            </section>

            <section className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.45)] p-5">
              <h3 className="card-title text-xl text-white">Seats & Amount</h3>

              <div className="mt-4">
                <DetailRow label="Seats" value={getSeatCodes(booking)} />
                <DetailRow
                  label="Seat Count"
                  value={`${booking.seats?.length ?? 0} seat(s)`}
                />
                <DetailRow
                  label="Total Amount"
                  value={formatCurrency(booking.totalAmount)}
                />
                <DetailRow
                  label="Booking Status"
                  value={
                    <span
                      className={[
                        "rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.12em]",
                        getBookingStatusClass(booking.status),
                      ].join(" ")}
                    >
                      {getStatusLabel(booking.status)}
                    </span>
                  }
                />
              </div>

              <div className="mt-5 grid gap-3">
                {(booking.seats ?? []).map((bookingSeat) => (
                  <div
                    key={bookingSeat.id}
                    className="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] p-3"
                  >
                    <div className="flex items-center justify-between gap-4">
                      <span className="font-semibold text-white">
                        {bookingSeat.seat?.code ??
                          `${bookingSeat.seat?.row ?? ""}${
                            bookingSeat.seat?.number ?? ""
                          }`}
                      </span>
                      <span className="text-sm text-muted">
                        {formatCurrency(bookingSeat.price)}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-muted">
                      Type: {bookingSeat.seat?.type ?? "STANDARD"}
                    </p>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.45)] p-5">
              <h3 className="card-title text-xl text-white">Payment & Ticket</h3>

              <div className="mt-4">
                <DetailRow
                  label="Payment Provider"
                  value={booking.payment?.provider || "N/A"}
                />
                <DetailRow
                  label="Payment Status"
                  value={
                    <span
                      className={[
                        "font-bold",
                        getPaymentStatusClass(booking.payment?.status),
                      ].join(" ")}
                    >
                      {getStatusLabel(booking.payment?.status)}
                    </span>
                  }
                />
                <DetailRow
                  label="Transaction ID"
                  value={booking.payment?.transactionId || "N/A"}
                />
                <DetailRow
                  label="Paid At"
                  value={
                    booking.payment?.paidAt
                      ? dayjs(booking.payment.paidAt).format("HH:mm DD/MM/YYYY")
                      : "N/A"
                  }
                />
                <DetailRow
                  label="Ticket Status"
                  value={
                    <span
                      className={[
                        "font-bold",
                        getTicketStatusClass(booking.ticket?.status),
                      ].join(" ")}
                    >
                      {getStatusLabel(booking.ticket?.status)}
                    </span>
                  }
                />
                <DetailRow
                  label="QR Code"
                  value={
                    booking.ticket?.qrCode ? (
                      <span className="break-all">{booking.ticket.qrCode}</span>
                    ) : (
                      "N/A"
                    )
                  }
                />
                <DetailRow
                  label="Checked In At"
                  value={
                    booking.ticket?.checkedInAt
                      ? dayjs(booking.ticket.checkedInAt).format(
                          "HH:mm DD/MM/YYYY",
                        )
                      : "N/A"
                  }
                />
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function BookingsPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | BookingStatus>("ALL");
  const [selectedBookingId, setSelectedBookingId] = useState<string | null>(null);

  const bookingsQuery = useQuery({
    queryKey: ["bookings"],
    queryFn: getBookings,
  });

  const selectedBookingQuery = useQuery({
    queryKey: ["booking", selectedBookingId],
    queryFn: () => getBookingById(selectedBookingId!),
    enabled: Boolean(selectedBookingId),
  });

  const bookings = bookingsQuery.data ?? [];

  const filteredBookings = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();

    return bookings.filter((booking) => {
      const movieTitle = booking.showtime?.movie?.title?.toLowerCase() ?? "";
      const roomName = booking.showtime?.room?.name?.toLowerCase() ?? "";
      const customerName = getCustomerName(booking).toLowerCase();
      const bookingCode = getBookingCode(booking).toLowerCase();
      const seatCodes = getSeatCodes(booking).toLowerCase();

      const matchSearch =
        !keyword ||
        movieTitle.includes(keyword) ||
        roomName.includes(keyword) ||
        customerName.includes(keyword) ||
        bookingCode.includes(keyword) ||
        seatCodes.includes(keyword);

      const matchStatus =
        statusFilter === "ALL" ? true : booking.status === statusFilter;

      return matchSearch && matchStatus;
    });
  }, [bookings, searchTerm, statusFilter]);

  const summary = useMemo(() => {
    const paidBookings = bookings.filter(
      (booking) =>
        booking.status === "PAID" || booking.status === "CHECKED_IN",
    );

    return {
      total: bookings.length,
      waiting: bookings.filter((booking) => booking.status === "WAITING_PAYMENT")
        .length,
      paid: bookings.filter((booking) => booking.status === "PAID").length,
      checkedIn: bookings.filter((booking) => booking.status === "CHECKED_IN")
        .length,
      revenue: paidBookings.reduce(
        (sum, booking) => sum + booking.totalAmount,
        0,
      ),
    };
  }, [bookings]);

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
            Booking Control
          </p>
          <h1 className="page-title mt-2 text-4xl text-white">Bookings</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted">
            View customer bookings, payment status, ticket status and seat
            details.
          </p>
        </div>

        <button
          type="button"
          onClick={() => bookingsQuery.refetch()}
          className="btn-dark action-button inline-flex items-center justify-center gap-2"
        >
          <RefreshCw size={17} />
          Refresh
        </button>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div className="eous-card p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">
            Total Bookings
          </p>
          <p className="mt-2 text-3xl font-extrabold text-white">
            {summary.total}
          </p>
        </div>

        <div className="eous-card p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">
            Waiting Payment
          </p>
          <p className="mt-2 text-3xl font-extrabold text-sky-300">
            {summary.waiting}
          </p>
        </div>

        <div className="eous-card p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">Paid</p>
          <p className="mt-2 text-3xl font-extrabold text-[var(--color-success)]">
            {summary.paid}
          </p>
        </div>

        <div className="eous-card p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">
            Checked In
          </p>
          <p className="mt-2 text-3xl font-extrabold text-[var(--color-primary)]">
            {summary.checkedIn}
          </p>
        </div>

        <div className="eous-card p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">
            Mock Revenue
          </p>
          <p className="mt-2 text-2xl font-extrabold text-white">
            {formatCurrency(summary.revenue)}
          </p>
        </div>
      </div>

      <div className="eous-card p-5">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
          <div>
            <h2 className="card-title text-2xl text-white">Booking List</h2>
            <p className="mt-1 text-sm text-muted">
              {filteredBookings.length} booking(s) found
            </p>
          </div>

          <div className="grid gap-3 md:grid-cols-[1fr_220px]">
            <div className="relative">
              <Search
                size={17}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
              />
              <input
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] py-3 pl-10 pr-4 text-sm text-white outline-none transition focus:border-[var(--color-primary)] md:w-96"
                placeholder="Search booking, customer, movie, seat..."
              />
            </div>

            <select
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(event.target.value as "ALL" | BookingStatus)
              }
              className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
            >
              <option value="ALL">All Status</option>
              <option value="WAITING_PAYMENT">Waiting Payment</option>
              <option value="PAID">Paid</option>
              <option value="CHECKED_IN">Checked In</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="EXPIRED">Expired</option>
            </select>
          </div>
        </div>
      </div>

      {bookingsQuery.isLoading && (
        <div className="eous-card p-8 text-center text-muted">
          Loading bookings...
        </div>
      )}

      {bookingsQuery.isError && (
        <div className="eous-card border-[rgba(255,59,59,0.4)] p-8 text-center text-[var(--color-danger)]">
          {getErrorMessage(bookingsQuery.error)}
        </div>
      )}

      {!bookingsQuery.isLoading && !bookingsQuery.isError && (
        <div className="grid gap-4">
          {filteredBookings.map((booking) => (
            <article key={booking.id} className="eous-card p-5">
              <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-3">
                    <h3 className="card-title text-2xl text-white">
                      {getBookingCode(booking)}
                    </h3>

                    <span
                      className={[
                        "rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.12em]",
                        getBookingStatusClass(booking.status),
                      ].join(" ")}
                    >
                      {getStatusLabel(booking.status)}
                    </span>
                  </div>

                  <p className="mt-1 text-sm text-muted">
                    Created at {dayjs(booking.createdAt).format("HH:mm DD/MM/YYYY")}
                  </p>
                </div>

                <button
                  type="button"
                  onClick={() => setSelectedBookingId(booking.id)}
                  className="btn-dark action-button inline-flex items-center justify-center gap-2"
                >
                  <Eye size={16} />
                  View Details
                </button>
              </div>

              <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                  <div className="flex items-center gap-2 text-muted">
                    <UserRound size={16} />
                    <p className="text-xs uppercase tracking-[0.16em]">
                      Customer
                    </p>
                  </div>
                  <p className="mt-2 truncate font-semibold text-white">
                    {getCustomerName(booking)}
                  </p>
                </div>

                <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                  <div className="flex items-center gap-2 text-muted">
                    <Film size={16} />
                    <p className="text-xs uppercase tracking-[0.16em]">Movie</p>
                  </div>
                  <p className="mt-2 truncate font-semibold text-white">
                    {booking.showtime?.movie?.title ?? "Unknown Movie"}
                  </p>
                </div>

                <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                  <div className="flex items-center gap-2 text-muted">
                    <CalendarDays size={16} />
                    <p className="text-xs uppercase tracking-[0.16em]">
                      Showtime
                    </p>
                  </div>
                  <p className="mt-2 font-semibold text-white">
                    {booking.showtime?.startTime
                      ? dayjs(booking.showtime.startTime).format(
                          "DD/MM/YYYY HH:mm",
                        )
                      : "N/A"}
                  </p>
                  <p className="mt-1 text-xs text-muted">
                    {booking.showtime?.room?.name ?? "Unknown Room"}
                  </p>
                </div>

                <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                  <div className="flex items-center gap-2 text-muted">
                    <CreditCard size={16} />
                    <p className="text-xs uppercase tracking-[0.16em]">
                      Amount
                    </p>
                  </div>
                  <p className="mt-2 font-semibold text-white">
                    {formatCurrency(booking.totalAmount)}
                  </p>
                </div>
              </div>

              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3">
                  <p className="text-xs uppercase tracking-[0.16em] text-muted">
                    Seats
                  </p>
                  <p className="mt-1 truncate font-semibold text-white">
                    {getSeatCodes(booking)}
                  </p>
                </div>

                <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3">
                  <p className="text-xs uppercase tracking-[0.16em] text-muted">
                    Payment
                  </p>
                  <p
                    className={[
                      "mt-1 font-semibold",
                      getPaymentStatusClass(booking.payment?.status),
                    ].join(" ")}
                  >
                    {getStatusLabel(booking.payment?.status)}
                  </p>
                </div>

                <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3">
                  <div className="flex items-center gap-2 text-muted">
                    <Ticket size={15} />
                    <p className="text-xs uppercase tracking-[0.16em]">
                      Ticket
                    </p>
                  </div>
                  <p
                    className={[
                      "mt-1 font-semibold",
                      getTicketStatusClass(booking.ticket?.status),
                    ].join(" ")}
                  >
                    {getStatusLabel(booking.ticket?.status)}
                  </p>
                </div>
              </div>
            </article>
          ))}

          {filteredBookings.length === 0 && (
            <div className="eous-card p-8 text-center text-muted">
              No bookings found.
            </div>
          )}
        </div>
      )}

      {selectedBookingQuery.isLoading && selectedBookingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-5 backdrop-blur">
          <div className="eous-card p-8 text-center text-muted">
            Loading booking detail...
          </div>
        </div>
      )}

      {selectedBookingQuery.data && (
        <BookingDetailModal
          booking={selectedBookingQuery.data}
          onClose={() => setSelectedBookingId(null)}
        />
      )}
    </div>
  );
}