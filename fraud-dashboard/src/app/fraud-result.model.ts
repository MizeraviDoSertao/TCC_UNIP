export interface FraudResult {
  transactionId: string;
  realFraud: boolean;
  predictedFraud: boolean;
  probability: number;
  classification: string;
  processedAt: string;
}