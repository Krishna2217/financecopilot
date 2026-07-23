import { useQuery } from '@tanstack/react-query';
import { getCashflow, getKpis, getSpendByCategory, getTrend } from '@/shared/api/analytics';

export function useKpis(month?: string) {
  return useQuery({ queryKey: ['kpis', month], queryFn: () => getKpis(month) });
}

export function useSpendByCategory(month?: string) {
  return useQuery({
    queryKey: ['spend-by-category', month],
    queryFn: () => getSpendByCategory(month),
  });
}

export function useCashflow(months = 6) {
  return useQuery({ queryKey: ['cashflow', months], queryFn: () => getCashflow(months) });
}

export function useTrend(category?: string, months = 6) {
  return useQuery({
    queryKey: ['trend', category, months],
    queryFn: () => getTrend(category, months),
  });
}
