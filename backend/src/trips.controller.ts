import {
  Body,
  Controller,
  Headers,
  Injectable,
  Post,
  UnauthorizedException,
} from '@nestjs/common';
import { randomUUID } from 'crypto';
import { parseBearerUserId } from './auth.util';
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

  constructor(private readonly progression: ProgressionService) {}

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

    // Integrity from telemetry trust signals — not client qualityScore alone
    const pointCount = body.pointCount ?? body.locations?.length ?? 0;
    let integrity = 88;
    if (pointCount < 5) integrity -= 12;
    if ((body.distanceM ?? 0) < 100) integrity -= 8;
    if ((body.events ?? []).filter((e) => e.type === 'BRAKING').length > 8) integrity -= 5;
    integrity = Math.max(55, Math.min(99, integrity));

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
    return {
      id: stored.id,
      clientTripId: stored.clientTripId,
      integrity: stored.integrity,
      duplicated: false,
      awards,
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
