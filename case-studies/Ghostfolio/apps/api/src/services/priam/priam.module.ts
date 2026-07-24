import { Global, Module } from '@nestjs/common';

import { PriamService } from './priam.service';

// Registered once in AppModule.imports as a global singleton (playbook
// §4bis "Minimal footprint in DI frameworks") so PriamService is injectable
// anywhere without importing this module again.
@Global()
@Module({
  exports: [PriamService],
  providers: [PriamService]
})
export class PriamModule {}
