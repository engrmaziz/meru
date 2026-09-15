import { Test, TestingModule } from '@nestjs/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [AuthService],
    }).compile();
    service = module.get(AuthService);
  });

  it('registers and logs in', () => {
    const registered = service.register('a@meru.app', 'secret1', 'A');
    expect(registered.user.email).toBe('a@meru.app');
    const loggedIn = service.login('a@meru.app', 'secret1');
    expect(loggedIn?.accessToken).toBeTruthy();
  });

  it('rejects bad password', () => {
    service.register('b@meru.app', 'secret1', 'B');
    expect(service.login('b@meru.app', 'nope')).toBeNull();
  });
});
