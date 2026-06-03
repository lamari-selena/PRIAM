import { HasPermission } from '@ghostfolio/api/decorators/has-permission.decorator';
import { HasPermissionGuard } from '@ghostfolio/api/guards/has-permission.guard';
import { ApiService } from '@ghostfolio/api/services/api/api.service';
import { PriamConsentService } from '@ghostfolio/api/app/priam/priam-consent.service';
import { AiPromptResponse } from '@ghostfolio/common/interfaces';
import { permissions } from '@ghostfolio/common/permissions';
import type { AiPromptMode, RequestWithUser } from '@ghostfolio/common/types';

import {
  Controller,
  ForbiddenException,
  Get,
  Inject,
  Param,
  Query,
  UseGuards
} from '@nestjs/common';
import { REQUEST } from '@nestjs/core';
import { AuthGuard } from '@nestjs/passport';

import { AiService } from './ai.service';

/**
 * Processing ID registered in PRIAM's Data & Processing Management service.
 * Matches the processingName declared in priam-integration/annotations.json.
 */
const AI_PROCESSING_ID = 'portfolio-ai-analysis';

@Controller('ai')
export class AiController {
  public constructor(
    private readonly aiService: AiService,
    private readonly apiService: ApiService,
    private readonly priamConsentService: PriamConsentService,
    @Inject(REQUEST) private readonly request: RequestWithUser
  ) {}

  @Get('prompt/:mode')
  @HasPermission(permissions.readAiPrompt)
  @UseGuards(AuthGuard('jwt'), HasPermissionGuard)
  public async getPrompt(
    @Param('mode') mode: AiPromptMode,
    @Query('accounts') filterByAccounts?: string,
    @Query('assetClasses') filterByAssetClasses?: string,
    @Query('dataSource') filterByDataSource?: string,
    @Query('symbol') filterBySymbol?: string,
    @Query('tags') filterByTags?: string
  ): Promise<AiPromptResponse> {
    const userId = this.request.user.id;

    // ── PRIAM consent check (CEP) ──────────────────────────────────────────
    // AI analysis sends portfolio holdings to an external LLM (OpenRouter).
    // This is an optional processing requiring explicit user consent (GDPR Art. 6(1)(a)).
    const canUse = await this.priamConsentService.getConsent(
      userId,
      AI_PROCESSING_ID
    );

    if (!canUse) {
      throw new ForbiddenException(
        'Consent for AI portfolio analysis has not been granted or has been withdrawn.'
      );
    }
    // ──────────────────────────────────────────────────────────────────────

    const filters = this.apiService.buildFiltersFromQueryParams({
      filterByAccounts,
      filterByAssetClasses,
      filterByDataSource,
      filterBySymbol,
      filterByTags
    });

    const prompt = await this.aiService.getPrompt({
      filters,
      mode,
      impersonationId: undefined,
      languageCode: this.request.user.settings.settings.language,
      userCurrency: this.request.user.settings.settings.baseCurrency,
      userId
    });

    return { prompt };
  }
}
