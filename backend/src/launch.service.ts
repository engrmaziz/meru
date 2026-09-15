import {
  ForbiddenException,
  HttpException,
  HttpStatus,
  Injectable,
} from '@nestjs/common';

export type LaunchFlags = {
  softLaunchCityId: string;
  softLaunchCityName: string;
  s2Leaderboards: boolean;
  s3Garage: boolean;
  s4Marketplace: boolean;
  ghostDriver: boolean;
  challengesEnabled: boolean;
  accountDeletionEnabled: boolean;
  integrityCompetitiveMin: number;
  weightsVersion: number;
  /** Simple global kill for bookings during incident */
  bookingsEnabled: boolean;
};

export type MetricCounters = {
  tripsUpserted: number;
  tripsSyncFailed: number;
  bookingsCreated: number;
  bookingErrors: number;
  invoicesConfirmed: number;
  authFailures: number;
  rateLimited: number;
};

type RateBucket = { count: number; resetAtMs: number };

@Injectable()
export class LaunchService {
  // ponytail: in-memory launch config + counters; ceiling = single box. Upgrade: Redis + remote config.
  private readonly flags: LaunchFlags = {
    softLaunchCityId: 'pk-pb-lhr',
    softLaunchCityName: 'Lahore',
    s2Leaderboards: true,
    s3Garage: true,
    s4Marketplace: true,
    ghostDriver: true,
    challengesEnabled: true,
    accountDeletionEnabled: true,
    integrityCompetitiveMin: 75,
    weightsVersion: 1,
    bookingsEnabled: true,
  };

  private readonly metrics: MetricCounters = {
    tripsUpserted: 0,
    tripsSyncFailed: 0,
    bookingsCreated: 0,
    bookingErrors: 0,
    invoicesConfirmed: 0,
    authFailures: 0,
    rateLimited: 0,
  };

  private readonly rateBuckets = new Map<string, RateBucket>();

  getFlags(): LaunchFlags {
    return { ...this.flags };
  }

  /** Public subset for Android — no admin secrets */
  publicFlags() {
    const f = this.flags;
    return {
      softLaunchCityId: f.softLaunchCityId,
      softLaunchCityName: f.softLaunchCityName,
      s2Leaderboards: f.s2Leaderboards,
      s3Garage: f.s3Garage,
      s4Marketplace: f.s4Marketplace,
      ghostDriver: f.ghostDriver,
      challengesEnabled: f.challengesEnabled,
      accountDeletionEnabled: f.accountDeletionEnabled,
      integrityCompetitiveMin: f.integrityCompetitiveMin,
      weightsVersion: f.weightsVersion,
      bookingsEnabled: f.bookingsEnabled,
    };
  }

  patchFlags(partial: Partial<LaunchFlags>) {
    if (partial.integrityCompetitiveMin != null) {
      partial.integrityCompetitiveMin = Math.min(
        99,
        Math.max(50, Math.round(partial.integrityCompetitiveMin)),
      );
    }
    Object.assign(this.flags, partial);
    return this.getFlags();
  }

  bump(metric: keyof MetricCounters, by = 1) {
    this.metrics[metric] += by;
  }

  getMetrics() {
    return {
      ...this.metrics,
      // Soft-launch targets (DEC-026) — beta agreement
      targets: {
        crashFreeSessionsPct: 99.0,
        tripSyncSuccessPct: 95.0,
        note: 'Measure on beta Play track; API counters are proxies until Crashlytics.',
      },
    };
  }

  assertAdmin(adminKey?: string) {
    const expected = process.env.MERU_ADMIN_KEY || 'meru-dev-admin';
    if (!adminKey || adminKey !== expected) {
      throw new ForbiddenException('Admin key required');
    }
  }

  /**
   * Token-bucket style limit: max N requests per window per key.
   * Used on auth + booking write paths.
   */
  consumeRate(key: string, max = 30, windowMs = 60_000) {
    const now = Date.now();
    let b = this.rateBuckets.get(key);
    if (!b || b.resetAtMs <= now) {
      b = { count: 0, resetAtMs: now + windowMs };
      this.rateBuckets.set(key, b);
    }
    b.count += 1;
    if (b.count > max) {
      this.bump('rateLimited');
      throw new HttpException(
        { code: 'RATE_LIMITED', message: 'Rate limited — try again shortly' },
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }
  }

  healthPayload() {
    return {
      status: 'ok',
      service: 'meru-api',
      phase: 10,
      softLaunch: {
        cityId: this.flags.softLaunchCityId,
        cityName: this.flags.softLaunchCityName,
      },
      flags: this.publicFlags(),
      uptimeSec: Math.floor(process.uptime()),
    };
  }
}
