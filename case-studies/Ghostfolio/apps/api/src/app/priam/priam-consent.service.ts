import { Injectable, Logger } from '@nestjs/common';

/**
 * Queries PRIAM's Consent Decision Point (CDP) to determine whether
 * a given user has granted consent for a specific processing activity.
 *
 * Maps to the ABAC pattern role: Consent Enforcement Point (CEP) caller.
 * CDP endpoint: GET /api/decision/{processingId}?idRefList={userId}
 * Response: { "<userId>": true | false }
 */
@Injectable()
export class PriamConsentService {
  private readonly logger = new Logger(PriamConsentService.name);

  private readonly cdpUrl =
    process.env['PRIAM_CDP_URL'] ?? 'http://localhost:8089';

  async getConsent(userId: string, processingId: string): Promise<boolean> {
    const url = `${this.cdpUrl}/api/decision/${encodeURIComponent(processingId)}?idRefList=${encodeURIComponent(userId)}`;

    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(3000)
      });

      if (!response.ok) {
        this.logger.warn(
          `CDP returned ${response.status} for processing "${processingId}" / user "${userId}". Denying by default.`
        );
        return false;
      }

      const decision: Record<string, boolean> = await response.json();
      return decision[userId] === true;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : String(error);
      this.logger.error(
        `Failed to reach PRIAM CDP at ${url}: ${message}. Denying by default.`
      );
      return false;
    }
  }
}
