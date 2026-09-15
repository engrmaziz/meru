import { Module } from '@nestjs/common';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { HealthController } from './health.controller';
import { TripsController, TripsService } from './trips.controller';

@Module({
  imports: [],
  controllers: [HealthController, AuthController, TripsController],
  providers: [AuthService, TripsService],
})
export class AppModule {}
