import { IsNotEmpty, IsObject, IsOptional, IsString } from 'class-validator';

export class RectificationDto {
  /** Ghostfolio user UUID — common identifier between Ghostfolio and PRIAM */
  @IsString()
  @IsNotEmpty()
  idRef!: string;

  /** Personal data attribute name (e.g. "name", "comment") */
  @IsString()
  @IsNotEmpty()
  dataName!: string;

  /** Prisma model name: "User" | "Account" | "Order" */
  @IsString()
  @IsNotEmpty()
  dataTypeName!: string;

  /** New value to apply */
  @IsString()
  @IsNotEmpty()
  newValue!: string;

  /**
   * Primary key values for non-User models.
   * For Account: { "id": "<accountId>" }
   * For Order:   { "id": "<orderId>" }
   */
  @IsObject()
  @IsOptional()
  primaryKeys?: Record<string, string>;
}