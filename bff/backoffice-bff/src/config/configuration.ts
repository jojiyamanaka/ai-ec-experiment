function parseIntEnv(name: string, defaultValue: number): number {
  const raw = process.env[name];
  if (raw === undefined || raw === '') {
    return defaultValue;
  }

  const parsed = Number.parseInt(raw, 10);
  if (Number.isNaN(parsed)) {
    throw new Error(`${name} must be a number`);
  }

  return parsed;
}

export default () => ({
  server: {
    port: parseIntEnv('PORT', 3002),
  },
  coreApi: {
    url: process.env.CORE_API_URL || 'http://localhost:8080',
    timeout: parseIntEnv('CORE_API_TIMEOUT', 5000),
    retry: parseIntEnv('CORE_API_RETRY', 2),
  },
  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
});
