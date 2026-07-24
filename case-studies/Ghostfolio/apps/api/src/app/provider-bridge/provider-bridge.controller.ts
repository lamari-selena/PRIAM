import { PrismaService } from '@ghostfolio/api/services/prisma/prisma.service';

import {
  Body,
  Controller,
  Get,
  Post,
  Query,
  Res,
  VERSION_NEUTRAL,
  Version
} from '@nestjs/common';
import { Response } from 'express';

// PRIAM Provider bridge (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §2). Bare
// /api/* (no /v1/, no auth - machine-to-machine, called only by PRIAM),
// achieved via @Version(VERSION_NEUTRAL) on each route (same per-method
// convention as auth.controller.ts/sitemap.controller.ts), combined with
// main.ts's global 'api' prefix + URI versioning.
type PrimaryKeys = Record<string, string>;

// Whitelist of attributes exposed per dataTypeName (§2 "restricted to a
// whitelist of fields allowed for a given dataTypeName") - must match
// Databases/db_insertion_script.sql's `data.data_name` rows exactly.
const WHITELISTS: Record<string, string[]> = {
  User: ['id', 'provider', 'thirdPartyId', 'createdAt'],
  Account: ['id', 'name', 'currency', 'balance'],
  Order: ['id', 'type', 'currency', 'quantity', 'unitPrice', 'fee', 'date', 'comment'],
  Analytics: ['country', 'activityCount', 'lastRequestAt']
};

// Fields accepted for rectification/erasure - a subset of WHITELISTS above.
// System/audit fields (id, enum-constrained provider/type, required
// date/createdAt/lastRequestAt timestamps) stay access-only - matches the
// data_usage c/r/u/d flags set in db_insertion_script.sql.
const MUTABLE: Record<string, string[]> = {
  User: ['thirdPartyId'],
  Account: ['name', 'currency', 'balance'],
  Order: ['currency', 'quantity', 'unitPrice', 'fee', 'comment'],
  Analytics: ['country', 'activityCount']
};

function toStringValue(value: unknown): string {
  return value === null || value === undefined ? '' : String(value);
}

function pickAttributes(record: Record<string, unknown>, attributes: string[]) {
  const allowed = attributes.length > 0 ? attributes : Object.keys(record);
  const out: Record<string, string> = {};

  for (const attribute of allowed) {
    if (Object.prototype.hasOwnProperty.call(record, attribute)) {
      out[attribute] = toStringValue(record[attribute]);
    }
  }

  return out;
}

function inferDataTypeName(dataName: string): string | undefined {
  return Object.keys(WHITELISTS).find((dataTypeName) =>
    WHITELISTS[dataTypeName].includes(dataName)
  );
}

@Controller()
export class ProviderBridgeController {
  public constructor(private readonly prismaService: PrismaService) {}

  private async loadRecords(dataTypeName: string, idRef: string) {
    if (dataTypeName === 'User') {
      const user = await this.prismaService.user.findUnique({
        where: { id: idRef }
      });

      return user ? [user] : [];
    }

    if (dataTypeName === 'Account') {
      return this.prismaService.account.findMany({
        orderBy: { id: 'asc' },
        where: { userId: idRef }
      });
    }

    if (dataTypeName === 'Order') {
      return this.prismaService.order.findMany({
        orderBy: { id: 'asc' },
        where: { userId: idRef }
      });
    }

    if (dataTypeName === 'Analytics') {
      const analytics = await this.prismaService.analytics.findUnique({
        where: { userId: idRef }
      });

      return analytics ? [analytics] : [];
    }

    return [];
  }

  // GET {CUSTOM_PROVIDER_URL}/api/dataAccessRight?idRef=...&dataTypeName=...&attributes=a,b,c
  // Always answers with a JSON array (§2), one element per row of
  // dataTypeName held by idRef (a single element for User/Analytics, one
  // per record for Account/Order).
  @Get('dataAccessRight')
  @Version(VERSION_NEUTRAL)
  public async getDataAccessRight(
    @Query('idRef') idRef: string,
    @Query('dataTypeName') dataTypeName: string,
    @Query('attributes') attributes = ''
  ) {
    if (!idRef || !dataTypeName || !WHITELISTS[dataTypeName]) {
      return [];
    }

    const requested = attributes
      .split(',')
      .map((attribute) => attribute.trim())
      .filter(Boolean);
    const allowed = requested.filter((attribute) =>
      WHITELISTS[dataTypeName].includes(attribute)
    );

    const records = await this.loadRecords(dataTypeName, idRef);

    return records.map((record) =>
      pickAttributes(record as Record<string, unknown>, allowed)
    );
  }

  // POST {CUSTOM_PROVIDER_URL}/api/rectification  body: {idRef, dataTypeName, dataName, newValue, primaryKeys}
  @Post('rectification')
  @Version(VERSION_NEUTRAL)
  public async postRectification(
    @Body()
    body: {
      idRef: string;
      dataTypeName: string;
      dataName: string;
      newValue: string;
      primaryKeys?: PrimaryKeys;
    },
    @Res() response: Response
  ) {
    const { idRef, dataTypeName, dataName, newValue, primaryKeys } = body;

    if (!MUTABLE[dataTypeName]?.includes(dataName)) {
      return response
        .status(400)
        .json({ error: 'Unknown or non-rectifiable field' });
    }

    const applied = await this.applyMutation({
      dataName,
      dataTypeName,
      idRef,
      primaryKeys,
      value: this.coerceValue(dataTypeName, dataName, newValue)
    });

    if (!applied) {
      return response.status(404).json({ error: 'Record not found' });
    }

    return response.status(200).json({ success: true });
  }

  // POST {CUSTOM_PROVIDER_URL}/api/erasure  body: {idRef, dataTypeName, dataName, primaryKeys}
  @Post('erasure')
  @Version(VERSION_NEUTRAL)
  public async postErasure(
    @Body()
    body: {
      idRef: string;
      dataTypeName: string;
      dataName: string;
      primaryKeys?: PrimaryKeys;
    },
    @Res() response: Response
  ) {
    const { idRef, dataTypeName, dataName, primaryKeys } = body;

    if (!MUTABLE[dataTypeName]?.includes(dataName)) {
      return response
        .status(400)
        .json({ error: 'Unknown or non-erasable field' });
    }

    const applied = await this.applyMutation({
      dataName,
      dataTypeName,
      idRef,
      primaryKeys,
      value: this.erasedValue(dataTypeName, dataName)
    });

    if (!applied) {
      return response.status(404).json({ error: 'Record not found' });
    }

    return response.status(200).json({ success: true });
  }

  // POST {CUSTOM_PROVIDER_URL}/api/dataValue  body: {idRef, dataName, primaryKeys}
  // No dataTypeName (§2/§8.2.f) - inferred from dataName's whitelist.
  @Post('dataValue')
  @Version(VERSION_NEUTRAL)
  public async postDataValue(
    @Body() body: { idRef: string; dataName: string; primaryKeys?: PrimaryKeys },
    @Res() response: Response
  ) {
    const { idRef, dataName, primaryKeys } = body;
    const dataTypeName = inferDataTypeName(dataName);

    if (!dataTypeName) {
      return response.status(404).json({ error: 'Unknown field' });
    }

    const records = await this.loadRecords(dataTypeName, idRef);
    const record = primaryKeys?.id
      ? records.find((row: any) => String(row.id) === String(primaryKeys.id))
      : records[0];

    if (!record) {
      return response.status(404).json({ error: 'Record not found' });
    }

    return response
      .status(200)
      .json({ value: toStringValue((record as Record<string, unknown>)[dataName]) });
  }

  private coerceValue(dataTypeName: string, dataName: string, newValue: string) {
    if (
      (dataTypeName === 'Account' && dataName === 'balance') ||
      (dataTypeName === 'Order' &&
        ['quantity', 'unitPrice', 'fee'].includes(dataName)) ||
      (dataTypeName === 'Analytics' && dataName === 'activityCount')
    ) {
      return Number(newValue);
    }

    return newValue;
  }

  private erasedValue(dataTypeName: string, dataName: string) {
    if (
      (dataTypeName === 'Account' && dataName === 'balance') ||
      (dataTypeName === 'Order' &&
        ['quantity', 'unitPrice', 'fee'].includes(dataName)) ||
      (dataTypeName === 'Analytics' && dataName === 'activityCount')
    ) {
      return 0;
    }

    return dataTypeName === 'User' ? null : '';
  }

  private async applyMutation({
    dataTypeName,
    idRef,
    dataName,
    value,
    primaryKeys
  }: {
    dataTypeName: string;
    idRef: string;
    dataName: string;
    value: unknown;
    primaryKeys?: PrimaryKeys;
  }): Promise<boolean> {
    if (dataTypeName === 'User') {
      const result = await this.prismaService.user
        .update({ data: { [dataName]: value }, where: { id: idRef } })
        .catch(() => undefined);

      return !!result;
    }

    if (dataTypeName === 'Analytics') {
      const result = await this.prismaService.analytics
        .update({ data: { [dataName]: value }, where: { userId: idRef } })
        .catch(() => undefined);

      return !!result;
    }

    if (dataTypeName === 'Account') {
      const id = primaryKeys?.id;

      if (!id) {
        return false;
      }

      const result = await this.prismaService.account
        .updateMany({ data: { [dataName]: value }, where: { id, userId: idRef } })
        .catch(() => ({ count: 0 }));

      return result.count > 0;
    }

    if (dataTypeName === 'Order') {
      const id = primaryKeys?.id;

      if (!id) {
        return false;
      }

      const result = await this.prismaService.order
        .updateMany({ data: { [dataName]: value }, where: { id, userId: idRef } })
        .catch(() => ({ count: 0 }));

      return result.count > 0;
    }

    return false;
  }
}
