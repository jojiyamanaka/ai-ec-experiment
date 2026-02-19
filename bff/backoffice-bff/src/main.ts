import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { ConfigService } from '@nestjs/config';
import { TraceInterceptor } from './common/interceptors/trace.interceptor';
import { LoggingInterceptor } from './common/interceptors/logging.interceptor';
import { HttpExceptionFilter } from './common/filters/http-exception.filter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const configService = app.get(ConfigService);

  app.useGlobalInterceptors(new TraceInterceptor());
  app.useGlobalInterceptors(new LoggingInterceptor());
  app.useGlobalFilters(new HttpExceptionFilter());

  app.enableCors({
    origin: 'http://localhost:5174',
    credentials: true,
    allowedHeaders: [
      'Content-Type', 'Authorization', 'X-Session-Id',
      'traceparent', 'tracestate',
    ],
  });

  const port = configService.get<number>('server.port');
  if (port === undefined || Number.isNaN(port)) {
    throw new Error('Missing or invalid config: server.port');
  }
  await app.listen(port);
  console.log(`BackOffice BFF is running on: http://localhost:${port}`);
}
bootstrap();
