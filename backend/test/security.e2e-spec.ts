import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppModule } from './../src/app.module';

/**
 * Phase 10 security regression suite (Strix stand-in for local/CI).
 * Covers IDOR + admin lock + share expiry proxies from Phase 9 checklist.
 */
describe('Summit security regressions (e2e)', () => {
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

  async function register(email: string) {
    const res = await request(app.getHttpServer())
      .post('/v1/auth/register')
      .send({ email, password: 'secret1', displayName: email.split('@')[0] })
      .expect(201);
    return res.body as { accessToken: string; user: { id: string } };
  }

  it('health reports phase 10 + Lahore soft launch', async () => {
    const res = await request(app.getHttpServer()).get('/health').expect(200);
    expect(res.body.status).toBe('ok');
    expect(res.body.phase).toBe(10);
    expect(res.body.softLaunch.cityId).toBe('pk-pb-lhr');
  });

  it('admin flags require X-Admin-Key', async () => {
    await request(app.getHttpServer()).get('/v1/admin/flags').expect(403);
    const ok = await request(app.getHttpServer())
      .get('/v1/admin/flags')
      .set('X-Admin-Key', 'meru-dev-admin')
      .expect(200);
    expect(ok.body.softLaunchCityId).toBe('pk-pb-lhr');
  });

  it('blocks invoice IDOR across users', async () => {
    const a = await register('idor-a@meru.app');
    const b = await register('idor-b@meru.app');

    const vehicle = await request(app.getHttpServer())
      .post('/v1/vehicles')
      .set('Authorization', `Bearer ${a.accessToken}`)
      .send({ make: 'Audi', model: 'A4', year: 2019 })
      .expect(201);

    const workshops = await request(app.getHttpServer())
      .get('/v1/workshops')
      .query({ make: 'Audi' })
      .expect(200);
    const wsId = workshops.body.items[0].id as string;
    const slots = await request(app.getHttpServer())
      .get(`/v1/workshops/${wsId}/slots`)
      .expect(200);

    await request(app.getHttpServer())
      .post('/v1/bookings')
      .set('Authorization', `Bearer ${a.accessToken}`)
      .send({
        workshopId: wsId,
        vehicleId: vehicle.body.id,
        slotId: slots.body.items[0].id,
      })
      .expect(201);

    const jobs = await request(app.getHttpServer())
      .get('/v1/jobs/mine')
      .set('Authorization', `Bearer ${a.accessToken}`)
      .expect(200);
    const jobId = jobs.body.items[0].id as string;

    await request(app.getHttpServer())
      .post(`/v1/jobs/${jobId}/check-in`)
      .set('Authorization', `Bearer ${a.accessToken}`)
      .set('X-Workshop-Id', wsId)
      .expect(201);

    const invoice = await request(app.getHttpServer())
      .post('/v1/invoices')
      .set('Authorization', `Bearer ${a.accessToken}`)
      .set('X-Workshop-Id', wsId)
      .send({ jobId })
      .expect(201);

    await request(app.getHttpServer())
      .get(`/v1/invoices/${invoice.body.id}`)
      .set('Authorization', `Bearer ${b.accessToken}`)
      .expect(403);

    await request(app.getHttpServer())
      .post(`/v1/invoices/${invoice.body.id}/confirm`)
      .set('Authorization', `Bearer ${b.accessToken}`)
      .expect(404);
  });

  it('deletes account and purges vehicles', async () => {
    const a = await register('delete-me@meru.app');
    await request(app.getHttpServer())
      .post('/v1/vehicles')
      .set('Authorization', `Bearer ${a.accessToken}`)
      .send({ make: 'Toyota', model: 'Corolla', year: 2018 })
      .expect(201);

    await request(app.getHttpServer())
      .delete('/v1/account')
      .set('Authorization', `Bearer ${a.accessToken}`)
      .expect(200);

    await request(app.getHttpServer())
      .get('/v1/vehicles')
      .set('Authorization', `Bearer ${a.accessToken}`)
      .expect(200)
      .expect((res) => {
        expect(res.body.vehicles).toHaveLength(0);
      });
  });
});
