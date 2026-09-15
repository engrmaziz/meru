import { Test, TestingModule } from '@nestjs/testing';
import { TripsService } from './trips.controller';

describe('TripsService', () => {
  let service: TripsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [TripsService],
    }).compile();
    service = module.get(TripsService);
  });

  it('upserts once and dedupes by clientTripId', () => {
    const body = {
      clientTripId: 'trip-1',
      startAtMs: 1,
      distanceM: 1200,
      qualityScore: 88,
      locations: [{ ts: 1, lat: 1, lon: 2 }],
      events: [],
    };
    const first = service.upsert('user-a', body);
    const second = service.upsert('user-a', body);
    expect(first.duplicated).toBe(false);
    expect(second.duplicated).toBe(true);
    expect(second.id).toBe(first.id);
    expect(service.countForUser('user-a')).toBe(1);
  });
});
