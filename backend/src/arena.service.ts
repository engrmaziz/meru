import { Injectable } from '@nestjs/common';
import { LaunchService } from './launch.service';
import { ProgressionService } from './progression.service';

export type GeoType = 'city' | 'province' | 'country' | 'global';

export type GeoNode = {
  id: string;
  type: GeoType;
  name: string;
  parentId: string | null;
};

/** Pilot geo hierarchy — Pakistan / Punjab / Lahore (Phase 6). */
export const GEO_TREE: GeoNode[] = [
  { id: 'global', type: 'global', name: 'Global', parentId: null },
  { id: 'pk', type: 'country', name: 'Pakistan', parentId: 'global' },
  { id: 'pk-pb', type: 'province', name: 'Punjab', parentId: 'pk' },
  { id: 'pk-pb-lhr', type: 'city', name: 'Lahore', parentId: 'pk-pb' },
];

export type Season = {
  id: string;
  name: string;
  startsAt: number;
  endsAt: number;
  primaryPeriod: 'season' | 'week' | 'month';
};

export type BoardEntry = {
  userId: string;
  displayName: string;
  rank: number;
  prevRank: number | null;
  score: number;
  level: number;
  driverRating: number;
  delta: number; // rank change: positive = climbed
};

type ArenaUser = {
  userId: string;
  displayName: string;
  homeCityId: string;
  boardOptIn: boolean;
  following: Set<string>;
  seasonScore: number;
  weekScore: number;
  lastQuality: number;
  lastRouteHash: string | null;
  cellsExplored: number;
};

type BoardKey = string; // seasonId:geoType:geoId:period

@Injectable()
export class ArenaService {
  // ponytail: in-memory boards; ceiling = single process. Upgrade: Redis top-N + Postgres leaderboard_entries.
  private readonly season: Season = {
    id: 'season-2026-s1',
    name: 'Ascent Season 1',
    startsAt: Date.parse('2026-01-01T00:00:00Z'),
    endsAt: Date.parse('2026-12-31T23:59:59Z'),
    primaryPeriod: 'season',
  };

  private readonly users = new Map<string, ArenaUser>();
  private readonly boards = new Map<BoardKey, BoardEntry[]>();

  constructor(
    private readonly progression: ProgressionService,
    private readonly launch: LaunchService,
  ) {
    this.seedBots();
  }

  currentSeason() {
    return { ...this.season, geo: GEO_TREE };
  }

  getFlags() {
    return this.launch.getFlags();
  }

  patchFlags(partial: Parameters<LaunchService['patchFlags']>[0]) {
    return this.launch.patchFlags(partial);
  }

  ensureUser(userId: string, displayName: string, homeCityId = 'pk-pb-lhr') {
    let u = this.users.get(userId);
    if (!u) {
      u = {
        userId,
        displayName,
        homeCityId,
        boardOptIn: true,
        following: new Set(),
        seasonScore: 0,
        weekScore: 0,
        lastQuality: 0,
        lastRouteHash: null,
        cellsExplored: 0,
      };
      this.users.set(userId, u);
    } else if (displayName) {
      u.displayName = displayName;
    }
    return u;
  }

  setPrivacy(userId: string, boardOptIn: boolean, displayName?: string) {
    const u = this.ensureUser(userId, displayName || 'Driver');
    u.boardOptIn = boardOptIn;
    this.reindexAll();
    return { boardOptIn: u.boardOptIn, homeCityId: u.homeCityId };
  }

  privacyMe(userId: string) {
    const u = this.ensureUser(userId, 'Driver');
    return {
      boardOptIn: u.boardOptIn,
      homeCityId: u.homeCityId,
      followingCount: u.following.size,
    };
  }

  follow(userId: string, targetId: string) {
    if (userId === targetId) return { ok: false };
    const u = this.ensureUser(userId, 'Driver');
    this.ensureUser(targetId, 'Driver');
    u.following.add(targetId);
    return { ok: true, following: [...u.following] };
  }

  unfollow(userId: string, targetId: string) {
    const u = this.ensureUser(userId, 'Driver');
    u.following.delete(targetId);
    return { ok: true, following: [...u.following] };
  }

  /**
   * Called after trip finalize — dirty reindex for user's geos.
   */
  onTripFinalized(
    userId: string,
    displayName: string,
    adventureScore: number,
    quality: number,
    newCells: number,
    competitiveEligible: boolean,
    routeHash: string | null,
  ) {
    const u = this.ensureUser(userId, displayName);
    if (!competitiveEligible) {
      u.lastQuality = quality;
      u.lastRouteHash = routeHash;
      return;
    }
    u.seasonScore += adventureScore;
    u.weekScore += adventureScore;
    u.lastQuality = quality;
    u.lastRouteHash = routeHash;
    u.cellsExplored += newCells;
    this.reindexAll();
  }

  board(
    viewerId: string,
    geoType: GeoType,
    geoId: string,
    period: string = 'season',
  ) {
    if (!this.launch.getFlags().s2Leaderboards) {
      return { disabled: true, entries: [], you: null, seasonId: this.season.id };
    }
    const key = this.key(this.season.id, geoType, geoId, period);
    let entries = this.boards.get(key);
    if (!entries) {
      this.reindexAll();
      entries = this.boards.get(key) ?? [];
    }
    const visible = entries.filter((e) => {
      const u = this.users.get(e.userId);
      return u?.boardOptIn !== false;
    });
    // re-rank after opt-out filter
    const ranked = visible.map((e, i) => ({ ...e, rank: i + 1 }));
    const youRaw = ranked.find((e) => e.userId === viewerId) ?? null;
    const you = youRaw
      ? youRaw
      : this.privacyHiddenYou(viewerId, geoType, geoId, period);

    return {
      disabled: false,
      seasonId: this.season.id,
      seasonName: this.season.name,
      geoType,
      geoId,
      geoName: GEO_TREE.find((g) => g.id === geoId)?.name ?? geoId,
      period,
      entries: ranked.slice(0, 50),
      you,
      podium: ranked.slice(0, 3),
    };
  }

  ranksMe(userId: string) {
    const u = this.ensureUser(userId, 'Driver');
    const geos: { geoType: GeoType; geoId: string }[] = [
      { geoType: 'city', geoId: u.homeCityId },
      { geoType: 'province', geoId: 'pk-pb' },
      { geoType: 'country', geoId: 'pk' },
      { geoType: 'global', geoId: 'global' },
    ];
    return {
      seasonId: this.season.id,
      boardOptIn: u.boardOptIn,
      ranks: geos.map((g) => {
        const b = this.board(userId, g.geoType, g.geoId, 'season');
        return {
          geoType: g.geoType,
          geoId: g.geoId,
          geoName: GEO_TREE.find((x) => x.id === g.geoId)?.name ?? g.geoId,
          rank: b.you?.rank ?? null,
          score: b.you?.score ?? 0,
          delta: b.you?.delta ?? 0,
        };
      }),
    };
  }

  friendsBoard(userId: string) {
    const u = this.ensureUser(userId, 'Driver');
    const ids = new Set([userId, ...u.following]);
    const rows: BoardEntry[] = [];
    for (const id of ids) {
      const f = this.users.get(id);
      if (!f || !f.boardOptIn) continue;
      const prog = this.progression.me(id);
      rows.push({
        userId: id,
        displayName: f.displayName,
        rank: 0,
        prevRank: null,
        score: Math.round(f.seasonScore || prog.adventureScore),
        level: prog.level,
        driverRating: prog.driverRating,
        delta: 0,
      });
    }
    rows.sort((a, b) => b.score - a.score);
    const ranked = rows.map((e, i) => ({ ...e, rank: i + 1 }));
    return {
      seasonId: this.season.id,
      entries: ranked,
      you: ranked.find((e) => e.userId === userId) ?? null,
    };
  }

  explorationMap(userId: string) {
    const u = this.ensureUser(userId, 'Driver');
    const prog = this.progression.me(userId);
    // Pilot city cell budget (~150m geohash7 over Lahore metro approx)
    const cityCellBudget = 2500;
    const cells = Math.max(u.cellsExplored, prog.totalCells);
    const pct = Math.min(100, (cells / cityCellBudget) * 100);
    return {
      cityId: u.homeCityId,
      cityName: 'Lahore',
      cellsExplored: cells,
      cityCellBudget,
      cityPercent: Math.round(pct * 10) / 10,
      // Sample cell ids for map overlay (client merges with local Room cells)
      sampleCells: this.syntheticCells(cells),
    };
  }

  ghostCompare(userId: string, routeHash: string | null, quality: number) {
    if (!this.launch.getFlags().ghostDriver) {
      return { enabled: false };
    }
    const u = this.ensureUser(userId, 'Driver');
    if (!routeHash || !u.lastRouteHash || routeHash !== u.lastRouteHash) {
      return {
        enabled: true,
        matched: false,
        message: 'No prior ghost on this route yet — drive it again for a quality compare.',
      };
    }
    const delta = quality - u.lastQuality;
    return {
      enabled: true,
      matched: true,
      priorQuality: u.lastQuality,
      currentQuality: quality,
      delta,
      message:
        delta >= 0
          ? `Ghost beaten by ${delta} quality points`
          : `Ghost ahead by ${Math.abs(delta)} — smoother next time`,
    };
  }

  shareCard(userId: string) {
    const u = this.ensureUser(userId, 'Driver');
    const ranks = this.ranksMe(userId);
    const prog = this.progression.me(userId);
    // Privacy-safe: no exact coords, no email
    return {
      displayName: u.boardOptIn ? u.displayName : 'Meru Driver',
      title: prog.title,
      level: prog.level,
      adventureScore: prog.adventureScore,
      cityRank: ranks.ranks.find((r) => r.geoType === 'city')?.rank ?? null,
      seasonName: this.season.name,
      cityName: 'Lahore',
      tagline: 'Every drive becomes an ascent.',
    };
  }

  private privacyHiddenYou(
    viewerId: string,
    _geoType: GeoType,
    _geoId: string,
    _period: string,
  ): BoardEntry | null {
    const u = this.users.get(viewerId);
    if (!u || u.boardOptIn) return null;
    return {
      userId: viewerId,
      displayName: u.displayName,
      rank: 0,
      prevRank: null,
      score: Math.round(u.seasonScore),
      level: this.progression.me(viewerId).level,
      driverRating: this.progression.me(viewerId).driverRating,
      delta: 0,
    };
  }

  private reindexAll() {
    const periods = ['season', 'week'] as const;
    const geos = GEO_TREE;
    for (const geo of geos) {
      for (const period of periods) {
        const key = this.key(this.season.id, geo.type, geo.id, period);
        const prev = new Map(
          (this.boards.get(key) ?? []).map((e) => [e.userId, e.rank]),
        );
        const candidates: ArenaUser[] = [];
        for (const u of this.users.values()) {
          if (!u.boardOptIn) continue;
          if (!this.userInGeo(u, geo)) continue;
          candidates.push(u);
        }
        candidates.sort((a, b) => {
          const sa = period === 'week' ? a.weekScore : a.seasonScore;
          const sb = period === 'week' ? b.weekScore : b.seasonScore;
          return sb - sa;
        });
        const entries: BoardEntry[] = candidates.map((u, i) => {
          const score = period === 'week' ? u.weekScore : u.seasonScore;
          const rank = i + 1;
          const prevRank = prev.get(u.userId) ?? null;
          const delta = prevRank == null ? 0 : prevRank - rank;
          const prog = this.progression.me(u.userId);
          return {
            userId: u.userId,
            displayName: u.displayName,
            rank,
            prevRank,
            score: Math.round(score),
            level: prog.level,
            driverRating: prog.driverRating,
            delta,
          };
        });
        this.boards.set(key, entries);
      }
    }
  }

  private userInGeo(u: ArenaUser, geo: GeoNode): boolean {
    if (geo.type === 'global') return true;
    if (geo.type === 'country') return u.homeCityId.startsWith(geo.id);
    if (geo.type === 'province') return u.homeCityId.startsWith(geo.id);
    if (geo.type === 'city') return u.homeCityId === geo.id;
    return false;
  }

  private key(seasonId: string, geoType: string, geoId: string, period: string): BoardKey {
    return `${seasonId}:${geoType}:${geoId}:${period}`;
  }

  private syntheticCells(n: number): string[] {
    // Stable fake geohash7 samples for map tint when Room has fewer cells
    const out: string[] = [];
    const base = 'ttsf3';
    for (let i = 0; i < Math.min(40, n); i++) {
      out.push(`${base}${i.toString(36).padStart(2, '0')}`.slice(0, 7));
    }
    return out;
  }

  private seedBots() {
    const bots = [
      { id: 'bot-aisha', name: 'Aisha R.', score: 420 },
      { id: 'bot-hamza', name: 'Hamza K.', score: 380 },
      { id: 'bot-sara', name: 'Sara M.', score: 350 },
      { id: 'bot-bilal', name: 'Bilal T.', score: 310 },
      { id: 'bot-noor', name: 'Noor F.', score: 290 },
    ];
    for (const b of bots) {
      const u = this.ensureUser(b.id, b.name);
      u.seasonScore = b.score;
      u.weekScore = Math.round(b.score * 0.3);
      u.boardOptIn = true;
      u.cellsExplored = 20 + Math.floor(b.score / 20);
    }
    this.reindexAll();
  }
}
