import {
  Body,
  Controller,
  Headers,
  Injectable,
  Post,
  UnauthorizedException,
} from '@nestjs/common';
import { randomUUID } from 'crypto';

export type TripLocationDto = {
  ts: number;
  lat: number;
  lon: number;
  alt?: number | null;
  speed?: number | null;
  bearing?: number | null;
  acc?: number | null;
};

export type TripEventDto = {
  ts: number;
  type: string;
  label: string;
  severity?: number;
  lat?: number | null;
  lon?: number | null;
};

export type TripUpsertBody = {
  clientTripId: string;
  startAtMs: number;
  endAtMs?: number | null;
  distanceM?: number;
  durationMs?: number;
  avgSpeedKmh?: number;
  maxSpeedKmh?: number;
  qualityScore?: number;
  explorationXp?: number;
  newCells?: number;
  elevationGainM?: number;
  stopCount?: number;
  pointCount?: number;
  locations?: TripLocationDto[];
  events?: TripEventDto[];
};

type StoredTrip = TripUpsertBody & {
  id: string;
  userId: string;
  integrity: number;
  createdAt: number;
};

@Injectable()
export class TripsService {
  // ponytail: in-memory trip store for Phase 4; ceiling = lost on restart. Upgrade: Postgres trips.
  private readonly byClientId = new Map<string, StoredTrip>();

  upsert(userId: string, body: TripUpsertBody) {
    const key = `${userId}:${body.clientTripId}`;
    const existing = this.byClientId.get(key);
    if (existing) {
      return {
        id: existing.id,
        clientTripId: existing.clientTripId,
        integrity: existing.integrity,
        duplicated: true,
      };
    }
    const integrity = Math.min(99, Math.max(70, (body.qualityScore ?? 80) + 2));
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
    return {
      id: stored.id,
      clientTripId: stored.clientTripId,
      integrity: stored.integrity,
      duplicated: false,
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

/** Tokens are `meru_<userId>_<uuid>` from AuthService. */
export function parseBearerUserId(authorization?: string): string | null {
  if (!authorization?.startsWith('Bearer ')) return null;
  const token = authorization.slice('Bearer '.length).trim();
  const parts = token.split('_');
  if (parts.length < 3 || parts[0] !== 'meru') return null;
  return parts[1] || null;
}
