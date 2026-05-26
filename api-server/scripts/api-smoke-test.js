const fs = require('fs');
const path = require('path');

const baseUrl = (process.env.EOUSX_API_BASE_URL || 'http://localhost:3000').replace(/\/$/, '');
const suffix = Date.now();
const reportPath = path.join(process.cwd(), 'api-smoke-report.json');

const results = [];
const createdIds = {
  suffix,
  users: [],
  movies: [],
  showtimes: [],
  locks: [],
  bookings: [],
  payments: [],
  tickets: [],
};

const state = {
  user1: {
    fullName: `Smoke Test User ${suffix}`,
    email: `smoke.${suffix}@eousx.com`,
    password: '123456',
    phone: '0900000001',
    token: null,
    id: null,
  },
  user2: {
    fullName: `Smoke Other User ${suffix}`,
    email: `smoke.other.${suffix}@eousx.com`,
    password: '123456',
    phone: '0900000002',
    token: null,
    id: null,
  },
  movieId: null,
  deleteMovieId: null,
  roomId: null,
  showtimeId: null,
  cancelShowtimeId: null,
  mainSeats: [],
  lockIds: [],
  bookingId: null,
  cancelBookingId: null,
  paymentId: null,
  ticketId: null,
  qrCode: null,
  startTime: null,
};

function shortBody(response) {
  const body = response && response.data !== undefined ? response.data : response && response.text;
  const value = typeof body === 'string' ? body : JSON.stringify(body);
  return value && value.length > 700 ? `${value.slice(0, 700)}...` : value;
}

function mark(status, name, details) {
  results.push({ status, name, details: details || null });
  const icon = status === 'PASS' ? '✅ PASS' : status === 'FAIL' ? '❌ FAIL' : '⚠️ SKIP';
  console.log(`${icon} - ${name}${details ? ` - ${details}` : ''}`);
}

function pass(name) {
  mark('PASS', name);
}

function fail(name, details) {
  mark('FAIL', name, details);
}

function skip(name, reason) {
  mark('SKIP', name, reason);
}

function retry(name, reason) {
  console.log(`⚠️ RETRY - ${name}${reason ? ` - ${reason}` : ''}`);
}

async function request(method, requestPath, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }

  const init = {
    method,
    headers,
  };

  if (options.body !== undefined) {
    init.body = JSON.stringify(options.body);
  }

  try {
    const response = await fetch(`${baseUrl}${requestPath}`, init);
    const text = await response.text();
    let data = null;

    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = null;
      }
    }

    return {
      status: response.status,
      ok: response.ok,
      data,
      text,
    };
  } catch (error) {
    return {
      status: 0,
      ok: false,
      data: null,
      text: error.message,
    };
  }
}

function expectStatus(name, response, expectedStatuses) {
  const expected = Array.isArray(expectedStatuses) ? expectedStatuses : [expectedStatuses];

  if (expected.includes(response.status)) {
    pass(name);
    return true;
  }

  fail(name, `expected ${expected.join('/')} got ${response.status}; body=${shortBody(response)}`);
  return false;
}

function assert(name, condition, details) {
  if (condition) {
    pass(name);
    return true;
  }

  fail(name, details || 'assertion failed');
  return false;
}

async function test(name, fn) {
  try {
    await fn();
  } catch (error) {
    fail(name, error && error.stack ? error.stack : String(error));
  }
}

function pickFirstArray(responseOrData) {
  const data = responseOrData && responseOrData.data !== undefined ? responseOrData.data : responseOrData;

  if (Array.isArray(data)) {
    return data;
  }

  if (!data || typeof data !== 'object') {
    return [];
  }

  for (const key of ['data', 'items', 'results', 'showtimes', 'seats']) {
    if (Array.isArray(data[key])) {
      return data[key];
    }
  }

  return [];
}

function flattenSeats(seatMapResponse) {
  const data = seatMapResponse && seatMapResponse.data !== undefined ? seatMapResponse.data : seatMapResponse;

  if (Array.isArray(data)) {
    return data;
  }

  if (!data || typeof data !== 'object') {
    throw new Error('Cannot extract seats from seat map response');
  }

  if (Array.isArray(data.seats)) {
    return data.seats;
  }

  if (Array.isArray(data.rows)) {
    const seats = [];

    for (const row of data.rows) {
      if (Array.isArray(row.seats)) {
        seats.push(...row.seats);
      } else if (Array.isArray(row.items)) {
        seats.push(...row.items);
      }
    }

    if (seats.length > 0) {
      return seats;
    }
  }

  throw new Error('Cannot extract seats from seat map response');
}

function getSeatStatus(seat) {
  const status = seat && (seat.status || seat.seatStatus);

  if (!status) {
    throw new Error(`Cannot extract seat status from seat: ${JSON.stringify(seat)}`);
  }

  return status;
}

function getSeatId(seat) {
  const id = seat && (seat.id || seat.seatId);

  if (!id) {
    throw new Error(`Cannot extract seat id from seat: ${JSON.stringify(seat)}`);
  }

  return id;
}

function todayDateString(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

function futureDateIso(offsetDays, hour, minute) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  date.setHours(hour, minute, 0, 0);

  return date.toISOString();
}

function requireValue(name, value) {
  const exists = Array.isArray(value) ? value.length > 0 : Boolean(value);

  if (!exists) {
    fail(name, 'Required value is missing. Check the root cause above.');
    return false;
  }

  return true;
}

function skipIfMissing(testName, dependencies) {
  const missing = dependencies
    .filter((dependency) => {
      const value = dependency.value;
      return Array.isArray(value) ? value.length === 0 : !value;
    })
    .map((dependency) => dependency.name);

  if (missing.length > 0) {
    skip(testName, `Missing dependency: ${missing.join(', ')}`);
    return true;
  }

  return false;
}

function isConflictResponse(response) {
  const body = `${response.text || ''} ${JSON.stringify(response.data || {})}`.toLowerCase();
  return response.status === 400 && (body.includes('conflict') || body.includes('already has a showtime') || body.includes('time range'));
}

function showtimeConflicts(existingShowtime, candidateStart, candidateEnd, roomId) {
  if (!existingShowtime || existingShowtime.roomId !== roomId || existingShowtime.status === 'CANCELLED') {
    return false;
  }

  const existingStart = new Date(existingShowtime.startTime);
  const existingEnd = new Date(existingShowtime.endTime);

  if (Number.isNaN(existingStart.getTime()) || Number.isNaN(existingEnd.getTime())) {
    return false;
  }

  return existingStart < candidateEnd && existingEnd > candidateStart;
}

async function findAvailableShowtimeSlot({
  roomId,
  preferredDateOffsetDays,
  durationMinutes,
  skipStartTimes = new Set(),
}) {
  const showtimesResponse = await request('GET', '/admin/showtimes');

  if (!showtimesResponse.ok) {
    throw new Error(`Cannot load /admin/showtimes while finding slot: status=${showtimesResponse.status}; body=${shortBody(showtimesResponse)}`);
  }

  const existingShowtimes = pickFirstArray(showtimesResponse);
  const hours = [
    [8, 0],
    [10, 30],
    [13, 0],
    [15, 30],
    [18, 0],
    [20, 30],
    [23, 0],
  ];
  const minuteOffset = suffix % 17;
  const now = new Date();

  for (let dayOffset = preferredDateOffsetDays; dayOffset <= 90; dayOffset += 1) {
    for (const [hour, minute] of hours) {
      const candidateStart = new Date(now);
      candidateStart.setDate(now.getDate() + dayOffset);
      candidateStart.setHours(hour, Math.min(minute + minuteOffset, 59), 0, 0);

      if (candidateStart <= now) {
        continue;
      }

      const candidateEnd = new Date(candidateStart.getTime() + durationMinutes * 60 * 1000);
      const candidateIso = candidateStart.toISOString();

      if (skipStartTimes.has(candidateIso)) {
        continue;
      }

      const hasConflict = existingShowtimes.some((showtime) =>
        showtimeConflicts(showtime, candidateStart, candidateEnd, roomId),
      );

      if (!hasConflict) {
        return {
          startTime: candidateIso,
          endTime: candidateEnd.toISOString(),
        };
      }
    }
  }

  throw new Error(`No available showtime slot found for room ${roomId} from +${preferredDateOffsetDays} to +90 days`);
}

async function createShowtimeWithRetries(name, body, options = {}) {
  const maxAttempts = options.maxAttempts || 10;
  const preferredDateOffsetDays = options.preferredDateOffsetDays || 30;
  const durationMinutes = options.durationMinutes || 120;
  const skippedStartTimes = new Set();
  let lastResponse = null;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const slot = await findAvailableShowtimeSlot({
      roomId: body.roomId,
      preferredDateOffsetDays,
      durationMinutes,
      skipStartTimes: skippedStartTimes,
    });
    const response = await request('POST', '/admin/showtimes', {
      body: {
        ...body,
        startTime: slot.startTime,
      },
    });
    lastResponse = response;

    if ([200, 201].includes(response.status)) {
      return {
        response,
        startTime: slot.startTime,
      };
    }

    skippedStartTimes.add(slot.startTime);

    if (isConflictResponse(response) && attempt < maxAttempts) {
      retry('showtime slot conflict, trying another slot', `${name}; attempt ${attempt}/${maxAttempts}`);
      continue;
    }

    return {
      response,
      startTime: slot.startTime,
    };
  }

  fail(`${name} root cause`, `showtime conflict retry exhausted after ${maxAttempts} attempts; last body=${shortBody(lastResponse)}`);

  return {
    response: lastResponse,
    startTime: null,
  };
}

function getBookingFromResponse(data) {
  return data && data.booking ? data.booking : data;
}

function containsId(list, id) {
  return Array.isArray(list) && list.some((item) => item && item.id === id);
}

function findSeatsByIds(seats, ids) {
  return seats.filter((seat) => ids.includes(getSeatId(seat)));
}

async function registerAndLoginUser(user, label) {
  const registerResponse = await request('POST', '/auth/register', {
    body: {
      fullName: user.fullName,
      email: user.email,
      password: user.password,
      phone: user.phone,
    },
  });
  expectStatus(`${label} register status`, registerResponse, [200, 201]);
  assert(`${label} register returns user`, Boolean(registerResponse.data && registerResponse.data.user));
  assert(`${label} register email matches`, registerResponse.data && registerResponse.data.user && registerResponse.data.user.email === user.email);
  assert(`${label} register hides password`, !(registerResponse.data && registerResponse.data.user && Object.prototype.hasOwnProperty.call(registerResponse.data.user, 'password')));
  assert(`${label} register returns accessToken`, Boolean(registerResponse.data && registerResponse.data.accessToken));

  if (registerResponse.data && registerResponse.data.user) {
    user.id = registerResponse.data.user.id;
    createdIds.users.push(user.id);
  }

  const loginResponse = await request('POST', '/auth/login', {
    body: {
      email: user.email,
      password: user.password,
    },
  });
  expectStatus(`${label} login status`, loginResponse, [200, 201]);
  assert(`${label} login returns accessToken`, Boolean(loginResponse.data && loginResponse.data.accessToken));
  assert(`${label} login returns user id`, Boolean(loginResponse.data && loginResponse.data.user && loginResponse.data.user.id));
  assert(`${label} login hides password`, !(loginResponse.data && loginResponse.data.user && Object.prototype.hasOwnProperty.call(loginResponse.data.user, 'password')));

  user.token = loginResponse.data && loginResponse.data.accessToken;
  user.id = (loginResponse.data && loginResponse.data.user && loginResponse.data.user.id) || user.id;
}

async function main() {
  console.log(`EousX API smoke test`);
  console.log(`Base URL: ${baseUrl}`);
  console.log(`Suffix: ${suffix}`);
  console.log('');

  await test('D1 Health', async () => {
    const response = await request('GET', '/health');
    expectStatus('GET /health', response, 200);
    assert('health payload is ok', Boolean(response.data && (response.data.status === 'ok' || response.data.app || response.data.timestamp)));
  });

  await test('D2 Auth API', async () => {
    await registerAndLoginUser(state.user1, 'user1');

    const meResponse = await request('GET', '/auth/me', { token: state.user1.token });
    expectStatus('GET /auth/me with token', meResponse, 200);
    assert('GET /auth/me email matches', meResponse.data && meResponse.data.email === state.user1.email);

    const noTokenResponse = await request('GET', '/auth/me');
    expectStatus('GET /auth/me without token', noTokenResponse, 401);

    await registerAndLoginUser(state.user2, 'user2');
  });

  await test('D2b Google Auth API optional', async () => {
    const invalidGoogleResponse = await request('POST', '/auth/google', {
      body: {
        idToken: 'invalid-token',
      },
    });
    expectStatus(
      'POST /auth/google invalid token or missing Firebase config',
      invalidGoogleResponse,
      [400, 401, 503],
    );

    const firebaseTestIdToken = process.env.FIREBASE_TEST_ID_TOKEN;

    if (!firebaseTestIdToken) {
      skip('POST /auth/google', 'FIREBASE_TEST_ID_TOKEN is not provided');
      return;
    }

    const googleLoginResponse = await request('POST', '/auth/google', {
      body: {
        idToken: firebaseTestIdToken,
      },
    });
    expectStatus('POST /auth/google with FIREBASE_TEST_ID_TOKEN', googleLoginResponse, [200, 201]);
    assert('Google login returns accessToken', Boolean(googleLoginResponse.data && googleLoginResponse.data.accessToken));
    assert('Google login returns user', Boolean(googleLoginResponse.data && googleLoginResponse.data.user));
    assert('Google login user email exists', Boolean(googleLoginResponse.data && googleLoginResponse.data.user && googleLoginResponse.data.user.email));
    assert('Google login hides password', !(googleLoginResponse.data && googleLoginResponse.data.user && Object.prototype.hasOwnProperty.call(googleLoginResponse.data.user, 'password')));

    const googleToken = googleLoginResponse.data && googleLoginResponse.data.accessToken;

    if (!googleToken) {
      fail('required Google backend accessToken after /auth/google', `body=${shortBody(googleLoginResponse)}`);
      return;
    }

    const googleMeResponse = await request('GET', '/auth/me', {
      token: googleToken,
    });
    expectStatus('GET /auth/me with Google backend token', googleMeResponse, 200);
  });

  await test('D3 Movies API', async () => {
    expectStatus('GET /movies', await request('GET', '/movies'), 200);
    expectStatus('GET /movies/now-showing', await request('GET', '/movies/now-showing'), 200);
    expectStatus('GET /movies/upcoming', await request('GET', '/movies/upcoming'), 200);

    const movieBody = {
      title: `Smoke Test Movie ${suffix}`,
      originalTitle: `Smoke Test Movie Original ${suffix}`,
      overview: 'Created by API smoke test.',
      posterUrl: 'https://example.com/poster.jpg',
      backdropUrl: 'https://example.com/backdrop.jpg',
      trailerKey: 'smoke123',
      runtime: 120,
      genres: ['Action', 'Drama'],
      rating: 8.1,
      ageRating: 'T13',
      status: 'NOW_SHOWING',
    };

    const createResponse = await request('POST', '/admin/movies', { body: movieBody });
    expectStatus('POST /admin/movies', createResponse, [200, 201]);
    state.movieId = createResponse.data && createResponse.data.id;
    if (state.movieId) createdIds.movies.push(state.movieId);
    assert('created movie id exists', Boolean(state.movieId));

    expectStatus('GET /movies/:movieId', await request('GET', `/movies/${state.movieId}`), 200);

    const updateResponse = await request('PUT', `/admin/movies/${state.movieId}`, {
      body: {
        overview: 'Updated by API smoke test.',
        rating: 8.4,
      },
    });
    expectStatus('PUT /admin/movies/:movieId', updateResponse, 200);
    assert('movie rating updated', !updateResponse.data || updateResponse.data.rating === 8.4);

    const deleteMovieResponse = await request('POST', '/admin/movies', {
      body: {
        ...movieBody,
        title: `Smoke Delete Movie ${suffix}`,
        originalTitle: `Smoke Delete Movie Original ${suffix}`,
      },
    });
    expectStatus('POST /admin/movies delete fixture', deleteMovieResponse, [200, 201]);
    state.deleteMovieId = deleteMovieResponse.data && deleteMovieResponse.data.id;
    if (state.deleteMovieId) createdIds.movies.push(state.deleteMovieId);

    expectStatus('DELETE /admin/movies/:deleteMovieId', await request('DELETE', `/admin/movies/${state.deleteMovieId}`), [200, 204]);
    expectStatus('GET /movies/:deleteMovieId after delete', await request('GET', `/movies/${state.deleteMovieId}`), 404);
  });

  await test('D4 Rooms API', async () => {
    const roomsResponse = await request('GET', '/rooms');
    expectStatus('GET /rooms', roomsResponse, 200);
    const rooms = pickFirstArray(roomsResponse);
    assert('GET /rooms has at least one room', rooms.length > 0);

    const adminRoomsResponse = await request('GET', '/admin/rooms');
    expectStatus('GET /admin/rooms', adminRoomsResponse, 200);
    const adminRooms = pickFirstArray(adminRoomsResponse);
    assert('GET /admin/rooms has at least one room', adminRooms.length > 0);

    state.roomId = (adminRooms[0] && adminRooms[0].id) || (rooms[0] && rooms[0].id);
    assert('selected test room id exists', Boolean(state.roomId));

    expectStatus('GET /rooms/:roomId', await request('GET', `/rooms/${state.roomId}`), 200);
    expectStatus('GET /admin/rooms/:roomId', await request('GET', `/admin/rooms/${state.roomId}`), 200);
  });

  await test('D5 Showtimes API', async () => {
    expectStatus('GET /showtimes', await request('GET', '/showtimes'), 200);
    expectStatus('GET /admin/showtimes', await request('GET', '/admin/showtimes'), 200);

    if (
      skipIfMissing('D5 Showtimes API create flow', [
        { name: 'movieId', value: state.movieId },
        { name: 'roomId', value: state.roomId },
      ])
    ) {
      return;
    }

    const createdShowtime = await createShowtimeWithRetries(
      'POST /admin/showtimes',
      {
        movieId: state.movieId,
        roomId: state.roomId,
        basePrice: 90000,
        status: 'OPEN',
      },
      {
        preferredDateOffsetDays: 30,
        durationMinutes: 120,
      },
    );
    const createResponse = createdShowtime.response;
    state.startTime = createdShowtime.startTime;
    expectStatus('POST /admin/showtimes', createResponse, [200, 201]);
    state.showtimeId = createResponse.data && createResponse.data.id;
    if (state.showtimeId) createdIds.showtimes.push(state.showtimeId);
    requireValue('required showtimeId after POST /admin/showtimes', state.showtimeId);
    assert('created showtime movieId matches', createResponse.data && createResponse.data.movieId === state.movieId);
    assert('created showtime roomId matches', createResponse.data && createResponse.data.roomId === state.roomId);
    assert('created showtime has time fields', Boolean(createResponse.data && (createResponse.data.endTime || createResponse.data.startTime)));
    assert('created showtime status OPEN', createResponse.data && createResponse.data.status === 'OPEN');

    if (!state.showtimeId) {
      return;
    }

    expectStatus('GET /showtimes/:showtimeId', await request('GET', `/showtimes/${state.showtimeId}`), 200);

    const movieShowtimesResponse = await request('GET', `/movies/${state.movieId}/showtimes`);
    expectStatus('GET /movies/:movieId/showtimes', movieShowtimesResponse, 200);
    const movieShowtimes = pickFirstArray(movieShowtimesResponse);
    assert('movie showtimes contains created showtime', containsId(movieShowtimes, state.showtimeId));

    const date = todayDateString(new Date(state.startTime));
    const datedResponse = await request('GET', `/movies/${state.movieId}/showtimes?date=${date}`);
    expectStatus('GET /movies/:movieId/showtimes?date=YYYY-MM-DD', datedResponse, 200);

    const updateResponse = await request('PUT', `/admin/showtimes/${state.showtimeId}`, {
      body: {
        basePrice: 95000,
      },
    });
    expectStatus('PUT /admin/showtimes/:showtimeId', updateResponse, 200);
    assert('showtime basePrice updated when returned', !updateResponse.data || updateResponse.data.basePrice === 95000);

    const conflictResponse = await request('POST', '/admin/showtimes', {
      body: {
        movieId: state.movieId,
        roomId: state.roomId,
        startTime: state.startTime,
        basePrice: 90000,
        status: 'OPEN',
      },
    });
    expectStatus('POST /admin/showtimes conflict same room/time', conflictResponse, 400);

    const cancelShowtime = await createShowtimeWithRetries(
      'POST /admin/showtimes cancel fixture',
      {
        movieId: state.movieId,
        roomId: state.roomId,
        basePrice: 90000,
        status: 'OPEN',
      },
      {
        preferredDateOffsetDays: 31,
        durationMinutes: 120,
      },
    );
    const cancelCreateResponse = cancelShowtime.response;
    expectStatus('POST /admin/showtimes cancel fixture', cancelCreateResponse, [200, 201]);
    state.cancelShowtimeId = cancelCreateResponse.data && cancelCreateResponse.data.id;
    if (state.cancelShowtimeId) createdIds.showtimes.push(state.cancelShowtimeId);

    if (!state.cancelShowtimeId) {
      fail('required cancelShowtimeId after POST /admin/showtimes cancel fixture', `body=${shortBody(cancelCreateResponse)}`);
      return;
    }

    const cancelResponse = await request('DELETE', `/admin/showtimes/${state.cancelShowtimeId}`);
    expectStatus('DELETE /admin/showtimes/:cancelShowtimeId', cancelResponse, 200);
    assert('cancelled showtime status CANCELLED if returned', !cancelResponse.data || cancelResponse.data.status === 'CANCELLED');
  });

  await test('D6 Seat Map API', async () => {
    if (skipIfMissing('D6 Seat Map API', [{ name: 'showtimeId', value: state.showtimeId }])) {
      return;
    }

    const seatMapResponse = await request('GET', `/showtimes/${state.showtimeId}/seats`);
    expectStatus('GET /showtimes/:showtimeId/seats', seatMapResponse, 200);
    const seats = flattenSeats(seatMapResponse);
    assert('seat map has seats', seats.length > 0);
    const available = seats.filter((seat) => getSeatStatus(seat) === 'AVAILABLE');
    assert('seat map has at least 2 AVAILABLE seats', available.length >= 2);
    state.mainSeats = available.slice(0, 2);

    const adminSeatMapResponse = await request('GET', `/admin/showtimes/${state.showtimeId}/seats`);
    expectStatus('GET /admin/showtimes/:showtimeId/seats', adminSeatMapResponse, 200);
    assert('admin seat map response is valid', flattenSeats(adminSeatMapResponse).length > 0);
  });

  await test('D7 Seat Lock API', async () => {
    if (
      skipIfMissing('D7 Seat Lock API', [
        { name: 'showtimeId', value: state.showtimeId },
        { name: 'mainSeats', value: state.mainSeats },
      ])
    ) {
      return;
    }

    const mainSeatIds = state.mainSeats.map(getSeatId);

    if (mainSeatIds.length < 2) {
      skip('POST /showtimes/:showtimeId/seat-locks user2 same seats', 'Need 2 mainSeatIds before lock tests');
      return;
    }

    const noTokenResponse = await request('POST', `/showtimes/${state.showtimeId}/seat-locks`, {
      body: {
        seatIds: mainSeatIds,
      },
    });
    expectStatus('POST /showtimes/:showtimeId/seat-locks without token', noTokenResponse, 401);

    const lockResponse = await request('POST', `/showtimes/${state.showtimeId}/seat-locks`, {
      token: state.user1.token,
      body: {
        seatIds: mainSeatIds,
      },
    });
    expectStatus('POST /showtimes/:showtimeId/seat-locks with user1 token', lockResponse, [200, 201]);
    assert('seat lock response has locks', Boolean(lockResponse.data && Array.isArray(lockResponse.data.locks)));
    assert('seat lock locks length = 2', lockResponse.data && lockResponse.data.locks && lockResponse.data.locks.length === 2);
    assert('seat lock response has lockedUntil', Boolean(lockResponse.data && lockResponse.data.lockedUntil));
    assert('seat lock duration is 3 minutes if returned', !lockResponse.data || lockResponse.data.lockDurationMinutes === 3 || lockResponse.data.lockDurationSeconds === 180);
    state.lockIds = lockResponse.data && lockResponse.data.locks ? lockResponse.data.locks.map((lock) => lock.id) : [];
    createdIds.locks.push(...state.lockIds);

    if (state.lockIds.length < 2) {
      fail('required lockIds after user1 seat lock', `body=${shortBody(lockResponse)}`);
      skip('POST /showtimes/:showtimeId/seat-locks user2 same seats', 'User1 lock failed or returned fewer than 2 lockIds');
      skip('GET /showtimes/:showtimeId/seats after lock', 'User1 lock failed or returned fewer than 2 lockIds');
      return;
    }

    expectStatus('GET /seat-locks/:lockId', await request('GET', `/seat-locks/${state.lockIds[0]}`), 200);

    const user2LockResponse = await request('POST', `/showtimes/${state.showtimeId}/seat-locks`, {
      token: state.user2.token,
      body: {
        seatIds: mainSeatIds,
      },
    });
    expectStatus('POST /showtimes/:showtimeId/seat-locks user2 same seats', user2LockResponse, 400);

    const afterLockMapResponse = await request('GET', `/showtimes/${state.showtimeId}/seats`);
    expectStatus('GET /showtimes/:showtimeId/seats after lock', afterLockMapResponse, 200);
    const seats = flattenSeats(afterLockMapResponse);
    const lockedSeats = findSeatsByIds(seats, mainSeatIds);
    assert('main seats show LOCKED after lock', lockedSeats.length === 2 && lockedSeats.every((seat) => getSeatStatus(seat) === 'LOCKED'));
  });

  await test('D8 Bookings API', async () => {
    if (
      skipIfMissing('D8 Bookings API', [
        { name: 'showtimeId', value: state.showtimeId },
        { name: 'lockIds', value: state.lockIds },
      ])
    ) {
      return;
    }

    const noTokenResponse = await request('POST', '/bookings', {
      body: {
        showtimeId: state.showtimeId,
        lockIds: state.lockIds,
      },
    });
    expectStatus('POST /bookings without token', noTokenResponse, 401);

    const createResponse = await request('POST', '/bookings', {
      token: state.user1.token,
      body: {
        showtimeId: state.showtimeId,
        lockIds: state.lockIds,
      },
    });
    expectStatus('POST /bookings with user1 token', createResponse, [200, 201]);
    const booking = getBookingFromResponse(createResponse.data);
    state.bookingId = booking && booking.id;
    if (state.bookingId) createdIds.bookings.push(state.bookingId);
    assert('booking exists', Boolean(booking && booking.id));
    assert('booking status WAITING_PAYMENT', booking && booking.status === 'WAITING_PAYMENT');
    assert('booking belongs to user1 if returned', !booking || !booking.user || booking.user.email === state.user1.email || booking.userId === state.user1.id);
    assert('booking has at least 2 seats', booking && Array.isArray(booking.seats) && booking.seats.length >= 2);
    assert('booking totalAmount > 0', booking && booking.totalAmount > 0);

    if (!state.bookingId) {
      fail('required bookingId after POST /bookings', `body=${shortBody(createResponse)}`);
      return;
    }

    const myBookingsResponse = await request('GET', '/bookings/me', { token: state.user1.token });
    expectStatus('GET /bookings/me with user1 token', myBookingsResponse, 200);
    assert('GET /bookings/me contains booking', containsId(pickFirstArray(myBookingsResponse), state.bookingId));

    expectStatus('GET /bookings/me without token', await request('GET', '/bookings/me'), 401);
    expectStatus('GET /bookings/:bookingId with user1 token', await request('GET', `/bookings/${state.bookingId}`, { token: state.user1.token }), 200);
    expectStatus('GET /bookings/:bookingId with user2 token', await request('GET', `/bookings/${state.bookingId}`, { token: state.user2.token }), [403, 404]);

    const adminBookingsResponse = await request('GET', '/admin/bookings');
    expectStatus('GET /admin/bookings', adminBookingsResponse, 200);
    assert('GET /admin/bookings contains booking', containsId(pickFirstArray(adminBookingsResponse), state.bookingId));
    expectStatus('GET /admin/bookings/:bookingId', await request('GET', `/admin/bookings/${state.bookingId}`), 200);

    const seatMapResponse = await request('GET', `/showtimes/${state.showtimeId}/seats`);
    const available = flattenSeats(seatMapResponse).filter((seat) => getSeatStatus(seat) === 'AVAILABLE');

    if (available.length < 1) {
      skip('cancel booking route fixture', 'No AVAILABLE seat left for cancel booking fixture');
      return;
    }

    const cancelSeatId = getSeatId(available[0]);
    const cancelLockResponse = await request('POST', `/showtimes/${state.showtimeId}/seat-locks`, {
      token: state.user1.token,
      body: {
        seatIds: [cancelSeatId],
      },
    });
    expectStatus('POST /showtimes/:showtimeId/seat-locks cancel fixture', cancelLockResponse, [200, 201]);
    const cancelLockIds = cancelLockResponse.data && cancelLockResponse.data.locks ? cancelLockResponse.data.locks.map((lock) => lock.id) : [];
    createdIds.locks.push(...cancelLockIds);

    const cancelBookingResponse = await request('POST', '/bookings', {
      token: state.user1.token,
      body: {
        showtimeId: state.showtimeId,
        lockIds: cancelLockIds,
      },
    });
    expectStatus('POST /bookings cancel fixture', cancelBookingResponse, [200, 201]);
    const cancelBooking = getBookingFromResponse(cancelBookingResponse.data);
    state.cancelBookingId = cancelBooking && cancelBooking.id;
    if (state.cancelBookingId) createdIds.bookings.push(state.cancelBookingId);

    const cancelResponse = await request('PATCH', `/bookings/${state.cancelBookingId}/cancel`, {
      token: state.user1.token,
    });
    expectStatus('PATCH /bookings/:cancelBookingId/cancel', cancelResponse, 200);
    const cancelled = getBookingFromResponse(cancelResponse.data);
    assert('cancel booking status CANCELLED if returned', !cancelled || cancelled.status === 'CANCELLED');
  });

  await test('D9 Payments API', async () => {
    if (
      skipIfMissing('D9 Payments API', [
        { name: 'showtimeId', value: state.showtimeId },
        { name: 'bookingId', value: state.bookingId },
        { name: 'mainSeats', value: state.mainSeats },
      ])
    ) {
      return;
    }

    const noTokenResponse = await request('POST', '/payments/mock-success', {
      body: {
        bookingId: state.bookingId,
        provider: 'MOCK',
      },
    });
    expectStatus('POST /payments/mock-success without token', noTokenResponse, 401);

    const user2Response = await request('POST', '/payments/mock-success', {
      token: state.user2.token,
      body: {
        bookingId: state.bookingId,
        provider: 'MOCK',
      },
    });
    expectStatus('POST /payments/mock-success with user2 token', user2Response, [403, 404]);

    const payResponse = await request('POST', '/payments/mock-success', {
      token: state.user1.token,
      body: {
        bookingId: state.bookingId,
        provider: 'MOCK',
      },
    });
    expectStatus('POST /payments/mock-success with user1 token', payResponse, [200, 201]);
    assert('paid booking status PAID', payResponse.data && payResponse.data.booking && payResponse.data.booking.status === 'PAID');
    assert('payment status SUCCESS', payResponse.data && payResponse.data.payment && payResponse.data.payment.status === 'SUCCESS');
    assert('ticket exists after payment', Boolean(payResponse.data && payResponse.data.ticket));
    assert('ticket qrCode exists after payment', Boolean(payResponse.data && payResponse.data.ticket && payResponse.data.ticket.qrCode));
    state.paymentId = payResponse.data && payResponse.data.payment && payResponse.data.payment.id;
    state.ticketId = payResponse.data && payResponse.data.ticket && payResponse.data.ticket.id;
    state.qrCode = payResponse.data && payResponse.data.ticket && payResponse.data.ticket.qrCode;
    if (state.paymentId) createdIds.payments.push(state.paymentId);
    if (state.ticketId) createdIds.tickets.push(state.ticketId);

    if (!state.paymentId || !state.ticketId || !state.qrCode) {
      fail('required paymentId/ticketId/qrCode after mock payment', `body=${shortBody(payResponse)}`);
      return;
    }

    expectStatus('GET /payments/:paymentId/status', await request('GET', `/payments/${state.paymentId}/status`, { token: state.user1.token }), 200);
    expectStatus('GET /bookings/:bookingId/payment', await request('GET', `/bookings/${state.bookingId}/payment`, { token: state.user1.token }), 200);

    const afterPaymentMapResponse = await request('GET', `/showtimes/${state.showtimeId}/seats`);
    expectStatus('GET /showtimes/:showtimeId/seats after payment', afterPaymentMapResponse, 200);
    const soldSeats = findSeatsByIds(flattenSeats(afterPaymentMapResponse), state.mainSeats.map(getSeatId));
    assert('main seats show SOLD after payment', soldSeats.length === 2 && soldSeats.every((seat) => getSeatStatus(seat) === 'SOLD'));
  });

  await test('D10 Tickets API', async () => {
    if (
      skipIfMissing('D10 Tickets API', [
        { name: 'bookingId', value: state.bookingId },
        { name: 'ticketId', value: state.ticketId },
        { name: 'qrCode', value: state.qrCode },
      ])
    ) {
      return;
    }

    expectStatus('GET /bookings/:bookingId/ticket without token', await request('GET', `/bookings/${state.bookingId}/ticket`), 401);
    expectStatus('GET /bookings/:bookingId/ticket with user2 token', await request('GET', `/bookings/${state.bookingId}/ticket`, { token: state.user2.token }), [403, 404]);

    const ticketResponse = await request('GET', `/bookings/${state.bookingId}/ticket`, {
      token: state.user1.token,
    });
    expectStatus('GET /bookings/:bookingId/ticket with user1 token', ticketResponse, 200);
    assert('ticket id or bookingId matches', ticketResponse.data && (ticketResponse.data.id === state.ticketId || ticketResponse.data.bookingId === state.bookingId));
    assert('ticket qrCode exists', Boolean(ticketResponse.data && ticketResponse.data.qrCode));
    assert('ticket status VALID', ticketResponse.data && ticketResponse.data.status === 'VALID');

    const verifyResponse = await request('GET', `/tickets/verify/${encodeURIComponent(state.qrCode)}`);
    expectStatus('GET /tickets/verify/:qrCode', verifyResponse, 200);
    assert('verify ticket status VALID', verifyResponse.data && verifyResponse.data.status === 'VALID');

    const checkInResponse = await request('POST', `/tickets/${state.ticketId}/check-in`);
    expectStatus('POST /tickets/:ticketId/check-in', checkInResponse, [200, 201]);
    assert('check-in ticket status USED', checkInResponse.data && checkInResponse.data.ticket && checkInResponse.data.ticket.status === 'USED');
    assert('check-in usedAt exists if returned', !checkInResponse.data || !checkInResponse.data.ticket || Boolean(checkInResponse.data.ticket.usedAt || checkInResponse.data.ticket.checkedInAt));

    const verifyAgainResponse = await request('GET', `/tickets/verify/${encodeURIComponent(state.qrCode)}`);
    expectStatus('GET /tickets/verify/:qrCode after check-in', verifyAgainResponse, 200);
    assert('verify after check-in is already used or used', verifyAgainResponse.data && (verifyAgainResponse.data.status === 'ALREADY_USED' || verifyAgainResponse.data.status === 'USED'));

    const adminBookingResponse = await request('GET', `/admin/bookings/${state.bookingId}`);
    expectStatus('GET /admin/bookings/:bookingId after check-in', adminBookingResponse, 200);
    assert('admin booking status CHECKED_IN if returned', !adminBookingResponse.data || adminBookingResponse.data.status === 'CHECKED_IN');
  });

  await test('D11 Dashboard/Admin support endpoints', async () => {
    if (
      skipIfMissing('D11 Dashboard/Admin support endpoints', [
        { name: 'bookingId', value: state.bookingId },
        { name: 'showtimeId', value: state.showtimeId },
      ])
    ) {
      return;
    }

    const adminBookingsResponse = await request('GET', '/admin/bookings');
    expectStatus('GET /admin/bookings dashboard support', adminBookingsResponse, 200);

    const adminShowtimesResponse = await request('GET', '/admin/showtimes');
    expectStatus('GET /admin/showtimes dashboard support', adminShowtimesResponse, 200);

    const adminBookings = pickFirstArray(adminBookingsResponse);
    const adminShowtimes = pickFirstArray(adminShowtimesResponse);
    assert('admin bookings has checked-in booking', adminBookings.some((booking) => booking.id === state.bookingId && booking.status === 'CHECKED_IN'));
    assert('admin showtimes has smoke showtime', containsId(adminShowtimes, state.showtimeId));
  });

  const passed = results.filter((result) => result.status === 'PASS').length;
  const failed = results.filter((result) => result.status === 'FAIL').length;
  const skipped = results.filter((result) => result.status === 'SKIP').length;
  const finalVerdict = failed === 0 ? 'STABLE' : 'UNSTABLE';

  const jsonReport = {
    timestamp: new Date().toISOString(),
    baseUrl,
    passed,
    failed,
    skipped,
    createdIds,
    results,
  };

  fs.writeFileSync(reportPath, `${JSON.stringify(jsonReport, null, 2)}\n`);

  console.log('');
  console.log('Summary');
  console.log(`Total: ${results.length}`);
  console.log(`Passed: ${passed}`);
  console.log(`Failed: ${failed}`);
  console.log(`Skipped: ${skipped}`);
  console.log(`Final verdict: ${finalVerdict}`);
  console.log(`JSON report: ${reportPath}`);

  process.exit(failed > 0 ? 1 : 0);
}

main().catch((error) => {
  fail('Unhandled smoke test error', error && error.stack ? error.stack : String(error));
  const passed = results.filter((result) => result.status === 'PASS').length;
  const failed = results.filter((result) => result.status === 'FAIL').length;
  const skipped = results.filter((result) => result.status === 'SKIP').length;

  fs.writeFileSync(
    reportPath,
    `${JSON.stringify(
      {
        timestamp: new Date().toISOString(),
        baseUrl,
        passed,
        failed,
        skipped,
        createdIds,
        results,
      },
      null,
      2,
    )}\n`,
  );

  console.log('');
  console.log('Summary');
  console.log(`Total: ${results.length}`);
  console.log(`Passed: ${passed}`);
  console.log(`Failed: ${failed}`);
  console.log(`Skipped: ${skipped}`);
  console.log('Final verdict: UNSTABLE');
  console.log(`JSON report: ${reportPath}`);
  process.exit(1);
});
