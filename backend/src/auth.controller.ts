import {
  Body,
  ConflictException,
  Controller,
  Post,
  UnauthorizedException,
} from '@nestjs/common';
import { randomUUID } from 'crypto';
import { AuthService } from './auth.service';
import { LaunchService } from './launch.service';

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
  constructor(
    private readonly auth: AuthService,
    private readonly launch: LaunchService,
  ) {}

  @Post('register')
  register(@Body() body: AuthBody) {
    this.launch.consumeRate(`auth:${body.email || 'anon'}`, 10, 60_000);
    if (!body.email?.includes('@') || !body.password || body.password.length < 6) {
      this.launch.bump('authFailures');
      throw new UnauthorizedException('Invalid email or password');
    }
    try {
      return this.auth.register(
        body.email.trim().toLowerCase(),
        body.password,
        body.displayName?.trim() || body.email.split('@')[0],
      );
    } catch {
      this.launch.bump('authFailures');
      throw new ConflictException('Email already registered');
    }
  }

  @Post('login')
  login(@Body() body: AuthBody) {
    this.launch.consumeRate(`auth:${body.email || 'anon'}`, 20, 60_000);
    const result = this.auth.login(body.email?.trim().toLowerCase(), body.password);
    if (!result) {
      this.launch.bump('authFailures');
      throw new UnauthorizedException('Invalid credentials');
    }
    return result;
  }

  @Post('google')
  google(@Body() body: GoogleBody) {
    this.launch.consumeRate(`auth:google:${body.email || 'anon'}`, 20, 60_000);
    const email = (body.email || 'driver@meru.app').toLowerCase();
    const displayName = body.displayName || 'Meru Driver';
    return this.auth.googleDev(email, displayName, body.idToken || randomUUID());
  }
}
