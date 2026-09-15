import { Injectable } from '@nestjs/common';
import type { TripUpsertBody } from './trips.types';

/** Versioned weights — never hard-code in the APK (DEC / Phase 5). */
export const SCORE_WEIGHTS_V1 = {
  version: 1,
  quality: 0.45,
  exploration: 0.35,
  activity: 0.2,
  xpPerQualityPoint: 2,
  xpPerExplorationPoint: 3,
  xpPerActivityPoint: 1,
  xpPerKm: 8,
  xpPerNewCell: 12,
  integrityCompetitiveMin: 75,
  levelXpStep: 1000,
};

export type ScoreWeights = typeof SCORE_WEIGHTS_V1;

export type AchievementDef = {
  id: string;
  title: string;
  description: string;
  rarity: 'common' | 'rare' | 'epic' | 'legendary';
  xpBonus: number;
};

export type ChallengeDef = {
  id: string;
  title: string;
  description: string;
  period: 'daily' | 'weekly';
  metric: 'distance_m' | 'trips' | 'new_cells' | 'quality_avg';
  target: number;
  xpReward: number;
};

const ACHIEVEMENTS: AchievementDef[] = [
  {
    id: 'first_ascent',
    title: 'First Ascent',
    description: 'Complete your first sealed drive',
    rarity: 'common',
    xpBonus: 50,
  },
  {
    id: 'explorer_10',
    title: 'Trailfinder',
    description: 'Unlock 10 exploration cells',
    rarity: 'rare',
    xpBonus: 120,
  },
  {
    id: 'smooth_90',
    title: 'Clean Line',
    description: 'Earn a server quality score of 90+',
    rarity: 'epic',
    xpBonus: 200,
  },
  {
    id: 'streak_3',
    title: 'Three-Day Flame',
    description: 'Drive on 3 consecutive days',
    rarity: 'rare',
    xpBonus: 150,
  },
];

const CHALLENGES: ChallengeDef[] = [
  {
    id: 'daily_km',
    title: 'Daily kilometers',
    description: 'Cover 5 km today',
    period: 'daily',
    metric: 'distance_m',
    target: 5000,
    xpReward: 80,
  },
  {
    id: 'daily_trip',
    title: 'One more ascent',
    description: 'Complete 1 drive today',
    period: 'daily',
    metric: 'trips',
    target: 1,
    xpReward: 40,
  },
  {
    id: 'weekly_explore',
    title: 'Weekly scout',
    description: 'Unlock 15 new cells this week',
    period: 'weekly',
    metric: 'new_cells',
    target: 15,
    xpReward: 250,
  },
  {
    id: 'weekly_quality',
    title: 'Steady hand',
    description: 'Average quality 85+ across the week',
    period: 'weekly',
    metric: 'quality_avg',
    target: 85,
    xpReward: 220,
  },
];

function titleForLevel(level: number): string {
  if (level >= 20) return 'Summit Legend';
  if (level >= 10) return 'Ridge Runner';
  if (level >= 5) return 'Trail Blazer';
  if (level >= 2) return 'Ascender';
  return 'Novice Driver';
}

function dayKey(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10);
}

function weekKey(ms: number): string {
  const d = new Date(ms);
  const oneJan = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((d.getTime() - oneJan.getTime()) / 86400000 + oneJan.getUTCDay() + 1) / 7);
  return `${d.getUTCFullYear()}-W${week}`;
}

type UserProgress = {
  xpTotal: number;
  xpLedger: { at: number; amount: number; reason: string; tripId?: string }[];
  adventureScore: number;
  driverRating: number;
  qualityScore: number;
  explorationScore: number;
  activityScore: number;
  totalDistanceM: number;
  tripCount: number;
  totalCells: number;
  streakDays: number;
  lastDriveDay: string | null;
  unlockedAchievements: string[];
  challengeProgress: Record<
    string,
    { value: number; samples: number; completed: boolean; periodKey: string }
  >;
  lastTripAwards: TripAwards | null;
};

export type TripAwards = {
  clientTripId: string;
  xpAwarded: number;
  level: number;
  title: string;
  adventureScore: number;
  driverRating: number;
  qualityScore: number;
  explorationScore: number;
  activityScore: number;
  integrity: number;
  competitiveEligible: boolean;
  unlockedAchievements: { id: string; title: string; rarity: string; xpBonus: number }[];
  challengeProgress: {
    id: string;
    title: string;
    progress: number;
    target: number;
    completed: boolean;
  }[];
  streakDays: number;
  weightsVersion: number;
};

@Injectable()
export class ProgressionService {
  // ponytail: in-memory progression for Phase 5; upgrade: Postgres xp_ledger + user_scores.
  private readonly users = new Map<string, UserProgress>();
  private readonly weights: ScoreWeights = SCORE_WEIGHTS_V1;

  getWeights() {
    return { ...this.weights };
  }

  getOrCreate(userId: string): UserProgress {
    let u = this.users.get(userId);
    if (!u) {
      u = {
        xpTotal: 0,
        xpLedger: [],
        adventureScore: 0,
        driverRating: 70,
        qualityScore: 0,
        explorationScore: 0,
        activityScore: 0,
        totalDistanceM: 0,
        tripCount: 0,
        totalCells: 0,
        streakDays: 0,
        lastDriveDay: null,
        unlockedAchievements: [],
        challengeProgress: {},
        lastTripAwards: null,
      };
      this.users.set(userId, u);
    }
    return u;
  }

  me(userId: string) {
    const u = this.getOrCreate(userId);
    const level = 1 + Math.floor(u.xpTotal / this.weights.levelXpStep);
    const into = u.xpTotal % this.weights.levelXpStep;
    return {
      xpTotal: u.xpTotal,
      level,
      title: titleForLevel(level),
      xpIntoLevel: into,
      xpForNextLevel: this.weights.levelXpStep,
      adventureScore: Math.round(u.adventureScore),
      driverRating: Math.round(u.driverRating),
      components: {
        quality: Math.round(u.qualityScore),
        exploration: Math.round(u.explorationScore),
        activity: Math.round(u.activityScore),
      },
      streakDays: u.streakDays,
      tripCount: u.tripCount,
      totalDistanceM: u.totalDistanceM,
      totalCells: u.totalCells,
      competitiveEligible:
        u.tripCount === 0 || (u.lastTripAwards?.competitiveEligible ?? true),
      weightsVersion: this.weights.version,
      lastTripAwards: u.lastTripAwards,
    };
  }

  achievementsMe(userId: string) {
    const u = this.getOrCreate(userId);
    return ACHIEVEMENTS.map((a) => ({
      ...a,
      unlocked: u.unlockedAchievements.includes(a.id),
    }));
  }

  challengesMe(userId: string) {
    const u = this.getOrCreate(userId);
    const now = Date.now();
    return CHALLENGES.map((c) => {
      const key = c.period === 'daily' ? dayKey(now) : weekKey(now);
      const prog = u.challengeProgress[c.id];
      const samePeriod = prog?.periodKey === key;
      const value = samePeriod ? prog.value : 0;
      const completed = samePeriod ? prog.completed : false;
      return {
        ...c,
        progress: value,
        completed,
        periodKey: key,
      };
    });
  }

  /**
   * Server-authoritative finalize. Client qualityScore / explorationXp are hints only —
   * competitive components are recomputed here (S2-01).
   */
  finalizeTrip(userId: string, tripId: string, body: TripUpsertBody, integrity: number) {
    const u = this.getOrCreate(userId);
    const w = this.weights;

    const hardBrakes = (body.events ?? []).filter((e) => e.type === 'BRAKING').length;
    const distanceM = body.distanceM ?? 0;
    const newCells = body.newCells ?? 0;
    const durationMs = body.durationMs ?? 0;

    // Ignore body.qualityScore for competitive score
    let quality = 92 - Math.min(30, hardBrakes * 4);
    if (distanceM < 200) quality -= 5;
    if ((body.pointCount ?? 0) < 5) quality -= 8;
    quality = Math.max(40, Math.min(99, quality));

    const exploration = Math.min(100, newCells * 8 + Math.min(20, distanceM / 500));
    const activity = Math.min(
      100,
      distanceM / 100 + Math.min(30, durationMs / 60000) * 2,
    );

    const adventure =
      quality * w.quality + exploration * w.exploration + activity * w.activity;
    // Driver rating leans on quality, not raw adventure/speed
    const driverRating = quality * 0.75 + Math.min(25, distanceM / 2000);

    let xp =
      Math.round(quality * w.xpPerQualityPoint) +
      Math.round(exploration * w.xpPerExplorationPoint) +
      Math.round(activity * w.xpPerActivityPoint) +
      Math.round((distanceM / 1000) * w.xpPerKm) +
      newCells * w.xpPerNewCell;

    // Client-sent explorationXp must not inflate awards
    void body.explorationXp;
    void body.qualityScore;

    u.qualityScore = (u.qualityScore * u.tripCount + quality) / (u.tripCount + 1 || 1);
    u.explorationScore =
      (u.explorationScore * u.tripCount + exploration) / (u.tripCount + 1 || 1);
    u.activityScore = (u.activityScore * u.tripCount + activity) / (u.tripCount + 1 || 1);
    u.adventureScore = (u.adventureScore * u.tripCount + adventure) / (u.tripCount + 1 || 1);
    u.driverRating = (u.driverRating * u.tripCount + driverRating) / (u.tripCount + 1 || 1);
    u.totalDistanceM += distanceM;
    u.totalCells += newCells;
    u.tripCount += 1;

    const today = dayKey(body.endAtMs ?? Date.now());
    if (u.lastDriveDay) {
      const prev = new Date(u.lastDriveDay + 'T00:00:00Z').getTime();
      const cur = new Date(today + 'T00:00:00Z').getTime();
      const diffDays = Math.round((cur - prev) / 86400000);
      if (diffDays === 0) {
        // same day
      } else if (diffDays === 1) {
        u.streakDays += 1;
      } else {
        u.streakDays = 1;
      }
    } else {
      u.streakDays = 1;
    }
    u.lastDriveDay = today;

    const unlockedNow: TripAwards['unlockedAchievements'] = [];
    const unlock = (id: string) => {
      if (u.unlockedAchievements.includes(id)) return;
      const def = ACHIEVEMENTS.find((a) => a.id === id);
      if (!def) return;
      u.unlockedAchievements.push(id);
      xp += def.xpBonus;
      unlockedNow.push({
        id: def.id,
        title: def.title,
        rarity: def.rarity,
        xpBonus: def.xpBonus,
      });
    };

    if (u.tripCount >= 1) unlock('first_ascent');
    if (u.totalCells >= 10) unlock('explorer_10');
    if (quality >= 90) unlock('smooth_90');
    if (u.streakDays >= 3) unlock('streak_3');

    const challengeSnap: TripAwards['challengeProgress'] = [];
    for (const c of CHALLENGES) {
      const pKey = c.period === 'daily' ? dayKey(Date.now()) : weekKey(Date.now());
      let prog = u.challengeProgress[c.id];
      if (!prog || prog.periodKey !== pKey) {
        prog = { value: 0, samples: 0, completed: false, periodKey: pKey };
        u.challengeProgress[c.id] = prog;
      }
      if (!prog.completed) {
        switch (c.metric) {
          case 'distance_m':
            prog.value += distanceM;
            break;
          case 'trips':
            prog.value += 1;
            break;
          case 'new_cells':
            prog.value += newCells;
            break;
          case 'quality_avg':
            prog.samples += 1;
            prog.value = (prog.value * (prog.samples - 1) + quality) / prog.samples;
            break;
        }
        if (prog.value >= c.target) {
          prog.completed = true;
          xp += c.xpReward;
        }
      }
      challengeSnap.push({
        id: c.id,
        title: c.title,
        progress: Math.min(c.target, prog.value),
        target: c.target,
        completed: prog.completed,
      });
    }

    u.xpTotal += xp;
    u.xpLedger.push({
      at: Date.now(),
      amount: xp,
      reason: 'trip_finalize',
      tripId,
    });

    const level = 1 + Math.floor(u.xpTotal / w.levelXpStep);
    const awards: TripAwards = {
      clientTripId: body.clientTripId,
      xpAwarded: xp,
      level,
      title: titleForLevel(level),
      adventureScore: Math.round(adventure),
      driverRating: Math.round(driverRating),
      qualityScore: Math.round(quality),
      explorationScore: Math.round(exploration),
      activityScore: Math.round(activity),
      integrity,
      competitiveEligible: integrity >= w.integrityCompetitiveMin,
      unlockedAchievements: unlockedNow,
      challengeProgress: challengeSnap,
      streakDays: u.streakDays,
      weightsVersion: w.version,
    };
    u.lastTripAwards = awards;
    return awards;
  }
}
