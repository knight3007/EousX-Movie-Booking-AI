import axios from "axios";
import dayjs from "dayjs";
import {
  Armchair,
  CalendarDays,
  Clock,
  Film,
  RefreshCw,
  Search,
  Timer,
} from "lucide-react";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getShowtimes } from "../showtimes/showtimesApi";
import { getAdminSeatMap } from "./seatMapApi";
import type { SeatMapSeat, SeatStatus } from "../../types/seat";
import type { Showtime } from "../../types/showtime";

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

function getSeatCode(seat: SeatMapSeat) {
  return seat.code ?? seat.seatCode ?? `${getSeatRow(seat)}${getSeatNumber(seat)}`;
}

function getSeatRow(seat: SeatMapSeat) {
  const code = seat.code ?? seat.seatCode ?? "";
  return seat.row ?? seat.rowLabel ?? code.charAt(0) ?? "?";
}

function getSeatNumber(seat: SeatMapSeat) {
  const code = seat.code ?? seat.seatCode ?? "";
  const codeNumber = Number(code.replace(/\D/g, ""));

  return seat.number ?? seat.seatNumber ?? codeNumber ?? 0;
}

function getSeatType(seat: SeatMapSeat) {
  return seat.type ?? seat.seatType ?? "STANDARD";
}

function getStatusClass(status: SeatStatus) {
  switch (status) {
    case "AVAILABLE":
      return "border-[rgba(50,213,131,0.45)] bg-[rgba(50,213,131,0.16)] text-[var(--color-success)]";
    case "LOCKED":
      return "border-[rgba(245,196,0,0.55)] bg-[rgba(245,196,0,0.18)] text-[var(--color-primary)]";
    case "SOLD":
      return "border-[rgba(255,59,59,0.55)] bg-[rgba(255,59,59,0.18)] text-[var(--color-danger)]";
    case "MAINTENANCE":
      return "border-[rgba(156,163,175,0.35)] bg-[rgba(156,163,175,0.14)] text-[var(--color-muted)]";
    default:
      return "border-[var(--color-border)] bg-[var(--color-card)] text-white";
  }
}

function getStatusDotClass(status: SeatStatus) {
  switch (status) {
    case "AVAILABLE":
      return "bg-[var(--color-success)]";
    case "LOCKED":
      return "bg-[var(--color-primary)]";
    case "SOLD":
      return "bg-[var(--color-danger)]";
    case "MAINTENANCE":
      return "bg-[var(--color-muted)]";
    default:
      return "bg-white";
  }
}

function formatCurrency(value?: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

function getSeatSummary(seats: SeatMapSeat[]) {
  return {
    total: seats.length,
    available: seats.filter((seat) => seat.status === "AVAILABLE").length,
    locked: seats.filter((seat) => seat.status === "LOCKED").length,
    sold: seats.filter((seat) => seat.status === "SOLD").length,
    maintenance: seats.filter((seat) => seat.status === "MAINTENANCE").length,
  };
}

function groupSeatsByRow(seats: SeatMapSeat[]) {
  const rows = new Map<string, SeatMapSeat[]>();

  seats.forEach((seat) => {
    const row = getSeatRow(seat);

    if (!rows.has(row)) {
      rows.set(row, []);
    }

    rows.get(row)?.push(seat);
  });

  return Array.from(rows.entries())
    .sort(([rowA], [rowB]) => rowA.localeCompare(rowB))
    .map(([row, rowSeats]) => ({
      row,
      seats: rowSeats.sort((a, b) => getSeatNumber(a) - getSeatNumber(b)),
    }));
}

function ShowtimeOption({ showtime }: { showtime: Showtime }) {
  return (
    <>
      {showtime.movie?.title ?? "Unknown Movie"} —{" "}
      {showtime.room?.name ?? "Unknown Room"} —{" "}
      {dayjs(showtime.startTime).format("DD/MM HH:mm")}
    </>
  );
}

export default function SeatMonitorPage() {
  const [selectedDate, setSelectedDate] = useState("");
  const [selectedShowtimeId, setSelectedShowtimeId] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedSeat, setSelectedSeat] = useState<SeatMapSeat | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const showtimesQuery = useQuery({
    queryKey: ["showtimes", selectedDate],
    queryFn: () => getShowtimes(selectedDate || undefined),
  });

  const showtimes = showtimesQuery.data ?? [];

  const selectedShowtime = useMemo(() => {
    return showtimes.find((showtime) => showtime.id === selectedShowtimeId);
  }, [showtimes, selectedShowtimeId]);

  const filteredShowtimes = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();

    return showtimes.filter((showtime) => {
      if (!keyword) return true;

      return (
        showtime.movie?.title?.toLowerCase().includes(keyword) ||
        showtime.room?.name?.toLowerCase().includes(keyword) ||
        showtime.id.toLowerCase().includes(keyword)
      );
    });
  }, [showtimes, searchTerm]);

  const seatMapQuery = useQuery({
    queryKey: ["seat-map", selectedShowtimeId],
    queryFn: () => getAdminSeatMap(selectedShowtimeId),
    enabled: Boolean(selectedShowtimeId),
    refetchInterval: autoRefresh && selectedShowtimeId ? 5000 : false,
  });

  const seats = seatMapQuery.data ?? [];
  const groupedRows = useMemo(() => groupSeatsByRow(seats), [seats]);
  const summary = useMemo(() => getSeatSummary(seats), [seats]);

  const selectedSeatCode = selectedSeat ? getSeatCode(selectedSeat) : "";

  function handleSelectShowtime(showtimeId: string) {
    setSelectedShowtimeId(showtimeId);
    setSelectedSeat(null);
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
            Live Seat Control
          </p>
          <h1 className="page-title mt-2 text-4xl text-white">Seat Monitor</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted">
            Monitor seat status by showtime. Auto refresh keeps the view stable
            before adding WebSocket realtime.
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row">
          <button
            type="button"
            onClick={() => setAutoRefresh((current) => !current)}
            className={[
              "action-button rounded-[10px] border px-[18px] py-[10px] font-bold uppercase tracking-[0.05em] transition",
              autoRefresh
                ? "border-[var(--color-primary)] bg-[rgba(245,196,0,0.14)] text-[var(--color-primary)]"
                : "border-[var(--color-border)] bg-[var(--color-card)] text-muted",
            ].join(" ")}
          >
            Auto Refresh: {autoRefresh ? "On" : "Off"}
          </button>

          <button
            type="button"
            onClick={() => seatMapQuery.refetch()}
            disabled={!selectedShowtimeId}
            className="btn-dark action-button inline-flex items-center justify-center gap-2 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <RefreshCw size={17} />
            Refresh Seats
          </button>
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[360px_1fr]">
        <section className="space-y-5">
          <div className="eous-card p-5">
            <h2 className="card-title text-2xl text-white">Select Showtime</h2>
            <p className="mt-1 text-sm text-muted">
              Choose a showtime to view its seat map.
            </p>

            <div className="mt-5 space-y-4">
              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Date Filter
                </label>
                <input
                  type="date"
                  value={selectedDate}
                  onChange={(event) => {
                    setSelectedDate(event.target.value);
                    setSelectedShowtimeId("");
                    setSelectedSeat(null);
                  }}
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                />

                {selectedDate && (
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedDate("");
                      setSelectedShowtimeId("");
                      setSelectedSeat(null);
                    }}
                    className="mt-3 text-xs font-bold uppercase tracking-[0.18em] text-[var(--color-primary)]"
                  >
                    Clear date filter
                  </button>
                )}
              </div>

              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Search Showtime
                </label>

                <div className="relative">
                  <Search
                    size={17}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                  />
                  <input
                    value={searchTerm}
                    onChange={(event) => setSearchTerm(event.target.value)}
                    className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] py-3 pl-10 pr-4 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                    placeholder="Movie, room, showtime id..."
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Showtime
                </label>
                <select
                  value={selectedShowtimeId}
                  onChange={(event) => handleSelectShowtime(event.target.value)}
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                >
                  <option value="">Select showtime</option>
                  {filteredShowtimes.map((showtime) => (
                    <option key={showtime.id} value={showtime.id}>
                      <ShowtimeOption showtime={showtime} />
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {selectedShowtime && (
            <div className="eous-card p-5">
              <h2 className="card-title text-2xl text-white">Showtime Info</h2>

              <div className="mt-5 space-y-4">
                <div className="flex gap-3">
                  <Film className="mt-0.5 text-[var(--color-primary)]" size={18} />
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-muted">
                      Movie
                    </p>
                    <p className="font-semibold text-white">
                      {selectedShowtime.movie?.title ?? "Unknown Movie"}
                    </p>
                  </div>
                </div>

                <div className="flex gap-3">
                  <Armchair
                    className="mt-0.5 text-[var(--color-primary)]"
                    size={18}
                  />
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-muted">
                      Room
                    </p>
                    <p className="font-semibold text-white">
                      {selectedShowtime.room?.name ?? "Unknown Room"} •{" "}
                      {selectedShowtime.room?.type ?? "Room"}
                    </p>
                  </div>
                </div>

                <div className="flex gap-3">
                  <CalendarDays
                    className="mt-0.5 text-[var(--color-primary)]"
                    size={18}
                  />
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-muted">
                      Date
                    </p>
                    <p className="font-semibold text-white">
                      {dayjs(selectedShowtime.startTime).format("DD/MM/YYYY")}
                    </p>
                  </div>
                </div>

                <div className="flex gap-3">
                  <Clock
                    className="mt-0.5 text-[var(--color-primary)]"
                    size={18}
                  />
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-muted">
                      Time
                    </p>
                    <p className="font-semibold text-white">
                      {dayjs(selectedShowtime.startTime).format("HH:mm")} -{" "}
                      {dayjs(selectedShowtime.endTime).format("HH:mm")}
                    </p>
                  </div>
                </div>

                <div className="flex gap-3">
                  <Timer
                    className="mt-0.5 text-[var(--color-primary)]"
                    size={18}
                  />
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-muted">
                      Base Price
                    </p>
                    <p className="font-semibold text-white">
                      {formatCurrency(selectedShowtime.basePrice)}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {selectedSeat && (
            <div className="eous-card p-5">
              <h2 className="card-title text-2xl text-white">Seat Detail</h2>

              <div className="mt-5 space-y-3 text-sm">
                <div className="flex items-center justify-between gap-4">
                  <span className="text-muted">Seat</span>
                  <span className="font-semibold text-white">{selectedSeatCode}</span>
                </div>

                <div className="flex items-center justify-between gap-4">
                  <span className="text-muted">Status</span>
                  <span
                    className={[
                      "rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.12em]",
                      getStatusClass(selectedSeat.status),
                    ].join(" ")}
                  >
                    {selectedSeat.status}
                  </span>
                </div>

                <div className="flex items-center justify-between gap-4">
                  <span className="text-muted">Type</span>
                  <span className="font-semibold text-white">
                    {getSeatType(selectedSeat)}
                  </span>
                </div>

                {selectedSeat.lockedUntil && (
                  <div className="flex items-center justify-between gap-4">
                    <span className="text-muted">Locked Until</span>
                    <span className="font-semibold text-white">
                      {dayjs(selectedSeat.lockedUntil).format("HH:mm:ss DD/MM")}
                    </span>
                  </div>
                )}

                {selectedSeat.user && (
                  <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.55)] p-3">
                    <p className="text-xs uppercase tracking-[0.16em] text-muted">
                      User
                    </p>
                    <p className="mt-1 font-semibold text-white">
                      {selectedSeat.user.fullName ?? selectedSeat.user.email ?? "N/A"}
                    </p>
                  </div>
                )}

                {selectedSeat.booking && (
                  <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.55)] p-3">
                    <p className="text-xs uppercase tracking-[0.16em] text-muted">
                      Booking
                    </p>
                    <p className="mt-1 font-semibold text-white">
                      {selectedSeat.booking.code ?? selectedSeat.booking.id}
                    </p>
                    <p className="text-xs text-muted">
                      {selectedSeat.booking.status}
                    </p>
                  </div>
                )}

                {selectedSeat.ticket && (
                  <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.55)] p-3">
                    <p className="text-xs uppercase tracking-[0.16em] text-muted">
                      Ticket
                    </p>
                    <p className="mt-1 truncate font-semibold text-white">
                      {selectedSeat.ticket.qrCode ?? selectedSeat.ticket.id}
                    </p>
                    <p className="text-xs text-muted">
                      {selectedSeat.ticket.status}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}
        </section>

        <section className="space-y-5">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            <div className="eous-card p-4">
              <p className="text-xs uppercase tracking-[0.16em] text-muted">
                Total Seats
              </p>
              <p className="mt-2 text-3xl font-extrabold text-white">
                {summary.total}
              </p>
            </div>

            <div className="eous-card p-4">
              <p className="text-xs uppercase tracking-[0.16em] text-muted">
                Available
              </p>
              <p className="mt-2 text-3xl font-extrabold text-[var(--color-success)]">
                {summary.available}
              </p>
            </div>

            <div className="eous-card p-4">
              <p className="text-xs uppercase tracking-[0.16em] text-muted">
                Locked
              </p>
              <p className="mt-2 text-3xl font-extrabold text-[var(--color-primary)]">
                {summary.locked}
              </p>
            </div>

            <div className="eous-card p-4">
              <p className="text-xs uppercase tracking-[0.16em] text-muted">
                Sold
              </p>
              <p className="mt-2 text-3xl font-extrabold text-[var(--color-danger)]">
                {summary.sold}
              </p>
            </div>

            <div className="eous-card p-4">
              <p className="text-xs uppercase tracking-[0.16em] text-muted">
                Maintenance
              </p>
              <p className="mt-2 text-3xl font-extrabold text-muted">
                {summary.maintenance}
              </p>
            </div>
          </div>

          <div className="eous-card p-6">
            <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
              <div>
                <h2 className="card-title text-2xl text-white">Seat Map</h2>
                <p className="mt-1 text-sm text-muted">
                  Last update:{" "}
                  {seatMapQuery.dataUpdatedAt
                    ? dayjs(seatMapQuery.dataUpdatedAt).format("HH:mm:ss")
                    : "N/A"}
                </p>
              </div>

              <div className="flex flex-wrap gap-3">
                {(["AVAILABLE", "LOCKED", "SOLD", "MAINTENANCE"] as SeatStatus[]).map(
                  (status) => (
                    <div
                      key={status}
                      className="flex items-center gap-2 rounded-full border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-2 text-xs font-bold uppercase tracking-[0.12em] text-muted"
                    >
                      <span
                        className={[
                          "h-2.5 w-2.5 rounded-full",
                          getStatusDotClass(status),
                        ].join(" ")}
                      />
                      {status}
                    </div>
                  ),
                )}
              </div>
            </div>

            {!selectedShowtimeId && (
              <div className="mt-8 rounded-2xl border border-dashed border-[var(--color-border)] p-10 text-center text-muted">
                Select a showtime to view seat map.
              </div>
            )}

            {selectedShowtimeId && seatMapQuery.isLoading && (
              <div className="mt-8 rounded-2xl border border-[var(--color-border)] p-10 text-center text-muted">
                Loading seat map...
              </div>
            )}

            {selectedShowtimeId && seatMapQuery.isError && (
              <div className="mt-8 rounded-2xl border border-[rgba(255,59,59,0.4)] bg-[rgba(255,59,59,0.08)] p-10 text-center text-[var(--color-danger)]">
                {getErrorMessage(seatMapQuery.error)}
              </div>
            )}

            {selectedShowtimeId && !seatMapQuery.isLoading && !seatMapQuery.isError && (
              <div className="mt-8 overflow-x-auto">
                <div className="mx-auto mb-8 max-w-2xl rounded-t-[44px] border border-[var(--color-primary)] bg-[rgba(245,196,0,0.12)] px-10 py-4 text-center">
                  <p className="brand-title text-xl text-[var(--color-primary)]">
                    Screen
                  </p>
                </div>

                <div className="mx-auto min-w-max space-y-3">
                  {groupedRows.map((group) => (
                    <div key={group.row} className="flex items-center justify-center gap-3">
                      <div className="brand-title flex h-10 w-10 items-center justify-center rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] text-sm text-muted">
                        {group.row}
                      </div>

                      <div className="flex gap-2">
                        {group.seats.map((seat) => {
                          const seatCode = getSeatCode(seat);
                          const isSelected =
                            selectedSeat && getSeatCode(selectedSeat) === seatCode;

                          return (
                            <button
                              key={seat.id ?? seatCode}
                              type="button"
                              onClick={() => setSelectedSeat(seat)}
                              title={`${seatCode} - ${seat.status}`}
                              className={[
                                "h-12 min-w-12 rounded-xl border px-2 text-xs font-extrabold uppercase transition hover:-translate-y-0.5",
                                getStatusClass(seat.status),
                                isSelected
                                  ? "ring-2 ring-[var(--color-primary)] ring-offset-2 ring-offset-[var(--color-bg)]"
                                  : "",
                              ].join(" ")}
                            >
                              {seatCode}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  ))}

                  {groupedRows.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-[var(--color-border)] p-10 text-center text-muted">
                      No seats found for this showtime.
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}