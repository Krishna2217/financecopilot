import { useMutation, useQuery as useTanstackQuery, useQueryClient } from '@tanstack/react-query';
import { getQueryHistory, submitQuery } from '@/shared/api/query';

export function useSubmitQuery() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (naturalLanguageQuery: string) => submitQuery(naturalLanguageQuery),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['query-history'] });
    },
  });
}

export function useQueryHistory(page = 0, size = 20) {
  return useTanstackQuery({
    queryKey: ['query-history', page, size],
    queryFn: () => getQueryHistory(page, size),
  });
}
