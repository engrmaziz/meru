import { Module } from '@nestjs/common';
import { ArenaController } from './arena.controller';
import { ArenaService } from './arena.service';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { HealthController } from './health.controller';
import { ProgressionController } from './progression.controller';
import { ProgressionService } from './progression.service';
import { TripsController, TripsService } from './trips.controller';
import { VaultController, VaultService } from './vault.controller';

@Module({
  imports: [],
  controllers: [
    HealthController,
    AuthController,
    TripsController,
    ProgressionController,
    ArenaController,
    VaultController,
  ],
  providers: [
    AuthService,
    TripsService,
    ProgressionService,
    ArenaService,
    VaultService,
  ],
})
export class AppModule {}
