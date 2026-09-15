import {
  BadRequestException,
  Body,
  ConflictException,
  Controller,
  Get,
  Headers,
  Injectable,
  NotFoundException,
  Param,
  Post,
  Query,
  UnauthorizedException,
} from '@nestjs/common';
import { randomUUID } from 'crypto';
import { parseBearerUserId } from './auth.util';
import { SERVICE_TAXONOMY, VaultService } from './vault.controller';

export type WorkshopKind = 'specialist' | 'multi' | 'general';

type Workshop = {
  id: string;
  name: string;
  kind: WorkshopKind;
  brands: string[];
  cityId: string;
  lat: number;
  lon: number;
  rating: number;
  jobsCompleted: number;
  kycStatus: 'pending' | 'verified';
  hours: { open: string; close: string };
  bays: number;
  serviceIds: string[];
  parts: { id: string; name: string; brand?: string; price: number }[];
  description: string;
};

type Slot = {
  id: string;
  workshopId: string;
  startAtMs: number;
  endAtMs: number;
  bay: number;
  status: 'open' | 'held' | 'booked';
};

type Hold = {
  slotId: string;
  userId: string;
  expiresAtMs: number;
};

type Booking = {
  id: string;
  userId: string;
  workshopId: string;
  vehicleId: string;
  slotId: string;
  serviceIds: string[];
  historyShareToken?: string;
  status: 'confirmed' | 'cancelled' | 'completed';
  createdAtMs: number;
  holdExpiresAtMs?: number;
};

type Notif = {
  id: string;
  userId: string;
  type: string;
  title: string;
  body: string;
  atMs: number;
  read: boolean;
};

const HOLD_TTL_MS = 120_000; // 2 minutes — Redis NX EX 120 equivalent

@Injectable()
export class WorkshopsService {
  // ponytail: in-memory workshops + holds; ceiling = single process. Upgrade: Postgres + Redis.
  private readonly workshops = new Map<string, Workshop>();
  private readonly slotById = new Map<string, Slot>();
  private readonly holds = new Map<string, Hold>(); // slotId → hold
  private readonly bookings = new Map<string, Booking>();
  private readonly notifications = new Map<string, Notif[]>();

  constructor(private readonly vault: VaultService) {
    this.seedLahore();
  }

  list(query: {
    make?: string;
    lat?: number;
    lon?: number;
    q?: string;
    verifiedOnly?: boolean;
  }) {
    this.expireHolds();
    let rows = [...this.workshops.values()];
    if (query.verifiedOnly) rows = rows.filter((w) => w.kycStatus === 'verified');
    if (query.q) {
      const q = query.q.toLowerCase();
      rows = rows.filter(
        (w) =>
          w.name.toLowerCase().includes(q) ||
          w.brands.some((b) => b.toLowerCase().includes(q)),
      );
    }
    const lat = query.lat ?? 31.5204;
    const lon = query.lon ?? 74.3587;
    const make = query.make?.trim();

    const ranked = rows
      .map((w) => {
        const distKm = haversineKm(lat, lon, w.lat, w.lon);
        const brandFit = make
          ? w.brands.some((b) => b.toLowerCase() === make.toLowerCase())
            ? w.kind === 'specialist'
              ? 3
              : w.kind === 'multi'
                ? 2
                : 1
            : 0
          : 0;
        return { workshop: w, distKm, brandFit };
      })
      .sort((a, b) => {
        if (b.brandFit !== a.brandFit) return b.brandFit - a.brandFit;
        if (a.distKm !== b.distKm) return a.distKm - b.distKm;
        return b.workshop.rating - a.workshop.rating;
      });

    return {
      items: ranked.map((r) => ({
        ...this.publicWorkshop(r.workshop),
        distanceKm: Math.round(r.distKm * 10) / 10,
        brandFit: r.brandFit,
      })),
    };
  }

  get(id: string) {
    const w = this.workshops.get(id);
    if (!w) throw new NotFoundException('Workshop not found');
    return {
      ...this.publicWorkshop(w),
      services: SERVICE_TAXONOMY.filter((s) => w.serviceIds.includes(s.id)),
      parts: w.parts,
      hours: w.hours,
      bays: w.bays,
    };
  }

  slots(workshopId: string, fromMs?: number) {
    this.expireHolds();
    const w = this.workshops.get(workshopId);
    if (!w) throw new NotFoundException('Workshop not found');
    const start = fromMs ?? Date.now();
    const end = start + 7 * 86400000;
    this.ensureSlots(w, start, end);
    const available = [...this.slotById.values()].filter(
      (s) =>
        s.workshopId === workshopId &&
        s.status === 'open' &&
        s.startAtMs >= start &&
        s.startAtMs <= end &&
        !this.holds.has(s.id),
    );
    return {
      items: available
        .sort((a, b) => a.startAtMs - b.startAtMs)
        .slice(0, 40)
        .map((s) => ({
          id: s.id,
          startAtMs: s.startAtMs,
          endAtMs: s.endAtMs,
          bay: s.bay,
        })),
    };
  }

  /**
   * Hold then confirm in one call for Phase 8 Android;
   * concurrency-safe via NX-style hold map.
   */
  createBooking(
    userId: string,
    body: {
      workshopId: string;
      vehicleId: string;
      slotId: string;
      serviceIds?: string[];
      historyShareToken?: string;
      vehicleMake?: string;
    },
  ) {
    this.expireHolds();
    const workshop = this.workshops.get(body.workshopId);
    if (!workshop) throw new NotFoundException('Workshop not found');
    const slot = this.slotById.get(body.slotId);
    if (!slot || slot.workshopId !== body.workshopId) {
      throw new NotFoundException('Slot not found');
    }
    if (slot.status === 'booked') {
      throw new ConflictException('Slot already booked');
    }

    const existingHold = this.holds.get(slot.id);
    if (existingHold && existingHold.userId !== userId) {
      throw new ConflictException('Slot held by another driver');
    }

    // Attach / validate history share from Phase 7
    let shareToken = body.historyShareToken;
    if (shareToken) {
      this.vault.assertShareValid(shareToken, body.vehicleId);
    } else {
      // Auto-create short share for booking (scope timeline_readonly)
      const created = this.vault.createShare(userId, body.vehicleId, {
        scope: 'timeline_readonly',
        ttlHours: 48,
      });
      shareToken = created.token;
    }

    // NX hold
    this.holds.set(slot.id, {
      slotId: slot.id,
      userId,
      expiresAtMs: Date.now() + HOLD_TTL_MS,
    });
    slot.status = 'held';

    // Confirm immediately for Phase 8 (hold proves exclusivity under concurrency)
    slot.status = 'booked';
    this.holds.delete(slot.id);

    const booking: Booking = {
      id: randomUUID(),
      userId,
      workshopId: body.workshopId,
      vehicleId: body.vehicleId,
      slotId: body.slotId,
      serviceIds: body.serviceIds?.length ? body.serviceIds : ['oil_change'],
      historyShareToken: shareToken,
      status: 'confirmed',
      createdAtMs: Date.now(),
    };
    this.bookings.set(booking.id, booking);

    this.pushNotif(userId, {
      type: 'booking_confirmed',
      title: 'Booking confirmed',
      body: `${workshop.name} · ${new Date(slot.startAtMs).toISOString()}`,
    });
    this.pushNotif(userId, {
      type: 'booking_reminder',
      title: 'Service reminder',
      body: `Reminder set for your visit at ${workshop.name}`,
    });

    return {
      ...booking,
      workshopName: workshop.name,
      startAtMs: slot.startAtMs,
      endAtMs: slot.endAtMs,
      holdTtlMs: HOLD_TTL_MS,
    };
  }

  /** Test helper: hold without confirm */
  holdSlot(userId: string, slotId: string) {
    this.expireHolds();
    const slot = this.slotById.get(slotId);
    if (!slot) throw new NotFoundException('Slot not found');
    if (slot.status === 'booked') throw new ConflictException('Slot already booked');
    const existing = this.holds.get(slotId);
    if (existing && existing.userId !== userId) {
      throw new ConflictException('Slot held by another driver');
    }
    this.holds.set(slotId, {
      slotId,
      userId,
      expiresAtMs: Date.now() + HOLD_TTL_MS,
    });
    slot.status = 'held';
    return { slotId, expiresAtMs: Date.now() + HOLD_TTL_MS, ttlMs: HOLD_TTL_MS };
  }

  releaseExpiredForTest() {
    this.expireHolds();
  }

  myBookings(userId: string) {
    return {
      items: [...this.bookings.values()]
        .filter((b) => b.userId === userId)
        .sort((a, b) => b.createdAtMs - a.createdAtMs)
        .map((b) => {
          const w = this.workshops.get(b.workshopId);
          const slot = this.slotById.get(b.slotId);
          return {
            ...b,
            workshopName: w?.name,
            startAtMs: slot?.startAtMs,
            endAtMs: slot?.endAtMs,
            statusLabel: b.status,
          };
        }),
    };
  }

  cancel(userId: string, bookingId: string) {
    const b = this.bookings.get(bookingId);
    if (!b || b.userId !== userId) throw new NotFoundException('Booking not found');
    if (b.status === 'cancelled') return b;
    b.status = 'cancelled';
    const slot = this.slotById.get(b.slotId);
    if (slot) slot.status = 'open';
    this.holds.delete(b.slotId);
    return b;
  }

  notifications(userId: string) {
    return { items: this.notifications.get(userId) ?? [] };
  }

  /** Exposed for concurrency unit tests */
  getSlot(slotId: string) {
    return this.slotById.get(slotId);
  }

  private expireHolds() {
    const now = Date.now();
    for (const [slotId, hold] of this.holds) {
      if (hold.expiresAtMs <= now) {
        this.holds.delete(slotId);
        const slot = this.slotById.get(slotId);
        if (slot && slot.status === 'held') slot.status = 'open';
      }
    }
  }

  private ensureSlots(w: Workshop, fromMs: number, toMs: number) {
    // Generate hourly slots for next days if missing
    const openH = parseInt(w.hours.open.split(':')[0], 10);
    const closeH = parseInt(w.hours.close.split(':')[0], 10);
    for (let t = startOfDay(fromMs); t < toMs; t += 86400000) {
      for (let h = openH; h < closeH; h++) {
        for (let bay = 1; bay <= w.bays; bay++) {
          const start = t + h * 3600000;
          if (start < fromMs) continue;
          const id = `${w.id}_${start}_${bay}`;
          if (this.slotById.has(id)) continue;
          this.slotById.set(id, {
            id,
            workshopId: w.id,
            startAtMs: start,
            endAtMs: start + 3600000,
            bay,
            status: 'open',
          });
        }
      }
    }
  }

  private pushNotif(
    userId: string,
    partial: { type: string; title: string; body: string },
  ) {
    const list = this.notifications.get(userId) ?? [];
    list.unshift({
      id: randomUUID(),
      userId,
      ...partial,
      atMs: Date.now(),
      read: false,
    });
    this.notifications.set(userId, list.slice(0, 50));
  }

  private publicWorkshop(w: Workshop) {
    return {
      id: w.id,
      name: w.name,
      kind: w.kind,
      brands: w.brands,
      cityId: w.cityId,
      lat: w.lat,
      lon: w.lon,
      rating: w.rating,
      jobsCompleted: w.jobsCompleted,
      kycStatus: w.kycStatus,
      verified: w.kycStatus === 'verified',
      description: w.description,
    };
  }

  private seedLahore() {
    const seeds: Workshop[] = [
      {
        id: 'ws-audi-lhr',
        name: 'Audi Peak Lahore',
        kind: 'specialist',
        brands: ['Audi'],
        cityId: 'pk-pb-lhr',
        lat: 31.4697,
        lon: 74.2728,
        rating: 4.8,
        jobsCompleted: 420,
        kycStatus: 'verified',
        hours: { open: '09:00', close: '18:00' },
        bays: 2,
        serviceIds: ['oil_change', 'brake_pads', 'inspection', 'ac_service'],
        parts: [
          { id: 'p1', name: 'OEM oil filter', brand: 'Audi', price: 4500 },
          { id: 'p2', name: 'Brake pad set', brand: 'Audi', price: 28000 },
        ],
        description: 'Brand specialist for Audi — certified techs.',
      },
      {
        id: 'ws-toyota-multi',
        name: 'Toyota & Friends Garage',
        kind: 'multi',
        brands: ['Toyota', 'Honda', 'Suzuki'],
        cityId: 'pk-pb-lhr',
        lat: 31.5102,
        lon: 74.3441,
        rating: 4.5,
        jobsCompleted: 890,
        kycStatus: 'verified',
        hours: { open: '08:00', close: '19:00' },
        bays: 3,
        serviceIds: ['oil_change', 'tire_rotation', 'battery', 'inspection'],
        parts: [{ id: 'p3', name: 'Oil 4L', brand: 'Castrol', price: 6500 }],
        description: 'Multi-brand bay with strong Toyota fit.',
      },
      {
        id: 'ws-general-gulberg',
        name: 'Gulberg General Motors',
        kind: 'general',
        brands: [],
        cityId: 'pk-pb-lhr',
        lat: 31.5304,
        lon: 74.3587,
        rating: 4.1,
        jobsCompleted: 1200,
        kycStatus: 'verified',
        hours: { open: '09:00', close: '17:00' },
        bays: 2,
        serviceIds: ['oil_change', 'tire_rotation', 'other'],
        parts: [{ id: 'p4', name: 'Air filter', price: 1200 }],
        description: 'General workshop — all makes welcome.',
      },
      {
        id: 'ws-bmw-pending',
        name: 'Bimmer Bay (KYC pending)',
        kind: 'specialist',
        brands: ['BMW'],
        cityId: 'pk-pb-lhr',
        lat: 31.482,
        lon: 74.3,
        rating: 4.6,
        jobsCompleted: 40,
        kycStatus: 'pending',
        hours: { open: '10:00', close: '18:00' },
        bays: 1,
        serviceIds: ['oil_change', 'inspection'],
        parts: [],
        description: 'New specialist — verification pending.',
      },
      {
        id: 'ws-honda-dha',
        name: 'DHA Honda Care',
        kind: 'specialist',
        brands: ['Honda'],
        cityId: 'pk-pb-lhr',
        lat: 31.4622,
        lon: 74.4105,
        rating: 4.4,
        jobsCompleted: 310,
        kycStatus: 'verified',
        hours: { open: '09:00', close: '18:00' },
        bays: 2,
        serviceIds: ['oil_change', 'brake_pads', 'ac_service'],
        parts: [{ id: 'p5', name: 'Cabin filter', brand: 'Honda', price: 2200 }],
        description: 'Honda-focused service lane in DHA.',
      },
    ];
    for (const w of seeds) this.workshops.set(w.id, w);
  }
}

@Controller('v1')
export class WorkshopsController {
  constructor(private readonly workshops: WorkshopsService) {}

  @Get('workshops')
  list(
    @Query('make') make?: string,
    @Query('lat') lat?: string,
    @Query('lon') lon?: string,
    @Query('q') q?: string,
    @Query('verifiedOnly') verifiedOnly?: string,
  ) {
    return this.workshops.list({
      make,
      lat: lat ? Number(lat) : undefined,
      lon: lon ? Number(lon) : undefined,
      q,
      verifiedOnly: verifiedOnly === '1' || verifiedOnly === 'true',
    });
  }

  @Get('workshops/:id')
  get(@Param('id') id: string) {
    return this.workshops.get(id);
  }

  @Get('workshops/:id/slots')
  slots(@Param('id') id: string, @Query('fromMs') fromMs?: string) {
    return this.workshops.slots(id, fromMs ? Number(fromMs) : undefined);
  }

  @Post('bookings')
  book(
    @Headers('authorization') authorization: string | undefined,
    @Body()
    body: {
      workshopId: string;
      vehicleId: string;
      slotId: string;
      serviceIds?: string[];
      historyShareToken?: string;
      vehicleMake?: string;
    },
  ) {
    const userId = requireUser(authorization);
    if (!body.workshopId || !body.vehicleId || !body.slotId) {
      throw new BadRequestException('workshopId, vehicleId, slotId required');
    }
    return this.workshops.createBooking(userId, body);
  }

  @Get('bookings')
  myBookings(@Headers('authorization') authorization?: string) {
    return this.workshops.myBookings(requireUser(authorization));
  }

  @Post('bookings/:id/cancel')
  cancel(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.workshops.cancel(requireUser(authorization), id);
  }

  @Get('notifications')
  notifications(@Headers('authorization') authorization?: string) {
    return this.workshops.notifications(requireUser(authorization));
  }
}

function requireUser(authorization?: string): string {
  const userId = parseBearerUserId(authorization);
  if (!userId) throw new UnauthorizedException('Missing or invalid token');
  return userId;
}

function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number) {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function startOfDay(ms: number) {
  const d = new Date(ms);
  return Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate());
}
