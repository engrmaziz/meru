import { Module } from '@nestjs/common';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { HealthController } from './health.controller';
import { ProgressionController } from './progression.controller';
import { ProgressionService } from './progression.service';
import { TripsController, TripsService } from './trips.controller';

@Module({
  imports: [],
  controllers: [HealthController, AuthController, TripsController, ProgressionController],
  providers: [AuthService, TripsService, ProgressionService],
})
export class AppModule {}
