import { apiGet, apiPost } from '@/shared/api/client';
import type { AnomalyResponse } from '@/shared/lib/types';

export function getAnomalies(month?: string): Promise<AnomalyResponse[]> {
  const query = month ? `?month=${month}` : '';
  return apiGet(`/v1/anomalies${query}`);
}

export function explainAnomaly(id: number): Promise<AnomalyResponse> {
  return apiPost(`/v1/anomalies/${id}/explain`);
}
