import { Test, TestingModule } from '@nestjs/testing';
import { ConflictException } from '@nestjs/common';
import { VaultService } from './vault.controller';
import { WorkshopsService } from './workshops.controller';

describe('WorkshopsService', () => {
  let workshops: WorkshopsService;
  let vault: VaultService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [VaultService, WorkshopsService],
    }).compile();
    workshops = module.get(WorkshopsService);
    vault = module.get(VaultService);
  });

  it('ranks Audi specialists higher for Audi make', () => {
    const list = workshops.list({
      make: 'Audi',
      lat: 31.52,
      lon: 74.35,
      verifiedOnly: true,
    });
    expect(list.items[0].brands).toContain('Audi');
    expect(list.items[0].kind).toBe('specialist');
    expect(list.items[0].brandFit).toBeGreaterThan(list.items[1].brandFit);
  });

  it('prevents double-book and frees slot after hold expiry', () => {
    const slots = workshops.slots('ws-audi-lhr');
    const slotId = slots.items[0].id;

    workshops.holdSlot('user-a', slotId);
    expect(() => workshops.holdSlot('user-b', slotId)).toThrow(ConflictException);

    // Force expiry
    const holdMap = (workshops as unknown as { holds: Map<string, { expiresAtMs: number }> }).holds;
    const hold = holdMap.get(slotId)!;
    hold.expiresAtMs = Date.now() - 1;
    workshops.releaseExpiredForTest();

    const again = workshops.holdSlot('user-b', slotId);
    expect(again.slotId).toBe(slotId);

    const vehicle = vault.createVehicle('user-b', {
      make: 'Audi',
      model: 'A4',
      year: 2019,
    });
    const booking = workshops.createBooking('user-b', {
      workshopId: 'ws-audi-lhr',
      vehicleId: vehicle.id,
      slotId,
      serviceIds: ['oil_change'],
    });
    expect(booking.historyShareToken).toBeTruthy();
    expect(booking.status).toBe('confirmed');

    expect(() =>
      workshops.createBooking('user-a', {
        workshopId: 'ws-audi-lhr',
        vehicleId: vehicle.id,
        slotId,
      }),
    ).toThrow();
  });
});
