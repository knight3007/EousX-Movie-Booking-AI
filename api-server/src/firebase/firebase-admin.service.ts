import {
  Injectable,
  ServiceUnavailableException,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { cert, getApps, initializeApp, ServiceAccount } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import * as fs from 'fs';
import * as path from 'path';

@Injectable()
export class FirebaseAdminService {
  private readonly configured: boolean;
  private readonly configError?: string;

  constructor(private readonly configService: ConfigService) {
    const existingApp = getApps()[0];

    if (existingApp) {
      this.configured = true;
      return;
    }

    const serviceAccountJson = this.configService.get<string>(
      'FIREBASE_SERVICE_ACCOUNT_JSON',
    );
    const serviceAccountPath = this.configService.get<string>(
      'FIREBASE_SERVICE_ACCOUNT_PATH',
    );

    if (!serviceAccountJson && !serviceAccountPath) {
      this.configured = false;
      return;
    }

    try {
      const serviceAccount = serviceAccountJson
        ? this.parseServiceAccountJson(serviceAccountJson)
        : this.readServiceAccountFile(serviceAccountPath as string);

      initializeApp({
        credential: cert(serviceAccount),
      });

      this.configured = true;
    } catch (error) {
      this.configured = false;
      this.configError =
        error instanceof Error ? error.message : 'Unknown Firebase config error';
    }
  }

  async verifyIdToken(idToken: string) {
    if (!this.configured) {
      throw new ServiceUnavailableException('Firebase Admin is not configured');
    }

    try {
      return await getAuth().verifyIdToken(idToken);
    } catch {
      throw new UnauthorizedException('Invalid Firebase ID token');
    }
  }

  private parseServiceAccountJson(value: string): ServiceAccount {
    try {
      return JSON.parse(value) as ServiceAccount;
    } catch {
      throw new Error('Invalid FIREBASE_SERVICE_ACCOUNT_JSON');
    }
  }

  private readServiceAccountFile(value: string): ServiceAccount {
    const resolvedPath = path.isAbsolute(value)
      ? value
      : path.resolve(process.cwd(), value);

    try {
      const fileContent = fs.readFileSync(resolvedPath, 'utf8');
      return JSON.parse(fileContent) as ServiceAccount;
    } catch {
      throw new Error('Invalid FIREBASE_SERVICE_ACCOUNT_PATH');
    }
  }
}
