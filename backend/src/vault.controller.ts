import {
  BadRequestException,
  Body,
  Controller,
  Delete,
  ForbiddenException,
  Get,
  Headers,
  Injectable,
  NotFoundException,
  Param,
  Patch,
  Post,
  UnauthorizedException,
} from '@nestjs/common';
import { randomUUID } from 'crypto';
import { parseBearerUserId } from './auth.util';

export const SERVICE_TAXONOMY = [
  { id: 'oil_change', label: 'Oil change', category: 'maintenance' },
  { id: 'brake_pads', label: 'Brake pads', category: 'brakes' },
  { id: 'tire_rotation', label: 'Tire rotation', category: 'tires' },
  { id: 'battery', label: 'Battery', category: 'electrical' },
  { id: 'inspection', label: 'Inspection', category: 'inspection' },
  { id: 'ac_service', label: 'A/C service', category: 'comfort' },
  { id: 'other', label: 'Other', category: 'other' },
];

type Vehicle = {
  id: string;
  userId: string;
  make: string;
  model: string;
  year: number;
  variant?: string;
  powertrain?: string;
  nickname: string;
  vinMasked?: string;
  odometerKm: number;
  purchaseAtMs?: number;
  createdAtMs: number;
  active: boolean;
};

type OwnershipEvent = {
  id: string;
  vehicleId: string;
  atMs: number;
  type: string;
  label: string;
};

type VehicleDocument = {
  id: string;
  vehicleId: string;
  type: string;
  title: string;
  expiresAtMs?: number;
  uploadUrl: string;
  uploaded: boolean;
  createdAtMs: number;
};

type ServicePart = { name: string; brand?: string; qty: number; price: number };

type ServiceRecord = {
  id: string;
  vehicleId: string;
  clientServiceId: string;
  atMs: number;
  odometerKm: number;
  workshopName?: string;
  serviceTypeIds: string[];
  notes?: string;
  parts: ServicePart[];
  laborCost: number;
  partsCost: number;
  nextDueAtMs?: number;
  nextDueOdometerKm?: number;
  certified: boolean;
  syncStatus: string;
  source?: string;
  invoiceId?: string;
};

type HistoryShare = {
  id: string;
  token: string;
  vehicleId: string;
  userId: string;
  scope: string;
  expiresAtMs: number;
};

type Entitlement = { maxVehicles: number };

@Injectable()
export class VaultService {
  // ponytail: in-memory vault; ceiling = lost on restart. Upgrade: Postgres vehicles + R2.
  private readonly vehicles = new Map<string, Vehicle>();
  private readonly ownership = new Map<string, OwnershipEvent[]>();
  private readonly documents = new Map<string, VehicleDocument[]>();
  private readonly services = new Map<string, ServiceRecord[]>();
  private readonly shares = new Map<string, HistoryShare>();
  private readonly entitlements = new Map<string, Entitlement>();

  entitlement(userId: string): Entitlement {
    let e = this.entitlements.get(userId);
    if (!e) {
      e = { maxVehicles: 1 }; // DEC free tier
      this.entitlements.set(userId, e);
    }
    return e;
  }

  verifyPlayPurchase(userId: string, _purchaseToken: string, sku: string) {
    // ponytail: accept any token in Phase 7; upgrade: Google Play Developer API verify.
    if (sku !== 'meru_extra_vehicle_slot' && sku !== 'extra_vehicle') {
      throw new BadRequestException('Unknown SKU');
    }
    const e = this.entitlement(userId);
    e.maxVehicles += 1;
    return { maxVehicles: e.maxVehicles, verified: true };
  }

  listVehicles(userId: string) {
    const list = [...this.vehicles.values()].filter((v) => v.userId === userId);
    const ent = this.entitlement(userId);
    return {
      maxVehicles: ent.maxVehicles,
      vehicles: list.map((v) => this.publicVehicle(v)),
      canAdd: list.length < ent.maxVehicles,
    };
  }

  createVehicle(
    userId: string,
    body: {
      make: string;
      model: string;
      year: number;
      variant?: string;
      powertrain?: string;
      nickname?: string;
      vin?: string;
      odometerKm?: number;
      purchaseAtMs?: number;
    },
  ) {
    const list = [...this.vehicles.values()].filter((v) => v.userId === userId);
    const ent = this.entitlement(userId);
    if (list.length >= ent.maxVehicles) {
      throw new ForbiddenException({
        code: 'VEHICLE_SLOT_REQUIRED',
        message: 'Purchase an extra vehicle slot',
        maxVehicles: ent.maxVehicles,
      });
    }
    if (!body.make?.trim() || !body.model?.trim() || !body.year) {
      throw new BadRequestException('make, model, year required');
    }
    const id = randomUUID();
    const vehicle: Vehicle = {
      id,
      userId,
      make: body.make.trim(),
      model: body.model.trim(),
      year: body.year,
      variant: body.variant,
      powertrain: body.powertrain,
      nickname: body.nickname?.trim() || `${body.make} ${body.model}`,
      vinMasked: body.vin ? maskVin(body.vin) : undefined,
      odometerKm: body.odometerKm ?? 0,
      purchaseAtMs: body.purchaseAtMs,
      createdAtMs: Date.now(),
      active: list.length === 0,
    };
    this.vehicles.set(id, vehicle);
    this.ownership.set(id, [
      {
        id: randomUUID(),
        vehicleId: id,
        atMs: body.purchaseAtMs ?? Date.now(),
        type: 'ACQUIRED',
        label: 'Added to Meru garage',
      },
    ]);
    this.documents.set(id, []);
    this.services.set(id, []);
    return this.publicVehicle(vehicle);
  }

  getVehicle(userId: string, vehicleId: string) {
    return this.publicVehicle(this.requireOwned(userId, vehicleId));
  }

  patchVehicle(
    userId: string,
    vehicleId: string,
    body: Partial<{ nickname: string; odometerKm: number; active: boolean }>,
  ) {
    const v = this.requireOwned(userId, vehicleId);
    if (body.nickname != null) v.nickname = body.nickname;
    if (body.odometerKm != null) v.odometerKm = body.odometerKm;
    if (body.active === true) {
      for (const other of this.vehicles.values()) {
        if (other.userId === userId) other.active = other.id === vehicleId;
      }
    }
    return this.publicVehicle(v);
  }

  deleteVehicle(userId: string, vehicleId: string) {
    this.requireOwned(userId, vehicleId);
    this.vehicles.delete(vehicleId);
    this.ownership.delete(vehicleId);
    this.documents.delete(vehicleId);
    this.services.delete(vehicleId);
    return { ok: true };
  }

  addOwnership(
    userId: string,
    vehicleId: string,
    body: { type: string; label: string; atMs?: number },
  ) {
    this.requireOwned(userId, vehicleId);
    const ev: OwnershipEvent = {
      id: randomUUID(),
      vehicleId,
      atMs: body.atMs ?? Date.now(),
      type: body.type || 'NOTE',
      label: body.label || 'Ownership update',
    };
    const list = this.ownership.get(vehicleId) ?? [];
    list.push(ev);
    this.ownership.set(vehicleId, list);
    return ev;
  }

  createDocument(
    userId: string,
    vehicleId: string,
    body: { type: string; title: string; expiresAtMs?: number },
  ) {
    this.requireOwned(userId, vehicleId);
    const id = randomUUID();
    // ponytail: signed URL stub (not real R2). Client "uploads" then confirms.
    const uploadUrl = `https://upload.meru.local/vault/${vehicleId}/${id}?sig=dev`;
    const doc: VehicleDocument = {
      id,
      vehicleId,
      type: body.type || 'other',
      title: body.title || 'Document',
      expiresAtMs: body.expiresAtMs,
      uploadUrl,
      uploaded: false,
      createdAtMs: Date.now(),
    };
    const list = this.documents.get(vehicleId) ?? [];
    list.push(doc);
    this.documents.set(vehicleId, list);
    return { ...doc, uploadUrl };
  }

  confirmDocumentUpload(userId: string, vehicleId: string, docId: string) {
    this.requireOwned(userId, vehicleId);
    const doc = (this.documents.get(vehicleId) ?? []).find((d) => d.id === docId);
    if (!doc) throw new NotFoundException('Document not found');
    doc.uploaded = true;
    return doc;
  }

  addService(
    userId: string,
    vehicleId: string,
    body: {
      clientServiceId: string;
      atMs?: number;
      odometerKm?: number;
      workshopName?: string;
      serviceTypeIds?: string[];
      notes?: string;
      parts?: ServicePart[];
      laborCost?: number;
      partsCost?: number;
      nextDueAtMs?: number;
      nextDueOdometerKm?: number;
    },
  ) {
    const v = this.requireOwned(userId, vehicleId);
    const existing = (this.services.get(vehicleId) ?? []).find(
      (s) => s.clientServiceId === body.clientServiceId,
    );
    if (existing) return { ...existing, duplicated: true };

    const parts = body.parts ?? [];
    const partsCost =
      body.partsCost ?? parts.reduce((sum, p) => sum + p.price * p.qty, 0);
    const laborCost = body.laborCost ?? 0;
    const rec: ServiceRecord = {
      id: randomUUID(),
      vehicleId,
      clientServiceId: body.clientServiceId,
      atMs: body.atMs ?? Date.now(),
      odometerKm: body.odometerKm ?? v.odometerKm,
      workshopName: body.workshopName,
      serviceTypeIds: body.serviceTypeIds ?? ['other'],
      notes: body.notes,
      parts,
      laborCost,
      partsCost,
      nextDueAtMs: body.nextDueAtMs,
      nextDueOdometerKm: body.nextDueOdometerKm,
      certified: false,
      syncStatus: 'synced',
    };
    if (body.odometerKm != null && body.odometerKm > v.odometerKm) {
      v.odometerKm = body.odometerKm;
    }
    const list = this.services.get(vehicleId) ?? [];
    list.push(rec);
    this.services.set(vehicleId, list);
    return { ...rec, duplicated: false };
  }

  /**
   * Owner-confirmed invoice writeback — only path that sets certified=true.
   * Idempotent on invoiceId.
   */
  writebackCertified(
    userId: string,
    vehicleId: string,
    body: {
      invoiceId: string;
      workshopName: string;
      serviceTypeIds: string[];
      parts?: ServicePart[];
      laborCost: number;
      partsCost: number;
      notes?: string;
      odometerKm?: number;
    },
  ) {
    const v = this.requireOwned(userId, vehicleId);
    const existing = (this.services.get(vehicleId) ?? []).find(
      (s) => s.invoiceId === body.invoiceId,
    );
    if (existing) return { ...existing, duplicated: true };

    const parts = body.parts ?? [];
    const rec: ServiceRecord = {
      id: randomUUID(),
      vehicleId,
      clientServiceId: `invoice:${body.invoiceId}`,
      atMs: Date.now(),
      odometerKm: body.odometerKm ?? v.odometerKm,
      workshopName: body.workshopName,
      serviceTypeIds: body.serviceTypeIds.length
        ? body.serviceTypeIds
        : ['other'],
      notes: body.notes,
      parts,
      laborCost: body.laborCost,
      partsCost: body.partsCost,
      certified: true,
      syncStatus: 'synced',
      source: 'mechanic_issued_bill',
      invoiceId: body.invoiceId,
    };
    const list = this.services.get(vehicleId) ?? [];
    list.push(rec);
    this.services.set(vehicleId, list);
    return { ...rec, duplicated: false };
  }

  /** Staff redeem — denies expired / wrong vehicle shares */
  redeemShare(token: string, vehicleId: string) {
    const share = this.assertShareValid(token, vehicleId);
    const v = this.vehicles.get(vehicleId);
    if (!v) throw new NotFoundException('Vehicle not found');
    const services = (this.services.get(vehicleId) ?? []).map((s) => ({
      id: s.id,
      atMs: s.atMs,
      workshopName: s.workshopName,
      serviceTypeIds: s.serviceTypeIds,
      odometerKm: s.odometerKm,
      certified: s.certified,
    }));
    return {
      scope: share.scope,
      vehicle: {
        id: v.id,
        make: v.make,
        model: v.model,
        year: v.year,
        odometerKm: v.odometerKm,
      },
      services,
    };
  }

  timeline(userId: string, vehicleId: string) {
    const v = this.requireOwned(userId, vehicleId);
    const items: {
      id: string;
      atMs: number;
      kind: string;
      title: string;
      subtitle?: string;
      meta?: Record<string, unknown>;
    }[] = [];

    for (const o of this.ownership.get(vehicleId) ?? []) {
      items.push({
        id: o.id,
        atMs: o.atMs,
        kind: 'ownership',
        title: o.label,
        subtitle: o.type,
      });
    }
    for (const d of this.documents.get(vehicleId) ?? []) {
      items.push({
        id: d.id,
        atMs: d.createdAtMs,
        kind: 'document',
        title: d.title,
        subtitle: d.uploaded ? d.type : `${d.type} · pending upload`,
        meta: { expiresAtMs: d.expiresAtMs, uploaded: d.uploaded },
      });
    }
    for (const s of this.services.get(vehicleId) ?? []) {
      items.push({
        id: s.id,
        atMs: s.atMs,
        kind: 'service',
        title: s.workshopName || 'Service',
        subtitle: `${s.odometerKm} km · ${money(s.laborCost + s.partsCost)}`,
        meta: {
          certified: s.certified,
          nextDueAtMs: s.nextDueAtMs,
          serviceTypeIds: s.serviceTypeIds,
          source: s.source,
          invoiceId: s.invoiceId,
        },
      });
    }
    // Trip links are client-merged; server stub notes trip association later
    items.sort((a, b) => b.atMs - a.atMs);

    const costs = (this.services.get(vehicleId) ?? []).reduce(
      (sum, s) => sum + s.laborCost + s.partsCost,
      0,
    );
    const nextDue = (this.services.get(vehicleId) ?? [])
      .map((s) => s.nextDueAtMs)
      .filter((x): x is number => !!x)
      .sort((a, b) => a - b)[0];

    return {
      vehicle: this.publicVehicle(v),
      summary: {
        lifetimeCost: costs,
        serviceVisits: (this.services.get(vehicleId) ?? []).length,
        documentCount: (this.documents.get(vehicleId) ?? []).length,
        nextDueAtMs: nextDue ?? null,
      },
      items,
    };
  }

  costs(userId: string, vehicleId: string) {
    this.requireOwned(userId, vehicleId);
    const services = this.services.get(vehicleId) ?? [];
    const byMonth = new Map<string, number>();
    for (const s of services) {
      const key = new Date(s.atMs).toISOString().slice(0, 7);
      byMonth.set(key, (byMonth.get(key) ?? 0) + s.laborCost + s.partsCost);
    }
    return {
      total: services.reduce((sum, s) => sum + s.laborCost + s.partsCost, 0),
      byMonth: [...byMonth.entries()]
        .map(([month, amount]) => ({ month, amount }))
        .sort((a, b) => a.month.localeCompare(b.month)),
    };
  }

  maintenanceDue(userId: string) {
    const out: {
      vehicleId: string;
      nickname: string;
      nextDueAtMs: number;
      nextDueOdometerKm?: number;
    }[] = [];
    for (const v of this.vehicles.values()) {
      if (v.userId !== userId) continue;
      for (const s of this.services.get(v.id) ?? []) {
        if (s.nextDueAtMs && s.nextDueAtMs > Date.now() - 86400000 * 7) {
          out.push({
            vehicleId: v.id,
            nickname: v.nickname,
            nextDueAtMs: s.nextDueAtMs,
            nextDueOdometerKm: s.nextDueOdometerKm,
          });
        }
      }
    }
    return { items: out.sort((a, b) => a.nextDueAtMs - b.nextDueAtMs) };
  }

  createShare(
    userId: string,
    vehicleId: string,
    body: { scope?: string; ttlHours?: number },
  ) {
    this.requireOwned(userId, vehicleId);
    const id = randomUUID();
    const token = randomUUID().replace(/-/g, '');
    const ttl = Math.min(168, Math.max(1, body.ttlHours ?? 24));
    const share: HistoryShare = {
      id,
      token,
      vehicleId,
      userId,
      scope: body.scope || 'timeline_readonly',
      expiresAtMs: Date.now() + ttl * 3600_000,
    };
    this.shares.set(token, share);
    return {
      id,
      token,
      scope: share.scope,
      expiresAtMs: share.expiresAtMs,
      // Phase 8 workshops redeem this
      redeemHint: `POST /v1/history-shares/${token}/redeem`,
    };
  }

  assertShareValid(token: string, vehicleId: string) {
    const share = this.shares.get(token);
    if (!share) throw new BadRequestException('Invalid history share token');
    if (share.vehicleId !== vehicleId) {
      throw new BadRequestException('Share token does not match vehicle');
    }
    if (share.expiresAtMs < Date.now()) {
      throw new BadRequestException('History share expired');
    }
    return share;
  }

  private requireOwned(userId: string, vehicleId: string): Vehicle {
    const v = this.vehicles.get(vehicleId);
    if (!v) throw new NotFoundException('Vehicle not found');
    if (v.userId !== userId) throw new ForbiddenException('Not your vehicle');
    return v;
  }

  private publicVehicle(v: Vehicle) {
    return {
      id: v.id,
      make: v.make,
      model: v.model,
      year: v.year,
      variant: v.variant,
      powertrain: v.powertrain,
      nickname: v.nickname,
      vinMasked: v.vinMasked,
      odometerKm: v.odometerKm,
      purchaseAtMs: v.purchaseAtMs,
      createdAtMs: v.createdAtMs,
      active: v.active,
    };
  }
}

function maskVin(vin: string): string {
  const clean = vin.replace(/\s/g, '');
  if (clean.length < 5) return '••••';
  return `${'•'.repeat(Math.max(0, clean.length - 4))}${clean.slice(-4)}`;
}

function money(n: number): string {
  return `PKR ${Math.round(n)}`;
}

@Controller('v1')
export class VaultController {
  constructor(private readonly vault: VaultService) {}

  @Get('service-types')
  serviceTypes() {
    return { items: SERVICE_TAXONOMY };
  }

  @Get('vehicles')
  list(@Headers('authorization') authorization?: string) {
    return this.vault.listVehicles(requireUser(authorization));
  }

  @Post('vehicles')
  create(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: Record<string, unknown>,
  ) {
    return this.vault.createVehicle(requireUser(authorization), body as never);
  }

  @Get('vehicles/:id')
  get(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.vault.getVehicle(requireUser(authorization), id);
  }

  @Patch('vehicles/:id')
  patch(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: Record<string, unknown>,
  ) {
    return this.vault.patchVehicle(requireUser(authorization), id, body as never);
  }

  @Delete('vehicles/:id')
  remove(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.vault.deleteVehicle(requireUser(authorization), id);
  }

  @Post('vehicles/:id/ownership-events')
  ownership(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: { type?: string; label?: string; atMs?: number },
  ) {
    return this.vault.addOwnership(requireUser(authorization), id, {
      type: body.type || 'NOTE',
      label: body.label || 'Update',
      atMs: body.atMs,
    });
  }

  @Post('vehicles/:id/documents')
  documents(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: { type?: string; title?: string; expiresAtMs?: number },
  ) {
    return this.vault.createDocument(requireUser(authorization), id, {
      type: body.type || 'other',
      title: body.title || 'Document',
      expiresAtMs: body.expiresAtMs,
    });
  }

  @Post('vehicles/:id/documents/:docId/confirm')
  confirmDoc(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Param('docId') docId: string,
  ) {
    return this.vault.confirmDocumentUpload(requireUser(authorization), id, docId);
  }

  @Post('vehicles/:id/services')
  services(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: Record<string, unknown>,
  ) {
    if (!body.clientServiceId) throw new BadRequestException('clientServiceId required');
    return this.vault.addService(requireUser(authorization), id, body as never);
  }

  @Get('vehicles/:id/timeline')
  timeline(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.vault.timeline(requireUser(authorization), id);
  }

  @Get('vehicles/:id/costs')
  costs(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.vault.costs(requireUser(authorization), id);
  }

  @Post('vehicles/:id/history-shares')
  share(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: { scope?: string; ttlHours?: number },
  ) {
    return this.vault.createShare(requireUser(authorization), id, body);
  }

  @Get('maintenance/due')
  due(@Headers('authorization') authorization?: string) {
    return this.vault.maintenanceDue(requireUser(authorization));
  }

  @Get('billing/entitlement')
  entitlement(@Headers('authorization') authorization?: string) {
    const userId = requireUser(authorization);
    return this.vault.entitlement(userId);
  }

  @Post('billing/play/verify')
  verify(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: { purchaseToken?: string; sku?: string },
  ) {
    const userId = requireUser(authorization);
    if (!body.purchaseToken || !body.sku) {
      throw new BadRequestException('purchaseToken and sku required');
    }
    return this.vault.verifyPlayPurchase(userId, body.purchaseToken, body.sku);
  }
}

function requireUser(authorization?: string): string {
  const userId = parseBearerUserId(authorization);
  if (!userId) throw new UnauthorizedException('Missing or invalid token');
  return userId;
}
