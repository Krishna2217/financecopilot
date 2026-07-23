import { apiGet } from '@/shared/api/client';
import type { CashflowPoint, CategorySpend, KpiSummary, TrendPoint } from '@/shared/lib/types';

export function getKpis(month?: string): Promise<KpiSummary> {
  const query = month ? `?month=${month}` : '';
  return apiGet(`/v1/analytics/kpis${query}`);
}

export function getSpendByCategory(month?: string): Promise<CategorySpend[]> {
  const query = month ? `?month=${month}` : '';
  return apiGet(`/v1/analytics/spend-by-category${query}`);
}

export function getCashflow(months = 6): Promise<CashflowPoint[]> {
  return apiGet(`/v1/analytics/cashflow?months=${months}`);
}

export function getTrend(category?: string, months = 6): Promise<TrendPoint[]> {
  const params = new URLSearchParams({ months: String(months) });
  if (category) params.set('category', category);
  return apiGet(`/v1/analytics/trend?${params.toString()}`);
}
