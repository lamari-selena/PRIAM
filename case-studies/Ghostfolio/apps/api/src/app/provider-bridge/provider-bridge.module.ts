import { PrismaModule } from '@ghostfolio/api/services/prisma/prisma.module';

import { Module } from '@nestjs/common';

import { ProviderBridgeController } from './provider-bridge.controller';

@Module({
  controllers: [ProviderBridgeController],
  imports: [PrismaModule]
})
export class ProviderBridgeModule {}
