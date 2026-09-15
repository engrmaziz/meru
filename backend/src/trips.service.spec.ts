import { Test, TestingModule } from '@nestjs/testing';
import { ArenaService } from './arena.service';
import { AuthService } from './auth.service';
import { LaunchService } from './launch.service';
import { ProgressionService } from './progression.service';
import { TripsService } from './trips.controller';

describe('TripsService', () => {
  let service: TripsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [LaunchService, ProgressionService, ArenaService, AuthService, TripsService],
    }).compile();
    service = module.get(TripsService);
  });

  it('upserts once and dedupes by clientTripId', () => {
    const body = {
      clientTripId: 'trip-1',
      startAtMs: 1,
      distanceM: 1200,
      durationMs: 60_000,
      pointCount: 20,
      locations: [{ ts: 1, lat: 1, lon: 1 }],
      events: [],
    };
    const first = service.upsert('u1', body);
    const second = service.upsert('u1', body);
    expect(first.duplicated).toBe(false);
    expect(second.duplicated).toBe(true);
    expect(second.id).toBe(first.id);
  });
});
