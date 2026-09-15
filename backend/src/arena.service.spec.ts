import { Test, TestingModule } from '@nestjs/testing';
import { ArenaService } from './arena.service';
import { AuthService } from './auth.service';
import { LaunchService } from './launch.service';
import { ProgressionService } from './progression.service';
import { TripsService } from './trips.controller';

describe('ArenaService', () => {
  let arena: ArenaService;
  let trips: TripsService;
  let auth: AuthService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [LaunchService, ProgressionService, ArenaService, AuthService, TripsService],
    }).compile();
    arena = module.get(ArenaService);
    trips = module.get(TripsService);
    auth = module.get(AuthService);
  });

  it('exposes 4 geo ranks after a trip and hides opted-out users', () => {
    const reg = auth.register('arena@meru.app', 'secret1', 'Arena Pilot');
    const userId = reg.user.id;

    trips.upsert(userId, {
      clientTripId: 'arena-1',
      startAtMs: Date.now() - 60_000,
      endAtMs: Date.now(),
      distanceM: 5000,
      durationMs: 60_000,
      pointCount: 40,
      newCells: 3,
      locations: [
        { ts: 1, lat: 31.52, lon: 74.35 },
        { ts: 2, lat: 31.53, lon: 74.36 },
      ],
      events: [],
    });

    const ranks = arena.ranksMe(userId);
    expect(ranks.ranks).toHaveLength(4);
    expect(ranks.ranks.map((r) => r.geoType)).toEqual([
      'city',
      'province',
      'country',
      'global',
    ]);
    expect(ranks.ranks.every((r) => r.rank != null && r.rank >= 1)).toBe(true);

    const city = arena.board(userId, 'city', 'pk-pb-lhr', 'season');
    expect(city.entries.some((e) => e.userId === userId)).toBe(true);

    arena.setPrivacy(userId, false);
    const hidden = arena.board(userId, 'city', 'pk-pb-lhr', 'season');
    expect(hidden.entries.some((e) => e.userId === userId)).toBe(false);
  });
});
