import { PrismaModule } from '@ghostfolio/api/services/prisma/prisma.module';
import { Module } from '@nestjs/common';

import { PriamConsentService } from './priam-consent.service';
import { PriamController } from './priam.controller';
import { PriamService } from './priam.service';

@Module({
  imports: [PrismaModule],
  controllers: [PriamController],
  providers: [PriamService, PriamConsentService],
  exports: [PriamConsentService]
})
export class PriamModule {}
