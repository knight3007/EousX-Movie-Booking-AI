import axios from "axios";
import dayjs from "dayjs";
import {
  CalendarDays,
  Clock,
  Edit3,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from "lucide-react";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getMovies } from "../movies/moviesApi";
import { getRooms } from "../rooms/roomsApi";
import {
  cancelShowtime,
  createShowtime,
  getShowtimes,
  updateShowtime,
} from "./showtimesApi";
import type {
  Showtime,
  ShowtimePayload,
  ShowtimeStatus,
} from "../../types/showtime";

type ShowtimeFormState = {
  movieId: string;
  roomId: string;
  date: string;
  startTime: string;
  endTime: string;
  basePrice: string;
  status: ShowtimeStatus;
};

const today = dayjs().format("YYYY-MM-DD");

const emptyForm: ShowtimeFormState = {
  movieId: "",
  roomId: "",
  date: today,
  startTime: "18:00",
  endTime: "",
  basePrice: "90000",
  status: "OPEN",
};

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

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);
}

function toLocalInputDateTime(date: string, time: string) {
  return dayjs(`${date}T${time}`).toISOString();
}

function toPayload(form: ShowtimeFormState): ShowtimePayload {
  const payload: ShowtimePayload = {
    movieId: form.movieId,
    roomId: form.roomId,
    startTime: toLocalInputDateTime(form.date, form.startTime),
    basePrice: Number(form.basePrice),
    status: form.status,
  };

  if (form.endTime.trim()) {
    payload.endTime = toLocalInputDateTime(form.date, form.endTime);
  }

  return payload;
}

function getStatusLabel(status: ShowtimeStatus) {
  if (status === "OPEN") return "Open";
  return "Cancelled";
}

function getStatusClass(status: ShowtimeStatus) {
  if (status === "OPEN") {
    return "border-[rgba(50,213,131,0.35)] bg-[rgba(50,213,131,0.12)] text-[var(--color-success)]";
  }

  return "border-[rgba(255,59,59,0.35)] bg-[rgba(255,59,59,0.12)] text-[var(--color-danger)]";
}

function getShowtimeDate(showtime: Showtime) {
  return dayjs(showtime.startTime).format("YYYY-MM-DD");
}

export default function ShowtimesPage() {
  const queryClient = useQueryClient();

  const [form, setForm] = useState<ShowtimeFormState>(emptyForm);
  const [editingShowtime, setEditingShowtime] = useState<Showtime | null>(null);
  const [selectedDate, setSelectedDate] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | ShowtimeStatus>(
    "ALL",
  );
  const [formError, setFormError] = useState<string | null>(null);

  const moviesQuery = useQuery({
    queryKey: ["movies"],
    queryFn: getMovies,
  });

  const roomsQuery = useQuery({
    queryKey: ["rooms"],
    queryFn: getRooms,
  });

  const showtimesQuery = useQuery({
    queryKey: ["showtimes", selectedDate],
    queryFn: () => getShowtimes(selectedDate || undefined),
  });

  const createMutation = useMutation({
    mutationFn: createShowtime,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["showtimes"] });
      resetForm();
    },
    onError: (error) => {
      setFormError(getErrorMessage(error));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: ShowtimePayload }) =>
      updateShowtime(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["showtimes"] });
      resetForm();
    },
    onError: (error) => {
      setFormError(getErrorMessage(error));
    },
  });

  const cancelMutation = useMutation({
    mutationFn: cancelShowtime,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["showtimes"] });
    },
    onError: (error) => {
      window.alert(getErrorMessage(error));
    },
  });

  const filteredShowtimes = useMemo(() => {
    const showtimes = showtimesQuery.data ?? [];
    const keyword = searchTerm.trim().toLowerCase();

    return showtimes.filter((showtime) => {
      const movieTitle = showtime.movie?.title?.toLowerCase() ?? "";
      const roomName = showtime.room?.name?.toLowerCase() ?? "";

      const matchSearch =
        !keyword ||
        movieTitle.includes(keyword) ||
        roomName.includes(keyword) ||
        showtime.id.toLowerCase().includes(keyword);

      const matchStatus =
        statusFilter === "ALL" ? true : showtime.status === statusFilter;

      return matchSearch && matchStatus;
    });
  }, [showtimesQuery.data, searchTerm, statusFilter]);

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  function resetForm() {
    setForm(emptyForm);
    setEditingShowtime(null);
    setFormError(null);
  }

  function validateForm() {
    if (!form.movieId) return "Movie is required";
    if (!form.roomId) return "Room is required";
    if (!form.date) return "Date is required";
    if (!form.startTime) return "Start time is required";

    const basePrice = Number(form.basePrice);
    if (!Number.isFinite(basePrice) || basePrice < 0) {
      return "Base price must be greater than or equal to 0";
    }

    if (form.endTime && form.endTime <= form.startTime) {
      return "End time must be after start time";
    }

    return null;
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    const error = validateForm();

    if (error) {
      setFormError(error);
      return;
    }

    const payload = toPayload(form);

    if (editingShowtime) {
      updateMutation.mutate({
        id: editingShowtime.id,
        payload,
      });
      return;
    }

    createMutation.mutate(payload);
  }

  function handleEdit(showtime: Showtime) {
    setEditingShowtime(showtime);

    setForm({
      movieId: showtime.movieId,
      roomId: showtime.roomId,
      date: dayjs(showtime.startTime).format("YYYY-MM-DD"),
      startTime: dayjs(showtime.startTime).format("HH:mm"),
      endTime: showtime.endTime ? dayjs(showtime.endTime).format("HH:mm") : "",
      basePrice: String(showtime.basePrice),
      status: showtime.status,
    });

    setFormError(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function handleCancel(showtime: Showtime) {
    const confirmed = window.confirm(
      `Cancel showtime "${showtime.movie?.title ?? showtime.id}" at ${dayjs(
        showtime.startTime,
      ).format("HH:mm DD/MM/YYYY")}?`,
    );

    if (!confirmed) return;

    cancelMutation.mutate(showtime.id);
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
            Schedule Control
          </p>
          <h1 className="page-title mt-2 text-4xl text-white">
            Showtime Management
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-muted">
            Create and manage movie screening schedules for seeded rooms.
          </p>
        </div>

        <button
          type="button"
          onClick={() => showtimesQuery.refetch()}
          className="btn-dark action-button inline-flex items-center justify-center gap-2"
        >
          <RefreshCw size={17} />
          Refresh
        </button>
      </div>

      <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
        <section className="eous-card h-fit p-6">
          <div className="mb-5 flex items-center justify-between">
            <div>
              <h2 className="card-title text-2xl text-white">
                {editingShowtime ? "Update Showtime" : "Create Showtime"}
              </h2>
              <p className="mt-1 text-sm text-muted">
                If end time is empty, backend calculates it from movie runtime.
              </p>
            </div>

            {editingShowtime && (
              <button
                type="button"
                onClick={resetForm}
                className="rounded-lg border border-[var(--color-border)] p-2 text-muted transition hover:text-white"
                title="Cancel edit"
              >
                <X size={18} />
              </button>
            )}
          </div>

          {formError && (
            <div className="mb-4 rounded-xl border border-[rgba(255,59,59,0.35)] bg-[rgba(255,59,59,0.12)] px-4 py-3 text-sm text-[var(--color-danger)]">
              {formError}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Movie
              </label>
              <select
                value={form.movieId}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    movieId: event.target.value,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
              >
                <option value="">Select movie</option>
                {(moviesQuery.data ?? []).map((movie) => (
                  <option key={movie.id} value={movie.id}>
                    {movie.title} — {movie.runtime} min
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Screening Room
              </label>
              <select
                value={form.roomId}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    roomId: event.target.value,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
              >
                <option value="">Select room</option>
                {(roomsQuery.data ?? []).map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.name} — {room.type} —{" "}
                    {room._count?.seats ?? room.totalSeats ?? 0} seats
                  </option>
                ))}
              </select>
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Date
                </label>
                <input
                  type="date"
                  value={form.date}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      date: event.target.value,
                    }))
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Start Time
                </label>
                <input
                  type="time"
                  value={form.startTime}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      startTime: event.target.value,
                    }))
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                />
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  End Time
                </label>
                <input
                  type="time"
                  value={form.endTime}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      endTime: event.target.value,
                    }))
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Base Price
                </label>
                <input
                  type="number"
                  min="0"
                  value={form.basePrice}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      basePrice: event.target.value,
                    }))
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                  placeholder="90000"
                />
              </div>
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Status
              </label>
              <select
                value={form.status}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    status: event.target.value as ShowtimeStatus,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
              >
                <option value="OPEN">Open</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary action-button flex w-full items-center justify-center gap-2 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Plus size={18} />
              {isSubmitting
                ? "Saving..."
                : editingShowtime
                  ? "Update Showtime"
                  : "Create Showtime"}
            </button>
          </form>
        </section>

        <section className="space-y-5">
          <div className="eous-card p-5">
            <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
              <div>
                <h2 className="card-title text-2xl text-white">
                  Showtime List
                </h2>
                <p className="mt-1 text-sm text-muted">
                  {filteredShowtimes.length} showtime(s) found
                </p>
              </div>

              <div className="grid gap-3 md:grid-cols-3">
                <div className="relative">
                  <Search
                    size={17}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                  />
                  <input
                    value={searchTerm}
                    onChange={(event) => setSearchTerm(event.target.value)}
                    className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] py-3 pl-10 pr-4 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                    placeholder="Search..."
                  />
                </div>

                <input
                  type="date"
                  value={selectedDate}
                  onChange={(event) => setSelectedDate(event.target.value)}
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                />

                <select
                  value={statusFilter}
                  onChange={(event) =>
                    setStatusFilter(event.target.value as "ALL" | ShowtimeStatus)
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                >
                  <option value="ALL">All Status</option>
                  <option value="OPEN">Open</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
            </div>

            {selectedDate && (
              <button
                type="button"
                onClick={() => setSelectedDate("")}
                className="mt-4 text-xs font-bold uppercase tracking-[0.18em] text-[var(--color-primary)]"
              >
                Clear date filter
              </button>
            )}
          </div>

          {showtimesQuery.isLoading && (
            <div className="eous-card p-8 text-center text-muted">
              Loading showtimes...
            </div>
          )}

          {showtimesQuery.isError && (
            <div className="eous-card border-[rgba(255,59,59,0.4)] p-8 text-center text-[var(--color-danger)]">
              {getErrorMessage(showtimesQuery.error)}
            </div>
          )}

          {!showtimesQuery.isLoading && !showtimesQuery.isError && (
            <div className="grid gap-4">
              {filteredShowtimes.map((showtime) => (
                <article key={showtime.id} className="eous-card p-5">
                  <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="card-title text-2xl text-white">
                          {showtime.movie?.title ?? "Unknown Movie"}
                        </h3>

                        <span
                          className={[
                            "rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.12em]",
                            getStatusClass(showtime.status),
                          ].join(" ")}
                        >
                          {getStatusLabel(showtime.status)}
                        </span>
                      </div>

                      <p className="mt-1 text-sm text-muted">
                        {showtime.room?.name ?? "Unknown Room"} •{" "}
                        {showtime.room?.type ?? "Room"}
                      </p>
                    </div>

                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => handleEdit(showtime)}
                        className="btn-dark action-button inline-flex items-center gap-2"
                      >
                        <Edit3 size={16} />
                        Edit
                      </button>

                      <button
                        type="button"
                        onClick={() => handleCancel(showtime)}
                        disabled={
                          cancelMutation.isPending ||
                          showtime.status === "CANCELLED"
                        }
                        className="action-button inline-flex items-center gap-2 rounded-[10px] bg-[var(--color-danger)] px-[18px] py-[10px] font-bold uppercase tracking-[0.05em] text-white transition hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <Trash2 size={16} />
                        Cancel
                      </button>
                    </div>
                  </div>

                  <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                    <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                      <div className="flex items-center gap-2 text-muted">
                        <CalendarDays size={16} />
                        <p className="text-xs uppercase tracking-[0.16em]">
                          Date
                        </p>
                      </div>
                      <p className="mt-2 font-semibold text-white">
                        {dayjs(showtime.startTime).format("DD/MM/YYYY")}
                      </p>
                    </div>

                    <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                      <div className="flex items-center gap-2 text-muted">
                        <Clock size={16} />
                        <p className="text-xs uppercase tracking-[0.16em]">
                          Time
                        </p>
                      </div>
                      <p className="mt-2 font-semibold text-white">
                        {dayjs(showtime.startTime).format("HH:mm")} -{" "}
                        {dayjs(showtime.endTime).format("HH:mm")}
                      </p>
                    </div>

                    <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                      <p className="text-xs uppercase tracking-[0.16em] text-muted">
                        Base Price
                      </p>
                      <p className="mt-2 font-semibold text-white">
                        {formatCurrency(showtime.basePrice)}
                      </p>
                    </div>

                    <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                      <p className="text-xs uppercase tracking-[0.16em] text-muted">
                        Showtime ID
                      </p>
                      <p className="mt-2 truncate font-semibold text-white">
                        {showtime.id}
                      </p>
                    </div>
                  </div>

                  <div className="mt-4 flex flex-wrap gap-2">
                    <span className="rounded-full border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-1 text-xs text-muted">
                      Movie ID: {showtime.movieId}
                    </span>
                    <span className="rounded-full border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-1 text-xs text-muted">
                      Room ID: {showtime.roomId}
                    </span>
                    <span className="rounded-full border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-1 text-xs text-muted">
                      Date: {getShowtimeDate(showtime)}
                    </span>
                  </div>
                </article>
              ))}

              {filteredShowtimes.length === 0 && (
                <div className="eous-card p-8 text-center text-muted">
                  No showtimes found.
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}