import axios from "axios";
import dayjs from "dayjs";
import {
  BadgeCheck,
  CalendarDays,
  CheckCircle2,
  CircleAlert,
  CreditCard,
  Film,
  QrCode,
  RefreshCw,
  TicketCheck,
  UserRound,
} from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { checkInTicket, verifyTicket } from "./ticketsApi";
import type { TicketDetail, VerifyTicketResponse } from "../../types/ticket";

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

function getStatusLabel(status?: string | null) {
  if (!status) return "N/A";

  return status
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function getSeatCodes(ticket: TicketDetail | null) {
  const seats = ticket?.booking?.seats ?? [];

  if (seats.length === 0) return "No seats";

  return seats
    .map((bookingSeat) => {
      const seat = bookingSeat.seat;
      return seat?.code ?? `${seat?.row ?? ""}${seat?.number ?? ""}`.trim();
    })
    .filter(Boolean)
    .join(", ");
}

function getVerifyStatusClass(status?: string) {
  switch (status) {
    case "VALID":
      return "border-[rgba(50,213,131,0.4)] bg-[rgba(50,213,131,0.12)] text-[var(--color-success)]";
    case "ALREADY_USED":
    case "USED":
      return "border-[rgba(245,196,0,0.45)] bg-[rgba(245,196,0,0.14)] text-[var(--color-primary)]";
    case "INVALID":
    case "EXPIRED":
    case "CANCELLED":
      return "border-[rgba(255,59,59,0.4)] bg-[rgba(255,59,59,0.12)] text-[var(--color-danger)]";
    default:
      return "border-[var(--color-border)] bg-[var(--color-card)] text-muted";
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

function TicketResultCard({
  result,
  onConfirm,
  isCheckingIn,
}: {
  result: VerifyTicketResponse;
  onConfirm: () => void;
  isCheckingIn: boolean;
}) {
  const ticket = result.ticket;
  const booking = ticket?.booking;
  const showtime = booking?.showtime;
  const customer =
    booking?.user?.fullName ||
    booking?.user?.email ||
    booking?.user?.phone ||
    booking?.userId ||
    "N/A";

  const canCheckIn = result.status === "VALID" && ticket?.id;

  return (
    <div className="grid gap-6 xl:grid-cols-[1fr_380px]">
      <section className="eous-card p-6">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
              Verification Result
            </p>

            <div className="mt-3 flex flex-wrap items-center gap-3">
              <h2 className="card-title text-3xl text-white">
                {getStatusLabel(result.status)}
              </h2>

              <span
                className={[
                  "rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.12em]",
                  getVerifyStatusClass(result.status),
                ].join(" ")}
              >
                {result.status}
              </span>
            </div>

            <p className="mt-2 text-sm text-muted">{result.message}</p>
          </div>

          {result.status === "VALID" ? (
            <CheckCircle2 size={46} className="text-[var(--color-success)]" />
          ) : (
            <CircleAlert size={46} className="text-[var(--color-danger)]" />
          )}
        </div>

        {ticket ? (
          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <div className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.45)] p-5">
              <h3 className="card-title text-xl text-white">Ticket</h3>

              <div className="mt-4">
                <DetailRow label="Ticket ID" value={ticket.id} />
                <DetailRow
                  label="Ticket Status"
                  value={
                    <span
                      className={[
                        "font-bold",
                        ticket.status === "VALID"
                          ? "text-[var(--color-success)]"
                          : ticket.status === "USED"
                            ? "text-[var(--color-primary)]"
                            : "text-[var(--color-danger)]",
                      ].join(" ")}
                    >
                      {getStatusLabel(ticket.status)}
                    </span>
                  }
                />
                <DetailRow
                  label="QR Code"
                  value={
                    ticket.qrCode ? (
                      <span className="break-all">{ticket.qrCode}</span>
                    ) : (
                      "N/A"
                    )
                  }
                />
                <DetailRow
                  label="Checked In At"
                  value={
                    ticket.checkedInAt
                      ? dayjs(ticket.checkedInAt).format("HH:mm DD/MM/YYYY")
                      : "N/A"
                  }
                />
              </div>
            </div>

            <div className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.45)] p-5">
              <h3 className="card-title text-xl text-white">Booking</h3>

              <div className="mt-4">
                <DetailRow label="Booking Code" value={booking?.code || booking?.id || "N/A"} />
                <DetailRow
                  label="Booking Status"
                  value={getStatusLabel(booking?.status)}
                />
                <DetailRow
                  label="Total Amount"
                  value={formatCurrency(booking?.totalAmount)}
                />
                <DetailRow label="Seats" value={getSeatCodes(ticket)} />
              </div>
            </div>

            <div className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.45)] p-5">
              <h3 className="card-title text-xl text-white">Customer</h3>

              <div className="mt-4">
                <DetailRow label="Customer" value={customer} />
                <DetailRow label="Email" value={booking?.user?.email || "N/A"} />
                <DetailRow label="Phone" value={booking?.user?.phone || "N/A"} />
              </div>
            </div>

            <div className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.45)] p-5">
              <h3 className="card-title text-xl text-white">Showtime</h3>

              <div className="mt-4">
                <DetailRow
                  label="Movie"
                  value={showtime?.movie?.title || "Unknown Movie"}
                />
                <DetailRow
                  label="Room"
                  value={`${showtime?.room?.name ?? "Unknown Room"} • ${
                    showtime?.room?.type ?? "Room"
                  }`}
                />
                <DetailRow
                  label="Date"
                  value={
                    showtime?.startTime
                      ? dayjs(showtime.startTime).format("DD/MM/YYYY")
                      : "N/A"
                  }
                />
                <DetailRow
                  label="Time"
                  value={
                    showtime?.startTime && showtime?.endTime
                      ? `${dayjs(showtime.startTime).format("HH:mm")} - ${dayjs(
                          showtime.endTime,
                        ).format("HH:mm")}`
                      : "N/A"
                  }
                />
              </div>
            </div>
          </div>
        ) : (
          <div className="mt-6 rounded-2xl border border-[rgba(255,59,59,0.35)] bg-[rgba(255,59,59,0.08)] p-6 text-sm text-[var(--color-danger)]">
            No ticket data returned from server.
          </div>
        )}
      </section>

      <aside className="eous-card h-fit p-6">
        <div className="flex items-start gap-4">
          <div className="rounded-2xl bg-[rgba(245,196,0,0.14)] p-4 text-[var(--color-primary)]">
            <TicketCheck size={30} />
          </div>

          <div>
            <h3 className="card-title text-2xl text-white">Staff Action</h3>
            <p className="mt-1 text-sm text-muted">
              This button is only for cinema staff to confirm ticket usage.
            </p>
          </div>
        </div>

        <div className="mt-6 rounded-2xl border border-[var(--color-border)] bg-[var(--color-bg)] p-4">
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-[var(--color-primary)]">
            Important Note
          </p>
          <p className="mt-2 text-sm leading-6 text-muted">
            Do not press this button unless the customer is physically at the
            cinema and the ticket has been verified.
          </p>
        </div>

        <button
          type="button"
          onClick={onConfirm}
          disabled={!canCheckIn || isCheckingIn}
          className="action-button mt-6 flex w-full items-center justify-center gap-2 rounded-[10px] bg-[var(--color-success)] px-[18px] py-[12px] font-bold uppercase tracking-[0.05em] text-[#06130a] transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-45"
        >
          <BadgeCheck size={18} />
          {isCheckingIn ? "Checking In..." : "Confirm Check-in"}
        </button>

        {!canCheckIn && (
          <p className="mt-3 text-center text-xs text-muted">
            Check-in is only available for VALID tickets.
          </p>
        )}
      </aside>
    </div>
  );
}

export default function TicketCheckInPage() {
  const queryClient = useQueryClient();

  const [qrCode, setQrCode] = useState("");
  const [verifyResult, setVerifyResult] = useState<VerifyTicketResponse | null>(
    null,
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const verifyMutation = useMutation({
    mutationFn: verifyTicket,
    onSuccess: (data) => {
      setVerifyResult(data);
      setErrorMessage(null);
    },
    onError: (error) => {
      setVerifyResult(null);
      setErrorMessage(getErrorMessage(error));
    },
  });

  const checkInMutation = useMutation({
    mutationFn: checkInTicket,
    onSuccess: (data) => {
      setVerifyResult({
        status: data.ticket.status === "USED" ? "ALREADY_USED" : data.ticket.status,
        message: data.message,
        ticket: data.ticket,
      });

      setErrorMessage(null);

      queryClient.invalidateQueries({ queryKey: ["bookings"] });
      queryClient.invalidateQueries({ queryKey: ["seat-map"] });
    },
    onError: (error) => {
      setErrorMessage(getErrorMessage(error));
    },
  });

  function handleVerify(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const value = qrCode.trim();

    if (!value) {
      setErrorMessage("QR code is required");
      setVerifyResult(null);
      return;
    }

    verifyMutation.mutate(value);
  }

  function handleConfirmCheckIn() {
    const ticketId = verifyResult?.ticket?.id;

    if (!ticketId) {
      setErrorMessage("Ticket ID was not found");
      return;
    }

    const confirmed = window.confirm(
      "Confirm check-in for this ticket? This action will mark the ticket as used.",
    );

    if (!confirmed) return;

    checkInMutation.mutate(ticketId);
  }

  function handleReset() {
    setQrCode("");
    setVerifyResult(null);
    setErrorMessage(null);
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
            Ticket Gate
          </p>
          <h1 className="page-title mt-2 text-4xl text-white">
            Ticket Check-in
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-muted">
            Verify a ticket QR code and confirm customer check-in.
          </p>
        </div>

        <button
          type="button"
          onClick={handleReset}
          className="btn-dark action-button inline-flex items-center justify-center gap-2"
        >
          <RefreshCw size={17} />
          Reset
        </button>
      </div>

      <section className="eous-card p-6">
        <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
          <div>
            <div className="flex items-start gap-4">
              <div className="rounded-2xl bg-[rgba(245,196,0,0.14)] p-4 text-[var(--color-primary)]">
                <QrCode size={32} />
              </div>

              <div>
                <h2 className="card-title text-2xl text-white">
                  Verify QR Code
                </h2>
                <p className="mt-1 text-sm text-muted">
                  Paste the QR code from the generated ticket.
                </p>
              </div>
            </div>

            <form onSubmit={handleVerify} className="mt-6 space-y-4">
              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  QR Code
                </label>
                <textarea
                  value={qrCode}
                  onChange={(event) => setQrCode(event.target.value)}
                  rows={4}
                  className="w-full resize-none rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                  placeholder="EOUSX-QR-..."
                />
              </div>

              {errorMessage && (
                <div className="rounded-xl border border-[rgba(255,59,59,0.35)] bg-[rgba(255,59,59,0.12)] px-4 py-3 text-sm text-[var(--color-danger)]">
                  {errorMessage}
                </div>
              )}

              <button
                type="submit"
                disabled={verifyMutation.isPending}
                className="btn-primary action-button inline-flex items-center justify-center gap-2 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <QrCode size={18} />
                {verifyMutation.isPending ? "Verifying..." : "Verify Ticket"}
              </button>
            </form>
          </div>

          <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-bg)] p-5">
            <h3 className="card-title text-xl text-white">How To Use</h3>

            <div className="mt-4 space-y-4 text-sm text-muted">
              <div className="flex gap-3">
                <UserRound size={18} className="mt-0.5 text-[var(--color-primary)]" />
                <p>Customer shows the QR code from the paid ticket.</p>
              </div>

              <div className="flex gap-3">
                <QrCode size={18} className="mt-0.5 text-[var(--color-primary)]" />
                <p>Staff pastes the QR code and verifies ticket validity.</p>
              </div>

              <div className="flex gap-3">
                <CreditCard size={18} className="mt-0.5 text-[var(--color-primary)]" />
                <p>Only paid and valid tickets can be checked in.</p>
              </div>

              <div className="flex gap-3">
                <TicketCheck
                  size={18}
                  className="mt-0.5 text-[var(--color-primary)]"
                />
                <p>Confirm check-in only when the customer enters the cinema.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {verifyResult && (
        <TicketResultCard
          result={verifyResult}
          onConfirm={handleConfirmCheckIn}
          isCheckingIn={checkInMutation.isPending}
        />
      )}
    </div>
  );
}