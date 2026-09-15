import { Injectable } from '@nestjs/common';
import { createHash, randomUUID, timingSafeEqual } from 'crypto';

type UserRecord = {
  id: string;
  email: string;
  displayName: string;
  passwordHash: string;
};

@Injectable()
export class AuthService {
  // ponytail: in-memory store for Phase 1; ceiling = single-process / lost on restart. Upgrade: Postgres users table.
  private readonly users = new Map<string, UserRecord>();

  register(email: string, password: string, displayName: string) {
    if (this.users.has(email)) {
      throw new Error('exists');
    }
    const user: UserRecord = {
      id: randomUUID(),
      email,
      displayName,
      passwordHash: this.hash(password),
    };
    this.users.set(email, user);
    return this.tokenResponse(user);
  }

  login(email: string, password: string) {
    const user = this.users.get(email);
    if (!user || !this.passwordMatches(user.passwordHash, password)) {
      return null;
    }
    return this.tokenResponse(user);
  }

  googleDev(email: string, displayName: string, _idToken: string) {
    let user = this.users.get(email);
    if (!user) {
      user = {
        id: randomUUID(),
        email,
        displayName,
        passwordHash: this.hash(randomUUID()),
      };
      this.users.set(email, user);
    }
    return this.tokenResponse(user);
  }

  private tokenResponse(user: UserRecord) {
    return {
      accessToken: `meru_${user.id}_${randomUUID()}`,
      user: {
        id: user.id,
        email: user.email,
        displayName: user.displayName,
      },
    };
  }

  private hash(value: string) {
    return createHash('sha256').update(value).digest('hex');
  }

  private passwordMatches(hash: string, password: string) {
    const left = Buffer.from(hash);
    const right = Buffer.from(this.hash(password));
    if (left.length !== right.length) return false;
    return timingSafeEqual(left, right);
  }
}
