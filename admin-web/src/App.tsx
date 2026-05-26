import MoviesPage from "./features/movies/MoviesPage";
import ShowtimesPage from "./features/showtimes/ShowtimesPage";
import SeatMonitorPage from "./features/seat-monitor/SeatMonitorPage";
import BookingsPage from "./features/bookings/BookingsPage";
import TicketCheckInPage from "./features/tickets/TicketCheckInPage";
import DashboardPage from "./features/dashboard/DashboardPage";
import {
  Armchair,
  CalendarDays,
  Clapperboard,
  Film,
  LayoutDashboard,
  ListChecks,
  TicketCheck,
} from "lucide-react";
import { NavLink, Navigate, Route, Routes } from "react-router-dom";

const navItems = [
  {
    label: "Dashboard",
    path: "/admin/dashboard",
    icon: LayoutDashboard,
  },
  {
    label: "Movies",
    path: "/admin/movies",
    icon: Film,
  },
  {
    label: "Showtimes",
    path: "/admin/showtimes",
    icon: CalendarDays,
  },
  {
    label: "Seat Monitor",
    path: "/admin/seat-monitor",
    icon: Armchair,
  },
  {
    label: "Bookings",
    path: "/admin/bookings",
    icon: ListChecks,
  },
  {
    label: "Ticket Check-in",
    path: "/admin/tickets/check-in",
    icon: TicketCheck,
  },
  {
    label: "Rooms",
    path: "/admin/rooms",
    icon: Clapperboard,
  },
];

function PlaceholderPage({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
          EousX Admin Console
        </p>
        <h1 className="page-title mt-2 text-4xl text-white">{title}</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted">{description}</p>
      </div>

      <div className="eous-card p-6">
        <h2 className="card-title text-2xl text-white">Page Status</h2>
        <p className="mt-3 text-sm text-muted">
          This page is ready for the next implementation step.
        </p>

        <div className="mt-6 flex gap-3">
          <button className="btn-primary action-button">Primary Action</button>
          <button className="btn-dark action-button">View Details</button>
        </div>
      </div>
    </div>
  );
}

function AdminLayout() {
  return (
    <div className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <aside className="fixed left-0 top-0 z-20 h-screen w-72 border-r border-[var(--color-border)] bg-[rgba(21,21,28,0.96)] px-5 py-6">
        <div className="mb-8">
          <div className="brand-title text-4xl leading-none text-[var(--color-primary)]">
            EOUSX
          </div>
          <div className="brand-title mt-1 text-lg text-white">
            Admin Console
          </div>
          <p className="mt-3 text-xs text-muted">
            Cinema operations dashboard
          </p>
        </div>

        <nav className="space-y-2">
          {navItems.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  [
                    "nav-item flex items-center gap-3 rounded-xl border px-4 py-3 text-sm transition",
                    isActive
                      ? "border-[var(--color-primary)] bg-[rgba(245,196,0,0.12)] text-[var(--color-primary)]"
                      : "border-transparent text-[var(--color-muted)] hover:border-[var(--color-border)] hover:bg-[var(--color-card)] hover:text-white",
                  ].join(" ")
                }
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </aside>

      <main className="ml-72 min-h-screen">
        <header className="sticky top-0 z-10 border-b border-[var(--color-border)] bg-[rgba(11,11,15,0.86)] px-8 py-5 backdrop-blur">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="brand-title text-2xl text-white">
                Cinema Control Center
              </h2>
              <p className="text-sm text-muted">
                Manage movies, showtimes, bookings and ticket check-in.
              </p>
            </div>

            <div className="rounded-full border border-[var(--color-border)] bg-[var(--color-card)] px-4 py-2 text-sm text-muted">
              API: {import.meta.env.VITE_API_BASE_URL}
            </div>
          </div>
        </header>

        <section className="p-8">
          <Routes>
            <Route path="/" element={<Navigate to="/admin/dashboard" replace />} />
            <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />

            <Route path="/admin/dashboard" element={<DashboardPage />} />

            <Route path="/admin/movies" element={<MoviesPage />} />

            <Route path="/admin/showtimes" element={<ShowtimesPage />} />

            <Route path="/admin/seat-monitor" element={<SeatMonitorPage />} />  

            <Route path="/admin/bookings" element={<BookingsPage />} />

            <Route path="/admin/tickets/check-in" element={<TicketCheckInPage />} />

            <Route
              path="/admin/rooms"
              element={
                <PlaceholderPage
                  title="Rooms & Seats"
                  description="Read-only view for seeded rooms and seats."
                />
              }
            />
          </Routes>
        </section>
      </main>
    </div>
  );
}

export default function App() {
  return <AdminLayout />;
}