import { UserService } from '@ghostfolio/api/app/user/user.service';
import { ApiKeyService } from '@ghostfolio/api/services/api-key/api-key.service';
import { ConfigurationService } from '@ghostfolio/api/services/configuration/configuration.service';
import {
  ANALYTICS_DATA_IDS,
  PriamService,
  USAGE_ANALYTICS_PROCESSING
} from '@ghostfolio/api/services/priam/priam.service';
import { PrismaService } from '@ghostfolio/api/services/prisma/prisma.service';
import { HEADER_KEY_TOKEN } from '@ghostfolio/common/config';
import { hasRole } from '@ghostfolio/common/permissions';

import { HttpException, Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { StatusCodes, getReasonPhrase } from 'http-status-codes';
import { HeaderAPIKeyStrategy } from 'passport-headerapikey';

@Injectable()
export class ApiKeyStrategy extends PassportStrategy(
  HeaderAPIKeyStrategy,
  'api-key'
) {
  public constructor(
    private readonly apiKeyService: ApiKeyService,
    private readonly configurationService: ConfigurationService,
    private readonly priamService: PriamService,
    private readonly prismaService: PrismaService,
    private readonly userService: UserService
  ) {
    super({ header: HEADER_KEY_TOKEN, prefix: 'Api-Key ' }, false);
  }

  public async validate(apiKey: string) {
    const user = await this.validateApiKey(apiKey);

    if (this.configurationService.get('ENABLE_FEATURE_SUBSCRIPTION')) {
      if (hasRole(user, 'INACTIVE')) {
        throw new HttpException(
          getReasonPhrase(StatusCodes.TOO_MANY_REQUESTS),
          StatusCodes.TOO_MANY_REQUESTS
        );
      }

      // CEP (§4) - same OPTIONAL Usage Analytics processing as jwt.strategy.ts.
      if (
        await this.priamService.getConsent(
          user.id,
          USAGE_ANALYTICS_PROCESSING
        )
      ) {
        await this.prismaService.analytics.upsert({
          create: { user: { connect: { id: user.id } } },
          update: {
            activityCount: { increment: 1 },
            lastRequestAt: new Date()
          },
          where: { userId: user.id }
        });

        this.priamService.reportProcessedData(user.id, ANALYTICS_DATA_IDS);
      }
    }

    return user;
  }

  private async validateApiKey(apiKey: string) {
    if (!apiKey) {
      throw new HttpException(
        getReasonPhrase(StatusCodes.UNAUTHORIZED),
        StatusCodes.UNAUTHORIZED
      );
    }

    try {
      const { id } = await this.apiKeyService.getUserByApiKey(apiKey);

      return this.userService.user({ id });
    } catch {
      throw new HttpException(
        getReasonPhrase(StatusCodes.UNAUTHORIZED),
        StatusCodes.UNAUTHORIZED
      );
    }
  }
}
