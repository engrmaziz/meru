import {
  Body,
  Controller,
  Headers,
  Injectable,
  Post,
  UnauthorizedException,
} from '@nestjs/common';
import { createHash, randomUUID } from 'crypto';
import { parseBearerUserId } from './auth.util';
import { ArenaService } from './arena.service';
import { AuthService } from './auth.service';
import { LaunchService } from './launch.service';
import { ProgressionService } from './progression.service';
import type { TripUpsertBody } from './trips.types';

type StoredTrip = TripUpsertBody & {
  id: string;
  userId: string;
  integrity: number;
  createdAt: number;
};

@Injectable()
export class TripsService {
  // ponytail: in-memory trip store; ceiling = lost on restart. Upgrade: Postgres trips.
  private readonly byClientId = new Map<string, StoredTrip>();

  constructor(
    private readonly progression: ProgressionService,
    private readonly arena: ArenaService,
    private readonly auth: AuthService,
    private readonly launch: LaunchService,
  ) {}

  upsert(userId: string, body: TripUpsertBody) {
    const key = `${userId}:${body.clientTripId}`;
    const existing = this.byClientId.get(key);
    if (existing) {
      const prior = this.progression.getOrCreate(userId).lastTripAwards;
      return {
        id: existing.id,
        clientTripId: existing.clientTripId,
        integrity: existing.integrity,
        duplicated: true,
        awards:
          prior?.clientTripId === existing.clientTripId
            ? prior
            : this.progression.me(userId).lastTripAwards,
      };
    }

    const pointCount = body.pointCount ?? body.locations?.length ?? 0;
    // Phase 10: slightly stricter integrity against sparse/spoofed traces
    let integrity = 90;
    if (pointCount < 8) integrity -= 15;
    if (pointCount < 5) integrity -= 10;
    if ((body.distanceM ?? 0) < 150) integrity -= 10;
    if ((body.events ?? []).filter((e) => e.type === 'BRAKING').length > 8) integrity -= 5;
    if ((body.maxSpeedKmh ?? 0) > 220) integrity -= 20; // spoof / crash outlier
    integrity = Math.max(40, Math.min(99, integrity));

    const stored: StoredTrip = {
      ...body,
      id: randomUUID(),
      userId,
      integrity,
      createdAt: Date.now(),
      locations: body.locations ?? [],
      events: body.events ?? [],
    };
    this.byClientId.set(key, stored);

    const awards = this.progression.finalizeTrip(userId, stored.id, body, integrity);
    const displayName = this.auth.findDisplayName(userId) || 'Driver';
    const routeHash = routeFingerprint(body);
    const ghost = this.arena.ghostCompare(userId, routeHash, awards.qualityScore);
    this.arena.onTripFinalized(
      userId,
      displayName,
      awards.adventureScore,
      awards.qualityScore,
      body.newCells ?? 0,
      awards.competitiveEligible,
      routeHash,
    );
    this.launch.bump('tripsUpserted');

    return {
      id: stored.id,
      clientTripId: stored.clientTripId,
      integrity: stored.integrity,
      duplicated: false,
      awards,
      ghost,
    };
  }

  countForUser(userId: string) {
    let n = 0;
    for (const t of this.byClientId.values()) {
      if (t.userId === userId) n++;
    }
    return n;
  }
}

@Controller('v1/trips')
export class TripsController {
  constructor(private readonly trips: TripsService) {}

  @Post()
  upsert(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: TripUpsertBody,
  ) {
    const userId = parseBearerUserId(authorization);
    if (!userId) throw new UnauthorizedException('Missing or invalid token');
    if (!body?.clientTripId || !body.startAtMs) {
      throw new UnauthorizedException('Invalid trip payload');
    }
    return this.trips.upsert(userId, body);
  }
}

function routeFingerprint(body: TripUpsertBody): string | null {
  const locs = body.locations ?? [];
  if (locs.length < 2) return null;
  const a = locs[0];
  const b = locs[locs.length - 1];
  const raw = `${a.lat.toFixed(3)},${a.lon.toFixed(3)}>${b.lat.toFixed(3)},${b.lon.toFixed(3)}`;
  return createHash('sha256').update(raw).digest('hex').slice(0, 16);
}
