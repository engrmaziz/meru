import { Test, TestingModule } from '@nestjs/testing';
import { BadRequestException, ForbiddenException } from '@nestjs/common';
import { JobsService } from './jobs.controller';
import { VaultService } from './vault.controller';
import { WorkshopsService } from './workshops.controller';

describe('JobsService writeback', () => {
  let jobs: JobsService;
  let vault: VaultService;
  let workshops: WorkshopsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [VaultService, WorkshopsService, JobsService],
    }).compile();
    jobs = module.get(JobsService);
    vault = module.get(VaultService);
    workshops = module.get(WorkshopsService);
  });

  function bookAndBill() {
    const vehicle = vault.createVehicle('owner', {
      make: 'Audi',
      model: 'A4',
      year: 2019,
    });
    const slots = workshops.slots('ws-audi-lhr');
    const booking = workshops.createBooking('owner', {
      workshopId: 'ws-audi-lhr',
      vehicleId: vehicle.id,
      slotId: slots.items[0].id,
      serviceIds: ['oil_change'],
    });
    const staffJobs = jobs.staffList('ws-audi-lhr').items;
    const job = staffJobs.find((j) => j.bookingId === booking.id)!;
    jobs.checkIn('ws-audi-lhr', job.id);
    jobs.start('ws-audi-lhr', job.id);
    const invoice = jobs.issueInvoice('ws-audi-lhr', job.id, {});
    return { vehicle, booking, job, invoice };
  }

  it('confirm writes certified taxonomy-mapped service into timeline', () => {
    const { vehicle, invoice } = bookAndBill();
    const result = jobs.confirmInvoice('owner', invoice.id);
    expect(result.writeback?.certified).toBe(true);
    expect(result.writeback?.serviceTypeIds).toContain('oil_change');
    expect(result.writeback?.source).toBe('mechanic_issued_bill');

    const tl = vault.timeline('owner', vehicle.id);
    const stamped = tl.items.find((i) => i.meta?.invoiceId === invoice.id);
    expect(stamped).toBeTruthy();
    expect(stamped?.meta?.certified).toBe(true);
  });

  it('no writeback without confirm; dispute blocks confirm', () => {
    const { vehicle, invoice } = bookAndBill();
    const before = vault.timeline('owner', vehicle.id).items.filter(
      (i) => i.kind === 'service' && i.meta?.certified,
    );
    expect(before).toHaveLength(0);

    jobs.disputeInvoice('owner', invoice.id, 'wrong parts');
    expect(() => jobs.confirmInvoice('owner', invoice.id)).toThrow(
      BadRequestException,
    );
    const after = vault.timeline('owner', vehicle.id).items.filter(
      (i) => i.kind === 'service' && i.meta?.certified,
    );
    expect(after).toHaveLength(0);
  });

  it('share expiry denies staff shared history', () => {
    const { job } = bookAndBill();
    // Force share expired via vault shares map
    const token = job.historyShareToken!;
    const shares = (vault as unknown as { shares: Map<string, { expiresAtMs: number }> })
      .shares;
    const share = shares.get(token)!;
    share.expiresAtMs = Date.now() - 1;

    expect(() => jobs.sharedHistory('ws-audi-lhr', job.id)).toThrow();
  });

  it('review requires completed job', () => {
    const { job, invoice } = bookAndBill();
    expect(() =>
      jobs.addReview('owner', { jobId: job.id, rating: 5 }),
    ).toThrow(BadRequestException);

    jobs.confirmInvoice('owner', invoice.id);
    const review = jobs.addReview('owner', {
      jobId: job.id,
      rating: 5,
      comment: 'Solid bay',
    });
    expect(review.rating).toBe(5);

    expect(() =>
      jobs.addReview('owner', { jobId: job.id, rating: 4 }),
    ).toThrow(BadRequestException);
  });

  it('staff cannot access another workshop job', () => {
    const { job } = bookAndBill();
    expect(() => jobs.checkIn('ws-toyota-multi', job.id)).toThrow(
      ForbiddenException,
    );
  });
});
