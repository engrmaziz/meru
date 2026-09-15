import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Param,
  Patch,
  Post,
  Query,
  UnauthorizedException,
} from '@nestjs/common';
import { parseBearerUserId } from './auth.util';
import { ArenaService, GeoType } from './arena.service';
import { AuthService } from './auth.service';

@Controller('v1')
export class ArenaController {
  constructor(
    private readonly arena: ArenaService,
    private readonly auth: AuthService,
  ) {}

  @Get('seasons/current')
  season() {
    return this.arena.currentSeason();
  }

  @Get('leaderboards/:geoType/:geoId')
  board(
    @Headers('authorization') authorization: string | undefined,
    @Param('geoType') geoType: string,
    @Param('geoId') geoId: string,
    @Query('period') period?: string,
  ) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    const gt = geoType as GeoType;
    if (!['city', 'province', 'country', 'global'].includes(gt)) {
      throw new UnauthorizedException('Invalid geoType');
    }
    return this.arena.board(userId, gt, geoId, period || 'season');
  }

  @Get('ranks/me')
  ranksMe(@Headers('authorization') authorization: string | undefined) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.arena.ranksMe(userId);
  }

  @Get('friends/leaderboard')
  friends(@Headers('authorization') authorization: string | undefined) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.arena.friendsBoard(userId);
  }

  @Post('friends/:targetId/follow')
  follow(
    @Headers('authorization') authorization: string | undefined,
    @Param('targetId') targetId: string,
  ) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.arena.follow(userId, targetId);
  }

  @Delete('friends/:targetId/follow')
  unfollow(
    @Headers('authorization') authorization: string | undefined,
    @Param('targetId') targetId: string,
  ) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.arena.unfollow(userId, targetId);
  }

  @Get('exploration/map')
  exploration(@Headers('authorization') authorization: string | undefined) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.arena.explorationMap(userId);
  }

  @Get('ghost/compare')
  ghost(
    @Headers('authorization') authorization: string | undefined,
    @Query('routeHash') routeHash?: string,
    @Query('quality') quality?: string,
  ) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.arena.ghostCompare(
      userId,
      routeHash || null,
      Number(quality) || 0,
    );
  }

  @Get('share/card')
  share(@Headers('authorization') authorization: string | undefined) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.arena.shareCard(userId);
  }

  @Get('privacy/me')
  privacyMe(@Headers('authorization') authorization: string | undefined) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    return this.arena.privacyMe(userId);
  }

  @Patch('privacy/me')
  privacyPatch(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: { boardOptIn?: boolean; displayName?: string },
  ) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    const name =
      body.displayName || this.auth.findDisplayName(userId) || 'Driver';
    return this.arena.setPrivacy(userId, body.boardOptIn !== false, name);
  }

  @Get('admin/flags')
  adminFlags() {
    // ponytail: unauthenticated admin for local Phase 6; upgrade: role guard.
    return this.arena.getFlags();
  }

  @Patch('admin/flags')
  adminPatch(@Body() body: Record<string, unknown>) {
    return this.arena.patchFlags(body as never);
  }
}
