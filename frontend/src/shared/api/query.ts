import { apiGet, apiPost } from '@/shared/api/client';
import type { Page, QueryHistoryItem, QueryResponse } from '@/shared/lib/types';

export function submitQuery(naturalLanguageQuery: string): Promise<QueryResponse> {
  return apiPost('/v1/query', { naturalLanguageQuery });
}

export function getQueryHistory(page = 0, size = 20): Promise<Page<QueryHistoryItem>> {
  return apiGet(`/v1/query/history?page=${page}&size=${size}&sort=createdAt,desc`);
}
