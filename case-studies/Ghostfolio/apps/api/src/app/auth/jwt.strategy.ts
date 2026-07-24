import { UserService } from '@ghostfolio/api/app/user/user.service';
import { ConfigurationService } from '@ghostfolio/api/services/configuration/configuration.service';
import {
  ANALYTICS_DATA_IDS,
  PriamService,
  USAGE_ANALYTICS_PROCESSING
} from '@ghostfolio/api/services/priam/priam.service';
import { PrismaService } from '@ghostfolio/api/services/prisma/prisma.service';
import {
  DEFAULT_CURRENCY,
  DEFAULT_LANGUAGE_CODE,
  HEADER_KEY_TIMEZONE
} from '@ghostfolio/common/config';
import { hasRole } from '@ghostfolio/common/permissions';

import { HttpException, Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import * as countriesAndTimezones from 'countries-and-timezones';
import { StatusCodes, getReasonPhrase } from 'http-status-codes';
import { ExtractJwt, Strategy } from 'passport-jwt';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy, 'jwt') {
  public constructor(
    private readonly configurationService: ConfigurationService,
    private readonly priamService: PriamService,
    private readonly prismaService: PrismaService,
    private readonly userService: UserService
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      passReqToCallback: true,
      secretOrKey: configurationService.get('JWT_SECRET_KEY')
    });
  }

  public async validate(request: Request, { id }: { id: string }) {
    try {
      const timezone = request.headers[HEADER_KEY_TIMEZONE.toLowerCase()];
      const user = await this.userService.user({ id });

      if (user) {
        if (this.configurationService.get('ENABLE_FEATURE_SUBSCRIPTION')) {
          if (hasRole(user, 'INACTIVE')) {
            throw new HttpException(
              getReasonPhrase(StatusCodes.TOO_MANY_REQUESTS),
              StatusCodes.TOO_MANY_REQUESTS
            );
          }

          const country =
            countriesAndTimezones.getCountryForTimezone(timezone)?.id;

          // CEP (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4) - Usage Analytics is
          // the OPTIONAL processing annotated in db_insertion_script.sql;
          // only this optional side effect is gated, never authentication
          // itself.
          if (
            await this.priamService.getConsent(
              user.id,
              USAGE_ANALYTICS_PROCESSING
            )
          ) {
            await this.prismaService.analytics.upsert({
              create: { country, user: { connect: { id: user.id } } },
              update: {
                country,
                activityCount: { increment: 1 },
                lastRequestAt: new Date()
              },
              where: { userId: user.id }
            });

            // §4bis bookkeeping for this OPTIONAL data_type - the Data
            // service dedupes repeat reports via nb_occurrences.
            this.priamService.reportProcessedData(
              user.id,
              ANALYTICS_DATA_IDS
            );
          }
        }

        if (!user.settings.settings.baseCurrency) {
          user.settings.settings.baseCurrency = DEFAULT_CURRENCY;
        }

        if (!user.settings.settings.language) {
          user.settings.settings.language = DEFAULT_LANGUAGE_CODE;
        }

        return user;
      } else {
        throw new HttpException(
          getReasonPhrase(StatusCodes.NOT_FOUND),
          StatusCodes.NOT_FOUND
        );
      }
    } catch (error) {
      if (error?.getStatus?.() === StatusCodes.TOO_MANY_REQUESTS) {
        throw error;
      } else {
        throw new HttpException(
          getReasonPhrase(StatusCodes.UNAUTHORIZED),
          StatusCodes.UNAUTHORIZED
        );
      }
    }
  }
}
