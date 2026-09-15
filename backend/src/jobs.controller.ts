import {
  BadRequestException,
  Body,
  Controller,
  ForbiddenException,
  Get,
  Headers,
  Inject,
  Injectable,
  NotFoundException,
  Param,
  Post,
  UnauthorizedException,
  forwardRef,
} from '@nestjs/common';
import { randomUUID } from 'crypto';
import { parseBearerUserId } from './auth.util';
import { SERVICE_TAXONOMY, VaultService } from './vault.controller';
import { WorkshopsService } from './workshops.controller';

export type JobStatus =
  | 'confirmed'
  | 'checked_in'
  | 'in_progress'
  | 'billed'
  | 'closed'
  | 'disputed';

type ExtraWork = {
  id: string;
  description: string;
  estimatedCost: number;
  status: 'pending' | 'approved' | 'denied';
};

type InvoiceLine = {
  kind: 'labor' | 'parts' | 'fee';
  label: string;
  qty: number;
  unitPrice: number;
  serviceTypeId?: string;
};

type Invoice = {
  id: string;
  jobId: string;
  userId: string;
  vehicleId: string;
  workshopId: string;
  lines: InvoiceLine[];
  laborTotal: number;
  partsTotal: number;
  feesTotal: number;
  total: number;
  pdfUrl: string;
  status: 'issued' | 'confirmed' | 'disputed';
  createdAtMs: number;
  confirmedAtMs?: number;
  serviceRecordId?: string;
};

type Job = {
  id: string;
  bookingId: string;
  userId: string;
  workshopId: string;
  vehicleId: string;
  serviceIds: string[];
  historyShareToken?: string;
  status: JobStatus;
  extras: ExtraWork[];
  invoiceId?: string;
  createdAtMs: number;
  checkedInAtMs?: number;
};

type Review = {
  id: string;
  jobId: string;
  userId: string;
  workshopId: string;
  rating: number;
  comment?: string;
  createdAtMs: number;
};

@Injectable()
export class JobsService {
  // ponytail: in-memory jobs/invoices; ceiling = single process. Upgrade: Postgres + R2 PDF.
  private readonly jobs = new Map<string, Job>();
  private readonly invoices = new Map<string, Invoice>();
  private readonly reviews = new Map<string, Review>();

  constructor(
    private readonly vault: VaultService,
    @Inject(forwardRef(() => WorkshopsService))
    private readonly workshops: WorkshopsService,
  ) {}

  createFromBooking(booking: {
    id: string;
    userId: string;
    workshopId: string;
    vehicleId: string;
    serviceIds: string[];
    historyShareToken?: string;
  }) {
    if ([...this.jobs.values()].some((j) => j.bookingId === booking.id)) {
      return;
    }
    const job: Job = {
      id: randomUUID(),
      bookingId: booking.id,
      userId: booking.userId,
      workshopId: booking.workshopId,
      vehicleId: booking.vehicleId,
      serviceIds: booking.serviceIds,
      historyShareToken: booking.historyShareToken,
      status: 'confirmed',
      extras: [],
      createdAtMs: Date.now(),
    };
    this.jobs.set(job.id, job);
    return job;
  }

  staffList(workshopId: string) {
    return {
      items: [...this.jobs.values()]
        .filter((j) => j.workshopId === workshopId)
        .sort((a, b) => b.createdAtMs - a.createdAtMs)
        .map((j) => this.publicJob(j)),
    };
  }

  ownerJobs(userId: string) {
    return {
      items: [...this.jobs.values()]
        .filter((j) => j.userId === userId)
        .sort((a, b) => b.createdAtMs - a.createdAtMs)
        .map((j) => this.publicJob(j)),
    };
  }

  get(jobId: string) {
    const job = this.requireJob(jobId);
    return this.publicJob(job);
  }

  checkIn(workshopId: string, jobId: string) {
    const job = this.requireStaffJob(workshopId, jobId);
    if (job.status !== 'confirmed' && job.status !== 'checked_in') {
      throw new BadRequestException('Job not ready for check-in');
    }
    job.status = 'checked_in';
    job.checkedInAtMs = Date.now();
    return this.publicJob(job);
  }

  start(workshopId: string, jobId: string) {
    const job = this.requireStaffJob(workshopId, jobId);
    if (job.status !== 'checked_in' && job.status !== 'in_progress') {
      throw new BadRequestException('Check in first');
    }
    job.status = 'in_progress';
    return this.publicJob(job);
  }

  sharedHistory(workshopId: string, jobId: string) {
    const job = this.requireStaffJob(workshopId, jobId);
    if (!job.historyShareToken) {
      throw new ForbiddenException('No history share on this job');
    }
    // assertShareValid throws if expired / mismatch — exit criterion
    return this.vault.redeemShare(job.historyShareToken, job.vehicleId);
  }

  proposeExtra(
    workshopId: string,
    jobId: string,
    body: { description: string; estimatedCost: number },
  ) {
    const job = this.requireStaffJob(workshopId, jobId);
    if (job.status !== 'in_progress' && job.status !== 'checked_in') {
      throw new BadRequestException('Job must be in progress');
    }
    if (!body.description?.trim()) throw new BadRequestException('description required');
    const extra: ExtraWork = {
      id: randomUUID(),
      description: body.description.trim(),
      estimatedCost: Math.max(0, body.estimatedCost ?? 0),
      status: 'pending',
    };
    job.extras.push(extra);
    job.status = 'in_progress';
    this.workshops.pushNotifPublic(job.userId, {
      type: 'extra_work',
      title: 'Extra work proposed',
      body: extra.description,
    });
    return extra;
  }

  decideExtra(userId: string, jobId: string, extraId: string, approve: boolean) {
    const job = this.requireOwnerJob(userId, jobId);
    const extra = job.extras.find((e) => e.id === extraId);
    if (!extra) throw new NotFoundException('Extra not found');
    if (extra.status !== 'pending') throw new BadRequestException('Already decided');
    extra.status = approve ? 'approved' : 'denied';
    return extra;
  }

  issueInvoice(
    workshopId: string,
    jobId: string,
    body: { lines?: InvoiceLine[] },
  ) {
    const job = this.requireStaffJob(workshopId, jobId);
    if (job.status === 'closed' || job.status === 'disputed') {
      throw new BadRequestException('Job already closed');
    }
    if (job.invoiceId) {
      const existing = this.invoices.get(job.invoiceId);
      if (existing) return existing;
    }

    const pending = job.extras.filter((e) => e.status === 'pending');
    if (pending.length) {
      throw new BadRequestException('Resolve pending extras first');
    }

    const lines =
      body.lines?.length
        ? body.lines
        : defaultLines(job.serviceIds, job.extras.filter((e) => e.status === 'approved'));

    let laborTotal = 0;
    let partsTotal = 0;
    let feesTotal = 0;
    for (const line of lines) {
      const amount = line.qty * line.unitPrice;
      if (line.kind === 'labor') laborTotal += amount;
      else if (line.kind === 'parts') partsTotal += amount;
      else feesTotal += amount;
    }

    const id = randomUUID();
    const invoice: Invoice = {
      id,
      jobId: job.id,
      userId: job.userId,
      vehicleId: job.vehicleId,
      workshopId: job.workshopId,
      lines,
      laborTotal,
      partsTotal,
      feesTotal,
      total: laborTotal + partsTotal + feesTotal,
      // ponytail: R2 PDF stub
      pdfUrl: `https://r2.stub/meru/invoices/${id}.pdf`,
      status: 'issued',
      createdAtMs: Date.now(),
    };
    this.invoices.set(id, invoice);
    job.invoiceId = id;
    job.status = 'billed';
    this.workshops.pushNotifPublic(job.userId, {
      type: 'invoice_ready',
      title: 'Invoice ready',
      body: `PKR ${invoice.total} — review & confirm`,
    });
    return invoice;
  }

  getInvoice(invoiceId: string) {
    const inv = this.invoices.get(invoiceId);
    if (!inv) throw new NotFoundException('Invoice not found');
    return inv;
  }

  ownerInvoices(userId: string) {
    return {
      items: [...this.invoices.values()]
        .filter((i) => i.userId === userId)
        .sort((a, b) => b.createdAtMs - a.createdAtMs),
    };
  }

  /**
   * Owner confirm → certified vault writeback (server authority).
   * No writeback without confirm.
   */
  confirmInvoice(userId: string, invoiceId: string) {
    const inv = this.invoices.get(invoiceId);
    if (!inv || inv.userId !== userId) throw new NotFoundException('Invoice not found');
    if (inv.status === 'confirmed') return { invoice: inv, writeback: null, duplicated: true };
    if (inv.status === 'disputed') {
      throw new BadRequestException('Disputed invoice cannot be confirmed');
    }

    const job = this.requireJob(inv.jobId);
    const workshop = this.workshops.getWorkshop(job.workshopId);
    const serviceTypeIds = [
      ...new Set(
        inv.lines
          .map((l) => l.serviceTypeId)
          .filter((x): x is string => !!x)
          .concat(job.serviceIds),
      ),
    ];
    const parts = inv.lines
      .filter((l) => l.kind === 'parts')
      .map((l) => ({
        name: l.label,
        qty: l.qty,
        price: l.unitPrice,
      }));

    const writeback = this.vault.writebackCertified(userId, inv.vehicleId, {
      invoiceId: inv.id,
      workshopName: workshop?.name ?? 'Workshop',
      serviceTypeIds,
      parts,
      laborCost: inv.laborTotal,
      partsCost: inv.partsTotal,
      notes: `Invoice ${inv.id.slice(0, 8)} confirmed`,
    });

    inv.status = 'confirmed';
    inv.confirmedAtMs = Date.now();
    inv.serviceRecordId = writeback.id;
    job.status = 'closed';
    this.workshops.markBookingCompleted(job.bookingId);

    // Update workshop rating aggregate lightly after close (reviews bump later)
    return { invoice: inv, writeback, duplicated: writeback.duplicated };
  }

  disputeInvoice(userId: string, invoiceId: string, reason?: string) {
    const inv = this.invoices.get(invoiceId);
    if (!inv || inv.userId !== userId) throw new NotFoundException('Invoice not found');
    if (inv.status === 'confirmed') {
      throw new BadRequestException('Already confirmed — no dispute');
    }
    inv.status = 'disputed';
    const job = this.requireJob(inv.jobId);
    job.status = 'disputed';
    return { ...inv, reason: reason ?? 'owner_dispute' };
  }

  addReview(
    userId: string,
    body: { jobId: string; rating: number; comment?: string },
  ) {
    const job = this.requireOwnerJob(userId, body.jobId);
    if (job.status !== 'closed') {
      throw new BadRequestException('Review requires completed job');
    }
    if ([...this.reviews.values()].some((r) => r.jobId === job.id)) {
      throw new BadRequestException('Already reviewed');
    }
    const rating = Math.min(5, Math.max(1, Math.round(body.rating)));
    const review: Review = {
      id: randomUUID(),
      jobId: job.id,
      userId,
      workshopId: job.workshopId,
      rating,
      comment: body.comment,
      createdAtMs: Date.now(),
    };
    this.reviews.set(review.id, review);
    this.workshops.applyReviewRating(job.workshopId, rating);
    return review;
  }

  private publicJob(job: Job) {
    const workshop = this.workshops.getWorkshop(job.workshopId);
    const invoice = job.invoiceId ? this.invoices.get(job.invoiceId) : undefined;
    return {
      ...job,
      workshopName: workshop?.name,
      invoice: invoice
        ? {
            id: invoice.id,
            total: invoice.total,
            status: invoice.status,
            pdfUrl: invoice.pdfUrl,
          }
        : undefined,
    };
  }

  private requireJob(jobId: string) {
    const job = this.jobs.get(jobId);
    if (!job) throw new NotFoundException('Job not found');
    return job;
  }

  private requireStaffJob(workshopId: string, jobId: string) {
    const job = this.requireJob(jobId);
    if (job.workshopId !== workshopId) {
      throw new ForbiddenException('Not your workshop job');
    }
    return job;
  }

  private requireOwnerJob(userId: string, jobId: string) {
    const job = this.requireJob(jobId);
    if (job.userId !== userId) throw new ForbiddenException('Not your job');
    return job;
  }
}

@Controller('v1')
export class JobsController {
  constructor(private readonly jobs: JobsService) {}

  @Get('jobs/mine')
  mine(@Headers('authorization') authorization?: string) {
    return this.jobs.ownerJobs(requireUser(authorization));
  }

  @Get('jobs/:id')
  get(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    const userId = requireUser(authorization);
    const job = this.jobs.get(id);
    if (job.userId !== userId) {
      // staff may pass workshop header — checked below for staff routes
      throw new ForbiddenException('Not your job');
    }
    return job;
  }

  @Get('workshop-staff/jobs')
  staffJobs(
    @Headers('authorization') authorization: string | undefined,
    @Headers('x-workshop-id') workshopId: string | undefined,
  ) {
    requireUser(authorization);
    return this.jobs.staffList(requireWorkshop(workshopId));
  }

  @Post('jobs/:id/check-in')
  checkIn(
    @Headers('authorization') authorization: string | undefined,
    @Headers('x-workshop-id') workshopId: string | undefined,
    @Param('id') id: string,
  ) {
    requireUser(authorization);
    return this.jobs.checkIn(requireWorkshop(workshopId), id);
  }

  @Post('jobs/:id/start')
  start(
    @Headers('authorization') authorization: string | undefined,
    @Headers('x-workshop-id') workshopId: string | undefined,
    @Param('id') id: string,
  ) {
    requireUser(authorization);
    return this.jobs.start(requireWorkshop(workshopId), id);
  }

  @Get('jobs/:id/shared-history')
  sharedHistory(
    @Headers('authorization') authorization: string | undefined,
    @Headers('x-workshop-id') workshopId: string | undefined,
    @Param('id') id: string,
  ) {
    requireUser(authorization);
    return this.jobs.sharedHistory(requireWorkshop(workshopId), id);
  }

  @Post('jobs/:id/extras')
  proposeExtra(
    @Headers('authorization') authorization: string | undefined,
    @Headers('x-workshop-id') workshopId: string | undefined,
    @Param('id') id: string,
    @Body() body: { description: string; estimatedCost: number },
  ) {
    requireUser(authorization);
    return this.jobs.proposeExtra(requireWorkshop(workshopId), id, body);
  }

  @Post('jobs/:id/extras/:eid/decision')
  decideExtra(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Param('eid') eid: string,
    @Body() body: { approve: boolean },
  ) {
    return this.jobs.decideExtra(
      requireUser(authorization),
      id,
      eid,
      !!body.approve,
    );
  }

  @Post('invoices')
  issueInvoice(
    @Headers('authorization') authorization: string | undefined,
    @Headers('x-workshop-id') workshopId: string | undefined,
    @Body() body: { jobId: string; lines?: InvoiceLine[] },
  ) {
    requireUser(authorization);
    if (!body.jobId) throw new BadRequestException('jobId required');
    return this.jobs.issueInvoice(requireWorkshop(workshopId), body.jobId, {
      lines: body.lines,
    });
  }

  @Get('invoices')
  myInvoices(@Headers('authorization') authorization?: string) {
    return this.jobs.ownerInvoices(requireUser(authorization));
  }

  @Get('invoices/:id')
  getInvoice(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    const userId = requireUser(authorization);
    const inv = this.jobs.getInvoice(id);
    if (inv.userId !== userId) throw new ForbiddenException('Not your invoice');
    return inv;
  }

  @Post('invoices/:id/confirm')
  confirm(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.jobs.confirmInvoice(requireUser(authorization), id);
  }

  @Post('invoices/:id/dispute')
  dispute(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: { reason?: string },
  ) {
    return this.jobs.disputeInvoice(requireUser(authorization), id, body?.reason);
  }

  @Post('reviews')
  review(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: { jobId: string; rating: number; comment?: string },
  ) {
    if (!body.jobId || body.rating == null) {
      throw new BadRequestException('jobId and rating required');
    }
    return this.jobs.addReview(requireUser(authorization), body);
  }
}

function requireUser(authorization?: string): string {
  const userId = parseBearerUserId(authorization);
  if (!userId) throw new UnauthorizedException('Missing or invalid token');
  return userId;
}

function requireWorkshop(workshopId?: string): string {
  if (!workshopId?.trim()) {
    throw new BadRequestException('X-Workshop-Id header required for staff');
  }
  return workshopId.trim();
}

function defaultLines(
  serviceIds: string[],
  approvedExtras: ExtraWork[],
): InvoiceLine[] {
  const lines: InvoiceLine[] = serviceIds.map((id) => {
    const tax = SERVICE_TAXONOMY.find((s) => s.id === id);
    return {
      kind: 'labor' as const,
      label: tax?.label ?? id,
      qty: 1,
      unitPrice: id === 'oil_change' ? 3500 : 5000,
      serviceTypeId: id,
    };
  });
  lines.push({
    kind: 'parts',
    label: 'OEM filter kit',
    qty: 1,
    unitPrice: 2200,
    serviceTypeId: serviceIds[0],
  });
  for (const e of approvedExtras) {
    lines.push({
      kind: 'fee',
      label: e.description,
      qty: 1,
      unitPrice: e.estimatedCost,
    });
  }
  return lines;
}
