import { NestFactory } from '@nestjs/core';
import { writeFileSync } from 'fs';
import { resolve } from 'path';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function generateOpenApi() {
  const app = await NestFactory.create(AppModule, { logger: false });
  await app.init();

  const config = new DocumentBuilder()
    .setTitle('Customer BFF API')
    .setDescription('顧客向け BFF エンドポイント')
    .setVersion('1.0')
    .build();
  const document = SwaggerModule.createDocument(app, config);

  const outputPath = resolve(process.cwd(), '../../docs/api/customer-bff-openapi.json');
  writeFileSync(outputPath, JSON.stringify(document, null, 2), 'utf-8');

  await app.close();
}

generateOpenApi().catch((error) => {
  console.error(error);
  process.exit(1);
});
