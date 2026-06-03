import { PrismaService } from '@ghostfolio/api/services/prisma/prisma.service';
import {
  BadRequestException,
  Injectable,
  Logger,
  NotFoundException
} from '@nestjs/common';

import { ErasureDto } from './dto/erasure.dto';
import { RectificationDto } from './dto/rectification.dto';

/** Nullable fields per model — fields that PRIAM is allowed to read, update or erase. */
const ALLOWED_FIELDS: Record<string, string[]> = {
  Account: ['name', 'comment', 'currency', 'balance'],
  Order: ['comment', 'fee', 'unitPrice', 'quantity'],
  User: ['accessToken', 'thirdPartyId', 'authChallenge']
};

@Injectable()
export class PriamService {
  private readonly logger = new Logger(PriamService.name);

  public constructor(private readonly prismaService: PrismaService) {}

  // ── Right of Access ────────────────────────────────────────────────────────

  async getDataAccessRight(
    idRef: string,
    dataTypeName: string,
    attributes: string[]
  ): Promise<Record<string, unknown>[]> {
    this.validateModel(dataTypeName, attributes);

    switch (dataTypeName) {
      case 'User': {
        const user = await this.prismaService.user.findUnique({
          where: { id: idRef }
        });
        if (!user) throw new NotFoundException(`User ${idRef} not found`);
        return attributes.map((attr) => ({ [attr]: user[attr] ?? null }));
      }

      case 'Account': {
        const accounts = await this.prismaService.account.findMany({
          where: { userId: idRef }
        });
        return accounts.flatMap((acc) =>
          attributes.map((attr) => ({
            accountId: acc.id,
            [attr]: acc[attr] ?? null
          }))
        );
      }

      case 'Order': {
        const orders = await this.prismaService.order.findMany({
          where: { userId: idRef }
        });
        return orders.flatMap((order) =>
          attributes.map((attr) => ({
            orderId: order.id,
            [attr]: order[attr] ?? null
          }))
        );
      }

      default:
        throw new BadRequestException(`Unknown dataTypeName: ${dataTypeName}`);
    }
  }

  // ── Right to Rectification ─────────────────────────────────────────────────

  async rectify(dto: RectificationDto): Promise<void> {
    const { idRef, dataName, dataTypeName, newValue, primaryKeys } = dto;
    this.validateModel(dataTypeName, [dataName]);

    switch (dataTypeName) {
      case 'User':
        await this.prismaService.user.update({
          where: { id: idRef },
          data: { [dataName]: newValue }
        });
        break;

      case 'Account': {
        const accountId = this.requirePrimaryKey(primaryKeys, 'id');
        await this.prismaService.account.update({
          where: { id_userId: { id: accountId, userId: idRef } },
          data: { [dataName]: dataName === 'balance' ? parseFloat(newValue) : newValue }
        });
        break;
      }

      case 'Order': {
        const orderId = this.requirePrimaryKey(primaryKeys, 'id');
        await this.prismaService.order.update({
          where: { id: orderId },
          data: {
            [dataName]: ['fee', 'unitPrice', 'quantity'].includes(dataName)
              ? parseFloat(newValue)
              : newValue
          }
        });
        break;
      }

      default:
        throw new BadRequestException(`Unknown dataTypeName: ${dataTypeName}`);
    }

    this.logger.log(
      `Rectification applied: user=${idRef} model=${dataTypeName} field=${dataName}`
    );
  }

  // ── Right to Erasure ───────────────────────────────────────────────────────

  async erase(dto: ErasureDto): Promise<void> {
    const { idRef, dataName, dataTypeName, primaryKeys } = dto;
    this.validateModel(dataTypeName, [dataName]);

    switch (dataTypeName) {
      case 'User':
        await this.prismaService.user.update({
          where: { id: idRef },
          data: { [dataName]: null }
        });
        break;

      case 'Account': {
        const accountId = this.requirePrimaryKey(primaryKeys, 'id');
        await this.prismaService.account.update({
          where: { id_userId: { id: accountId, userId: idRef } },
          data: { [dataName]: dataName === 'balance' ? 0 : null }
        });
        break;
      }

      case 'Order': {
        const orderId = this.requirePrimaryKey(primaryKeys, 'id');
        // For financial transaction fields, set to 0; for text fields, null them
        await this.prismaService.order.update({
          where: { id: orderId },
          data: {
            [dataName]: ['fee', 'unitPrice', 'quantity'].includes(dataName)
              ? 0
              : null
          }
        });
        break;
      }

      default:
        throw new BadRequestException(`Unknown dataTypeName: ${dataTypeName}`);
    }

    this.logger.log(
      `Erasure applied: user=${idRef} model=${dataTypeName} field=${dataName}`
    );
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private validateModel(dataTypeName: string, fields: string[]): void {
    const allowed = ALLOWED_FIELDS[dataTypeName];
    if (!allowed) {
      throw new BadRequestException(`Unknown dataTypeName: ${dataTypeName}`);
    }
    const forbidden = fields.filter((f) => !allowed.includes(f));
    if (forbidden.length > 0) {
      throw new BadRequestException(
        `Fields not allowed for ${dataTypeName}: ${forbidden.join(', ')}`
      );
    }
  }

  private requirePrimaryKey(
    primaryKeys: Record<string, string> | undefined,
    key: string
  ): string {
    if (!primaryKeys?.[key]) {
      throw new BadRequestException(
        `primaryKeys.${key} is required for this operation`
      );
    }
    return primaryKeys[key];
  }
}
