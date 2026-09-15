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
});
