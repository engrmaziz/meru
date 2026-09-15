import { Test, TestingModule } from '@nestjs/testing';
import { ProgressionService } from './progression.service';
import { TripsService } from './trips.controller';

describe('ProgressionService', () => {
  let progression: ProgressionService;
  let trips: TripsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ProgressionService, TripsService],
    }).compile();
    progression = module.get(ProgressionService);
    trips = module.get(TripsService);
  });

  it('ignores spoofed client qualityScore for competitive awards', () => {
    const spoofed = trips.upsert('user-1', {
      clientTripId: 't1',
      startAtMs: Date.now() - 60_000,
      endAtMs: Date.now(),
      distanceM: 4000,
      durationMs: 60_000,
      pointCount: 40,
      newCells: 2,
      qualityScore: 99,
      explorationXp: 9999,
      events: [
        { ts: 1, type: 'BRAKING', label: 'hard', severity: 3 },
        { ts: 2, type: 'BRAKING', label: 'hard', severity: 3 },
        { ts: 3, type: 'BRAKING', label: 'hard', severity: 3 },
        { ts: 4, type: 'BRAKING', label: 'hard', severity: 3 },
        { ts: 5, type: 'BRAKING', label: 'hard', severity: 3 },
      ],
      locations: [{ ts: 1, lat: 1, lon: 2 }],
    });

    expect(spoofed.duplicated).toBe(false);
    expect(spoofed.awards?.qualityScore).toBeLessThan(90);
    expect(spoofed.awards?.xpAwarded).toBeLessThan(9999);
    expect(spoofed.awards?.weightsVersion).toBe(1);

    const me = progression.me('user-1');
    expect(me.xpTotal).toBe(spoofed.awards!.xpAwarded);
    expect(me.level).toBeGreaterThanOrEqual(1);
  });

  it('dedupes trip awards on second upsert', () => {
    const body = {
      clientTripId: 'dup',
      startAtMs: 1,
      distanceM: 2000,
      pointCount: 20,
      newCells: 1,
      events: [] as { ts: number; type: string; label: string }[],
    };
    const a = trips.upsert('u2', body);
    const xp = progression.me('u2').xpTotal;
    const b = trips.upsert('u2', body);
    expect(b.duplicated).toBe(true);
    expect(progression.me('u2').xpTotal).toBe(xp);
    expect(a.awards?.clientTripId).toBe('dup');
  });
});
