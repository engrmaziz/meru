import { Controller, Get, Headers, UnauthorizedException } from '@nestjs/common';
import { parseBearerUserId } from './auth.util';
import { ProgressionService } from './progression.service';

@Controller('v1')
export class ProgressionController {
  constructor(private readonly progression: ProgressionService) {}

  @Get('scores/config')
  config() {
    return this.progression.getWeights();
  }

  @Get('scores/me')
  scoresMe(@Headers('authorization') authorization: string | undefined) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.progression.me(userId);
  }

  @Get('achievements/me')
  achievementsMe(@Headers('authorization') authorization: string | undefined) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return { items: this.progression.achievementsMe(userId) };
  }

  @Get('challenges')
  challenges(@Headers('authorization') authorization: string | undefined) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return { items: this.progression.challengesMe(userId) };
  }
}
