import { Test, TestingModule } from '@nestjs/testing';
import { LaunchService } from './launch.service';

describe('LaunchService', () => {
  let launch: LaunchService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [LaunchService],
    }).compile();
    launch = module.get(LaunchService);
  });

  it('exposes Lahore soft-launch flags', () => {
    const f = launch.publicFlags();
    expect(f.softLaunchCityId).toBe('pk-pb-lhr');
    expect(f.s4Marketplace).toBe(true);
  });

  it('requires admin key', () => {
    expect(() => launch.assertAdmin('wrong')).toThrow();
    expect(() => launch.assertAdmin('meru-dev-admin')).not.toThrow();
  });

  it('rate limits after burst', () => {
    for (let i = 0; i < 5; i++) launch.consumeRate('t', 5, 60_000);
    expect(() => launch.consumeRate('t', 5, 60_000)).toThrow();
  });
});
