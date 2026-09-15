import { Controller, Get } from '@nestjs/common';
import { LaunchService } from './launch.service';

@Controller()
export class HealthController {
  constructor(private readonly launch: LaunchService) {}

  @Get('health')
  health() {
    return this.launch.healthPayload();
  }
}
