import {
  Body,
  Controller,
  Get,
  HttpCode,
  HttpStatus,
  Post,
  Query
} from '@nestjs/common';

import { ErasureDto } from './dto/erasure.dto';
import { RectificationDto } from './dto/rectification.dto';
import { PriamService } from './priam.service';

/**
 * Exposes the three Provider endpoints required by PRIAM's Right Management
 * service (GDPR Art. 15, 16, 17). These endpoints are called by PRIAM after
 * an application owner approves a data subject rights request.
 *
 * No authentication is enforced here in the evaluation environment.
 * In production, these endpoints should be protected by an API key or
 * restricted to the PRIAM internal network.
 */
@Controller('priam')
export class PriamController {
  public constructor(private readonly priamService: PriamService) {}

  /**
   * Right of Access — GDPR Art. 15
   * Returns the current values of requested personal data attributes.
   *
   * @param idRef     Ghostfolio user UUID (= PRIAM referenceId)
   * @param dataTypeName  Prisma model name: "User" | "Account" | "Order"
   * @param attributes    Comma-separated list of field names to retrieve
   */
  @Get('dataAccessRight')
  public async dataAccessRight(
    @Query('idRef') idRef: string,
    @Query('dataTypeName') dataTypeName: string,
    @Query('attributes') attributes: string | string[]
  ) {
    const attrs = Array.isArray(attributes)
      ? attributes
      : attributes.split(',').map((a) => a.trim());

    return this.priamService.getDataAccessRight(idRef, dataTypeName, attrs);
  }

  /**
   * Right to Rectification — GDPR Art. 16
   * Updates a personal data attribute with the approved new value.
   */
  @Post('rectification')
  @HttpCode(HttpStatus.OK)
  public async rectification(@Body() dto: RectificationDto): Promise<void> {
    return this.priamService.rectify(dto);
  }

  /**
   * Right to Erasure — GDPR Art. 17
   * Nullifies or zeroes a personal data attribute.
   */
  @Post('erasure')
  @HttpCode(HttpStatus.OK)
  public async erasure(@Body() dto: ErasureDto): Promise<void> {
    return this.priamService.erase(dto);
  }
}
