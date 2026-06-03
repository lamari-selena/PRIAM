import { IsNotEmpty, IsObject, IsOptional, IsString } from 'class-validator';

export class ErasureDto {
  /** Ghostfolio user UUID */
  @IsString()
  @IsNotEmpty()
  idRef!: string;

  /** Personal data attribute name to erase */
  @IsString()
  @IsNotEmpty()
  dataName!: string;

  /** Prisma model name: "User" | "Account" | "Order" */
  @IsString()
  @IsNotEmpty()
  dataTypeName!: string;

  /** Primary key values for Account/Order records */
  @IsObject()
  @IsOptional()
  primaryKeys?: Record<string, string>;
}