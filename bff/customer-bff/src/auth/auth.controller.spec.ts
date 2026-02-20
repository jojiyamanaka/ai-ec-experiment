import { AuthController } from './auth.controller';

describe('AuthController', () => {
  const authService = {
    register: jest.fn(),
    login: jest.fn(),
    logout: jest.fn(),
  };

  let controller: AuthController;

  beforeEach(() => {
    jest.clearAllMocks();
    controller = new AuthController(authService as never);
  });

  it('uses username and fullName as fallbacks in register', async () => {
    authService.register.mockResolvedValue({ success: true, data: {} });

    await controller.register(
      { username: 'user-1', password: 'secret', fullName: 'User One' },
      { traceId: 'trace-1' },
    );

    expect(authService.register).toHaveBeenCalledWith('user-1', 'User One', 'secret', 'trace-1');
  });

  it('forwards session id in login request', async () => {
    authService.login.mockResolvedValue({ success: true, data: {} });

    await controller.login(
      { email: 'user@example.com', password: 'secret' },
      { traceId: 'trace-2', session: { sessionId: 'session-2' } },
    );

    expect(authService.login).toHaveBeenCalledWith('user@example.com', 'secret', 'trace-2', 'session-2');
  });

  it('forwards token in logout request', async () => {
    authService.logout.mockResolvedValue({ success: true, data: undefined });

    await controller.logout({ token: 'token-1', traceId: 'trace-3' });

    expect(authService.logout).toHaveBeenCalledWith('token-1', 'trace-3');
  });
});
