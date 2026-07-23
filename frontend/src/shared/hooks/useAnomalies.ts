import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { explainAnomaly, getAnomalies } from '@/shared/api/anomalies';

export function useAnomalies(month?: string) {
  return useQuery({ queryKey: ['anomalies', month], queryFn: () => getAnomalies(month) });
}

export function useExplainAnomaly() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => explainAnomaly(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['anomalies'] });
    },
  });
}
