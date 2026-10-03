export interface FraudResult {
  readonly transactionId: string;
  readonly realFraud: boolean | null;
  readonly predictedFraud: boolean | null;
  readonly probability: number;
  readonly scoreType: string;
  readonly riskLevel: string;
  readonly threshold: number | null;
  readonly classification: string;
  readonly modelVersion: string | null;
  readonly reasons: readonly string[];
  readonly processedAt: string;
}
