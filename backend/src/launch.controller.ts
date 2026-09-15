import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Patch,
  Post,
} from '@nestjs/common';
import { parseBearerUserId } from './auth.util';
import { AuthService } from './auth.service';
import { LaunchService, LaunchFlags } from './launch.service';
import { VaultService } from './vault.controller';
import { UnauthorizedException } from '@nestjs/common';

@Controller('v1')
export class LaunchController {
  constructor(
    private readonly launch: LaunchService,
    private readonly auth: AuthService,
    private readonly vault: VaultService,
  ) {}

  @Get('flags')
  flags() {
    return this.launch.publicFlags();
  }

  @Get('admin/flags')
  adminFlags(@Headers('x-admin-key') adminKey?: string) {
    this.launch.assertAdmin(adminKey);
    return this.launch.getFlags();
  }

  @Patch('admin/flags')
  adminPatch(
    @Headers('x-admin-key') adminKey: string | undefined,
    @Body() body: Partial<LaunchFlags>,
  ) {
    this.launch.assertAdmin(adminKey);
    return this.launch.patchFlags(body);
  }

  @Get('admin/metrics')
  metrics(@Headers('x-admin-key') adminKey?: string) {
    this.launch.assertAdmin(adminKey);
    return this.launch.getMetrics();
  }

  @Post('admin/support/lookup')
  supportLookup(
    @Headers('x-admin-key') adminKey: string | undefined,
    @Body() body: { userId?: string; email?: string },
  ) {
    this.launch.assertAdmin(adminKey);
    const user = body.userId
      ? this.auth.findById(body.userId)
      : body.email
        ? this.auth.findByEmail(body.email.trim().toLowerCase())
        : null;
    if (!user) return { found: false };
    const vehicles = this.vault.listVehicles(user.id);
    return {
      found: true,
      user: { id: user.id, email: user.email, displayName: user.displayName },
      vehicles: vehicles.vehicles.length,
      maxVehicles: vehicles.maxVehicles,
    };
  }

  @Delete('account')
  deleteAccount(@Headers('authorization') authorization?: string) {
    if (!this.launch.getFlags().accountDeletionEnabled) {
      throw new UnauthorizedException('Account deletion temporarily disabled');
    }
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    this.vault.purgeUser(userId);
    this.auth.deleteUser(userId);
    return { deleted: true, retentionNote: 'Telemetry purged from in-memory store; Postgres retention policy applies in prod (DEC-026).' };
  }
}
