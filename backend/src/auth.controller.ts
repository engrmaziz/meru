import { Body, ConflictException, Controller, Post, UnauthorizedException } from '@nestjs/common';
import { randomUUID } from 'crypto';
import { AuthService } from './auth.service';

class AuthBody {
  email!: string;
  password!: string;
  displayName?: string;
}

class GoogleBody {
  idToken?: string;
  displayName?: string;
  email?: string;
}

@Controller('v1/auth')
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  @Post('register')
  register(@Body() body: AuthBody) {
    if (!body.email?.includes('@') || !body.password || body.password.length < 6) {
      throw new UnauthorizedException('Invalid email or password');
    }
    try {
      return this.auth.register(
        body.email.trim().toLowerCase(),
        body.password,
        body.displayName?.trim() || body.email.split('@')[0],
      );
    } catch {
      throw new ConflictException('Email already registered');
    }
  }

  @Post('login')
  login(@Body() body: AuthBody) {
    const result = this.auth.login(body.email?.trim().toLowerCase(), body.password);
    if (!result) throw new UnauthorizedException('Invalid credentials');
    return result;
  }

  @Post('google')
  google(@Body() body: GoogleBody) {
    // Phase 1: accept stub token for local Android Google button.
    const email = (body.email || 'driver@meru.app').toLowerCase();
    const displayName = body.displayName || 'Meru Driver';
    return this.auth.googleDev(email, displayName, body.idToken || randomUUID());
  }
}
