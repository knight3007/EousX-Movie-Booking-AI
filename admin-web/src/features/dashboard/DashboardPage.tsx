import axios from "axios";
import dayjs from "dayjs";
import {
  CalendarDays,
  CheckCircle2,
  Clock,
  CreditCard,
  Film,
  LayoutDashboard,
  RefreshCw,
  TicketCheck,
  Timer,
  TrendingUp,
  UsersRound,
} from "lucide-react";
import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { getBookings } from "../bookings/bookingsApi";
import { getShowtimes } from "../showtimes/showtimesApi";
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

function getStatusLabel(status?: string | null) {
  if (!status) return "N/A";

  return status
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function getBookingCode(booking: Booking) {
  return booking.code || booking.id;
}

function getCustomerName(booking: Booking) {
  return (
    booking.user?.fullName ||
    booking.user?.email ||
    booking.user?.phone ||
    booking.userId
  );
}

function getSeatCount(booking: Booking) {
  return booking.seats?.length ?? 0;
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

function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  tone = "white",
}: {
  title: string;
  value: string | number;
  subtitle: string;
  icon: React.ElementType;
  tone?: "white" | "yellow" | "green" | "red" | "blue";
}) {
  const toneClass = {
    white: "text-white",
    yellow: "text-[var(--color-primary)]",
    green: "text-[var(--color-success)]",
    red: "text-[var(--color-danger)]",
    blue: "text-sky-300",
  }[tone];

  return (
    <div className="eous-card p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.16em] text-muted">
            {title}
          </p>
          <p className={`mt-3 text-3xl font-extrabold ${toneClass}`}>
            {value}
          </p>
          <p className="mt-2 text-sm text-muted">{subtitle}</p>
        </div>

        <div className="rounded-2xl bg-[rgba(245,196,0,0.12)] p-3 text-[var(--color-primary)]">
          <Icon size={24} />
        </div>
      </div>
    </div>
  );
}

function ProgressBar({
  label,
  value,
  total,
}: {
  label: string;
  value: number;
  total: number;
}) {
  const percent = total > 0 ? Math.min((value / total) * 100, 100) : 0;

  return (
    <div>
      <div className="mb-2 flex items-center justify-between gap-4">
        <span className="text-sm text-muted">{label}</span>
        <span className="text-sm font-semibold text-white">
          {value}/{total}
        </span>
      </div>

      <div className="h-2 overflow-hidden rounded-full bg-[var(--color-bg)]">
        <div
          className="h-full rounded-full bg-[var(--color-primary)] transition-all"
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const bookingsQuery = useQuery({
    queryKey: ["bookings"],
    queryFn: getBookings,
  });

  const showtimesQuery = useQuery({
    queryKey: ["showtimes", "dashboard"],
    queryFn: () => getShowtimes(),
  });

  const bookings = bookingsQuery.data ?? [];
  const showtimes = showtimesQuery.data ?? [];

  const dashboard = useMemo(() => {
    const today = dayjs();

    const paidBookings = bookings.filter(
      (booking) =>
        booking.status === "PAID" || booking.status === "CHECKED_IN",
    );

    const waitingBookings = bookings.filter(
      (booking) => booking.status === "WAITING_PAYMENT",
    );

    const checkedInBookings = bookings.filter(
      (booking) => booking.status === "CHECKED_IN",
    );

    const todayShowtimes = showtimes.filter((showtime) =>
      dayjs(showtime.startTime).isSame(today, "day"),
    );

    const upcomingTodayShowtimes = todayShowtimes.filter(
      (showtime) =>
        showtime.status === "OPEN" && dayjs(showtime.startTime).isAfter(dayjs()),
    );

    const revenue = paidBookings.reduce(
      (sum, booking) => sum + booking.totalAmount,
      0,
    );

    const soldTickets = paidBookings.reduce(
      (sum, booking) => sum + getSeatCount(booking),
      0,
    );

    const movieMap = new Map<
      string,
      {
        movieTitle: string;
        bookings: number;
        tickets: number;
        revenue: number;
      }
    >();

    paidBookings.forEach((booking) => {
      const movieTitle = booking.showtime?.movie?.title ?? "Unknown Movie";
      const current = movieMap.get(movieTitle) ?? {
        movieTitle,
        bookings: 0,
        tickets: 0,
        revenue: 0,
      };

      current.bookings += 1;
      current.tickets += getSeatCount(booking);
      current.revenue += booking.totalAmount;

      movieMap.set(movieTitle, current);
    });

    const topMovies = Array.from(movieMap.values())
      .sort((a, b) => b.revenue - a.revenue)
      .slice(0, 5);

    const latestBookings = [...bookings]
      .sort((a, b) => dayjs(b.createdAt).valueOf() - dayjs(a.createdAt).valueOf())
      .slice(0, 5);

    return {
      totalBookings: bookings.length,
      waitingPayment: waitingBookings.length,
      paidBookings: paidBookings.length,
      checkedIn: checkedInBookings.length,
      revenue,
      soldTickets,
      todayShowtimes: todayShowtimes.length,
      upcomingTodayShowtimes: upcomingTodayShowtimes.length,
      topMovies,
      latestBookings,
    };
  }, [bookings, showtimes]);

  const isLoading = bookingsQuery.isLoading || showtimesQuery.isLoading;
  const isError = bookingsQuery.isError || showtimesQuery.isError;

  const errorMessage =
    bookingsQuery.isError
      ? getErrorMessage(bookingsQuery.error)
      : showtimesQuery.isError
        ? getErrorMessage(showtimesQuery.error)
        : null;

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
            Cinema Control Center
          </p>
          <h1 className="page-title mt-2 text-4xl text-white">Dashboard</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted">
            Overview of bookings, mock revenue, tickets and today&apos;s
            showtimes.
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            bookingsQuery.refetch();
            showtimesQuery.refetch();
          }}
          className="btn-dark action-button inline-flex items-center justify-center gap-2"
        >
          <RefreshCw size={17} />
          Refresh
        </button>
      </div>

      {isLoading && (
        <div className="eous-card p-8 text-center text-muted">
          Loading dashboard...
        </div>
      )}

      {isError && (
        <div className="eous-card border-[rgba(255,59,59,0.4)] p-8 text-center text-[var(--color-danger)]">
          {errorMessage}
        </div>
      )}

      {!isLoading && !isError && (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <StatCard
              title="Total Bookings"
              value={dashboard.totalBookings}
              subtitle="All booking records"
              icon={LayoutDashboard}
              tone="white"
            />

            <StatCard
              title="Mock Revenue"
              value={formatCurrency(dashboard.revenue)}
              subtitle="Paid and checked-in bookings"
              icon={TrendingUp}
              tone="yellow"
            />

            <StatCard
              title="Sold Tickets"
              value={dashboard.soldTickets}
              subtitle="Seats from paid bookings"
              icon={TicketCheck}
              tone="green"
            />

            <StatCard
              title="Today Showtimes"
              value={dashboard.todayShowtimes}
              subtitle={`${dashboard.upcomingTodayShowtimes} upcoming today`}
              icon={CalendarDays}
              tone="blue"
            />
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <StatCard
              title="Waiting Payment"
              value={dashboard.waitingPayment}
              subtitle="Bookings not paid yet"
              icon={Clock}
              tone="blue"
            />

            <StatCard
              title="Paid Bookings"
              value={dashboard.paidBookings}
              subtitle="Paid or checked-in"
              icon={CreditCard}
              tone="green"
            />

            <StatCard
              title="Checked In"
              value={dashboard.checkedIn}
              subtitle="Tickets used at cinema"
              icon={CheckCircle2}
              tone="yellow"
            />

            <StatCard
              title="Active Records"
              value={bookings.length + showtimes.length}
              subtitle="Bookings plus showtimes"
              icon={UsersRound}
              tone="white"
            />
          </div>

          <div className="grid gap-6 xl:grid-cols-[1fr_420px]">
            <section className="eous-card p-6">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <h2 className="card-title text-2xl text-white">
                    Latest Bookings
                  </h2>
                  <p className="mt-1 text-sm text-muted">
                    Most recent customer booking records.
                  </p>
                </div>

                <Timer size={26} className="text-[var(--color-primary)]" />
              </div>

              <div className="mt-6 space-y-3">
                {dashboard.latestBookings.map((booking) => (
                  <div
                    key={booking.id}
                    className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-4"
                  >
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="truncate font-bold text-white">
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
                          {getCustomerName(booking)}
                        </p>
                      </div>

                      <p className="text-sm font-semibold text-[var(--color-primary)]">
                        {formatCurrency(booking.totalAmount)}
                      </p>
                    </div>

                    <div className="mt-3 grid gap-3 md:grid-cols-3">
                      <div>
                        <p className="text-xs uppercase tracking-[0.16em] text-muted">
                          Movie
                        </p>
                        <p className="mt-1 truncate text-sm font-semibold text-white">
                          {booking.showtime?.movie?.title ?? "Unknown Movie"}
                        </p>
                      </div>

                      <div>
                        <p className="text-xs uppercase tracking-[0.16em] text-muted">
                          Showtime
                        </p>
                        <p className="mt-1 text-sm font-semibold text-white">
                          {booking.showtime?.startTime
                            ? dayjs(booking.showtime.startTime).format(
                                "DD/MM HH:mm",
                              )
                            : "N/A"}
                        </p>
                      </div>

                      <div>
                        <p className="text-xs uppercase tracking-[0.16em] text-muted">
                          Seats
                        </p>
                        <p className="mt-1 text-sm font-semibold text-white">
                          {getSeatCount(booking)} seat(s)
                        </p>
                      </div>
                    </div>
                  </div>
                ))}

                {dashboard.latestBookings.length === 0 && (
                  <div className="rounded-2xl border border-dashed border-[var(--color-border)] p-8 text-center text-muted">
                    No bookings yet.
                  </div>
                )}
              </div>
            </section>

            <section className="space-y-6">
              <div className="eous-card p-6">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <h2 className="card-title text-2xl text-white">
                      Booking Status
                    </h2>
                    <p className="mt-1 text-sm text-muted">
                      Quick status breakdown.
                    </p>
                  </div>

                  <CreditCard size={26} className="text-[var(--color-primary)]" />
                </div>

                <div className="mt-6 space-y-5">
                  <ProgressBar
                    label="Waiting Payment"
                    value={dashboard.waitingPayment}
                    total={Math.max(dashboard.totalBookings, 1)}
                  />

                  <ProgressBar
                    label="Paid"
                    value={dashboard.paidBookings}
                    total={Math.max(dashboard.totalBookings, 1)}
                  />

                  <ProgressBar
                    label="Checked In"
                    value={dashboard.checkedIn}
                    total={Math.max(dashboard.totalBookings, 1)}
                  />
                </div>
              </div>

              <div className="eous-card p-6">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <h2 className="card-title text-2xl text-white">
                      Top Movies
                    </h2>
                    <p className="mt-1 text-sm text-muted">
                      Ranked by mock revenue.
                    </p>
                  </div>

                  <Film size={26} className="text-[var(--color-primary)]" />
                </div>

                <div className="mt-6 space-y-3">
                  {dashboard.topMovies.map((movie, index) => (
                    <div
                      key={movie.movieTitle}
                      className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-4"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div className="min-w-0">
                          <div className="flex items-center gap-3">
                            <span className="brand-title flex h-8 w-8 items-center justify-center rounded-xl bg-[rgba(245,196,0,0.14)] text-[var(--color-primary)]">
                              {index + 1}
                            </span>

                            <p className="truncate font-bold text-white">
                              {movie.movieTitle}
                            </p>
                          </div>

                          <p className="mt-2 text-sm text-muted">
                            {movie.bookings} booking(s) • {movie.tickets} ticket(s)
                          </p>
                        </div>

                        <p className="text-sm font-bold text-[var(--color-primary)]">
                          {formatCurrency(movie.revenue)}
                        </p>
                      </div>
                    </div>
                  ))}

                  {dashboard.topMovies.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-[var(--color-border)] p-8 text-center text-muted">
                      No paid movie data yet.
                    </div>
                  )}
                </div>
              </div>
            </section>
          </div>

          <section className="eous-card p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h2 className="card-title text-2xl text-white">
                  Today&apos;s Showtimes
                </h2>
                <p className="mt-1 text-sm text-muted">
                  Screening schedule for today.
                </p>
              </div>

              <CalendarDays size={26} className="text-[var(--color-primary)]" />
            </div>

            <div className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              {showtimes
                .filter((showtime) => dayjs(showtime.startTime).isSame(dayjs(), "day"))
                .sort(
                  (a, b) =>
                    dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf(),
                )
                .map((showtime) => (
                  <div
                    key={showtime.id}
                    className="rounded-2xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-4"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0">
                        <p className="truncate font-bold text-white">
                          {showtime.movie?.title ?? "Unknown Movie"}
                        </p>

                        <p className="mt-1 text-sm text-muted">
                          {showtime.room?.name ?? "Unknown Room"} •{" "}
                          {showtime.room?.type ?? "Room"}
                        </p>
                      </div>

                      <span
                        className={[
                          "rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.12em]",
                          showtime.status === "OPEN"
                            ? "border-[rgba(50,213,131,0.35)] bg-[rgba(50,213,131,0.12)] text-[var(--color-success)]"
                            : "border-[rgba(255,59,59,0.35)] bg-[rgba(255,59,59,0.12)] text-[var(--color-danger)]",
                        ].join(" ")}
                      >
                        {showtime.status}
                      </span>
                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-3">
                      <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] p-3">
                        <p className="text-xs uppercase tracking-[0.16em] text-muted">
                          Time
                        </p>
                        <p className="mt-1 font-semibold text-white">
                          {dayjs(showtime.startTime).format("HH:mm")} -{" "}
                          {dayjs(showtime.endTime).format("HH:mm")}
                        </p>
                      </div>

                      <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] p-3">
                        <p className="text-xs uppercase tracking-[0.16em] text-muted">
                          Price
                        </p>
                        <p className="mt-1 font-semibold text-white">
                          {formatCurrency(showtime.basePrice)}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}

              {showtimes.filter((showtime) =>
                dayjs(showtime.startTime).isSame(dayjs(), "day"),
              ).length === 0 && (
                <div className="rounded-2xl border border-dashed border-[var(--color-border)] p-8 text-center text-muted md:col-span-2 xl:col-span-3">
                  No showtimes today.
                </div>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}