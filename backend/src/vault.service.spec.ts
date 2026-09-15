import { Test, TestingModule } from '@nestjs/testing';
import { ForbiddenException } from '@nestjs/common';
import { VaultService } from './vault.controller';

describe('VaultService', () => {
  let vault: VaultService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [VaultService],
    }).compile();
    vault = module.get(VaultService);
  });

  it('allows one free car and blocks the second until purchase', () => {
    const first = vault.createVehicle('u1', {
      make: 'Toyota',
      model: 'Corolla',
      year: 2018,
      nickname: 'Daily',
    });
    expect(first.id).toBeTruthy();
    expect(vault.listVehicles('u1').canAdd).toBe(false);

    expect(() =>
      vault.createVehicle('u1', {
        make: 'Honda',
        model: 'City',
        year: 2020,
      }),
    ).toThrow(ForbiddenException);

    const verified = vault.verifyPlayPurchase('u1', 'tok', 'meru_extra_vehicle_slot');
    expect(verified.maxVehicles).toBe(2);

    const second = vault.createVehicle('u1', {
      make: 'Honda',
      model: 'City',
      year: 2020,
    });
    expect(second.id).toBeTruthy();
    expect(vault.listVehicles('u1').vehicles).toHaveLength(2);
  });

  it('merges timeline and idempotent service sync', () => {
    const v = vault.createVehicle('u2', {
      make: 'Suzuki',
      model: 'Cultus',
      year: 2019,
    });
    vault.createDocument('u2', v.id, { type: 'registration', title: 'Reg book' });
    const s1 = vault.addService('u2', v.id, {
      clientServiceId: 'svc-1',
      laborCost: 2000,
      partsCost: 5000,
      serviceTypeIds: ['oil_change'],
      nextDueAtMs: Date.now() + 86400000 * 90,
    });
    const s2 = vault.addService('u2', v.id, {
      clientServiceId: 'svc-1',
      laborCost: 2000,
      partsCost: 5000,
    });
    expect(s1.duplicated).toBe(false);
    expect(s2.duplicated).toBe(true);

    const tl = vault.timeline('u2', v.id);
    expect(tl.items.some((i) => i.kind === 'service')).toBe(true);
    expect(tl.items.some((i) => i.kind === 'document')).toBe(true);
    expect(tl.items.some((i) => i.kind === 'ownership')).toBe(true);
    expect(tl.summary.lifetimeCost).toBe(7000);
  });
});
