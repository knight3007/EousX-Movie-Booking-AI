import { PrismaClient, MovieStatus, RoomType, SeatType, BookingStatus, PaymentStatus, TicketStatus, SeatLockStatus, AdminRole } from '@prisma/client';
import * as bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

function addMinutes(date: Date, minutes: number): Date {
  return new Date(date.getTime() + minutes * 60 * 1000);
}

function createShowtimeDate(hour: number, minute: number): Date {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  date.setHours(hour, minute, 0, 0);
  return date;
}

async function createSeats(roomId: string, rows: number, columns: number, vipRows: string[]) {
  const data = [];

  for (let rowIndex = 0; rowIndex < rows; rowIndex++) {
    const row = String.fromCharCode(65 + rowIndex);

    for (let number = 1; number <= columns; number++) {
      const code = `${row}${number}`;
      const type = vipRows.includes(row) ? SeatType.VIP : SeatType.STANDARD;

      data.push({
        roomId,
        row,
        number,
        code,
        type,
        isActive: true
      });
    }
  }

  await prisma.seat.createMany({
    data,
    skipDuplicates: true
  });
}

async function main() {
  console.log('Start seeding EousX database...');

  await prisma.ticket.deleteMany();
  await prisma.payment.deleteMany();
  await prisma.bookingSeat.deleteMany();
  await prisma.booking.deleteMany();
  await prisma.seatLock.deleteMany();
  await prisma.seat.deleteMany();
  await prisma.showtime.deleteMany();
  await prisma.movie.deleteMany();
  await prisma.room.deleteMany();
  await prisma.cinema.deleteMany();
  await prisma.admin.deleteMany();
  await prisma.user.deleteMany();

  const cinema = await prisma.cinema.create({
    data: {
      name: 'EousX Cinema',
      address: 'Khu đô thị ĐHQG, TP. Thủ Đức, TP.HCM',
      phone: '0900000000'
    }
  });

  const room1 = await prisma.room.create({
    data: {
      cinemaId: cinema.id,
      name: 'Room 1 - Standard 2D',
      type: RoomType.STANDARD_2D,
      rows: 5,
      columns: 8
    }
  });

  const room2 = await prisma.room.create({
    data: {
      cinemaId: cinema.id,
      name: 'Room 2 - Standard 2D',
      type: RoomType.STANDARD_2D,
      rows: 6,
      columns: 10
    }
  });

  const roomVip = await prisma.room.create({
    data: {
      cinemaId: cinema.id,
      name: 'Room VIP',
      type: RoomType.VIP,
      rows: 4,
      columns: 6
    }
  });

  await createSeats(room1.id, 5, 8, ['D', 'E']);
  await createSeats(room2.id, 6, 10, ['E', 'F']);
  await createSeats(roomVip.id, 4, 6, ['A', 'B', 'C', 'D']);

  const insideOut2 = await prisma.movie.create({
    data: {
      title: 'Inside Out 2',
      originalTitle: 'Inside Out 2',
      overview: 'Riley bước vào tuổi thiếu niên với những cảm xúc mới xuất hiện trong đầu.',
      posterUrl: 'https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg',
      backdropUrl: 'https://image.tmdb.org/t/p/original/stKGOm8UyhuLPR9sZLjs5AkmncA.jpg',
      trailerKey: 'LEjhY15eCx0',
      runtime: 96,
      genres: ['Animation', 'Family', 'Comedy'],
      rating: 8.0,
      ageRating: 'P',
      status: MovieStatus.NOW_SHOWING
    }
  });

  const dune = await prisma.movie.create({
    data: {
      title: 'Dune: Part Two',
      originalTitle: 'Dune: Part Two',
      overview: 'Paul Atreides tiếp tục hành trình cùng người Fremen để chống lại thế lực thù địch.',
      posterUrl: 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg',
      backdropUrl: 'https://image.tmdb.org/t/p/original/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg',
      trailerKey: 'Way9Dexny3w',
      runtime: 166,
      genres: ['Science Fiction', 'Adventure'],
      rating: 8.5,
      ageRating: 'T13',
      status: MovieStatus.NOW_SHOWING
    }
  });

  const kungFuPanda = await prisma.movie.create({
    data: {
      title: 'Kung Fu Panda 4',
      originalTitle: 'Kung Fu Panda 4',
      overview: 'Po phải tìm người kế nhiệm Chiến binh Rồng và đối đầu kẻ thù mới.',
      posterUrl: 'https://image.tmdb.org/t/p/w500/kDp1vUBnMpe8ak4rjgl3cLELqjU.jpg',
      backdropUrl: 'https://image.tmdb.org/t/p/original/kYgQzzjNis5jJalYtIHgrom0gOx.jpg',
      trailerKey: '_inKs4eeHiI',
      runtime: 94,
      genres: ['Animation', 'Action', 'Comedy'],
      rating: 7.1,
      ageRating: 'P',
      status: MovieStatus.NOW_SHOWING
    }
  });

  const godzilla = await prisma.movie.create({
    data: {
      title: 'Godzilla x Kong: The New Empire',
      originalTitle: 'Godzilla x Kong: The New Empire',
      overview: 'Godzilla và Kong đối mặt với mối đe dọa khổng lồ mới từ Hollow Earth.',
      posterUrl: 'https://image.tmdb.org/t/p/w500/z1p34vh7dEOnLDmyCrlUVLuoDzd.jpg',
      backdropUrl: 'https://image.tmdb.org/t/p/original/1XDDXPXGiI8id7MrUxK36ke7gkX.jpg',
      trailerKey: 'lV1OOlGwExM',
      runtime: 115,
      genres: ['Action', 'Science Fiction', 'Adventure'],
      rating: 7.2,
      ageRating: 'T13',
      status: MovieStatus.NOW_SHOWING
    }
  });

  const mai = await prisma.movie.create({
    data: {
      title: 'Mai',
      originalTitle: 'Mai',
      overview: 'Câu chuyện tâm lý tình cảm xoay quanh cuộc đời của Mai.',
      posterUrl: 'https://image.tmdb.org/t/p/w500/example-mai.jpg',
      backdropUrl: 'https://image.tmdb.org/t/p/original/example-mai-backdrop.jpg',
      trailerKey: 'example',
      runtime: 131,
      genres: ['Drama', 'Romance'],
      rating: 7.8,
      ageRating: 'T18',
      status: MovieStatus.NOW_SHOWING
    }
  });

  await prisma.movie.createMany({
    data: [
      {
        title: 'Despicable Me 4',
        originalTitle: 'Despicable Me 4',
        overview: 'Gru và gia đình trở lại trong cuộc phiêu lưu mới cùng các Minions.',
        posterUrl: 'https://image.tmdb.org/t/p/w500/wWba3TaojhK7NdycRhoQpsG0FaH.jpg',
        backdropUrl: 'https://image.tmdb.org/t/p/original/example-despicable-me-4.jpg',
        trailerKey: 'qQlr9-rF32A',
        runtime: 95,
        genres: ['Animation', 'Comedy', 'Family'],
        rating: 7.0,
        ageRating: 'P',
        status: MovieStatus.UPCOMING
      },
      {
        title: 'Deadpool & Wolverine',
        originalTitle: 'Deadpool & Wolverine',
        overview: 'Deadpool hợp tác cùng Wolverine trong một nhiệm vụ hỗn loạn xuyên đa vũ trụ.',
        posterUrl: 'https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg',
        backdropUrl: 'https://image.tmdb.org/t/p/original/example-deadpool-wolverine.jpg',
        trailerKey: '73_1biulkYk',
        runtime: 127,
        genres: ['Action', 'Comedy', 'Science Fiction'],
        rating: 8.0,
        ageRating: 'T18',
        status: MovieStatus.UPCOMING
      }
    ]
  });

  const showtimeInsideOutMorning = await prisma.showtime.create({
    data: {
      movieId: insideOut2.id,
      roomId: room1.id,
      startTime: createShowtimeDate(10, 0),
      endTime: addMinutes(createShowtimeDate(10, 0), insideOut2.runtime),
      basePrice: 90000
    }
  });

  await prisma.showtime.createMany({
    data: [
      {
        movieId: insideOut2.id,
        roomId: room1.id,
        startTime: createShowtimeDate(14, 0),
        endTime: addMinutes(createShowtimeDate(14, 0), insideOut2.runtime),
        basePrice: 90000
      },
      {
        movieId: insideOut2.id,
        roomId: roomVip.id,
        startTime: createShowtimeDate(20, 30),
        endTime: addMinutes(createShowtimeDate(20, 30), insideOut2.runtime),
        basePrice: 120000
      },
      {
        movieId: dune.id,
        roomId: room2.id,
        startTime: createShowtimeDate(11, 0),
        endTime: addMinutes(createShowtimeDate(11, 0), dune.runtime),
        basePrice: 95000
      },
      {
        movieId: dune.id,
        roomId: room2.id,
        startTime: createShowtimeDate(19, 0),
        endTime: addMinutes(createShowtimeDate(19, 0), dune.runtime),
        basePrice: 95000
      },
      {
        movieId: kungFuPanda.id,
        roomId: roomVip.id,
        startTime: createShowtimeDate(9, 30),
        endTime: addMinutes(createShowtimeDate(9, 30), kungFuPanda.runtime),
        basePrice: 120000
      },
      {
        movieId: kungFuPanda.id,
        roomId: room1.id,
        startTime: createShowtimeDate(16, 0),
        endTime: addMinutes(createShowtimeDate(16, 0), kungFuPanda.runtime),
        basePrice: 90000
      },
      {
        movieId: godzilla.id,
        roomId: room2.id,
        startTime: createShowtimeDate(13, 30),
        endTime: addMinutes(createShowtimeDate(13, 30), godzilla.runtime),
        basePrice: 95000
      },
      {
        movieId: godzilla.id,
        roomId: roomVip.id,
        startTime: createShowtimeDate(21, 30),
        endTime: addMinutes(createShowtimeDate(21, 30), godzilla.runtime),
        basePrice: 120000
      },
      {
        movieId: mai.id,
        roomId: room1.id,
        startTime: createShowtimeDate(18, 0),
        endTime: addMinutes(createShowtimeDate(18, 0), mai.runtime),
        basePrice: 90000
      }
    ]
  });

  const demoUserPassword = await bcrypt.hash('demo-password', 10);
  const adminPassword = await bcrypt.hash('admin-password', 10);

  const demoUser = await prisma.user.create({
    data: {
      fullName: 'Demo User',
      email: 'user@eousx.com',
      phone: '0911111111',
      password: demoUserPassword
    }
  });

  await prisma.admin.create({
    data: {
      fullName: 'EousX Admin',
      email: 'admin@eousx.com',
      password: adminPassword,
      role: AdminRole.ADMIN
    }
  });

  const soldSeatA1 = await prisma.seat.findFirstOrThrow({
    where: {
      roomId: room1.id,
      code: 'A1'
    }
  });

  const soldSeatA2 = await prisma.seat.findFirstOrThrow({
    where: {
      roomId: room1.id,
      code: 'A2'
    }
  });

  const lockedSeatB5 = await prisma.seat.findFirstOrThrow({
    where: {
      roomId: room1.id,
      code: 'B5'
    }
  });

  const maintenanceSeatC3 = await prisma.seat.findFirstOrThrow({
    where: {
      roomId: room1.id,
      code: 'C3'
    }
  });

  await prisma.seat.update({
    where: {
      id: maintenanceSeatC3.id
    },
    data: {
      type: SeatType.MAINTENANCE,
      isActive: false
    }
  });

  const booking = await prisma.booking.create({
    data: {
      userId: demoUser.id,
      showtimeId: showtimeInsideOutMorning.id,
      code: 'EOUSX-DEMO-001',
      status: BookingStatus.PAID,
      totalAmount: 180000,
      seats: {
        create: [
          {
            seatId: soldSeatA1.id,
            price: 90000
          },
          {
            seatId: soldSeatA2.id,
            price: 90000
          }
        ]
      },
      payment: {
        create: {
          provider: 'DEMO',
          transactionId: 'DEMO-TXN-001',
          amount: 180000,
          status: PaymentStatus.SUCCESS,
          paidAt: new Date()
        }
      },
      ticket: {
        create: {
          qrCode: 'EOUSX-QR-DEMO-001',
          status: TicketStatus.VALID
        }
      }
    }
  });

  await prisma.seatLock.create({
    data: {
      userId: demoUser.id,
      showtimeId: showtimeInsideOutMorning.id,
      seatId: lockedSeatB5.id,
      status: SeatLockStatus.ACTIVE,
      lockedUntil: addMinutes(new Date(), 10)
    }
  });

  console.log('Seed completed!');
  console.log(`Cinema: ${cinema.name}`);
  console.log(`Rooms: ${room1.name}, ${room2.name}, ${roomVip.name}`);
  console.log('Total seats: 124');
  console.log(`Demo booking: ${booking.code}`);
}

main()
  .catch((error) => {
    console.error('Seed failed:', error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
