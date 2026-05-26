import {
  ConflictException,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcryptjs';
import { PrismaService } from '../database/prisma.service';
import { FirebaseAdminService } from '../firebase/firebase-admin.service';
import { GoogleLoginDto } from './dto/google-login.dto';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';

type UserWithPassword = {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
  firebaseUid: string | null;
  password: string | null;
};

type SanitizedUser = Omit<UserWithPassword, 'password' | 'firebaseUid'>;

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
    private readonly firebaseAdminService: FirebaseAdminService,
  ) {}

  async register(dto: RegisterDto) {
    const email = this.normalizeEmail(dto.email);
    const existingUser = await this.prisma.user.findUnique({
      where: {
        email,
      },
    });

    if (existingUser) {
      throw new ConflictException('Email already exists');
    }

    const password = await bcrypt.hash(dto.password, 10);
    const user = await this.prisma.user.create({
      data: {
        fullName: dto.fullName,
        email,
        password,
        phone: dto.phone,
      },
      select: this.userSelectWithPassword(),
    });
    const sanitizedUser = this.sanitizeUser(user);

    return {
      message: 'Register successfully',
      accessToken: await this.signAccessToken(sanitizedUser),
      user: sanitizedUser,
    };
  }

  async login(dto: LoginDto) {
    const email = this.normalizeEmail(dto.email);
    const user = await this.prisma.user.findUnique({
      where: {
        email,
      },
      select: this.userSelectWithPassword(),
    });

    if (!user || !user.password) {
      throw new UnauthorizedException('Invalid email or password');
    }

    const passwordMatches = await bcrypt.compare(dto.password, user.password);
    const isPlainTextSeedPassword =
      !passwordMatches &&
      !user.password.startsWith('$2') &&
      dto.password === user.password;

    if (!passwordMatches && !isPlainTextSeedPassword) {
      throw new UnauthorizedException('Invalid email or password');
    }

    if (isPlainTextSeedPassword) {
      const hashedPassword = await bcrypt.hash(dto.password, 10);
      await this.prisma.user.update({
        where: {
          id: user.id,
        },
        data: {
          password: hashedPassword,
        },
      });
    }

    const sanitizedUser = this.sanitizeUser(user);

    return {
      message: 'Login successfully',
      accessToken: await this.signAccessToken(sanitizedUser),
      user: sanitizedUser,
    };
  }

  async me(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: {
        id: userId,
      },
      select: {
        id: true,
        fullName: true,
        email: true,
        phone: true,
        avatarUrl: true,
      },
    });

    if (!user) {
      throw new NotFoundException('User not found');
    }

    return user;
  }

  async googleLogin(dto: GoogleLoginDto) {
    const decoded = await this.firebaseAdminService.verifyIdToken(dto.idToken);
    const firebaseUid = decoded.uid;
    const email = decoded.email?.trim().toLowerCase();
    const emailVerified = decoded.email_verified;
    const fullName =
      decoded.name || decoded.email?.split('@')[0] || 'Google User';
    const avatarUrl = decoded.picture || null;

    if (!email) {
      throw new UnauthorizedException('Google account email is required');
    }

    if (emailVerified === false) {
      throw new UnauthorizedException('Google email is not verified');
    }

    let user = await this.prisma.user.findUnique({
      where: {
        firebaseUid,
      },
      select: this.userSelectWithPassword(),
    });

    if (!user) {
      user = await this.prisma.user.findUnique({
        where: {
          email,
        },
        select: this.userSelectWithPassword(),
      });
    }

    if (!user) {
      user = await this.prisma.user.create({
        data: {
          email,
          fullName,
          phone: null,
          avatarUrl,
          firebaseUid,
          password: null,
        },
        select: this.userSelectWithPassword(),
      });
    } else {
      const updateData: {
        firebaseUid?: string;
        avatarUrl?: string | null;
        fullName?: string;
      } = {};

      if (!user.firebaseUid) {
        updateData.firebaseUid = firebaseUid;
      }

      if (avatarUrl && user.avatarUrl !== avatarUrl) {
        updateData.avatarUrl = avatarUrl;
      }

      if (!user.fullName) {
        updateData.fullName = fullName;
      }

      if (Object.keys(updateData).length > 0) {
        user = await this.prisma.user.update({
          where: {
            id: user.id,
          },
          data: updateData,
          select: this.userSelectWithPassword(),
        });
      }
    }

    const sanitizedUser = this.sanitizeUser(user);

    return {
      message: 'Google login successfully',
      accessToken: await this.signAccessToken(sanitizedUser),
      user: sanitizedUser,
    };
  }

  async signAccessToken(user: SanitizedUser) {
    return this.jwtService.signAsync({
      sub: user.id,
      email: user.email,
    });
  }

  sanitizeUser(user: UserWithPassword): SanitizedUser {
    const { password: _password, firebaseUid: _firebaseUid, ...sanitizedUser } =
      user;

    return sanitizedUser;
  }

  private normalizeEmail(email: string) {
    return email.trim().toLowerCase();
  }

  private userSelectWithPassword() {
    return {
      id: true,
      fullName: true,
      email: true,
      phone: true,
      avatarUrl: true,
      firebaseUid: true,
      password: true,
    };
  }
}
