import axios from "axios";
import { Edit3, Film, Plus, RefreshCw, Search, Trash2, X } from "lucide-react";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createMovie,
  deleteMovie,
  getMovies,
  updateMovie,
} from "./moviesApi";
import type { Movie, MoviePayload, MovieStatus } from "../../types/movie";

type MovieFormState = {
  title: string;
  originalTitle: string;
  overview: string;
  posterUrl: string;
  backdropUrl: string;
  trailerKey: string;
  runtime: string;
  genres: string;
  rating: string;
  ageRating: string;
  status: MovieStatus;
};

const emptyForm: MovieFormState = {
  title: "",
  originalTitle: "",
  overview: "",
  posterUrl: "",
  backdropUrl: "",
  trailerKey: "",
  runtime: "120",
  genres: "Action, Drama",
  rating: "7.5",
  ageRating: "T13",
  status: "NOW_SHOWING",
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

function toPayload(form: MovieFormState): MoviePayload {
  const genres = form.genres
    .split(",")
    .map((genre) => genre.trim())
    .filter(Boolean);

  return {
    title: form.title.trim(),
    originalTitle: form.originalTitle.trim() || undefined,
    overview: form.overview.trim(),
    posterUrl: form.posterUrl.trim() || undefined,
    backdropUrl: form.backdropUrl.trim() || undefined,
    trailerKey: form.trailerKey.trim() || undefined,
    runtime: Number(form.runtime),
    genres,
    rating: Number(form.rating),
    ageRating: form.ageRating.trim(),
    status: form.status,
  };
}

function getStatusLabel(status: MovieStatus) {
  if (status === "NOW_SHOWING") return "Now Showing";
  return "Coming Soon";
}

function getStatusClass(status: MovieStatus) {
  if (status === "NOW_SHOWING") {
    return "border-[rgba(50,213,131,0.35)] bg-[rgba(50,213,131,0.12)] text-[var(--color-success)]";
  }

  return "border-[rgba(245,196,0,0.35)] bg-[rgba(245,196,0,0.12)] text-[var(--color-primary)]";
}

export default function MoviesPage() {
  const queryClient = useQueryClient();

  const [form, setForm] = useState<MovieFormState>(emptyForm);
  const [editingMovie, setEditingMovie] = useState<Movie | null>(null);
  const [statusFilter, setStatusFilter] = useState<"ALL" | MovieStatus>("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const moviesQuery = useQuery({
    queryKey: ["movies"],
    queryFn: getMovies,
  });

  const createMutation = useMutation({
    mutationFn: createMovie,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["movies"] });
      resetForm();
    },
    onError: (error) => {
      setFormError(getErrorMessage(error));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: MoviePayload }) =>
      updateMovie(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["movies"] });
      resetForm();
    },
    onError: (error) => {
      setFormError(getErrorMessage(error));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteMovie,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["movies"] });
    },
  });

  const filteredMovies = useMemo(() => {
    const movies = moviesQuery.data ?? [];
    const keyword = searchTerm.trim().toLowerCase();

    return movies.filter((movie) => {
      const matchStatus =
        statusFilter === "ALL" ? true : movie.status === statusFilter;

      const matchSearch =
        !keyword ||
        movie.title.toLowerCase().includes(keyword) ||
        movie.originalTitle?.toLowerCase().includes(keyword) ||
        movie.genres.join(" ").toLowerCase().includes(keyword);

      return matchStatus && matchSearch;
    });
  }, [moviesQuery.data, searchTerm, statusFilter]);

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  function resetForm() {
    setForm(emptyForm);
    setEditingMovie(null);
    setFormError(null);
  }

  function handleEdit(movie: Movie) {
    setEditingMovie(movie);
    setForm({
      title: movie.title ?? "",
      originalTitle: movie.originalTitle ?? "",
      overview: movie.overview ?? "",
      posterUrl: movie.posterUrl ?? "",
      backdropUrl: movie.backdropUrl ?? "",
      trailerKey: movie.trailerKey ?? "",
      runtime: String(movie.runtime ?? 120),
      genres: movie.genres?.join(", ") ?? "",
      rating: String(movie.rating ?? 0),
      ageRating: movie.ageRating ?? "T13",
      status: movie.status,
    });
    setFormError(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function validateForm() {
    if (!form.title.trim()) return "Title is required";
    if (!form.overview.trim()) return "Overview is required";
    if (!form.ageRating.trim()) return "Age rating is required";

    const runtime = Number(form.runtime);
    if (!Number.isFinite(runtime) || runtime <= 0) {
      return "Runtime must be greater than 0";
    }

    const rating = Number(form.rating);
    if (!Number.isFinite(rating) || rating < 0 || rating > 10) {
      return "Rating must be between 0 and 10";
    }

    const genres = form.genres
      .split(",")
      .map((genre) => genre.trim())
      .filter(Boolean);

    if (genres.length === 0) return "At least one genre is required";

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

    if (editingMovie) {
      updateMutation.mutate({
        id: editingMovie.id,
        payload,
      });
      return;
    }

    createMutation.mutate(payload);
  }

  function handleDelete(movie: Movie) {
    const confirmed = window.confirm(
      `Delete "${movie.title}"? This action cannot be undone.`,
    );

    if (!confirmed) return;

    deleteMutation.mutate(movie.id);
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--color-primary)]">
            Catalog Control
          </p>
          <h1 className="page-title mt-2 text-4xl text-white">
            Movie Management
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-muted">
            Add, update and manage movies for EousX Cinema.
          </p>
        </div>

        <button
          type="button"
          onClick={() => moviesQuery.refetch()}
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
                {editingMovie ? "Update Movie" : "Add New Movie"}
              </h2>
              <p className="mt-1 text-sm text-muted">
                Use English labels to keep the admin UI clean.
              </p>
            </div>

            {editingMovie && (
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
                Title
              </label>
              <input
                value={form.title}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    title: event.target.value,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                placeholder="Inside Out 2"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Original Title
              </label>
              <input
                value={form.originalTitle}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    originalTitle: event.target.value,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                placeholder="Inside Out 2"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Overview
              </label>
              <textarea
                value={form.overview}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    overview: event.target.value,
                  }))
                }
                rows={4}
                className="w-full resize-none rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                placeholder="Movie description..."
              />
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Runtime
                </label>
                <input
                  type="number"
                  min="1"
                  value={form.runtime}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      runtime: event.target.value,
                    }))
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Rating
                </label>
                <input
                  type="number"
                  min="0"
                  max="10"
                  step="0.1"
                  value={form.rating}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      rating: event.target.value,
                    }))
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                />
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                  Age Rating
                </label>
                <input
                  value={form.ageRating}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      ageRating: event.target.value,
                    }))
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                  placeholder="T13"
                />
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
                      status: event.target.value as MovieStatus,
                    }))
                  }
                  className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                >
                  <option value="NOW_SHOWING">Now Showing</option>
                  <option value="UPCOMING">Coming Soon</option>
                </select>
              </div>
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Genres
              </label>
              <input
                value={form.genres}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    genres: event.target.value,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                placeholder="Action, Drama, Comedy"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Poster URL
              </label>
              <input
                value={form.posterUrl}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    posterUrl: event.target.value,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                placeholder="https://image.tmdb.org/..."
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Backdrop URL
              </label>
              <input
                value={form.backdropUrl}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    backdropUrl: event.target.value,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                placeholder="https://image.tmdb.org/..."
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold uppercase tracking-[0.18em] text-muted">
                Trailer Key
              </label>
              <input
                value={form.trailerKey}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    trailerKey: event.target.value,
                  }))
                }
                className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                placeholder="YouTube video key"
              />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="btn-primary action-button flex w-full items-center justify-center gap-2 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Plus size={18} />
              {isSubmitting
                ? "Saving..."
                : editingMovie
                  ? "Update Movie"
                  : "Add Movie"}
            </button>
          </form>
        </section>

        <section className="space-y-5">
          <div className="eous-card p-5">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 className="card-title text-2xl text-white">Movie List</h2>
                <p className="mt-1 text-sm text-muted">
                  {filteredMovies.length} movie(s) found
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row">
                <div className="relative">
                  <Search
                    size={17}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                  />
                  <input
                    value={searchTerm}
                    onChange={(event) => setSearchTerm(event.target.value)}
                    className="w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] py-3 pl-10 pr-4 text-sm text-white outline-none transition focus:border-[var(--color-primary)] sm:w-72"
                    placeholder="Search movies..."
                  />
                </div>

                <select
                  value={statusFilter}
                  onChange={(event) =>
                    setStatusFilter(event.target.value as "ALL" | MovieStatus)
                  }
                  className="rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-4 py-3 text-sm text-white outline-none transition focus:border-[var(--color-primary)]"
                >
                  <option value="ALL">All Status</option>
                  <option value="NOW_SHOWING">Now Showing</option>
                  <option value="UPCOMING">Coming Soon</option>
                </select>
              </div>
            </div>
          </div>

          {moviesQuery.isLoading && (
            <div className="eous-card p-8 text-center text-muted">
              Loading movies...
            </div>
          )}

          {moviesQuery.isError && (
            <div className="eous-card border-[rgba(255,59,59,0.4)] p-8 text-center text-[var(--color-danger)]">
              {getErrorMessage(moviesQuery.error)}
            </div>
          )}

          {!moviesQuery.isLoading && !moviesQuery.isError && (
            <div className="grid gap-4">
              {filteredMovies.map((movie) => (
                <article key={movie.id} className="eous-card overflow-hidden">
                  <div className="grid gap-0 md:grid-cols-[150px_1fr]">
                    <div className="h-64 bg-[var(--color-panel)] md:h-full">
                      {movie.posterUrl ? (
                        <img
                          src={movie.posterUrl}
                          alt={movie.title}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <div className="flex h-full items-center justify-center text-muted">
                          <Film size={42} />
                        </div>
                      )}
                    </div>

                    <div className="p-5">
                      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <h3 className="card-title text-2xl text-white">
                              {movie.title}
                            </h3>
                            <span
                              className={[
                                "rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-[0.12em]",
                                getStatusClass(movie.status),
                              ].join(" ")}
                            >
                              {getStatusLabel(movie.status)}
                            </span>
                          </div>

                          <p className="mt-1 text-sm text-muted">
                            {movie.originalTitle || "No original title"}
                          </p>
                        </div>

                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => handleEdit(movie)}
                            className="btn-dark action-button inline-flex items-center gap-2"
                          >
                            <Edit3 size={16} />
                            Edit
                          </button>

                          <button
                            type="button"
                            onClick={() => handleDelete(movie)}
                            disabled={deleteMutation.isPending}
                            className="action-button inline-flex items-center gap-2 rounded-[10px] bg-[var(--color-danger)] px-[18px] py-[10px] font-bold uppercase tracking-[0.05em] text-white transition hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            <Trash2 size={16} />
                            Delete
                          </button>
                        </div>
                      </div>

                      <p className="mt-4 line-clamp-3 text-sm leading-6 text-muted">
                        {movie.overview}
                      </p>

                      <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                        <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                          <p className="text-xs uppercase tracking-[0.16em] text-muted">
                            Runtime
                          </p>
                          <p className="mt-1 font-semibold text-white">
                            {movie.runtime} min
                          </p>
                        </div>

                        <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                          <p className="text-xs uppercase tracking-[0.16em] text-muted">
                            Rating
                          </p>
                          <p className="mt-1 font-semibold text-white">
                            {movie.rating}/10
                          </p>
                        </div>

                        <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                          <p className="text-xs uppercase tracking-[0.16em] text-muted">
                            Age Rating
                          </p>
                          <p className="mt-1 font-semibold text-white">
                            {movie.ageRating}
                          </p>
                        </div>

                        <div className="rounded-xl border border-[var(--color-border)] bg-[rgba(11,11,15,0.5)] p-3">
                          <p className="text-xs uppercase tracking-[0.16em] text-muted">
                            Trailer
                          </p>
                          <p className="mt-1 truncate font-semibold text-white">
                            {movie.trailerKey || "N/A"}
                          </p>
                        </div>
                      </div>

                      <div className="mt-4 flex flex-wrap gap-2">
                        {movie.genres.map((genre) => (
                          <span
                            key={`${movie.id}-${genre}`}
                            className="rounded-full border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-1 text-xs text-muted"
                          >
                            {genre}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                </article>
              ))}

              {filteredMovies.length === 0 && (
                <div className="eous-card p-8 text-center text-muted">
                  No movies found.
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}