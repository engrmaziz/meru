import { Module } from '@nestjs/common';
import { ArenaController } from './arena.controller';
import { ArenaService } from './arena.service';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { HealthController } from './health.controller';
import { JobsController, JobsService } from './jobs.controller';
import { LaunchController } from './launch.controller';
import { LaunchService } from './launch.service';
import { ProgressionController } from './progression.controller';
import { ProgressionService } from './progression.service';
import { TripsController, TripsService } from './trips.controller';
import { VaultController, VaultService } from './vault.controller';
import { WorkshopsController, WorkshopsService } from './workshops.controller';

@Module({
  imports: [],
  controllers: [
    HealthController,
    AuthController,
    TripsController,
    ProgressionController,
    ArenaController,
    VaultController,
    WorkshopsController,
    JobsController,
    LaunchController,
  ],
  providers: [
    AuthService,
    LaunchService,
    TripsService,
    ProgressionService,
    ArenaService,
    VaultService,
    WorkshopsService,
    JobsService,
  ],
})
export class AppModule {}
