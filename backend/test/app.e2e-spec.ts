import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppModule } from './../src/app.module';

describe('Meru API (e2e)', () => {
  let app: INestApplication<App>;

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  afterEach(async () => {
    await app.close();
  });

  it('/health (GET)', () => {
    return request(app.getHttpServer()).get('/health').expect(200).expect((res) => {
      expect(res.body.status).toBe('ok');
    });
  });

  it('/v1/auth/register (POST)', () => {
    return request(app.getHttpServer())
      .post('/v1/auth/register')
      .send({ email: 'e2e@meru.app', password: 'secret1', displayName: 'E2E' })
      .expect(201)
      .expect((res) => {
        expect(res.body.accessToken).toBeTruthy();
      });
  });

  it('/v1/trips (POST) idempotent upsert', async () => {
    const auth = await request(app.getHttpServer())
      .post('/v1/auth/register')
      .send({ email: 'trips@meru.app', password: 'secret1', displayName: 'Trips' })
      .expect(201);

    const token = auth.body.accessToken as string;
    const payload = {
      clientTripId: 'client-trip-1',
      startAtMs: Date.now() - 60_000,
      endAtMs: Date.now(),
      distanceM: 3200,
      durationMs: 60_000,
      pointCount: 30,
      qualityScore: 90,
      locations: [{ ts: Date.now(), lat: 24.86, lon: 67.0 }],
      events: [{ ts: Date.now(), type: 'START', label: 'Drive started' }],
    };

    const first = await request(app.getHttpServer())
      .post('/v1/trips')
      .set('Authorization', `Bearer ${token}`)
      .send(payload)
      .expect(201);

    const second = await request(app.getHttpServer())
      .post('/v1/trips')
      .set('Authorization', `Bearer ${token}`)
      .send(payload)
      .expect(201);

    expect(first.body.duplicated).toBe(false);
    expect(first.body.awards.xpAwarded).toBeGreaterThan(0);
    expect(second.body.duplicated).toBe(true);
    expect(second.body.id).toBe(first.body.id);

    const scores = await request(app.getHttpServer())
      .get('/v1/scores/me')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);
    expect(scores.body.xpTotal).toBe(first.body.awards.xpAwarded);
    expect(scores.body.weightsVersion).toBe(1);
  });

  it('/v1/workshops brand-fit + booking share', async () => {
    const list = await request(app.getHttpServer())
      .get('/v1/workshops')
      .query({ make: 'Audi', lat: 31.52, lon: 74.35, verifiedOnly: 'true' })
      .expect(200);
    expect(list.body.items[0].brands).toContain('Audi');
    expect(list.body.items[0].kind).toBe('specialist');

    const auth = await request(app.getHttpServer())
      .post('/v1/auth/register')
      .send({ email: 'bay@meru.app', password: 'secret1', displayName: 'Bay' })
      .expect(201);
    const token = auth.body.accessToken as string;

    const vehicle = await request(app.getHttpServer())
      .post('/v1/vehicles')
      .set('Authorization', `Bearer ${token}`)
      .send({ make: 'Audi', model: 'A4', year: 2019 })
      .expect(201);

    const slots = await request(app.getHttpServer())
      .get(`/v1/workshops/${list.body.items[0].id}/slots`)
      .expect(200);
    expect(slots.body.items.length).toBeGreaterThan(0);

    const booking = await request(app.getHttpServer())
      .post('/v1/bookings')
      .set('Authorization', `Bearer ${token}`)
      .send({
        workshopId: list.body.items[0].id,
        vehicleId: vehicle.body.id,
        slotId: slots.body.items[0].id,
        serviceIds: ['oil_change'],
      })
      .expect(201);
    expect(booking.body.historyShareToken).toBeTruthy();
    expect(booking.body.status).toBe('confirmed');
  });
});
